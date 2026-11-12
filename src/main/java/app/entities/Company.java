package app.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "company")
@Getter
@Setter
@NoArgsConstructor
public class Company {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Location location;

    private String name;

    // OSM variables
    private String cvrNumber;
    private String website;
    private String phone;
    private String email;
    private String category;
    private String openingHours;

    private LocalDate foundedDate;
    private LocalDate dissolvedDate;
    private String status;
}
