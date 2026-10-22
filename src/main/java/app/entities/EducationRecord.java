package app.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Table(name = "education_record")
@Getter
@Setter
@NoArgsConstructor
public class EducationRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Citizen citizen;

    @ManyToOne(optional = false)
    private Institution institution;

    private LocalDate startDate;
    private LocalDate endDate;
}
