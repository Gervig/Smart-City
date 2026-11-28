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
    @DisplayName("accepts a valid active company")
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
    @DisplayName("rejects a blank name")
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
    @DisplayName("[EP-69, BVA-56, BVA-57] accepts a name of 1 and of 150 characters")
    void validateNameLength(int length)
    {
        // Arrange
        CompanyRequestDTO request = request("a".repeat(length), LocalDate.now(), null, "active");

        // Act / Assert
        assertDoesNotThrow(() -> CompanyValidator.validate(request));
    }

    @Test
    @DisplayName("[EP-70, BVA-58] rejects a name of 151 characters")
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

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    @DisplayName("rejects a missing status")
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
