package app.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HousingDTO {

    private Long id;
    private Long locationId;
    private String housingType;
    private Integer capacity;
}
