package app.dtos;

import java.time.LocalDate;

public record CompanyRequestDTO(Long locationId, String name, LocalDate foundedDate, LocalDate dissolvedDate, String status) {
}
