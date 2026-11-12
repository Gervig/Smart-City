package app.security.daos;

import app.config.HibernateConfig;
import app.exceptions.ApiException;
import app.exceptions.ValidationException;
import app.security.entities.Role;
import app.security.entities.User;
import dk.bugelhartmann.UserDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test: UserDAO against a real PostgreSQL started by Testcontainers (Docker must be running).
 * Maven runs the *DAOTest classes in the verify phase (see the pom), so `mvn test` stays fast and needs no Docker.
 * <p>
 * Every test starts from the same known data (see seed()), so the tests do not depend on each other
 * or on the order they run in:
 * alice / Password123 / roles: user
 * bob   / Password456 / roles: user, admin
 */
class UserDAOTest
{
    private static EntityManagerFactory emf;
    private static UserDAO userDAO;

    @BeforeAll
    static void initDatabase()
    {
        emf = HibernateConfig.getEntityManagerFactoryForTest();
        userDAO = UserDAO.getInstance(emf);
    }

    // Runs BEFORE each test (not after), so a failed test can never leave dirty data for the next one
    @BeforeEach
    void seed()
    {
        try (EntityManager em = emf.createEntityManager())
        {
            em.getTransaction().begin();

            // wipes all three tables, including the user_role join table
            em.createNativeQuery("TRUNCATE TABLE user_role, users, roles CASCADE").executeUpdate();

            Role userRole = new Role("user");
            Role adminRole = new Role("admin");
            em.persist(userRole);
            em.persist(adminRole);

            User alice = new User("alice", "Password123");
            alice.addRole(userRole);

            User bob = new User("bob", "Password456");
            bob.addRole(userRole);
            bob.addRole(adminRole);

            em.persist(alice);
            em.persist(bob);

            em.getTransaction().commit();
        }
    }

    // ---------- read ----------

    @Test
    @DisplayName("returns the user with that username")
    void read()
    {
        // Act
        User actual = userDAO.read("alice");

        // Assert
        assertEquals("alice", actual.getUsername());
    }

    @Test
    @DisplayName("unknown username throws ApiException")
    void readUnknownUser()
    {
        assertThrows(ApiException.class, () -> userDAO.read("nobody"));
    }

    @Test
    @DisplayName("returns all users")
    void readAll()
    {
        // Act
        List<User> users = userDAO.readAll();

        // Assert
        Set<String> usernames = users.stream().map(User::getUsername).collect(Collectors.toSet());
        assertEquals(Set.of("alice", "bob"), usernames);
    }

    // ---------- create ----------
    // Each test reads the result back from the database. Checking the object that create() returns
    // proves nothing, because create() returns the same object it was given.

    @Test
    @DisplayName("user without roles gets the default user role")
    void create()
    {
        // Arrange
        User carol = new User("carol", "Password789");

        // Act
        userDAO.create(carol);

        // Assert
        assertEquals(Set.of("user"), rolesOf("carol"));
    }

    @Test
    @DisplayName("user with an existing role is saved with that role")
    void createWithExistingRole()
    {
        // Arrange
        User dave = new User("dave", "Password789");
        dave.addRole(new Role("admin"));

        // Act
        userDAO.create(dave);

        // Assert
        assertEquals(Set.of("admin"), rolesOf("dave"));
    }

    @Test
    @DisplayName("user with an unknown role is not saved")
    void createWithUnknownRole()
    {
        // Arrange
        User eve = new User("eve", "Password789");
        eve.addRole(new Role("ghost")); // this role does not exist in the database

        // Act
        userDAO.create(eve);

        // Assert
        // NOTE: create() currently swallows the exception (catch Throwable + printStackTrace),
        // so the caller is never told that saving failed. If you change create() to let the
        // exception through, replace the act step with assertThrows(...).
        assertEquals(2, userDAO.readAll().size());
        assertThrows(ApiException.class, () -> userDAO.read("eve"));
    }

    // ---------- getVerifiedUser ----------

    @Test
    @DisplayName("correct password returns a DTO with username and roles")
    void getVerifiedUser() throws ValidationException
    {
        // Act
        UserDTO dto = userDAO.getVerifiedUser("bob", "Password456");

        // Assert
        assertEquals("bob", dto.getUsername());
        assertEquals(Set.of("user", "admin"), dto.getRoles());
    }

    @ParameterizedTest(name = "{0} with password \"{1}\" is rejected")
    @CsvSource({
            "alice, wrongPassword",
            "alice, password123",   // right password, wrong capitalisation
            "bob,   Password123"    // another user's password
    })
    void getVerifiedUserWrongPassword(String username, String wrongPassword)
    {
        assertThrows(ValidationException.class, () -> userDAO.getVerifiedUser(username, wrongPassword));
    }

    @Test
    @DisplayName("unknown user throws EntityNotFoundException")
    void getVerifiedUserUnknownUser()
    {
        assertThrows(EntityNotFoundException.class, () -> userDAO.getVerifiedUser("nobody", "Password123"));
    }

    // ---------- update ----------

    @Test
    @DisplayName("adding a role saves the new role")
    void update()
    {
        // Arrange
        User alice = loadUserWithRoles("alice");
        alice.addRole(new Role("admin"));

        // Act
        userDAO.update(alice);

        // Assert
        assertEquals(Set.of("user", "admin"), rolesOf("alice"));
    }

    // ---------- delete ----------

    @Test
    @DisplayName("removes only that user")
    void delete()
    {
        // Act
        userDAO.delete("alice");

        // Assert
        List<User> remaining = userDAO.readAll();
        assertEquals(1, remaining.size());
        assertEquals("bob", remaining.get(0).getUsername());
        assertEquals(Set.of("user", "admin"), rolesOf("bob")); // bob and the roles are untouched
    }

    @Test
    @DisplayName("unknown username throws ApiException")
    void deleteUnknownUser()
    {
        assertThrows(ApiException.class, () -> userDAO.delete("nobody"));
    }

    // ---------- helpers: look at the database directly, without using the DAO under test ----------

    private static Set<String> rolesOf(String username)
    {
        return loadUserWithRoles(username).getRolesAsStrings();
    }

    // LEFT JOIN FETCH loads the lazy roles while the EntityManager is still open
    private static User loadUserWithRoles(String username)
    {
        try (EntityManager em = emf.createEntityManager())
        {
            return em.createQuery(
                            "SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.username = :name", User.class)
                    .setParameter("name", username)
                    .getSingleResult();
        }
    }
}
