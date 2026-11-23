package app.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Covers requirement(s): Business rule, not yet in the SRS
 */
class ExceptionsTest
{
    @Test
    @DisplayName("stores the API error code and message")
    void apiException()
    {
        // Arrange
        int code = 422;
        String message = "Invalid request";

        // Act
        ApiException exception = new ApiException(code, message);

        // Assert
        assertEquals(code, exception.getCode());
        assertEquals(message, exception.getMessage());
    }

    @Test
    @DisplayName("stores the API error cause")
    void apiExceptionWithCause()
    {
        // Arrange
        Exception cause = new Exception("Underlying failure");

        // Act
        ApiException exception = new ApiException(500, "Failure", cause);

        // Assert
        assertEquals(500, exception.getCode());
        assertEquals("Failure", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    @DisplayName("stores the authorization status code and message")
    void notAuthorizedException()
    {
        // Arrange
        int statusCode = 403;
        String message = "Forbidden";

        // Act
        NotAuthorizedException exception = new NotAuthorizedException(statusCode, message);

        // Assert
        assertEquals(statusCode, exception.getStatusCode());
        assertEquals(message, exception.getMessage());
    }

    @Test
    @DisplayName("stores the authorization exception cause")
    void notAuthorizedExceptionWithCause()
    {
        // Arrange
        Throwable cause = new IllegalStateException("No credentials");

        // Act
        NotAuthorizedException exception = new NotAuthorizedException(401, "Unauthorized", cause);

        // Assert
        assertEquals(401, exception.getStatusCode());
        assertEquals("Unauthorized", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @ParameterizedTest
    @ValueSource(strings = {"Missing", "Unknown id"})
    @DisplayName("stores the not-found message")
    void notFoundException(String message)
    {
        // Arrange / Act
        NotFoundException exception = new NotFoundException(message);

        // Assert
        assertEquals(message, exception.getMessage());
    }

    @Test
    @DisplayName("stores the validation message")
    void validationException()
    {
        // Arrange / Act
        ValidationException exception = new ValidationException("Invalid input");

        // Assert
        assertEquals("Invalid input", exception.getMessage());
    }
}
