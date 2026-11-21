package app.dtos;

import java.time.LocalDate;

public record EducationRecordDTO(Long id, Long citizenId, Long institutionId, LocalDate startDate, LocalDate endDate) {
}
