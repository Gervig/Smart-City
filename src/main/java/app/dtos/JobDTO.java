package app.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobDTO {

    private Long id;
    private Long citizenId;
    private Long companyId;
    private Long institutionId;
    private String title;
    private BigDecimal salary;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
}
