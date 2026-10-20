package app.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "company")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "founded_date", nullable = false)
    private LocalDate foundedDate;

    @Column(name = "dissolved_date")
    private LocalDate dissolvedDate;

    @Column(nullable = false, length = 20)
    private String status = "active";

    @OneToMany(mappedBy = "company")
    private Set<Job> jobs = new HashSet<>();

    public Company() {
    }

    public Company(Location location, String name,
                   LocalDate foundedDate) {
        this.location = location;
        this.name = name;
        this.foundedDate = foundedDate;
    }
}
