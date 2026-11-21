package app.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.DynamicUpdate;

import java.time.LocalDate;

@Entity
@Table(name = "company")
@Getter
@Setter
@NoArgsConstructor
@DynamicUpdate
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Location location;

    private String name;

    // OSM variables
    private String cvrNumber;
    private String productionUnitNumber;
    private String website;
    private String phone;
    private String email;
    private String category;
    private String openingHours;

    private LocalDate foundedDate;
    private LocalDate dissolvedDate;
    private String status;
}
