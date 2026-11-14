package app.mappers;

import app.dtos.JobRequestDTO;
import app.entities.Citizen;
import app.entities.Company;
import app.entities.Institution;
import app.entities.Job;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Covers requirement(s): FR-16
 */
class JobMapperTest
{
    private JobMapper mapper;

    @BeforeEach
    void setUp()
    {
        mapper = JobMapper.INSTANCE;
    }

    @Test
    @DisplayName("maps citizen, company and institution relations to ids")
    void toDTO()
    {
        // Arrange
        Citizen citizen = new Citizen();
        citizen.setId(1L);
        Company company = new Company();
        company.setId(2L);
        Institution institution = new Institution();
        institution.setId(3L);
        Job job = new Job();
        job.setCitizen(citizen);
        job.setCompany(company);
        job.setInstitution(institution);

        // Act
        var result = mapper.toDTO(job);

        // Assert
        assertEquals(1L, result.citizenId());
        assertEquals(2L, result.companyId());
        assertEquals(3L, result.institutionId());
    }

    @Test
    @DisplayName("maps a null optional company relation to a null id")
    void toDTONullCompany()
    {
        // Arrange
        Citizen citizen = new Citizen();
        citizen.setId(1L);
        Job job = new Job();
        job.setCitizen(citizen);

        // Act
        var result = mapper.toDTO(job);

        // Assert
        assertNull(result.companyId());
        assertNull(result.institutionId());
    }

    @Test
    @DisplayName("maps plain fields and leaves relations unset")
    void toEntity()
    {
        // Arrange
        LocalDate date = LocalDate.now();
        JobRequestDTO request = new JobRequestDTO(1L, 2L, null, "Engineer",
                BigDecimal.TEN, date, null, "active");

        // Act
        Job result = mapper.toEntity(request);

        // Assert
        assertEquals("Engineer", result.getTitle());
        assertEquals(BigDecimal.TEN, result.getSalary());
        assertEquals(date, result.getStartDate());
        assertNull(result.getCitizen());
        assertNull(result.getCompany());
        assertNull(result.getInstitution());
    }

    @Test
    @DisplayName("updates plain fields and preserves existing relations")
    void update()
    {
        // Arrange
        Citizen citizen = new Citizen();
        Company company = new Company();
        Institution institution = new Institution();
        Job job = new Job();
        job.setCitizen(citizen);
        job.setCompany(company);
        job.setInstitution(institution);
        LocalDate date = LocalDate.now();
        JobRequestDTO request = new JobRequestDTO(4L, null, 5L, "Teacher",
                BigDecimal.ONE, date, null, "active");

        // Act
        mapper.update(request, job);

        // Assert
        assertEquals("Teacher", job.getTitle());
        assertEquals(BigDecimal.ONE, job.getSalary());
        assertSame(citizen, job.getCitizen());
        assertSame(company, job.getCompany());
        assertSame(institution, job.getInstitution());
    }
}
