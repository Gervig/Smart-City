package app.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
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

    @ManyToOne(optional = true)
    @JoinColumn(name = "location_id", nullable = true)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    private Location location;

    private String housingType;
    private Integer capacity;

    // OSM variables
    private Integer numberOfLevels;
    private Integer numberOfFlats;
    private Double height;

}
