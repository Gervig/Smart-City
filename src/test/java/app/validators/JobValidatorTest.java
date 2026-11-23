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
    @DisplayName("accepts a valid job with one employer")
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
        return Stream.of(arguments(BigDecimal.ZERO, false), arguments(new BigDecimal("-0.01"), true));
    }

    @ParameterizedTest
    @MethodSource("salaries")
    @DisplayName("allows zero salary and rejects negative salary")
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
    @DisplayName("rejects a blank title")
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

    @Test
    @DisplayName("rejects a missing salary")
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
    @DisplayName("rejects a missing start date")
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
    @DisplayName("rejects an end date before the start date")
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
        assertTrue(exception.getMessage().startsWith("endDate"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"active", "inactive"})
    @DisplayName("accepts supported statuses with matching dates")
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
    @DisplayName("rejects an unsupported status")
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
    @DisplayName("rejects a missing status")
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
    @DisplayName("requires active jobs to have no end date")
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
    @DisplayName("requires inactive jobs to have an end date")
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
    @DisplayName("rejects a job without an employer")
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
    @DisplayName("rejects a job with two employers")
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
    @DisplayName("accepts a job with only an institution")
    void validateInstitutionEmployer()
    {
        // Arrange
        JobRequestDTO request = request(null, 2L, "Teacher", BigDecimal.ZERO,
                LocalDate.now(), null, "active");

        // Act / Assert
        assertDoesNotThrow(() -> JobValidator.validate(request));
    }
}
