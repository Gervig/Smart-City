package app.validators;

import app.dtos.CitizenRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Covers requirement(s): FR-08, FR-09
 */
class CitizenValidatorTest
{
    private static CitizenRequestDTO request(String firstName, String lastName, LocalDate birthDate,
                                             LocalDate deathDate, String status, Long housingId)
    {
        return new CitizenRequestDTO(firstName, lastName, birthDate, deathDate, status, null, 1L, housingId);
    }

    @Test
    @DisplayName("accepts a valid alive citizen")
    void validate()
    {
        // Arrange
        CitizenRequestDTO request = request("Alex", "Smith", LocalDate.now(), null, "alive", null);

        // Act / Assert
        assertDoesNotThrow(() -> CitizenValidator.validate(request));
    }

    @Test
    @DisplayName("accepts a dead citizen with a valid death date and no housing")
    void validateDeadCitizen()
    {
        // Arrange
        LocalDate date = LocalDate.now().minusYears(20);
        CitizenRequestDTO request = request("Alex", "Smith", date, date, "dead", null);

        // Act / Assert
        assertDoesNotThrow(() -> CitizenValidator.validate(request));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    @DisplayName("rejects a missing first name")
    void validateFirstName(String value)
    {
        // Arrange
        CitizenRequestDTO request = request(value, "Smith", LocalDate.now(), null, "alive", null);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> CitizenValidator.validate(request));

        // Assert
        assertTrue(exception.getMessage().startsWith("firstName"));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    @DisplayName("rejects a missing last name")
    void validateLastName(String value)
    {
        // Arrange
        CitizenRequestDTO request = request("Alex", value, LocalDate.now(), null, "alive", null);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> CitizenValidator.validate(request));

        // Assert
        assertTrue(exception.getMessage().startsWith("lastName"));
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> birthDates()
    {
        return Stream.of(
                arguments(LocalDate.now(), false),
                arguments(LocalDate.now().plusDays(1), true)
        );
    }

    @ParameterizedTest
    @MethodSource("birthDates")
    @DisplayName("allows today but rejects a future birth date")
    void validateBirthDate(LocalDate date, boolean invalid)
    {
        // Arrange
        CitizenRequestDTO request = request("Alex", "Smith", date, null, "alive", null);

        // Act
        if (invalid)
        {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> CitizenValidator.validate(request));
            assertTrue(exception.getMessage().startsWith("birthDate"));
        }
        else
        {
            assertDoesNotThrow(() -> CitizenValidator.validate(request));
        }
    }

    @Test
    @DisplayName("rejects a missing birth date")
    void validateMissingBirthDate()
    {
        // Arrange
        CitizenRequestDTO request = request("Alex", "Smith", null, null, "alive", null);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> CitizenValidator.validate(request));

        // Assert
        assertTrue(exception.getMessage().startsWith("birthDate"));
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> deathDates()
    {
        LocalDate birthDate = LocalDate.now().minusYears(20);
        return Stream.of(
                arguments(birthDate, false),
                arguments(birthDate.minusDays(1), true)
        );
    }

    @ParameterizedTest
    @MethodSource("deathDates")
    @DisplayName("allows death on birth date but rejects an earlier death date")
    void validateDeathDate(LocalDate date, boolean invalid)
    {
        // Arrange
        LocalDate birthDate = LocalDate.now().minusYears(20);
        CitizenRequestDTO request = request("Alex", "Smith", birthDate, date, "dead", null);

        // Act
        if (invalid)
        {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> CitizenValidator.validate(request));
            assertTrue(exception.getMessage().startsWith("deathDate"));
        }
        else
        {
            assertDoesNotThrow(() -> CitizenValidator.validate(request));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"alive", "dead"})
    @DisplayName("accepts allowed status values when dates agree")
    void validateStatus(String status)
    {
        // Arrange
        LocalDate date = LocalDate.now().minusYears(20);
        CitizenRequestDTO request = request("Alex", "Smith", date,
                "dead".equals(status) ? date : null, status, null);

        // Act / Assert
        assertDoesNotThrow(() -> CitizenValidator.validate(request));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"unknown", "", " "})
    @DisplayName("rejects an invalid status")
    void validateInvalidStatus(String status)
    {
        // Arrange
        CitizenRequestDTO request = request("Alex", "Smith", LocalDate.now(), null, status, null);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> CitizenValidator.validate(request));

        // Assert
        assertTrue(exception.getMessage().startsWith("status"));
    }

    @Test
    @DisplayName("requires a living citizen to have no death date")
    void validateAliveDeathDate()
    {
        // Arrange
        CitizenRequestDTO request = request("Alex", "Smith", LocalDate.now().minusYears(1),
                LocalDate.now(), "alive", null);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> CitizenValidator.validate(request));

        // Assert
        assertTrue(exception.getMessage().startsWith("deathDate"));
    }

    @Test
    @DisplayName("requires a dead citizen to have a death date")
    void validateDeadDeathDate()
    {
        // Arrange
        CitizenRequestDTO request = request("Alex", "Smith", LocalDate.now().minusYears(1),
                null, "dead", null);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> CitizenValidator.validate(request));

        // Assert
        assertTrue(exception.getMessage().startsWith("deathDate"));
    }

    @Test
    @DisplayName("rejects housing for a dead citizen")
    void validateDeadHousing()
    {
        // Arrange
        LocalDate date = LocalDate.now().minusYears(20);
        CitizenRequestDTO request = request("Alex", "Smith", date, date, "dead", 1L);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> CitizenValidator.validate(request));

        // Assert
        assertTrue(exception.getMessage().startsWith("housingId"));
    }

    @Test
    @DisplayName("accepts a death date after birth")
    void validateDeathDateAfterBirth()
    {
        // Arrange
        LocalDate birthDate = LocalDate.now().minusYears(20);
        CitizenRequestDTO request = request("Alex", "Smith", birthDate, birthDate.plusDays(1), "dead", null);

        // Act / Assert
        assertDoesNotThrow(() -> CitizenValidator.validate(request));
    }
}
