package app.validators;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Covers requirement(s): Business rule, not yet in the SRS
 */
class CheckTest
{
    @Test
    @DisplayName("checks required text and null values")
    void requireText()
    {
        // Arrange / Act / Assert
        assertDoesNotThrow(() -> Check.requireText("name", "value"));
        assertThrows(IllegalArgumentException.class, () -> Check.requireText("name", null));
        assertThrows(IllegalArgumentException.class, () -> Check.requireText("name", " "));
    }

    @Test
    @DisplayName("checks non-null values")
    void requireNotNull()
    {
        // Arrange / Act / Assert
        assertDoesNotThrow(() -> Check.requireNotNull("value", new Object()));
        assertThrows(IllegalArgumentException.class, () -> Check.requireNotNull("value", null));
    }

    @Test
    @DisplayName("checks allowed values and missing text")
    void requireOneOf()
    {
        // Arrange / Act / Assert
        assertDoesNotThrow(() -> Check.requireOneOf("type", "allowed", "allowed"));
        assertThrows(IllegalArgumentException.class, () -> Check.requireOneOf("type", "other", "allowed"));
        assertThrows(IllegalArgumentException.class, () -> Check.requireOneOf("type", null, "allowed"));
        assertThrows(IllegalArgumentException.class, () -> Check.requireOneOf("type", " ", "allowed"));
    }

    @Test
    @DisplayName("checks date ordering for null, equal, later and earlier dates")
    void requireNotBefore()
    {
        // Arrange
        LocalDate date = LocalDate.now();

        // Act / Assert
        assertDoesNotThrow(() -> Check.requireNotBefore("date", null, "otherDate", date));
        assertDoesNotThrow(() -> Check.requireNotBefore("date", date, "otherDate", null));
        assertDoesNotThrow(() -> Check.requireNotBefore("date", date, "otherDate", date));
        assertDoesNotThrow(() -> Check.requireNotBefore("date", date.plusDays(1), "otherDate", date));
        assertThrows(IllegalArgumentException.class,
                () -> Check.requireNotBefore("date", date.minusDays(1), "otherDate", date));
    }

    @Test
    @DisplayName("checks today and future dates")
    void requireNotAfterToday()
    {
        // Arrange
        LocalDate today = LocalDate.now();

        // Act / Assert
        assertDoesNotThrow(() -> Check.requireNotAfterToday("date", null));
        assertDoesNotThrow(() -> Check.requireNotAfterToday("date", today));
        assertThrows(IllegalArgumentException.class,
                () -> Check.requireNotAfterToday("date", today.plusDays(1)));
    }
}
