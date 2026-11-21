package app.validators;

import app.dtos.JobRequestDTO;

public final class JobValidator {
    // [CHANGED] Added explicit max length constants for title and status
    public static final int MAX_TITLE_LENGTH = 150;
    public static final int MAX_STATUS_LENGTH = 20;

    private JobValidator() {
    }

    public static void validate(JobRequestDTO request) {
        // [CHANGED] Guard against a null request DTO before reading fields
        Check.requireNotNull("request", request);
        Check.requireText("title", request.title());
        // [CHANGED] Enforce max length and reject control characters on title
        Check.requireMaxLength("title", request.title(), MAX_TITLE_LENGTH);
        Check.requireNoControlChars("title", request.title());

        Check.requireNotNull("salary", request.salary());
        if (request.salary().signum() < 0) {
            throw new IllegalArgumentException("salary must be at least 0");
        }
        Check.requireNotNull("startDate", request.startDate());
        Check.requireNotBefore("endDate", request.endDate(), "startDate", request.startDate());

        // [CHANGED] Enforce max length and reject control characters on status
        Check.requireMaxLength("status", request.status(), MAX_STATUS_LENGTH);
        Check.requireNoControlChars("status", request.status());
        Check.requireOneOf("status", request.status(), "active", "inactive");

        if ("active".equals(request.status()) && request.endDate() != null) {
            throw new IllegalArgumentException("endDate must be null when status is active");
        }
        if ("inactive".equals(request.status()) && request.endDate() == null) {
            throw new IllegalArgumentException("endDate is required when status is inactive");
        }
        if (request.companyId() == null && request.institutionId() == null) {
            throw new IllegalArgumentException("A job needs a company or an institution");
        }
        if (request.companyId() != null && request.institutionId() != null) {
            throw new IllegalArgumentException("A job cannot have both a company and an institution");
        }
    }
}