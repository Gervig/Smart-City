package app.services;

import app.daos.GenericDAO;
import app.dtos.JobDTO;
import app.dtos.JobRequestDTO;
import app.entities.Citizen;
import app.entities.Company;
import app.entities.Institution;
import app.entities.Job;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers requirement(s): FR-16
 */
@ExtendWith(MockitoExtension.class)
class JobServiceTest
{
    @Mock
    private GenericDAO<Job> jobDAO;
    @Mock
    private GenericDAO<Citizen> citizenDAO;
    @Mock
    private GenericDAO<Company> companyDAO;
    @Mock
    private GenericDAO<Institution> institutionDAO;

    private JobService service;

    @BeforeEach
    void setUp()
    {
        service = new JobService(jobDAO, citizenDAO, companyDAO, institutionDAO);
    }

    private JobRequestDTO request(Long companyId, Long institutionId, LocalDate startDate)
    {
        return new JobRequestDTO(1L, companyId, institutionId, "Engineer", BigDecimal.ZERO,
                startDate, null, "active");
    }

    private Citizen citizen(LocalDate birthDate, LocalDate deathDate)
    {
        Citizen citizen = new Citizen();
        citizen.setBirthDate(birthDate);
        citizen.setDeathDate(deathDate);
        return citizen;
    }

    @Test
    @DisplayName("[JM-I1] rejects a job without an employer")
    void createNoEmployer()
    {
        // Arrange
        JobRequestDTO request = request(null, null, LocalDate.now());

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.create(request));

        // Assert
        assertEquals("A job needs a company or an institution", exception.getMessage());
        verify(jobDAO, never()).persist(any());
    }

    @Test
    @DisplayName("[JM-I2] rejects a job with two employers")
    void createBothEmployers()
    {
        // Arrange
        JobRequestDTO request = request(2L, 3L, LocalDate.now());

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.create(request));

        // Assert
        assertEquals("A job cannot have both a company and an institution", exception.getMessage());
        verify(jobDAO, never()).persist(any());
    }

    @Test
    @DisplayName("[JD-I2] rejects a job starting before the citizen was born")
    void createBeforeBirth()
    {
        // Arrange
        LocalDate birthDate = LocalDate.now().minusYears(30);
        when(citizenDAO.findByIdOrThrow(1L)).thenReturn(citizen(birthDate, null));

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.create(request(2L, null, birthDate.minusDays(1))));

        // Assert
        assertEquals("startDate must not be before the citizen's birthDate", exception.getMessage());
        verify(jobDAO, never()).persist(any());
    }

    @Test
    @DisplayName("[JD-I3] rejects a job starting after the citizen died")
    void createAfterDeath()
    {
        // Arrange
        LocalDate birthDate = LocalDate.now().minusYears(40);
        LocalDate deathDate = LocalDate.now().minusYears(1);
        when(citizenDAO.findByIdOrThrow(1L)).thenReturn(citizen(birthDate, deathDate));

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.create(request(2L, null, deathDate.plusDays(1))));

        // Assert
        assertEquals("startDate must not be after the citizen's deathDate", exception.getMessage());
        verify(jobDAO, never()).persist(any());
    }

    @Test
    @DisplayName("[JD-V1] persists a valid job")
    void create()
    {
        // Arrange
        LocalDate startDate = LocalDate.now();
        Citizen citizen = citizen(startDate.minusYears(20), null);
        citizen.setId(1L);
        Company company = new Company();
        company.setId(2L);
        when(citizenDAO.findByIdOrThrow(1L)).thenReturn(citizen);
        when(companyDAO.findByIdOrThrow(2L)).thenReturn(company);
        when(jobDAO.persist(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        JobDTO result = service.create(request(2L, null, startDate));

        // Assert
        ArgumentCaptor<Job> saved = ArgumentCaptor.forClass(Job.class);
        verify(jobDAO).persist(saved.capture());
        assertSame(citizen, saved.getValue().getCitizen());
        assertSame(company, saved.getValue().getCompany());
        assertEquals("Engineer", saved.getValue().getTitle());
        assertEquals(0, BigDecimal.ZERO.compareTo(saved.getValue().getSalary()));
        assertEquals(startDate, saved.getValue().getStartDate());
        assertEquals("active", saved.getValue().getStatus());
        assertEquals(1L, result.citizenId());
        assertEquals(2L, result.companyId());
        assertNull(result.institutionId());
        assertEquals(startDate, result.startDate());
    }

    @ParameterizedTest
    @ValueSource(strings = {"birth", "death"})
    @DisplayName("[JD-V2, JD-V3] accepts a job starting on the day the citizen was born or died")
    void createOnBirthOrDeathDay(String day)
    {
        // Arrange
        LocalDate birthDate = LocalDate.now().minusYears(40);
        LocalDate deathDate = LocalDate.now().minusYears(1);
        LocalDate startDate = "birth".equals(day) ? birthDate : deathDate;
        when(citizenDAO.findByIdOrThrow(1L)).thenReturn(citizen(birthDate, deathDate));
        when(companyDAO.findByIdOrThrow(2L)).thenReturn(new Company());
        when(jobDAO.persist(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        JobDTO result = service.create(request(2L, null, startDate));

        // Assert
        assertEquals(startDate, result.startDate());
        verify(jobDAO).persist(any());
    }
}
