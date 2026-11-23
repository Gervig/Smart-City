package app.dtos;

import java.time.LocalDate;

public record CompanyDTO(Long id, Long locationId, String name, LocalDate foundedDate, LocalDate dissolvedDate, String status) {
}
