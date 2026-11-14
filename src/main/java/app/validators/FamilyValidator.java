package app.validators;

import app.dtos.FamilyRequestDTO;

public final class FamilyValidator {
    private FamilyValidator() {
    }

    public static void validate(FamilyRequestDTO request) {
        Check.requireText("familyName", request.familyName());
    }
}
