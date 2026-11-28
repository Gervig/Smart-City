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
    @DisplayName("[CN-V1, CB-V1, CD-V3, CS-V1] accepts a valid alive citizen")
    void validate()
    {
        // Arrange
        CitizenRequestDTO request = request("Alex", "Smith", LocalDate.now(), null, "alive", null);

        // Act / Assert
        assertDoesNotThrow(() -> CitizenValidator.validate(request));
    }

    @Test
    @DisplayName("[CD-V1, CS-V2] accepts a deceased citizen with a valid death date and no housing")
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
    @DisplayName("[CN-I1, CN-I2, CN-I3] rejects a missing first name")
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
    @DisplayName("[CN-I1, CN-I2, CN-I3] rejects a missing last name")
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
    @DisplayName("[CN-V3, CN-V4] accepts first and last names of 1 and of 100 characters")
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
    @DisplayName("[CN-I4] rejects a first or last name of 101 characters")
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
    @DisplayName("[CB-V1, CB-I1] allows today but rejects a future birth date")
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
    @DisplayName("[CB-I2] rejects a missing birth date")
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
    @DisplayName("[CD-V1, CD-I1] validates death date against birth date")
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
    @DisplayName("[CB-V2, CS-V1, CS-V2] accepts allowed status values when dates agree")
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
    @DisplayName("[CS-I1, CS-I2] rejects an invalid status")
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
    @DisplayName("[CS-I5] requires a living citizen to have no death date")
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
    @DisplayName("[CS-I6] requires a deceased citizen to have a death date")
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
    @DisplayName("[CS-I7] rejects housing for a deceased citizen")
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
    @DisplayName("[CD-V2] accepts a death date after birth")
    void validateDeathDateAfterBirth()
    {
        // Arrange
        LocalDate birthDate = LocalDate.now().minusYears(20);
        CitizenRequestDTO request = request("Alex", "Smith", birthDate, birthDate.plusDays(1), "deceased", null);

        // Act / Assert
        assertDoesNotThrow(() -> CitizenValidator.validate(request));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Søren", "Anne-Marie", "O'Brien", "Jean Luc"})
    @DisplayName("[CN-V2] accepts names with Danish letters, hyphens, apostrophes and spaces")
    void validNameCharacters(String name)
    {
        // Arrange
        CitizenRequestDTO request = request(name, name, LocalDate.now(), null, "alive", null);

        // Act / Assert
        assertDoesNotThrow(() -> CitizenValidator.validate(request));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Anna\n", "An\tna", "Anna\0", "Anna\u007F"})
    @DisplayName("[CN-I5] rejects a first name with a control character")
    void nameWithControlCharacter(String value)
    {
        // Arrange
        CitizenRequestDTO request = request(value, "Smith", LocalDate.now(), null, "alive", null);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> CitizenValidator.validate(request));

        // Assert
        assertEquals("firstName must not contain control characters", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Alive", "ALIVE", "alive ", " deceased"})
    @DisplayName("[CS-I3, CS-I4] rejects a status in the wrong case or with surrounding spaces")
    void statusWrongCaseOrSpaces(String value)
    {
        // Arrange
        CitizenRequestDTO request = request("Alex", "Smith", LocalDate.now(), null, value, null);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> CitizenValidator.validate(request));

        // Assert
        assertEquals("status must be one of alive, deceased", exception.getMessage());
    }
}
