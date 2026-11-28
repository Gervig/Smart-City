package app.validators;

import app.dtos.CitizenRequestDTO;

public final class CitizenValidator {
    public static final int MAX_NAME_LENGTH = 100;
    public static final int MAX_STATUS_LENGTH = 20;

    private CitizenValidator() {
    }

    public static void validate(CitizenRequestDTO request) {
        Check.requireNotNull("request", request);
        Check.requireText("firstName", request.firstName());
        Check.requireMaxLength("firstName", request.firstName(), MAX_NAME_LENGTH);
        Check.requireNoControlChars("firstName", request.firstName());

        Check.requireText("lastName", request.lastName());
        Check.requireMaxLength("lastName", request.lastName(), MAX_NAME_LENGTH);
        Check.requireNoControlChars("lastName", request.lastName());

        Check.requireNotNull("birthDate", request.birthDate());
        Check.requireNotAfterToday("birthDate", request.birthDate());
        Check.requireNotBefore("deathDate", request.deathDate(), "birthDate", request.birthDate());

        Check.requireMaxLength("status", request.status(), MAX_STATUS_LENGTH);
        Check.requireNoControlChars("status", request.status());
        Check.requireOneOf("status", request.status(), "alive", "deceased");

        if ("alive".equals(request.status()) && request.deathDate() != null) {
            throw new IllegalArgumentException("deathDate must be null when status is alive");
        }
        if ("deceased".equals(request.status()) && request.deathDate() == null) {
            throw new IllegalArgumentException("deathDate is required when status is deceased");
        }
        if ("deceased".equals(request.status()) && request.housingId() != null) {
            throw new IllegalArgumentException("housingId must be null for a deceased citizen");
        }
    }
}