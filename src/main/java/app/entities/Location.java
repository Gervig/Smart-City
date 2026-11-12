package app.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Table(
        name = "location",
        uniqueConstraints = @UniqueConstraint(name = "OSM_OBJ", columnNames = {"osm_id", "osm_type"})
)
@Getter
@Setter
@NoArgsConstructor
@DynamicUpdate
public class Location {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // OSM variables
    private String name;
    private Double latitude;
    private Double longitude;
    @Column(name="osm_id")
    private Long osmId;
    @Column(name="osm_type")
    private String osmType;

    // TODO: refactor address logic?
    private String streetAddress;
}
