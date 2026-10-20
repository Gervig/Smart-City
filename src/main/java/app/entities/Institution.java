package app.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "institution")
public class Institution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 30)
    private String type;

    @OneToMany(mappedBy = "institution")
    private Set<Job> jobs = new HashSet<>();

    @OneToMany(mappedBy = "institution")
    private Set<EducationRecord> educationRecords = new HashSet<>();

    public Institution() {
    }

    public Institution(Location location, String name,
                       String type) {
        this.location = location;
        this.name = name;
        this.type = type;
    }
}