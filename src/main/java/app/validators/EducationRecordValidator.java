package app.validators;

import app.dtos.EducationRecordRequestDTO;

public final class EducationRecordValidator {
    public static final int MAX_PROGRAM_LENGTH = 150;
    public static final int MAX_STATUS_LENGTH = 30;

    private EducationRecordValidator() {
    }

    public static void validate(EducationRecordRequestDTO request) {
        Check.requireNotNull("request", request);
        Check.requireNotNull("startDate", request.startDate());
        Check.requireNotBefore("endDate", request.endDate(), "startDate", request.startDate());
    }
}