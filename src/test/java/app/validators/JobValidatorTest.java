package app.validators;

import app.dtos.JobRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Covers requirement(s): FR-16
 */
class JobValidatorTest
{
    private static JobRequestDTO request(Long companyId, Long institutionId, String title, BigDecimal salary,
                                         LocalDate start, LocalDate end, String status)
    {
        return new JobRequestDTO(1L, companyId, institutionId, title, salary, start, end, status);
    }

    @Test
    @DisplayName("[JT-V1, JS-V1, JE-V1, JST-V1, JM-V1] accepts a valid job with one employer")
    void validate()
    {
        // Arrange
        JobRequestDTO request = request(1L, null, "Engineer", BigDecimal.ZERO,
                LocalDate.now(), null, "active");

        // Act / Assert
        assertDoesNotThrow(() -> JobValidator.validate(request));
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> salaries()
    {
        return Stream.of(arguments(new BigDecimal("-0.01"), true), arguments(BigDecimal.ZERO, false),
                arguments(new BigDecimal("0.01"), false), arguments(new BigDecimal("-50000"), true),
                arguments(new BigDecimal("45000.50"), false));
    }

    @ParameterizedTest
    @MethodSource("salaries")
    @DisplayName("[JS-V1, JS-V2, JS-V3, JS-I1, JS-I2] allows zero salary and rejects negative salary")
    void validateSalary(BigDecimal salary, boolean invalid)
    {
        // Arrange
        JobRequestDTO request = request(1L, null, "Engineer", salary, LocalDate.now(), null, "active");

        // Act
        if (invalid)
        {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> JobValidator.validate(request));
            assertTrue(exception.getMessage().startsWith("salary"));
        }
        else
        {
            assertDoesNotThrow(() -> JobValidator.validate(request));
        }
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    @DisplayName("[JT-I1] rejects a blank title")
    void validateTitle(String title)
    {
        // Arrange
        JobRequestDTO request = request(1L, null, title, BigDecimal.ZERO, LocalDate.now(), null, "active");

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> JobValidator.validate(request));

        // Assert
        assertTrue(exception.getMessage().startsWith("title"));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, JobValidator.MAX_TITLE_LENGTH})
    @DisplayName("[JT-V2, JT-V3] accepts a title of 1 and of 150 characters")
    void validateTitleLength(int length)
    {
        // Arrange
        JobRequestDTO request = request(1L, null, "a".repeat(length), BigDecimal.ZERO, LocalDate.now(), null, "active");

        // Act / Assert
        assertDoesNotThrow(() -> JobValidator.validate(request));
    }

    @Test
    @DisplayName("[JT-I2] rejects a title of 151 characters")
    void validateTitleTooLong()
    {
        // Arrange
        JobRequestDTO request = request(1L, null, "a".repeat(JobValidator.MAX_TITLE_LENGTH + 1), BigDecimal.ZERO,
                LocalDate.now(), null, "active");

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> JobValidator.validate(request));

        // Assert
        assertEquals("title must be at most 150 characters", exception.getMessage());
    }

    @Test
    @DisplayName("[JS-I3] rejects a missing salary")
    void validateMissingSalary()
    {
        // Arrange
        JobRequestDTO request = request(1L, null, "Engineer", null, LocalDate.now(), null, "active");

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> JobValidator.validate(request));

        // Assert
        assertTrue(exception.getMessage().startsWith("salary"));
    }

    @Test
    @DisplayName("[JD-I1] rejects a missing start date")
    void validateMissingStartDate()
    {
        // Arrange
        JobRequestDTO request = request(1L, null, "Engineer", BigDecimal.ZERO, null, null, "active");

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> JobValidator.validate(request));

        // Assert
        assertTrue(exception.getMessage().startsWith("startDate"));
    }

    @Test
    @DisplayName("[JE-I1] rejects an end date before the start date")
    void validateEndDate()
    {
        // Arrange
        LocalDate start = LocalDate.now();
        JobRequestDTO request = request(1L, null, "Engineer", BigDecimal.ZERO, start,
                start.minusDays(1), "inactive");

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> JobValidator.validate(request));

        // Assert
        assertEquals("endDate must not be before startDate", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    @DisplayName("[JE-V2, JE-V3] accepts an end date on or after the start date")
    void validateEndDateOnOrAfterStart(int daysAfterStart)
    {
        // Arrange
        LocalDate start = LocalDate.now().minusYears(1);
        JobRequestDTO request = request(1L, null, "Engineer", BigDecimal.ZERO, start,
                start.plusDays(daysAfterStart), "inactive");

        // Act / Assert
        assertDoesNotThrow(() -> JobValidator.validate(request));
    }

    @ParameterizedTest
    @ValueSource(strings = {"active", "inactive"})
    @DisplayName("[JST-V1, JST-V2] accepts supported statuses with matching dates")
    void validateStatus(String status)
    {
        // Arrange
        LocalDate date = LocalDate.now();
        JobRequestDTO request = request(1L, null, "Engineer", BigDecimal.ZERO, date,
                "inactive".equals(status) ? date : null, status);

        // Act / Assert
        assertDoesNotThrow(() -> JobValidator.validate(request));
    }

    @Test
    @DisplayName("[JST-I1] rejects an unsupported status")
    void validateInvalidStatus()
    {
        // Arrange
        JobRequestDTO request = request(1L, null, "Engineer", BigDecimal.ZERO,
                LocalDate.now(), null, "paused");

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> JobValidator.validate(request));

        // Assert
        assertTrue(exception.getMessage().startsWith("status"));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    @DisplayName("[JST-I2] rejects a missing status")
    void validateMissingStatus(String status)
    {
        // Arrange
        JobRequestDTO request = request(1L, null, "Engineer", BigDecimal.ZERO,
                LocalDate.now(), null, status);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> JobValidator.validate(request));

        // Assert
        assertTrue(exception.getMessage().startsWith("status"));
    }

    @Test
    @DisplayName("[JST-I4] requires active jobs to have no end date")
    void validateActiveDates()
    {
        // Arrange
        LocalDate date = LocalDate.now();
        JobRequestDTO request = request(1L, null, "Engineer", BigDecimal.ZERO, date, date, "active");

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> JobValidator.validate(request));

        // Assert
        assertTrue(exception.getMessage().startsWith("endDate"));
    }

    @Test
    @DisplayName("[JST-I5] requires inactive jobs to have an end date")
    void validateInactiveDates()
    {
        // Arrange
        JobRequestDTO request = request(1L, null, "Engineer", BigDecimal.ZERO,
                LocalDate.now(), null, "inactive");

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> JobValidator.validate(request));

        // Assert
        assertTrue(exception.getMessage().startsWith("endDate"));
    }

    @Test
    @DisplayName("[JM-I1] rejects a job without an employer")
    void validateMissingEmployer()
    {
        // Arrange
        JobRequestDTO request = request(null, null, "Engineer", BigDecimal.ZERO,
                LocalDate.now(), null, "active");

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> JobValidator.validate(request));

        // Assert
        assertEquals("A job needs a company or an institution", exception.getMessage());
    }

    @Test
    @DisplayName("[JM-I2] rejects a job with two employers")
    void validateBothEmployers()
    {
        // Arrange
        JobRequestDTO request = request(1L, 2L, "Engineer", BigDecimal.ZERO,
                LocalDate.now(), null, "active");

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> JobValidator.validate(request));

        // Assert
        assertEquals("A job cannot have both a company and an institution", exception.getMessage());
    }

    @Test
    @DisplayName("[JM-V2] accepts a job with only an institution")
    void validateInstitutionEmployer()
    {
        // Arrange
        JobRequestDTO request = request(null, 2L, "Teacher", BigDecimal.ZERO,
                LocalDate.now(), null, "active");

        // Act / Assert
        assertDoesNotThrow(() -> JobValidator.validate(request));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Anna\n", "An\tna", "Anna\0", "Anna\u007F"})
    @DisplayName("[JT-I3] rejects a title with a control character")
    void titleWithControlCharacter(String value)
    {
        // Arrange
        JobRequestDTO request = request(1L, null, value, BigDecimal.ZERO, LocalDate.now(), null, "active");

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> JobValidator.validate(request));

        // Assert
        assertEquals("title must not contain control characters", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Active", "ACTIVE", "active ", " inactive"})
    @DisplayName("[JST-I3] rejects a status in the wrong case or with surrounding spaces")
    void statusWrongCaseOrSpaces(String value)
    {
        // Arrange
        JobRequestDTO request = request(1L, null, "Engineer", BigDecimal.ZERO, LocalDate.now(), null, value);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> JobValidator.validate(request));

        // Assert
        assertEquals("status must be one of active, inactive", exception.getMessage());
    }
}
