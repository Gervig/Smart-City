package app.validators;

import app.dtos.CitizenRelationshipRequestDTO;

public final class CitizenRelationshipValidator {
    private CitizenRelationshipValidator() {
    }

    public static void validate(CitizenRelationshipRequestDTO request) {
        Check.requireNotNull("citizenId1", request.citizenId1());
        Check.requireNotNull("citizenId2", request.citizenId2());
        if (request.citizenId1().equals(request.citizenId2())) {
            throw new IllegalArgumentException("citizenId2 must differ from citizenId1");
        }
        Check.requireOneOf("relationshipType", request.relationshipType(), "parent_child", "marriage");
        Check.requireNotNull("startDate", request.startDate());
        Check.requireNotBefore("endDate", request.endDate(), "startDate", request.startDate());
    }
}
