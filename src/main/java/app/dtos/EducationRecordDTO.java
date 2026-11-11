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
public class EducationRecordDTO {

    private Long id;
    private Long citizenId;
    private Long institutionId;
    private LocalDate startDate;
    private LocalDate endDate;
}
