package app.security.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Covers requirement(s): Business rule, not yet in the SRS
 */
class RoleTest
{
    @Test
    @DisplayName("declares roles in the expected order")
    void values()
    {
        // Arrange
        Role[] expected = {Role.ANYONE, Role.USER, Role.ADMIN};

        // Act
        Role[] actual = Role.values();

        // Assert
        assertArrayEquals(expected, actual);
    }

    @ParameterizedTest
    @EnumSource(Role.class)
    @DisplayName("looks up each role by name")
    void valueOf(Role role)
    {
        // Arrange
        String name = role.name();

        // Act
        Role actual = Role.valueOf(name);

        // Assert
        assertEquals(role, actual);
    }

    @Test
    @DisplayName("rejects an unknown role name")
    void valueOfUnknown()
    {
        // Arrange / Act / Assert
        assertThrows(IllegalArgumentException.class, () -> Role.valueOf("UNKNOWN"));
    }
}
