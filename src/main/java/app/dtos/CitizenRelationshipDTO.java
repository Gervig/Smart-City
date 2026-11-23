package app.dtos;

import java.time.LocalDate;

public record CitizenRelationshipDTO(Long id, Long citizenId1, Long citizenId2, String relationshipType, LocalDate startDate, LocalDate endDate) {
}
