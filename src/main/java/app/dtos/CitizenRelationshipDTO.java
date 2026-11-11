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
public class CitizenRelationshipDTO {

    private Long id;
    private Long citizen1Id;
    private Long citizen2Id;
    private String relationshipType;
    private LocalDate startDate;
    private LocalDate endDate;
}
