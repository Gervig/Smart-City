package app.daos;

import app.config.HibernateConfig;
import app.populators.AdminPopulator;
import app.security.entities.Role;
import app.security.entities.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers requirement(s): Business rule, not yet in the SRS
 */
class AdminPopulatorTest
{
    private static EntityManagerFactory emf;

    @BeforeAll
    static void initDatabase()
    {
        emf = HibernateConfig.getEntityManagerFactoryForTest();
    }

    @BeforeEach
    void clearDatabase()
    {
        try (EntityManager em = emf.createEntityManager())
        {
            em.getTransaction().begin();
            em.createNativeQuery("TRUNCATE TABLE user_role, users, roles CASCADE").executeUpdate();
            em.getTransaction().commit();
        }
    }

    @Test
    @DisplayName("does nothing when the password is null")
    void populateNullPassword()
    {
        // Arrange

        // Act
        AdminPopulator.populate(emf, null);

        // Assert
        try (EntityManager em = emf.createEntityManager())
        {
            assertEquals(0L, em.createQuery("SELECT COUNT(u) FROM User u", Long.class).getSingleResult());
            assertEquals(0L, em.createQuery("SELECT COUNT(r) FROM Role r", Long.class).getSingleResult());
        }
    }

    @Test
    @DisplayName("rejects a blank password without creating data")
    void populateBlankPassword()
    {
        // Arrange / Act
        assertThrows(IllegalArgumentException.class, () -> AdminPopulator.populate(emf, "  "));

        // Assert
        try (EntityManager em = emf.createEntityManager())
        {
            assertEquals(0L, em.createQuery("SELECT COUNT(u) FROM User u", Long.class).getSingleResult());
            assertEquals(0L, em.createQuery("SELECT COUNT(r) FROM Role r", Long.class).getSingleResult());
        }
    }

    @Test
    @DisplayName("creates the admin with the admin role and a hashed password")
    void populateValidPassword()
    {
        // Arrange
        String password = "AdminPassword123";

        // Act
        AdminPopulator.populate(emf, password);

        // Assert
        User admin = loadAdmin();
        assertEquals("admin", admin.getUsername());
        assertEquals(Set.of("admin"), admin.getRolesAsStrings());
        String storedPassword = storedPassword();
        assertNotEquals(password, storedPassword);
        assertTrue(BCrypt.checkpw(password, storedPassword));
    }

    @Test
    @DisplayName("can run repeatedly without creating duplicate admins")
    void populateTwice()
    {
        // Arrange
        String password = "AdminPassword123";

        // Act
        AdminPopulator.populate(emf, password);
        AdminPopulator.populate(emf, password);

        // Assert
        try (EntityManager em = emf.createEntityManager())
        {
            assertEquals(1L, em.createQuery("SELECT COUNT(u) FROM User u WHERE u.username = 'admin'", Long.class)
                    .getSingleResult());
            assertEquals(1L, em.createQuery("SELECT COUNT(r) FROM Role r WHERE r.name = 'admin'", Long.class)
                    .getSingleResult());
        }
        assertEquals(Set.of("admin"), loadAdmin().getRolesAsStrings());
    }

    @Test
    @DisplayName("adds the admin role to an existing admin user")
    void populateExistingAdminWithoutRole()
    {
        // Arrange
        try (EntityManager em = emf.createEntityManager())
        {
            em.getTransaction().begin();
            em.persist(new Role("admin"));
            em.persist(new User("admin", "ExistingPassword123"));
            em.getTransaction().commit();
        }

        // Act
        AdminPopulator.populate(emf, "AdminPassword123");

        // Assert
        User admin = loadAdmin();
        assertEquals(Set.of("admin"), admin.getRolesAsStrings());
    }

    private User loadAdmin()
    {
        try (EntityManager em = emf.createEntityManager())
        {
            return em.createQuery("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.username = :name", User.class)
                    .setParameter("name", "admin")
                    .getSingleResult();
        }
    }

    private String storedPassword()
    {
        try (EntityManager em = emf.createEntityManager())
        {
            return (String) em.createNativeQuery("SELECT password FROM users WHERE username = 'admin'")
                    .getSingleResult();
        }
    }
}
