package app.populators.generators;

import app.dtos.JobRequestDTO;
import app.entities.Citizen;
import app.entities.Company;
import app.entities.Institution;
import app.entities.Job;
import app.validators.JobValidator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;

public final class JobGenerator
{
    private static final List<String> TITLES = List.of("lærer", "sygeplejerske", "ingeniør", "assistent", "leder", "tekniker");
    private final Random random;

    public JobGenerator(Random random)
    {
        this.random = random;
    }

    public Job generate(Citizen citizen, Company company, Institution institution)
    {
        if ((company == null) == (institution == null))
        {
            throw new IllegalArgumentException("exactly one employer is required");
        }
        LocalDate latest = citizen.getDeathDate() == null ? LocalDate.now() : citizen.getDeathDate();
        LocalDate start = randomDate(citizen.getBirthDate(), latest);
        boolean active = citizen.getDeathDate() == null && random.nextBoolean();
        LocalDate end = active ? null : randomDate(start, latest);

        Job job = new Job();
        job.setCitizen(citizen);
        job.setCompany(company);
        job.setInstitution(institution);
        job.setTitle(TITLES.get(random.nextInt(TITLES.size())));
        job.setSalary(BigDecimal.valueOf(random.nextInt(80001)));
        job.setStartDate(start);
        job.setEndDate(end);
        job.setStatus(active ? "active" : "inactive");
        JobValidator.validate(new JobRequestDTO(null, company == null ? null : 1L,
                institution == null ? null : 1L, job.getTitle(), job.getSalary(), start, end,
                job.getStatus()));
        return job;
    }

    private LocalDate randomDate(LocalDate start, LocalDate end)
    {
        long days = end.toEpochDay() - start.toEpochDay();
        return start.plusDays(days == 0 ? 0 : random.nextInt((int) days + 1));
    }
}
