package app.validators;

import app.dtos.LocationRequestDTO;

public final class LocationValidator {
    private LocationValidator() {
    }

    public static void validate(LocationRequestDTO request) {
        Check.requireText("name", request.name());
    }
}
