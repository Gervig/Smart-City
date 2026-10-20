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
@Table(name = "citizen")
public class Citizen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "death_date")
    private LocalDate deathDate;

    @Column(nullable = false, length = 10)
    private String status = "alive";

    @Column(length = 50)
    private String gender;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "family_id", nullable = false)
    private Family family;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "housing_id")
    private Housing housing;

    @OneToMany(mappedBy = "citizen")
    private Set<Job> jobs = new HashSet<>();

    @OneToMany(mappedBy = "citizen")
    private Set<EducationRecord> educationRecords = new HashSet<>();


    @OneToMany(mappedBy = "citizen1")
    private Set<CitizenRelationship> relationshipsAsCitizen1 =
            new HashSet<>();

    @OneToMany(mappedBy = "citizen2")
    private Set<CitizenRelationship> relationshipsAsCitizen2 =
            new HashSet<>();

    public Citizen() {
    }

    public Citizen(String firstName, String lastName,
                   LocalDate birthDate, String gender,
                   Family family, Housing housing) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.birthDate = birthDate;
        this.gender = gender;
        this.family = family;
        this.housing = housing;
    }
}
