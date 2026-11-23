package app.validators;

import app.dtos.CompanyRequestDTO;

public final class CompanyValidator {
    public static final int MAX_NAME_LENGTH = 150;
    public static final int MAX_STATUS_LENGTH = 20;

    private CompanyValidator() {
    }

    public static void validate(CompanyRequestDTO request) {
        Check.requireNotNull("request", request);
        Check.requireText("name", request.name());
        Check.requireMaxLength("name", request.name(), MAX_NAME_LENGTH);
        Check.requireNoControlChars("name", request.name());

        Check.requireNotNull("foundedDate", request.foundedDate());
        Check.requireNotBefore("dissolvedDate", request.dissolvedDate(), "foundedDate", request.foundedDate());

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