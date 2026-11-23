package app.validators;

import app.dtos.HousingRequestDTO;

public final class HousingValidator {
    public static final int MAX_HOUSING_TYPE_LENGTH = 30;
    public static final int MAX_CAPACITY = 10_000;

    private HousingValidator() {
    }

    public static void validate(HousingRequestDTO request) {
        Check.requireNotNull("request", request);
        Check.requireMaxLength("housingType", request.housingType(), MAX_HOUSING_TYPE_LENGTH);
        Check.requireNoControlChars("housingType", request.housingType());
        Check.requireOneOf("housingType", request.housingType(), "house", "apartment");
        if (request.capacity() < 1 || request.capacity() > MAX_CAPACITY) {
            throw new IllegalArgumentException("capacity must be between 1 and " + MAX_CAPACITY);
        }
    }
}