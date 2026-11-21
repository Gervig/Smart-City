package app.validators;

import app.dtos.CompanyRequestDTO;

public final class CompanyValidator {
    private CompanyValidator() {
    }

    public static void validate(CompanyRequestDTO request) {
        Check.requireText("name", request.name());
        Check.requireNotNull("foundedDate", request.foundedDate());
        Check.requireNotBefore("dissolvedDate", request.dissolvedDate(), "foundedDate", request.foundedDate());
        Check.requireOneOf("status", request.status(), "active", "dissolved");
        if ("active".equals(request.status()) && request.dissolvedDate() != null) {
            throw new IllegalArgumentException("dissolvedDate must be null when status is active");
        }
        if ("dissolved".equals(request.status()) && request.dissolvedDate() == null) {
            throw new IllegalArgumentException("dissolvedDate is required when status is dissolved");
        }
    }
}
