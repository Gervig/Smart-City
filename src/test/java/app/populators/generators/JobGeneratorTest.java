package app.populators.generators;

import app.dtos.JobRequestDTO;
import app.entities.Citizen;
import app.entities.Company;
import app.entities.Job;
import app.validators.JobValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Business rule, not yet in the SRS
 * <p>
 * Uses real deterministic entities and validators; no database or network is needed for job histories.
 */
class JobGeneratorTest
{
    @Test
    @DisplayName("generates deterministic jobs within each citizen's lifetime")
    void generateDeterministic()
    {
        // Arrange
        var first = generateSequence(1);
        var same = generateSequence(1);
        var different = generateSequence(2);

        // Act
        var samples = IntStream.range(0, 50).mapToObj(this::generated).toList();

        // Assert
        assertEquals(first, same);
        assertNotEquals(first, different);
        for (var sample : samples)
        {
            Job job = sample.job();
            Citizen citizen = sample.citizen();
            JobValidator.validate(new JobRequestDTO(null, job.getCompany() == null ? null : 1L,
                    job.getInstitution() == null ? null : 1L, job.getTitle(), job.getSalary(), job.getStartDate(),
                    job.getEndDate(), job.getStatus()));
            assertFalse(job.getStartDate().isBefore(citizen.getBirthDate()));
            if (citizen.getDeathDate() != null)
            {
                assertFalse(job.getStartDate().isAfter(citizen.getDeathDate()));
                if (job.getEndDate() != null)
                {
                    assertFalse(job.getEndDate().isAfter(citizen.getDeathDate()));
                }
            }
            assertTrue(job.getCompany() == null ^ job.getInstitution() == null);
        }
    }

    private java.util.List<String> generateSequence(long seed)
    {
        return IntStream.range(0, 50).mapToObj(i ->
        {
            var sample = generated(seed + i);
            return sample.job().getTitle() + sample.job().getStartDate() + sample.job().getSalary();
        }).toList();
    }

    private Sample generated(long seed)
    {
        Random random = new Random(seed);
        Citizen citizen = new CitizenGenerator(random).generate();
        Company company = new CompanyGenerator(random).generate();
        Job job = new JobGenerator(random).generate(citizen, company, null);
        return new Sample(citizen, job);
    }

    private record Sample(Citizen citizen, Job job)
    {
    }
}
