package app.validators;

import app.dtos.CompanyRequestDTO;

public final class CompanyValidator {
    // [CHANGED] Added explicit max length constants for name and status fields
    public static final int MAX_NAME_LENGTH = 150;
    public static final int MAX_STATUS_LENGTH = 20;

    private CompanyValidator() {
    }

    public static void validate(CompanyRequestDTO request) {
        // [CHANGED] Guard against a null request DTO before reading fields
        Check.requireNotNull("request", request);
        Check.requireText("name", request.name());
        // [CHANGED] Enforce max length and reject control characters on name
        Check.requireMaxLength("name", request.name(), MAX_NAME_LENGTH);
        Check.requireNoControlChars("name", request.name());

        Check.requireNotNull("foundedDate", request.foundedDate());
        Check.requireNotBefore("dissolvedDate", request.dissolvedDate(), "foundedDate", request.foundedDate());

        // [CHANGED] Enforce max length and reject control characters on status
        Check.requireMaxLength("status", request.status(), MAX_STATUS_LENGTH);
        Check.requireNoControlChars("status", request.status());
        Check.requireOneOf("status", request.status(), "active", "dissolved");

        if ("active".equals(request.status()) && request.dissolvedDate() != null) {
            throw new IllegalArgumentException("dissolvedDate must be null when status is active");
        }
        if ("dissolved".equals(request.status()) && request.dissolvedDate() == null) {
            throw new IllegalArgumentException("dissolvedDate is required when status is dissolved");
        }
    }
}