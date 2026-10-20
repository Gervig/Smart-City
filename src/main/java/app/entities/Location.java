package app.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "location")
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(name = "street_address", nullable = false, length = 255)
    private String streetAddress;

    @OneToMany(mappedBy = "location")
    private Set<Company> companies = new HashSet<>();

    @OneToMany(mappedBy = "location")
    private Set<Institution> institutions = new HashSet<>();

    @OneToMany(mappedBy = "location")
    private Set<Housing> housings = new HashSet<>();

    public Location() {
    }

    public Location(String name, String streetAddress) {
        this.name = name;
        this.streetAddress = streetAddress;
    }
}
