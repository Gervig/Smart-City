package app.dtos;

import java.time.LocalDate;

public record CitizenRequestDTO(String firstName, String lastName, LocalDate birthDate, LocalDate deathDate, String status, String gender, Long familyId, Long housingId) {
}
