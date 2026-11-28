package app.validators;

import app.dtos.LocationRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Covers requirement(s): Business rule, not yet in the SRS
 */
class LocationValidatorTest
{
    @Test
    @DisplayName("accepts a location with a name")
    void validate()
    {
        // Arrange
        LocationRequestDTO request = new LocationRequestDTO("Central", null);

        // Act / Assert
        assertDoesNotThrow(() -> LocationValidator.validate(request));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "  "})
    @DisplayName("rejects a missing name")
    void validateBlankName(String name)
    {
        // Arrange
        LocationRequestDTO request = new LocationRequestDTO(name, null);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> LocationValidator.validate(request));

        // Assert
        assertTrue(exception.getMessage().startsWith("name"));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, LocationValidator.MAX_NAME_LENGTH})
    @DisplayName("[EP-69, BVA-56, BVA-57] accepts a name of 1 and of 150 characters")
    void validateNameLength(int length)
    {
        // Arrange
        LocationRequestDTO request = new LocationRequestDTO("a".repeat(length), null);

        // Act / Assert
        assertDoesNotThrow(() -> LocationValidator.validate(request));
    }

    @Test
    @DisplayName("[EP-70, BVA-58] rejects a name of 151 characters")
    void validateNameTooLong()
    {
        // Arrange
        LocationRequestDTO request = new LocationRequestDTO("a".repeat(LocationValidator.MAX_NAME_LENGTH + 1), null);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> LocationValidator.validate(request));

        // Assert
        assertEquals("name must be at most 150 characters", exception.getMessage());
    }
}
