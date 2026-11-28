package app.validators;

import app.dtos.InstitutionRequestDTO;
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
class InstitutionValidatorTest
{
    @Test
    @DisplayName("[IN-V1, IT-V2] accepts an institution with supported type")
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
    @DisplayName("[IN-I1] rejects a missing name")
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
    @ValueSource(ints = {1, InstitutionValidator.MAX_NAME_LENGTH})
    @DisplayName("[IN-V2, IN-V3] accepts a name of 1 and of 150 characters")
    void validateNameLength(int length)
    {
        // Arrange
        InstitutionRequestDTO request = new InstitutionRequestDTO(1L, "a".repeat(length), "college");

        // Act / Assert
        assertDoesNotThrow(() -> InstitutionValidator.validate(request));
    }

    @Test
    @DisplayName("[IN-I2] rejects a name of 151 characters")
    void validateNameTooLong()
    {
        // Arrange
        InstitutionRequestDTO request =
                new InstitutionRequestDTO(1L, "a".repeat(InstitutionValidator.MAX_NAME_LENGTH + 1), "college");

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> InstitutionValidator.validate(request));

        // Assert
        assertEquals("name must be at most 150 characters", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"high_school", "college", "hospital"})
    @DisplayName("[IT-V1, IT-V2, IT-V3] accepts supported institution types")
    void validateType(String type)
    {
        // Arrange
        InstitutionRequestDTO request = new InstitutionRequestDTO(1L, "School", type);

        // Act / Assert
        assertDoesNotThrow(() -> InstitutionValidator.validate(request));
    }

    @Test
    @DisplayName("[IT-I1] rejects an unsupported type")
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

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    @DisplayName("[IT-I2] rejects a missing institution type")
    void validateMissingType(String type)
    {
        // Arrange
        InstitutionRequestDTO request = new InstitutionRequestDTO(1L, "School", type);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> InstitutionValidator.validate(request));

        // Assert
        assertTrue(exception.getMessage().startsWith("type"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Anna\n", "An\tna", "Anna\0", "Anna\u007F"})
    @DisplayName("[IN-I3] rejects a name with a control character")
    void nameWithControlCharacter(String value)
    {
        // Arrange
        InstitutionRequestDTO request = new InstitutionRequestDTO(1L, value, "college");

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> InstitutionValidator.validate(request));

        // Assert
        assertEquals("name must not contain control characters", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"College", "high school", "high-school", "hospital "})
    @DisplayName("[IT-I3] rejects a type in the wrong case, spelling or with surrounding spaces")
    void typeWrongCaseOrSpelling(String value)
    {
        // Arrange
        InstitutionRequestDTO request = new InstitutionRequestDTO(1L, "School", value);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> InstitutionValidator.validate(request));

        // Assert
        assertEquals("type must be one of high_school, college, hospital", exception.getMessage());
    }
}
