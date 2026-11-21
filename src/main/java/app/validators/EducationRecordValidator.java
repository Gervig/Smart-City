package app.validators;

import app.dtos.EducationRecordRequestDTO;

public final class EducationRecordValidator {
    private EducationRecordValidator() {
    }

    public static void validate(EducationRecordRequestDTO request) {
        Check.requireNotNull("startDate", request.startDate());
        Check.requireNotBefore("endDate", request.endDate(), "startDate", request.startDate());
    }
}
