package app.validators;

import app.dtos.HousingRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers requirement(s): FR-13
 */
class HousingValidatorTest
{
    @Test
    @DisplayName("accepts a valid housing request")
    void validate()
    {
        // Arrange
        HousingRequestDTO request = new HousingRequestDTO(1L, "house", 1);

        // Act / Assert
        assertDoesNotThrow(() -> HousingValidator.validate(request));
    }

    @ParameterizedTest
    @CsvSource({"house,1", "apartment,1", "house,2"})
    @DisplayName("accepts allowed housing types and capacities")
    void validateAllowedValues(String type, int capacity)
    {
        // Arrange
        HousingRequestDTO request = new HousingRequestDTO(1L, type, capacity);

        // Act / Assert
        assertDoesNotThrow(() -> HousingValidator.validate(request));
    }

    @ParameterizedTest
    @CsvSource({"house,0,capacity", "house,-1,capacity", "villa,1,housingType", "'',1,housingType"})
    @DisplayName("rejects invalid type or capacity")
    void validateInvalidValues(String type, int capacity, String field)
    {
        // Arrange
        HousingRequestDTO request = new HousingRequestDTO(1L, type, capacity);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> HousingValidator.validate(request));

        // Assert
        assertTrue(exception.getMessage().startsWith(field));
    }
}
