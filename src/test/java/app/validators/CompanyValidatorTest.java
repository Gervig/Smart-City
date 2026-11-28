package app.validators;

import app.dtos.CompanyRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers requirement(s): Business rule, not yet in the SRS
 */
class CompanyValidatorTest
{
    private static CompanyRequestDTO request(String name, LocalDate founded, LocalDate dissolved, String status)
    {
        return new CompanyRequestDTO(1L, name, founded, dissolved, status);
    }

    @Test
    @DisplayName("[CON-V1, COS-V1] accepts a valid active company")
    void validate()
    {
        // Arrange
        CompanyRequestDTO request = request("Acme", LocalDate.now(), null, "active");

        // Act / Assert
        assertDoesNotThrow(() -> CompanyValidator.validate(request));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    @DisplayName("[CON-I1] rejects a blank name")
    void validateName(String name)
    {
        // Arrange
        CompanyRequestDTO request = request(name, LocalDate.now(), null, "active");

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> CompanyValidator.validate(request));

        // Assert
        assertTrue(exception.getMessage().startsWith("name"));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, CompanyValidator.MAX_NAME_LENGTH})
    @DisplayName("[CON-V2, CON-V3] accepts a name of 1 and of 150 characters")
    void validateNameLength(int length)
    {
        // Arrange
        CompanyRequestDTO request = request("a".repeat(length), LocalDate.now(), null, "active");

        // Act / Assert
        assertDoesNotThrow(() -> CompanyValidator.validate(request));
    }

    @Test
    @DisplayName("[CON-I2] rejects a name of 151 characters")
    void validateNameTooLong()
    {
        // Arrange
        CompanyRequestDTO request =
                request("a".repeat(CompanyValidator.MAX_NAME_LENGTH + 1), LocalDate.now(), null, "active");

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> CompanyValidator.validate(request));

        // Assert
        assertEquals("name must be at most 150 characters", exception.getMessage());
    }

    @Test
    @DisplayName("[COD-I1] rejects a missing founded date")
    void validateFoundedDate()
    {
        // Arrange
        CompanyRequestDTO request = request("Acme", null, null, "active");

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> CompanyValidator.validate(request));

        // Assert
        assertTrue(exception.getMessage().startsWith("foundedDate"));
    }

    @Test
    @DisplayName("[COD-I2] rejects a dissolved date before founding")
    void validateDissolvedDate()
    {
        // Arrange
        LocalDate founded = LocalDate.now();
        CompanyRequestDTO request = request("Acme", founded, founded.minusDays(1), "dissolved");

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> CompanyValidator.validate(request));

        // Assert
        assertTrue(exception.getMessage().startsWith("dissolvedDate"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"active", "dissolved"})
    @DisplayName("[COD-V1, COS-V1, COS-V2] accepts supported company statuses with matching dates")
    void validateStatus(String status)
    {
        // Arrange
        LocalDate founded = LocalDate.now().minusYears(1);
        CompanyRequestDTO request = request("Acme", founded,
                "dissolved".equals(status) ? founded : null, status);

        // Act / Assert
        assertDoesNotThrow(() -> CompanyValidator.validate(request));
    }

    @Test
    @DisplayName("[COS-I1] rejects an unsupported status")
    void validateInvalidStatus()
    {
        // Arrange
        CompanyRequestDTO request = request("Acme", LocalDate.now(), null, "closed");

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> CompanyValidator.validate(request));

        // Assert
        assertTrue(exception.getMessage().startsWith("status"));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    @DisplayName("[COS-I2] rejects a missing status")
    void validateMissingStatus(String status)
    {
        // Arrange
        CompanyRequestDTO request = request("Acme", LocalDate.now(), null, status);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> CompanyValidator.validate(request));

        // Assert
        assertTrue(exception.getMessage().startsWith("status"));
    }

    @Test
    @DisplayName("[COS-I4] requires active companies to have no dissolved date")
    void validateActiveDates()
    {
        // Arrange
        LocalDate date = LocalDate.now();
        CompanyRequestDTO request = request("Acme", date, date, "active");

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> CompanyValidator.validate(request));

        // Assert
        assertTrue(exception.getMessage().startsWith("dissolvedDate"));
    }

    @Test
    @DisplayName("[COS-I5] requires dissolved companies to have a dissolved date")
    void validateDissolvedDates()
    {
        // Arrange
        CompanyRequestDTO request = request("Acme", LocalDate.now(), null, "dissolved");

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> CompanyValidator.validate(request));

        // Assert
        assertTrue(exception.getMessage().startsWith("dissolvedDate"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Anna\n", "An\tna", "Anna\0", "Anna\u007F"})
    @DisplayName("[CON-I3] rejects a name with a control character")
    void nameWithControlCharacter(String value)
    {
        // Arrange
        CompanyRequestDTO request = request(value, LocalDate.now(), null, "active");

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> CompanyValidator.validate(request));

        // Assert
        assertEquals("name must not contain control characters", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Active", "DISSOLVED", "active "})
    @DisplayName("[COS-I3] rejects a status in the wrong case or with surrounding spaces")
    void statusWrongCaseOrSpaces(String value)
    {
        // Arrange
        CompanyRequestDTO request = request("Acme", LocalDate.now(), null, value);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> CompanyValidator.validate(request));

        // Assert
        assertEquals("status must be one of active, dissolved", exception.getMessage());
    }
}
