package app.validators;

import app.dtos.EducationRecordRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers requirement(s): Business rule, not yet in the SRS
 */
class EducationRecordValidatorTest
{
    @Test
    @DisplayName("accepts a valid education period")
    void validate()
    {
        // Arrange
        LocalDate start = LocalDate.now();
        EducationRecordRequestDTO request = new EducationRecordRequestDTO(1L, 1L, start, start);

        // Act / Assert
        assertDoesNotThrow(() -> EducationRecordValidator.validate(request));
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
