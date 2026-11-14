package app.validators;

import app.dtos.EducationRecordRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Covers requirement(s): Business rule, not yet in the SRS
 */
class EducationRecordValidatorTest
{
    @ParameterizedTest
    @MethodSource("validEndDates")
    @DisplayName("accepts a valid education period")
    void validate(LocalDate endDate)
    {
        // Arrange
        LocalDate start = LocalDate.now();
        EducationRecordRequestDTO request = new EducationRecordRequestDTO(1L, 1L, start, endDate);

        // Act / Assert
        assertDoesNotThrow(() -> EducationRecordValidator.validate(request));
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> validEndDates()
    {
        LocalDate today = LocalDate.now();
        return Stream.of(arguments((LocalDate) null), arguments(today), arguments(today.plusDays(1)));
    }

    @Test
    @DisplayName("rejects a missing start date")
    void validateMissingStartDate()
    {
        // Arrange
        EducationRecordRequestDTO request = new EducationRecordRequestDTO(1L, 1L, null, null);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> EducationRecordValidator.validate(request));

        // Assert
        assertTrue(exception.getMessage().startsWith("startDate"));
    }

    @Test
    @DisplayName("rejects an end date before the start date")
    void validateEndDate()
    {
        // Arrange
        LocalDate start = LocalDate.now();
        EducationRecordRequestDTO request = new EducationRecordRequestDTO(1L, 1L, start, start.minusDays(1));

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> EducationRecordValidator.validate(request));

        // Assert
        assertTrue(exception.getMessage().startsWith("endDate"));
    }
}
