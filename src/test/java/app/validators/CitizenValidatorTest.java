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
import static org.junit.jupiter.api.Assertions.assertEquals;
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
    @DisplayName("[EP-01, EP-06] accepts a valid alive citizen")
    void validate()
    {
        // Arrange
        CitizenRequestDTO request = request("Alex", "Smith", LocalDate.now(), null, "alive", null);

        // Act / Assert
        assertDoesNotThrow(() -> CitizenValidator.validate(request));
    }

    @Test
    @DisplayName("[EP-04, EP-06] accepts a deceased citizen with a valid death date and no housing")
    void validateDeadCitizen()
    {
        // Arrange
        LocalDate date = LocalDate.now().minusYears(20);
        CitizenRequestDTO request = request("Alex", "Smith", date, date, "deceased", null);

        // Act / Assert
        assertDoesNotThrow(() -> CitizenValidator.validate(request));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    @DisplayName("[EP-12] rejects a missing first name")
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
    @DisplayName("[EP-12] rejects a missing last name")
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

    @ParameterizedTest
    @ValueSource(ints = {1, CitizenValidator.MAX_NAME_LENGTH})
    @DisplayName("[EP-11, BVA-06, BVA-07] accepts first and last names of 1 and of 100 characters")
    void validateNameLength(int length)
    {
        // Arrange
        String name = "a".repeat(length);
        CitizenRequestDTO request = request(name, name, LocalDate.now(), null, "alive", null);

        // Act / Assert
        assertDoesNotThrow(() -> CitizenValidator.validate(request));
    }

    @ParameterizedTest
    @ValueSource(strings = {"firstName", "lastName"})
    @DisplayName("[EP-13, BVA-08] rejects a first or last name of 101 characters")
    void validateNameTooLong(String field)
    {
        // Arrange
        String tooLong = "a".repeat(CitizenValidator.MAX_NAME_LENGTH + 1);
        CitizenRequestDTO request = "firstName".equals(field)
                ? request(tooLong, "Smith", LocalDate.now(), null, "alive", null)
                : request("Alex", tooLong, LocalDate.now(), null, "alive", null);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> CitizenValidator.validate(request));

        // Assert
        assertEquals(field + " must be at most 100 characters", exception.getMessage());
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
    @DisplayName("[EP-01, EP-02, BVA-01, BVA-02] allows today but rejects a future birth date")
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
    @DisplayName("[EP-03] rejects a missing birth date")
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
    @DisplayName("[EP-04, EP-05, BVA-03, BVA-04] validates death date against birth date")
    void validateDeathDate(LocalDate date, boolean invalid) {
        // Arrange
        LocalDate birthDate = LocalDate.now().minusYears(20);
        CitizenRequestDTO request =
                request("Alex", "Smith", birthDate, date, "deceased", null);

        // Act & Assert
        if (invalid) {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> CitizenValidator.validate(request));
            assertEquals("deathDate must not be before birthDate", exception.getMessage());
        } else {
            assertDoesNotThrow(() -> CitizenValidator.validate(request));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"alive", "deceased"})
    @DisplayName("[EP-06] accepts allowed status values when dates agree")
    void validateStatus(String status)
    {
        // Arrange
        LocalDate date = LocalDate.now().minusYears(20);
        CitizenRequestDTO request = request("Alex", "Smith", date,
                "deceased".equals(status) ? date : null, status, null);

        // Act / Assert
        assertDoesNotThrow(() -> CitizenValidator.validate(request));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"unknown", "", " "})
    @DisplayName("[EP-07] rejects an invalid status")
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
    @DisplayName("[EP-08] requires a living citizen to have no death date")
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
    @DisplayName("[EP-09] requires a deceased citizen to have a death date")
    void validateDeadDeathDate()
    {
        // Arrange
        CitizenRequestDTO request = request("Alex", "Smith", LocalDate.now().minusYears(1),
                null, "deceased", null);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> CitizenValidator.validate(request));

        // Assert
        assertTrue(exception.getMessage().startsWith("deathDate"));
    }

    @Test
    @DisplayName("[EP-10] rejects housing for a deceased citizen")
    void validateDeadHousing()
    {
        // Arrange
        LocalDate date = LocalDate.now().minusYears(20);
        CitizenRequestDTO request = request("Alex", "Smith", date, date, "deceased", 1L);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> CitizenValidator.validate(request));

        // Assert
        assertTrue(exception.getMessage().startsWith("housingId"));
    }

    @Test
    @DisplayName("[EP-04, BVA-05] accepts a death date after birth")
    void validateDeathDateAfterBirth()
    {
        // Arrange
        LocalDate birthDate = LocalDate.now().minusYears(20);
        CitizenRequestDTO request = request("Alex", "Smith", birthDate, birthDate.plusDays(1), "deceased", null);

        // Act / Assert
        assertDoesNotThrow(() -> CitizenValidator.validate(request));
    }
}
