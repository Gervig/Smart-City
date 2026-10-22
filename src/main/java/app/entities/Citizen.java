package app.entities;

import app.dtos.CitizenDTO;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;
import org.hibernate.annotations.DynamicUpdate;

@DynamicUpdate
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
@ToString
@Entity
public class Citizen
{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    // constructor
    public Citizen(CitizenDTO dto)
    {
        //TODO: map entity to dto here
    }
}
