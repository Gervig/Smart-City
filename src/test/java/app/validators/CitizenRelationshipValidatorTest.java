package app.validators;

import app.dtos.CitizenRelationshipRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullSource;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers requirement(s): Business rule, not yet in the SRS
 */
class CitizenRelationshipValidatorTest
{
    private static CitizenRelationshipRequestDTO request(Long id1, Long id2, String type,
                                                          LocalDate start, LocalDate end)
    {
        return new CitizenRelationshipRequestDTO(id1, id2, type, start, end);
    }

    @Test
    @DisplayName("accepts a valid relationship")
    void validate()
    {
        // Arrange
        LocalDate today = LocalDate.now();
        CitizenRelationshipRequestDTO request = request(1L, 2L, "marriage", today, today);

        // Act / Assert
        assertDoesNotThrow(() -> CitizenRelationshipValidator.validate(request));
    }

    @Test
    @DisplayName("rejects a missing first citizen")
    void validateMissingCitizenId1()
    {
        // Arrange
        CitizenRelationshipRequestDTO request = request(null, 2L, "marriage", LocalDate.now(), null);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> CitizenRelationshipValidator.validate(request));

        // Assert
        assertTrue(exception.getMessage().startsWith("citizenId1"));
    }

    @Test
    @DisplayName("rejects a missing second citizen")
    void validateMissingCitizenId2()
    {
        // Arrange
        CitizenRelationshipRequestDTO request = request(1L, null, "marriage", LocalDate.now(), null);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> CitizenRelationshipValidator.validate(request));

        // Assert
        assertTrue(exception.getMessage().startsWith("citizenId2"));
    }

    @Test
    @DisplayName("rejects the same citizen twice")
    void validateSameCitizen()
    {
        // Arrange
        CitizenRelationshipRequestDTO request = request(1L, 1L, "marriage", LocalDate.now(), null);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> CitizenRelationshipValidator.validate(request));

        // Assert
        assertTrue(exception.getMessage().startsWith("citizenId2"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"parent_child", "marriage"})
    @DisplayName("accepts supported relationship types")
    void validateRelationshipType(String type)
    {
        // Arrange
        CitizenRelationshipRequestDTO request = request(1L, 2L, type, LocalDate.now(), null);

        // Act / Assert
        assertDoesNotThrow(() -> CitizenRelationshipValidator.validate(request));
    }

    @Test
    @DisplayName("rejects an unsupported relationship type")
    void validateInvalidRelationshipType()
    {
        // Arrange
        CitizenRelationshipRequestDTO request = request(1L, 2L, "friend", LocalDate.now(), null);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> CitizenRelationshipValidator.validate(request));

        // Assert
        assertTrue(exception.getMessage().startsWith("relationshipType"));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    @DisplayName("rejects a missing relationship type")
    void validateMissingRelationshipType(String type)
    {
        // Arrange
        CitizenRelationshipRequestDTO request = request(1L, 2L, type, LocalDate.now(), null);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> CitizenRelationshipValidator.validate(request));

        // Assert
        assertTrue(exception.getMessage().startsWith("relationshipType"));
    }

    @Test
    @DisplayName("rejects a missing start date")
    void validateMissingStartDate()
    {
        // Arrange
        CitizenRelationshipRequestDTO request = request(1L, 2L, "marriage", null, null);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> CitizenRelationshipValidator.validate(request));

        // Assert
        assertTrue(exception.getMessage().startsWith("startDate"));
    }

    @Test
    @DisplayName("rejects an end date before the start date")
    void validateEndDate()
    {
        // Arrange
        LocalDate start = LocalDate.now();
        CitizenRelationshipRequestDTO request = request(1L, 2L, "marriage", start, start.minusDays(1));

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> CitizenRelationshipValidator.validate(request));

        // Assert
        assertTrue(exception.getMessage().startsWith("endDate"));
    }
}
