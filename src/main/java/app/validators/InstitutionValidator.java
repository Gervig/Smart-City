package app.validators;

import app.dtos.InstitutionRequestDTO;

public final class InstitutionValidator {
    public static final int MAX_NAME_LENGTH = 150;
    public static final int MAX_TYPE_LENGTH = 30;

    private InstitutionValidator() {
    }

    public static void validate(InstitutionRequestDTO request) {
        Check.requireNotNull("request", request);
        Check.requireText("name", request.name());
        Check.requireMaxLength("name", request.name(), MAX_NAME_LENGTH);
        Check.requireNoControlChars("name", request.name());

        Check.requireMaxLength("type", request.type(), MAX_TYPE_LENGTH);
        Check.requireNoControlChars("type", request.type());
        Check.requireOneOf("type", request.type(), "high_school", "college", "hospital");
    }
}