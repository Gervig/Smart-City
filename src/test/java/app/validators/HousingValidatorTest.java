package app.validators;

import app.dtos.HousingRequestDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers requirement(s): FR-13
 */
class HousingValidatorTest
{
    @Test
    @DisplayName("[HC-V1, HT-V1] accepts a valid housing request")
    void validate()
    {
        // Arrange
        HousingRequestDTO request = new HousingRequestDTO(1L, "house", 1);

        // Act / Assert
        assertDoesNotThrow(() -> HousingValidator.validate(request));
    }

    @ParameterizedTest
    @CsvSource({"house,1", "apartment,1", "house,2", "house,9999", "house,10000"})
    @DisplayName("[HC-V1, HC-V2, HC-V3, HC-V4, HT-V1, HT-V2] accepts allowed housing types and capacities from 1 to 10000")
    void validateAllowedValues(String type, int capacity)
    {
        // Arrange
        HousingRequestDTO request = new HousingRequestDTO(1L, type, capacity);

        // Act / Assert
        assertDoesNotThrow(() -> HousingValidator.validate(request));
    }

    // [CHANGED] Added capacity above MAX_CAPACITY (10001) to cover the upper-bound rejection branch
    @ParameterizedTest
    @CsvSource({"house,0,capacity", "house,-1,capacity", "house,10001,capacity", "villa,1,housingType", "'',1,housingType"})
    @DisplayName("[HC-I1, HC-I2, HC-I3, HT-I1, HT-I2] rejects invalid type or capacity")
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

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {" "})
    @DisplayName("[HT-I3] rejects null and blank housing types")
    void validateMissingHousingType(String type)
    {
        // Arrange
        HousingRequestDTO request = new HousingRequestDTO(1L, type, 1);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> HousingValidator.validate(request));

        // Assert
        assertTrue(exception.getMessage().startsWith("housingType"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"House", "APARTMENT", "house "})
    @DisplayName("[HT-I4, HT-I5] rejects a housing type in the wrong case or with surrounding spaces")
    void typeWrongCaseOrSpaces(String value)
    {
        // Arrange
        HousingRequestDTO request = new HousingRequestDTO(1L, value, 1);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> HousingValidator.validate(request));

        // Assert
        assertEquals("housingType must be one of house, apartment", exception.getMessage());
    }
}
