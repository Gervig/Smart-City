package app.validators;

import app.dtos.LocationRequestDTO;

public final class LocationValidator {
    public static final int MAX_NAME_LENGTH = 150;

    private LocationValidator() {
    }

    public static void validate(LocationRequestDTO request) {
        Check.requireNotNull("request", request);
        Check.requireText("name", request.name());
        Check.requireMaxLength("name", request.name(), MAX_NAME_LENGTH);
        Check.requireNoControlChars("name", request.name());
    }
}