package app.validators;

import app.dtos.CitizenRelationshipRequestDTO;

public final class CitizenRelationshipValidator {
    // [CHANGED] Added explicit max length constant for relationshipType
    public static final int MAX_RELATIONSHIP_TYPE_LENGTH = 32;

    private CitizenRelationshipValidator() {
    }

    public static void validate(CitizenRelationshipRequestDTO request) {
        // [CHANGED] Guard against a null request DTO before reading fields
        Check.requireNotNull("request", request);
        Check.requireNotNull("citizenId1", request.citizenId1());
        Check.requireNotNull("citizenId2", request.citizenId2());
        if (request.citizenId1().equals(request.citizenId2())) {
            throw new IllegalArgumentException("citizenId2 must differ from citizenId1");
        }
        // [CHANGED] Enforce max length and reject control characters on relationshipType
        Check.requireMaxLength("relationshipType", request.relationshipType(), MAX_RELATIONSHIP_TYPE_LENGTH);
        Check.requireNoControlChars("relationshipType", request.relationshipType());
        Check.requireOneOf("relationshipType", request.relationshipType(), "parent_child", "marriage");
        Check.requireNotNull("startDate", request.startDate());
        Check.requireNotBefore("endDate", request.endDate(), "startDate", request.startDate());
    }
}