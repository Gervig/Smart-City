package app.validators;

import app.dtos.CitizenRequestDTO;

public final class CitizenValidator {
    private CitizenValidator() {
    }

    public static void validate(CitizenRequestDTO request) {
        Check.requireText("firstName", request.firstName());
        Check.requireText("lastName", request.lastName());
        Check.requireNotNull("birthDate", request.birthDate());
        Check.requireNotAfterToday("birthDate", request.birthDate());
        Check.requireNotBefore("deathDate", request.deathDate(), "birthDate", request.birthDate());
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
