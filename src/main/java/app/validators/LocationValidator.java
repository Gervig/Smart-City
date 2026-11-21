package app.validators;

import app.dtos.LocationRequestDTO;

public final class LocationValidator {
    // [CHANGED] Added explicit max length constant for name
    public static final int MAX_NAME_LENGTH = 150;

    private LocationValidator() {
    }

    public static void validate(LocationRequestDTO request) {
        // [CHANGED] Guard against a null request DTO before reading fields
        Check.requireNotNull("request", request);
        Check.requireText("name", request.name());
        // [CHANGED] Enforce max length and reject control characters on name
        Check.requireMaxLength("name", request.name(), MAX_NAME_LENGTH);
        Check.requireNoControlChars("name", request.name());
    }
}