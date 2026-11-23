package app.validators;

/**
 * Rules for the username and password sent to login and register (FR-01, FR-02). Breaking a rule throws
 * IllegalArgumentException, which the API answers with 400.
 */
public final class CredentialsValidator {
    // FR-01: a password shorter than 8 characters is rejected (the number is still a proposal, see OI-03)
    public static final int MIN_PASSWORD_LENGTH = 8;
    // users.username is varchar(255)
    public static final int MAX_USERNAME_LENGTH = 255;

    private CredentialsValidator() {
    }

    public static void validateLogin(String username, String password) {
        Check.requireText("username", username);
        Check.requireText("password", password);
    }

    public static void validateRegistration(String username, String password) {
        validateLogin(username, password);
        if (username.length() > MAX_USERNAME_LENGTH) {
            throw new IllegalArgumentException("username must be at most " + MAX_USERNAME_LENGTH + " characters");
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
    }
}
