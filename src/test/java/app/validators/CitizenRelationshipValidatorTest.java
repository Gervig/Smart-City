package app.validators;

import app.dtos.CitizenRelationshipRequestDTO;
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
class CitizenRelationshipValidatorTest
{
    private static CitizenRelationshipRequestDTO request(Long id1, Long id2, String type,
                                                          LocalDate start, LocalDate end)
    {
        return new CitizenRelationshipRequestDTO(id1, id2, type, start, end);
    }

    @Test
    @DisplayName("[RI-V1, RT-V2, RD-V1] accepts a valid relationship")
    void validate()
    {
        // Arrange
        LocalDate today = LocalDate.now();
        CitizenRelationshipRequestDTO request = request(1L, 2L, "marriage", today, today);

        // Act / Assert
        assertDoesNotThrow(() -> CitizenRelationshipValidator.validate(request));
    }

    @Test
    @DisplayName("[RI-I1] rejects a missing first citizen")
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
    @DisplayName("[RI-I2] rejects a missing second citizen")
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
    @DisplayName("[RI-I3] rejects the same citizen twice")
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
    @DisplayName("[RT-V1, RT-V2, RD-V2] accepts supported relationship types")
    void validateRelationshipType(String type)
    {
        // Arrange
        CitizenRelationshipRequestDTO request = request(1L, 2L, type, LocalDate.now(), null);

        // Act / Assert
        assertDoesNotThrow(() -> CitizenRelationshipValidator.validate(request));
    }

    @Test
    @DisplayName("[RT-I1] rejects an unsupported relationship type")
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
    @DisplayName("[RT-I2] rejects a missing relationship type")
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
    @DisplayName("[RD-I1] rejects a missing start date")
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
    @DisplayName("[RD-I2] rejects an end date before the start date")
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

    @ParameterizedTest
    @ValueSource(strings = {"Marriage", "MARRIAGE", "parent-child", "marriage "})
    @DisplayName("[RT-I3] rejects a relationship type in the wrong case, spelling or with surrounding spaces")
    void relationshipTypeWrongCaseOrSpelling(String value)
    {
        // Arrange
        CitizenRelationshipRequestDTO request = request(1L, 2L, value, LocalDate.now(), null);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> CitizenRelationshipValidator.validate(request));

        // Assert
        assertEquals("relationshipType must be one of parent_child, marriage", exception.getMessage());
    }
}
