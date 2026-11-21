package app.validators;

import app.dtos.InstitutionRequestDTO;

public final class InstitutionValidator {
    private InstitutionValidator() {
    }

    public static void validate(InstitutionRequestDTO request) {
        Check.requireText("name", request.name());
        Check.requireOneOf("type", request.type(), "high_school", "college", "hospital");
    }
}
