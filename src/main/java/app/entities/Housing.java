package app.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "housing")
public class Housing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    @Column(name = "housing_type", nullable = false, length = 30)
    private String housingType;

    @Column(nullable = false)
    private Integer capacity;

    @OneToMany(mappedBy = "housing")
    private Set<Citizen> citizens = new HashSet<>();

    public Housing() {
    }

    public Housing(Location location, String housingType,
                   Integer capacity) {
        this.location = location;
        this.housingType = housingType;
        this.capacity = capacity;
    }
}
