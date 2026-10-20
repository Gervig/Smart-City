package app.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "family")
public class Family {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "family_name", nullable = false, length = 150)
    private String familyName;

    @OneToMany(mappedBy = "family")
    private Set<Citizen> citizens = new HashSet<>();

    public Family() {
    }

    public Family(String familyName) {
        this.familyName = familyName;
    }
}
