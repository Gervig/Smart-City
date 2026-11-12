package app.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Table(name = "housing")
@Getter
@Setter
@NoArgsConstructor
@DynamicUpdate
public class Housing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Location location;

    private String housingType;
    private Integer capacity;

    // OSM variables
    private Integer numberOfLevels;
    private Integer numberOfFlats;
    private Double height;

}
