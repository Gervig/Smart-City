package app.populators;

import app.security.entities.Role;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Covers requirement(s): Business rule, not yet in the SRS
 */
class RolePopulatorTest
{
    @Test
    @DisplayName("populates the user and admin roles")
    void populate()
    {
        // Arrange

        // Act
        List<Role> roles = RolePopulator.populate();

        // Assert
        assertEquals(2, roles.size());
        assertEquals("user", roles.get(0).getName());
        assertEquals("admin", roles.get(1).getName());
    }
}
