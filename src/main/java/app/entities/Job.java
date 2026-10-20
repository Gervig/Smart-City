package app.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "job")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "citizen_id", nullable = false)
    private Citizen citizen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "institution_id")
    private Institution institution;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal salary;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(nullable = false, length = 20)
    private String status = "active";

    public Job() {
    }

    public Job(Citizen citizen, Company company,
               String title, BigDecimal salary,
               LocalDate startDate) {
        this.citizen = citizen;
        this.company = company;
        this.title = title;
        this.salary = salary;
        this.startDate = startDate;
    }

    public Job(Citizen citizen, Institution institution,
               String title, BigDecimal salary,
               LocalDate startDate) {
        this.citizen = citizen;
        this.institution = institution;
        this.title = title;
        this.salary = salary;
        this.startDate = startDate;
    }
}
