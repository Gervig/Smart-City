package app.validators;

import app.dtos.HousingRequestDTO;

public final class HousingValidator {
    private HousingValidator() {
    }

    public static void validate(HousingRequestDTO request) {
        Check.requireOneOf("housingType", request.housingType(), "house", "apartment");
        if (request.capacity() < 1) {
            throw new IllegalArgumentException("capacity must be at least 1");
        }
    }
}
