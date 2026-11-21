package app.validators;

import app.dtos.HousingRequestDTO;

public final class HousingValidator {
    // [CHANGED] Added explicit max length and upper capacity bound constants
    public static final int MAX_HOUSING_TYPE_LENGTH = 30;
    public static final int MAX_CAPACITY = 10_000;

    private HousingValidator() {
    }

    public static void validate(HousingRequestDTO request) {
        // [CHANGED] Guard against a null request DTO before reading fields
        Check.requireNotNull("request", request);
        // [CHANGED] Enforce max length and reject control characters on housingType
        Check.requireMaxLength("housingType", request.housingType(), MAX_HOUSING_TYPE_LENGTH);
        Check.requireNoControlChars("housingType", request.housingType());
        Check.requireOneOf("housingType", request.housingType(), "house", "apartment");
        // [CHANGED] Added upper bound check (MAX_CAPACITY) alongside lower bound >= 1
        if (request.capacity() < 1 || request.capacity() > MAX_CAPACITY) {
            throw new IllegalArgumentException("capacity must be between 1 and " + MAX_CAPACITY);
        }
    }
}