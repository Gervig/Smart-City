package app.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "citizen_relationship")
public class CitizenRelationship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "citizen_id_1", nullable = false)
    private Citizen citizen1;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "citizen_id_2", nullable = false)
    private Citizen citizen2;

    @Column(name = "relationship_type", nullable = false, length = 20)
    private String relationshipType;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    public CitizenRelationship() {
    }

    public CitizenRelationship(Citizen citizen1,
                               Citizen citizen2,
                               String relationshipType,
                               LocalDate startDate) {
        this.citizen1 = citizen1;
        this.citizen2 = citizen2;
        this.relationshipType = relationshipType;
        this.startDate = startDate;
    }
}