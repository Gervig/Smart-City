package app.services;

import app.daos.GenericDAO;
import app.dtos.JobRequestDTO;
import app.entities.Citizen;
import app.entities.Company;
import app.entities.Institution;
import app.entities.Job;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

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
    @DisplayName("rejects a job without an employer")
    void createNoEmployer()
    {
        // Arrange
        JobRequestDTO request = request(null, null, LocalDate.now());

        // Act
        assertThrows(IllegalArgumentException.class, () -> service.create(request));

        // Assert
        verify(jobDAO, never()).persist(any());
    }

    @Test
    @DisplayName("rejects a job with two employers")
    void createBothEmployers()
    {
        // Arrange
        JobRequestDTO request = request(2L, 3L, LocalDate.now());

        // Act
        assertThrows(IllegalArgumentException.class, () -> service.create(request));

        // Assert
        verify(jobDAO, never()).persist(any());
    }

    @Test
    @DisplayName("rejects a job starting before the citizen was born")
    void createBeforeBirth()
    {
        // Arrange
        LocalDate birthDate = LocalDate.now().minusYears(30);
        when(citizenDAO.findByIdOrThrow(1L)).thenReturn(citizen(birthDate, null));

        // Act
        assertThrows(IllegalArgumentException.class,
                () -> service.create(request(2L, null, birthDate.minusDays(1))));

        // Assert
        verify(jobDAO, never()).persist(any());
    }

    @Test
    @DisplayName("rejects a job starting after the citizen died")
    void createAfterDeath()
    {
        // Arrange
        LocalDate birthDate = LocalDate.now().minusYears(40);
        LocalDate deathDate = LocalDate.now().minusYears(1);
        when(citizenDAO.findByIdOrThrow(1L)).thenReturn(citizen(birthDate, deathDate));

        // Act
        assertThrows(IllegalArgumentException.class,
                () -> service.create(request(2L, null, deathDate.plusDays(1))));

        // Assert
        verify(jobDAO, never()).persist(any());
    }

    @Test
    @DisplayName("persists a valid job")
    void create()
    {
        // Arrange
        LocalDate startDate = LocalDate.now();
        when(citizenDAO.findByIdOrThrow(1L)).thenReturn(citizen(startDate.minusYears(20), null));
        when(companyDAO.findByIdOrThrow(2L)).thenReturn(new Company());
        when(jobDAO.persist(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        service.create(request(2L, null, startDate));

        // Assert
        verify(jobDAO).persist(any());
    }
}
