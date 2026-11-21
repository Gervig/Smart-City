package app.validators;

import app.dtos.CitizenRequestDTO;

public final class CitizenValidator {
    // [CHANGED] Added explicit max length constants for name and status fields
    public static final int MAX_NAME_LENGTH = 100;
    public static final int MAX_STATUS_LENGTH = 20;

    private CitizenValidator() {
    }

    public static void validate(CitizenRequestDTO request) {
        // [CHANGED] Guard against a null request DTO before accessing fields
        Check.requireNotNull("request", request);
        Check.requireText("firstName", request.firstName());
        // [CHANGED] Enforce max length and reject control characters on firstName
        Check.requireMaxLength("firstName", request.firstName(), MAX_NAME_LENGTH);
        Check.requireNoControlChars("firstName", request.firstName());

        Check.requireText("lastName", request.lastName());
        // [CHANGED] Enforce max length and reject control characters on lastName
        Check.requireMaxLength("lastName", request.lastName(), MAX_NAME_LENGTH);
        Check.requireNoControlChars("lastName", request.lastName());

        Check.requireNotNull("birthDate", request.birthDate());
        Check.requireNotAfterToday("birthDate", request.birthDate());
        Check.requireNotBefore("deathDate", request.deathDate(), "birthDate", request.birthDate());

        // [CHANGED] Enforce max length and reject control characters on status
        Check.requireMaxLength("status", request.status(), MAX_STATUS_LENGTH);
        Check.requireNoControlChars("status", request.status());
        Check.requireOneOf("status", request.status(), "alive", "dead");

        if ("alive".equals(request.status()) && request.deathDate() != null) {
            throw new IllegalArgumentException("deathDate must be null when status is alive");
        }
        if ("dead".equals(request.status()) && request.deathDate() == null) {
            throw new IllegalArgumentException("deathDate is required when status is dead");
        }
        if ("dead".equals(request.status()) && request.housingId() != null) {
            throw new IllegalArgumentException("housingId must be null for a dead citizen");
        }
    }
}