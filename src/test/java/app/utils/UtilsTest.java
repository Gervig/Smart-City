package app.utils;

import app.exceptions.ApiException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers requirement(s): Business rule, not yet in the SRS
 */
class UtilsTest
{
    @Test
    @DisplayName("returns a property value")
    void getPropertyValueFound()
    {
        // Arrange
        String propertyName = "plain.value";

        // Act
        String value = Utils.getPropertyValue(propertyName, "test-config.properties");

        // Assert
        assertEquals("plain", value);
    }

    @Test
    @DisplayName("trims whitespace around a property value")
    void getPropertyValueTrimsValue()
    {
        // Arrange
        String propertyName = "spaced.value";

        // Act
        String value = Utils.getPropertyValue(propertyName, "test-config.properties");

        // Assert
        assertEquals("trim me", value);
    }

    @Test
    @DisplayName("throws an API exception when a property is missing")
    void getPropertyValueMissingProperty()
    {
        // Arrange
        String propertyName = "missing.value";

        // Act
        ApiException exception = assertThrows(ApiException.class,
                () -> Utils.getPropertyValue(propertyName, "test-config.properties"));

        // Assert
        assertEquals(500, exception.getCode());
        assertTrue(exception.getMessage().contains(propertyName));
    }

    @Test
    @DisplayName("throws an API exception when the resource is missing")
    void getPropertyValueMissingResource()
    {
        // Arrange
        String resourceName = "not-a-real-test-config.properties";

        // Act
        ApiException exception = assertThrows(ApiException.class,
                () -> Utils.getPropertyValue("some.value", resourceName));

        // Assert
        assertEquals(500, exception.getCode());
    }
}
