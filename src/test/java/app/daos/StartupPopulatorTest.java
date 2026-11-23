package app.daos;

import app.config.HibernateConfig;
import app.populators.StartupPopulator;
import app.security.entities.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers requirement(s): FR-01 (new accounts get the role user), FR-05 (an admin account exists)
 */
class StartupPopulatorTest
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
    @DisplayName("fills an empty database with the roles, the admin and the test users")
    void populateEmptyDatabase()
    {
        // Arrange

        // Act
        StartupPopulator.populate(emf, "AdminPassword123");

        // Assert
        assertEquals(2L, count("Role"));
        assertEquals(3L, count("User"));
        User admin = find("admin");
        assertEquals(Set.of("admin"), admin.getRolesAsStrings());
        assertTrue(admin.verifyPassword("AdminPassword123"));
        assertEquals(Set.of("user"), find("User1").getRolesAsStrings());
        assertTrue(find("User1").verifyPassword("Password123"));
        assertEquals(Set.of("user"), find("User2").getRolesAsStrings());
    }

    @Test
    @DisplayName("a second start adds nothing and keeps the passwords")
    void populateTwice()
    {
        // Arrange
        StartupPopulator.populate(emf, "AdminPassword123");

        // Act
        StartupPopulator.populate(emf, "OtherPassword456");

        // Assert
        assertEquals(2L, count("Role"));
        assertEquals(3L, count("User"));
        assertTrue(find("admin").verifyPassword("AdminPassword123"));
        assertEquals(1, find("User1").getRolesAsStrings().size());
    }

    @Test
    @DisplayName("without an admin password the roles and test users are still created")
    void populateWithoutAdminPassword()
    {
        // Arrange

        // Act
        StartupPopulator.populate(emf, null);

        // Assert
        assertEquals(2L, count("Role"));
        assertEquals(2L, count("User"));
        assertNull(find("admin"));
    }

    private long count(String entity)
    {
        try (EntityManager em = emf.createEntityManager())
        {
            return em.createQuery("SELECT COUNT(e) FROM " + entity + " e", Long.class).getSingleResult();
        }
    }

    private User find(String username)
    {
        try (EntityManager em = emf.createEntityManager())
        {
            User user = em.find(User.class, username);
            if (user != null)
            {
                user.getRoles().size(); // load the roles while the EntityManager is open
            }
            return user;
        }
    }
}
