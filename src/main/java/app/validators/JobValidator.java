package app.validators;

import app.dtos.JobRequestDTO;

public final class JobValidator {
    private JobValidator() {
    }

    public static void validate(JobRequestDTO request) {
        Check.requireText("title", request.title());
        Check.requireNotNull("salary", request.salary());
        if (request.salary().signum() < 0) {
            throw new IllegalArgumentException("salary must be at least 0");
        }
        Check.requireNotNull("startDate", request.startDate());
        Check.requireNotBefore("endDate", request.endDate(), "startDate", request.startDate());
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
