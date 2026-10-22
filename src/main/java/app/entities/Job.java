package app.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "job")
@Getter
@Setter
@NoArgsConstructor
public class Job {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Citizen citizen;

    @ManyToOne
    private Company company;

    @ManyToOne
    private Institution institution;

    private String title;
    private BigDecimal salary;
    private LocalDate startDate;
    private LocalDate endDate;
    private String status;
}
