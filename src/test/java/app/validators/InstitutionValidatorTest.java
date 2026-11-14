package app.validators;

import app.dtos.InstitutionRequestDTO;
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
class InstitutionValidatorTest
{
    @Test
    @DisplayName("accepts an institution with supported type")
    void validate()
    {
        // Arrange
        InstitutionRequestDTO request = new InstitutionRequestDTO(1L, "School", "college");

        // Act / Assert
        assertDoesNotThrow(() -> InstitutionValidator.validate(request));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    @DisplayName("rejects a missing name")
    void validateName(String name)
    {
        // Arrange
        InstitutionRequestDTO request = new InstitutionRequestDTO(1L, name, "college");

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> InstitutionValidator.validate(request));

        // Assert
        assertTrue(exception.getMessage().startsWith("name"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"high_school", "college", "hospital"})
    @DisplayName("accepts supported institution types")
    void validateType(String type)
    {
        // Arrange
        InstitutionRequestDTO request = new InstitutionRequestDTO(1L, "School", type);

        // Act / Assert
        assertDoesNotThrow(() -> InstitutionValidator.validate(request));
    }

    @Test
    @DisplayName("rejects an unsupported type")
    void validateInvalidType()
    {
        // Arrange
        InstitutionRequestDTO request = new InstitutionRequestDTO(1L, "School", "university");

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> InstitutionValidator.validate(request));

        // Assert
        assertTrue(exception.getMessage().startsWith("type"));
    }
}
