package app.validators;

import app.dtos.CompanyRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
    @DisplayName("accepts a valid active company")
    void validate()
    {
        // Arrange
        CompanyRequestDTO request = request("Acme", LocalDate.now(), null, "active");

        // Act / Assert
        assertDoesNotThrow(() -> CompanyValidator.validate(request));
    }

    @Test
    @DisplayName("rejects a blank name")
    void validateName()
    {
        // Arrange
        CompanyRequestDTO request = request(" ", LocalDate.now(), null, "active");

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> CompanyValidator.validate(request));

        // Assert
        assertTrue(exception.getMessage().startsWith("name"));
    }

    @Test
    @DisplayName("rejects a missing founded date")
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
    @DisplayName("rejects a dissolved date before founding")
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
    @DisplayName("accepts supported company statuses with matching dates")
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
    @DisplayName("rejects an unsupported status")
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

    @Test
    @DisplayName("requires active companies to have no dissolved date")
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
    @DisplayName("requires dissolved companies to have a dissolved date")
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
}
