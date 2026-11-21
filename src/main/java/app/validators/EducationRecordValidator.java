package app.validators;

import app.dtos.EducationRecordRequestDTO;

public final class EducationRecordValidator {
    public static final int MAX_PROGRAM_LENGTH = 150; // [CHANGED] Kept constant if program is present
    public static final int MAX_STATUS_LENGTH = 30; // [CHANGED] Kept constant if status is present

    private EducationRecordValidator() {
    }

    public static void validate(EducationRecordRequestDTO request) {
        // [CHANGED] Guard against a null request DTO before reading fields
        Check.requireNotNull("request", request);
        Check.requireNotNull("startDate", request.startDate());
        Check.requireNotBefore("endDate", request.endDate(), "startDate", request.startDate());
    }
}