package app.populators;

import app.security.entities.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers requirement(s): Business rule, not yet in the SRS
 */
class UserPopulatorTest
{
    @Test
    @DisplayName("populates the two default users")
    void populate()
    {
        // Arrange

        // Act
        List<User> users = UserPopulator.populate();

        // Assert
        assertEquals(2, users.size());
        assertEquals("User1", users.get(0).getUsername());
        assertTrue(users.get(0).verifyPassword("Password123"));
        assertEquals("User2", users.get(1).getUsername());
        assertTrue(users.get(1).verifyPassword("Password456"));
    }
}
