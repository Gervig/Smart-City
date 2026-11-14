package app.daos;

import app.config.HibernateConfig;
import app.dtos.CitizenRequestDTO;
import app.dtos.JobRequestDTO;
import app.entities.Citizen;
import app.entities.Company;
import app.entities.Family;
import app.entities.Housing;
import app.entities.Location;
import app.services.CitizenService;
import app.services.JobService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Covers requirement(s): FR-08, FR-09, FR-13, FR-16
 */
class ServiceWithDatabaseTest
{
    private EntityManagerFactory emf;
    private GenericDAO<Citizen> citizenDAO;
    private GenericDAO<Company> companyDAO;
    private CitizenService citizenService;
    private JobService jobService;
    private Family family;
    private Housing housing;
    private Location location;

    @BeforeEach
    void setUp()
    {
        emf = HibernateConfig.getEntityManagerFactoryForTest();
        try (EntityManager em = emf.createEntityManager())
        {
            em.getTransaction().begin();
            em.createNativeQuery("TRUNCATE TABLE citizen_relationship, job, education_record, citizen, company, institution, housing, family, location RESTART IDENTITY CASCADE")
                    .executeUpdate();
            em.getTransaction().commit();
        }

        citizenDAO = new GenericDAO<>(emf, Citizen.class);
        GenericDAO<Family> familyDAO = new GenericDAO<>(emf, Family.class);
        GenericDAO<Housing> housingDAO = new GenericDAO<>(emf, Housing.class);
        GenericDAO<Location> locationDAO = new GenericDAO<>(emf, Location.class);
        companyDAO = new GenericDAO<>(emf, Company.class);
        citizenService = new CitizenService(citizenDAO, familyDAO, housingDAO);
        jobService = new JobService(new GenericDAO<>(emf, app.entities.Job.class), citizenDAO,
                companyDAO, new GenericDAO<>(emf, app.entities.Institution.class));

        family = new Family();
        family.setFamilyName("Test");
        family = familyDAO.persist(family);

        location = new Location();
        location.setName("Test location");
        location = locationDAO.persist(location);

        housing = new Housing();
        housing.setLocation(location);
        housing.setHousingType("house");
        housing.setCapacity(1);
        housing = housingDAO.persist(housing);
    }

    private CitizenRequestDTO citizenRequest(String firstName, LocalDate birthDate, Long housingId)
    {
        return new CitizenRequestDTO(firstName, "Smith", birthDate, null, "alive", "unknown",
                family.getId(), housingId);
    }

    @Test
    @DisplayName("creates a valid citizen and reads it from the database")
    void createCitizen()
    {
        // Arrange
        CitizenRequestDTO request = citizenRequest("Alex", LocalDate.now(), null);

        // Act
        var created = citizenService.create(request);

        // Assert
        try (EntityManager em = emf.createEntityManager())
        {
            Citizen row = em.find(Citizen.class, created.id());
            assertNotNull(row);
            assertEquals("Alex", row.getFirstName());
            assertEquals(family.getId(), row.getFamily().getId());
        }
    }

    @Test
    @DisplayName("rejects future birth date without inserting a citizen")
    void createInvalidCitizen()
    {
        // Arrange
        CitizenRequestDTO request = citizenRequest("Alex", LocalDate.now().plusDays(1), null);

        // Act
        assertThrows(IllegalArgumentException.class, () -> citizenService.create(request));

        // Assert
        assertEquals(0L, citizenDAO.countWhere("firstName", "Alex"));
    }

    @Test
    @DisplayName("enforces housing capacity without counting the updated resident twice")
    void createHousingFull()
    {
        // Arrange
        CitizenRequestDTO first = citizenRequest("Alex", LocalDate.now(), housing.getId());
        CitizenRequestDTO second = citizenRequest("Jamie", LocalDate.now(), housing.getId());

        // Act
        var created = citizenService.create(first);
        assertThrows(IllegalArgumentException.class, () -> citizenService.create(second));
        var updated = citizenService.update(created.id(),
                citizenRequest("Alex", LocalDate.now(), housing.getId()));

        // Assert
        assertEquals(created.id(), updated.id());
        assertEquals(1L, citizenDAO.countWhere("housing.id", housing.getId()));
    }

    @Test
    @DisplayName("rejects a job before birth without inserting a job")
    void createJobBeforeBirth()
    {
        // Arrange
        LocalDate birthDate = LocalDate.now().minusYears(25);
        var createdCitizen = citizenService.create(citizenRequest("Alex", birthDate, null));
        Company company = new Company();
        company.setLocation(location);
        company.setName("Test company");
        company.setFoundedDate(LocalDate.now().minusYears(10));
        company.setStatus("active");
        company = companyDAO.persist(company);
        JobRequestDTO request = new JobRequestDTO(createdCitizen.id(), company.getId(), null,
                "Engineer", BigDecimal.ZERO, birthDate.minusDays(1), null, "active");

        // Act
        assertThrows(IllegalArgumentException.class, () -> jobService.create(request));

        // Assert
        assertEquals(0L, new GenericDAO<>(emf, app.entities.Job.class).findAll().size());
    }
}
