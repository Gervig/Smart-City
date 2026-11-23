package app.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

public record JobDTO(Long id, Long citizenId, Long companyId, Long institutionId, String title, BigDecimal salary, LocalDate startDate, LocalDate endDate, String status) {
}
