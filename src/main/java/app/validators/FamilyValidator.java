package app.validators;

import app.dtos.FamilyRequestDTO;

public final class FamilyValidator {
    // [CHANGED] Added explicit max length constant for familyName
    public static final int MAX_FAMILY_NAME_LENGTH = 100;

    private FamilyValidator() {
    }

    public static void validate(FamilyRequestDTO request) {
        // [CHANGED] Guard against a null request DTO before reading fields
        Check.requireNotNull("request", request);
        Check.requireText("familyName", request.familyName());
        // [CHANGED] Enforce max length and reject control characters on familyName
        Check.requireMaxLength("familyName", request.familyName(), MAX_FAMILY_NAME_LENGTH);
        Check.requireNoControlChars("familyName", request.familyName());
    }
}