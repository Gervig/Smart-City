package app.validators;

import app.dtos.FamilyRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @ParameterizedTest
    @ValueSource(ints = {1, FamilyValidator.MAX_FAMILY_NAME_LENGTH})
    @DisplayName("[EP-69, BVA-59, BVA-60] accepts a family name of 1 and of 100 characters")
    void validateFamilyNameLength(int length)
    {
        // Arrange
        FamilyRequestDTO request = new FamilyRequestDTO("a".repeat(length));

        // Act / Assert
        assertDoesNotThrow(() -> FamilyValidator.validate(request));
    }

    @Test
    @DisplayName("[EP-70, BVA-61] rejects a family name of 101 characters")
    void validateFamilyNameTooLong()
    {
        // Arrange
        FamilyRequestDTO request = new FamilyRequestDTO("a".repeat(FamilyValidator.MAX_FAMILY_NAME_LENGTH + 1));

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> FamilyValidator.validate(request));

        // Assert
        assertEquals("familyName must be at most 100 characters", exception.getMessage());
    }
}
