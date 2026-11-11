package app.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompanyDTO {

    private Long id;
    private Long locationId;
    private String name;
    private LocalDate foundedDate;
    private LocalDate dissolvedDate;
    private String status;
}
