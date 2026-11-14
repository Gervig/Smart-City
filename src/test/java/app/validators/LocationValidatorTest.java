package app.validators;

import app.dtos.LocationRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
}
