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
    @DisplayName("populates the admin role")
    void populate()
    {
        // Arrange

        // Act
        List<Role> roles = RolePopulator.populate();

        // Assert
        assertEquals(1, roles.size());
        assertEquals("admin", roles.get(0).getName());
    }
}
