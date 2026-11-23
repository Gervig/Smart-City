package app.validators;

import app.dtos.FamilyRequestDTO;

public final class FamilyValidator {
    public static final int MAX_FAMILY_NAME_LENGTH = 100;

    private FamilyValidator() {
    }

    public static void validate(FamilyRequestDTO request) {
        Check.requireNotNull("request", request);
        Check.requireText("familyName", request.familyName());
        Check.requireMaxLength("familyName", request.familyName(), MAX_FAMILY_NAME_LENGTH);
        Check.requireNoControlChars("familyName", request.familyName());
    }
}