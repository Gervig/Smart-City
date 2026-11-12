package app.security.daos;

import app.security.entities.Role;
import app.security.entities.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure unit tests for the User entity: no database, no Docker, runs in milliseconds with `mvn test`.
 */
class UserTest
{
    // ---------- verifyPassword ----------

    // Equivalence partitions: the exact password (valid) versus near misses and empty (invalid)
    @ParameterizedTest(name = "stored Password123, attempt \"{0}\" -> {1}")
    @CsvSource({
            "Password123,   true",   // exact match
            "password123,   false", // wrong capitalisation
            "Password1234,  false", // one character too long
            "Password12,    false", // one character too short
            "'',            false"  // empty
    })
    void verifyPassword(String attempt, boolean expected)
    {
        // Arrange
        User user = new User("alice", "Password123");

        // Act
        boolean actual = user.verifyPassword(attempt);

        // Assert
        assertEquals(expected, actual);
    }

    // ---------- addRole / getRolesAsStrings ----------

    @Test
    @DisplayName("links user and role on both sides")
    void addRole()
    {
        // Arrange
        User user = new User("alice", "Password123");
        Role admin = new Role("admin");

        // Act
        user.addRole(admin);

        // Assert
        assertTrue(user.getRoles().contains(admin));
        assertTrue(admin.getUsers().contains(user));
    }

    @Test
    @DisplayName("returns the names of all roles")
    void getRolesAsStrings()
    {
        // Arrange
        User user = new User("alice", "Password123");
        user.addRole(new Role("user"));
        user.addRole(new Role("admin"));

        // Act
        Set<String> names = user.getRolesAsStrings();

        // Assert
        assertEquals(Set.of("user", "admin"), names);
    }

    // ---------- removeRole ----------

    @Test
    @DisplayName("removes the only role")
    void removeRole()
    {
        // Arrange
        User user = new User("alice", "Password123");
        user.addRole(new Role("user"));

        // Act
        user.removeRole("user");

        // Assert
        assertTrue(user.getRoles().isEmpty());
    }

    @Test
    @DisplayName("unknown role changes nothing")
    void removeRoleUnknownRole()
    {
        // Arrange
        User user = new User("alice", "Password123");
        user.addRole(new Role("user"));

        // Act
        user.removeRole("admin");

        // Assert
        assertEquals(Set.of("user"), user.getRolesAsStrings());
    }

    // FAILS until User.removeRole is fixed (see the notes that came with this file).
    // removeRole removes from the set while looping over it, which throws a ConcurrentModificationException
    // unless the removed role happens to be the last one visited. Removing the role that the loop visits
    // FIRST makes the failure repeatable instead of depending on hash order.
    @Test
    @DisplayName("removes only that role when the user has two")
    void removeRoleOneOfTwoRoles()
    {
        // Arrange
        User user = new User("alice", "Password123");
        user.addRole(new Role("user"));
        user.addRole(new Role("admin"));
        String firstVisited = user.getRoles().iterator().next().getName();

        // Act
        user.removeRole(firstVisited);

        // Assert
        assertEquals(1, user.getRoles().size());
        assertFalse(user.getRolesAsStrings().contains(firstVisited));
    }
}
