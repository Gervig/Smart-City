package app.validators;

import app.dtos.FamilyRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers requirement(s): Business rule, not yet in the SRS
 */
class FamilyValidatorTest
{
    @Test
    @DisplayName("accepts a family name")
    void validate()
    {
        // Arrange
        FamilyRequestDTO request = new FamilyRequestDTO("Smith");

        // Act / Assert
        assertDoesNotThrow(() -> FamilyValidator.validate(request));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "  "})
    @DisplayName("rejects a missing family name")
    void validateBlankFamilyName(String familyName)
    {
        // Arrange
        FamilyRequestDTO request = new FamilyRequestDTO(familyName);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> FamilyValidator.validate(request));

        // Assert
        assertTrue(exception.getMessage().startsWith("familyName"));
    }
}
