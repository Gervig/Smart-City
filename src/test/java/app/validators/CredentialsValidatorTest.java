package app.validators;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Covers requirement(s): FR-01, FR-02
 */
class CredentialsValidatorTest
{
    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    @DisplayName("[UN-I1] login rejects a missing username")
    void loginMissingUsername(String username)
    {
        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> CredentialsValidator.validateLogin(username, "Password123"));

        // Assert
        assertEquals("username is required", exception.getMessage());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    @DisplayName("[PW-I3] login rejects a missing password")
    void loginMissingPassword(String password)
    {
        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> CredentialsValidator.validateLogin("User1", password));

        // Assert
        assertEquals("password is required", exception.getMessage());
    }

    @ParameterizedTest
    @CsvSource({"1, 8", "255, 8", "5, 9", "5, 64"})
    @DisplayName("[UN-V1, UN-V2, UN-V3, PW-V1, PW-V2, PW-V3] registration accepts usernames of 1 to 255 characters and passwords of 8 or more")
    void registrationValidLengths(int usernameLength, int passwordLength)
    {
        // Arrange
        String username = "u".repeat(usernameLength);
        String password = "p".repeat(passwordLength);

        // Act / Assert
        assertDoesNotThrow(() -> CredentialsValidator.validateRegistration(username, password));
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 7})
    @DisplayName("[PW-I1, PW-I2] registration rejects a password shorter than 8 characters")
    void registrationShortPassword(int passwordLength)
    {
        // Arrange
        String password = "p".repeat(passwordLength);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> CredentialsValidator.validateRegistration("User1", password));

        // Assert
        assertEquals("password must be at least 8 characters", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(ints = {256, 1000})
    @DisplayName("[UN-I2, UN-I3] registration rejects a username longer than 255 characters")
    void registrationLongUsername(int usernameLength)
    {
        // Arrange
        String username = "u".repeat(usernameLength);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> CredentialsValidator.validateRegistration(username, "Password123"));

        // Assert
        assertEquals("username must be at most 255 characters", exception.getMessage());
    }
}
