package app.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "citizen_relationship")
@Getter
@Setter
@NoArgsConstructor
public class CitizenRelationship {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "citizen_id_1", nullable = false)
    private Citizen citizen1;

    @ManyToOne(optional = false)
    @JoinColumn(name = "citizen_id_2", nullable = false)
    private Citizen citizen2;

    private String relationshipType;
    private LocalDate startDate;
    private LocalDate endDate;
}
