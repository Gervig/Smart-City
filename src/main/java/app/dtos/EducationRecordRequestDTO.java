package app.dtos;

import java.time.LocalDate;

public record EducationRecordRequestDTO(Long citizenId, Long institutionId, LocalDate startDate, LocalDate endDate) {
}
