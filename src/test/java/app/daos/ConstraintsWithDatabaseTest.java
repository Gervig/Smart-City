package app.daos;

import app.config.HibernateConfig;
import app.dtos.CitizenRequestDTO;
import app.entities.Citizen;
import app.entities.Family;
import app.entities.Housing;
import app.entities.Location;
import app.services.CitizenService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Covers requirement(s): Business rule, not yet in the SRS
 */
class ConstraintsWithDatabaseTest
{
    private EntityManagerFactory emf;
    private GenericDAO<Citizen> citizenDAO;
    private GenericDAO<Family> familyDAO;
    private GenericDAO<Housing> housingDAO;
    private GenericDAO<Location> locationDAO;
    private CitizenService citizenService;
    private Family family;
    private Housing fullHousing;

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
        familyDAO = new GenericDAO<>(emf, Family.class);
        housingDAO = new GenericDAO<>(emf, Housing.class);
        locationDAO = new GenericDAO<>(emf, Location.class);
        citizenService = new CitizenService(citizenDAO, familyDAO, housingDAO);

        family = new Family();
        family.setFamilyName("Test");
        family = familyDAO.persist(family);

        fullHousing = housing("Full", 1);
        fullHousing = housingDAO.persist(fullHousing);
    }

    private Housing housing(String name, int capacity)
    {
        Location location = new Location();
        location.setName(name);
        location = locationDAO.persist(location);

        Housing housing = new Housing();
        housing.setLocation(location);
        housing.setHousingType("house");
        housing.setCapacity(capacity);
        return housing;
    }

    private CitizenRequestDTO citizenRequest(String firstName, Long housingId)
    {
        return new CitizenRequestDTO(firstName, "Smith", LocalDate.now().minusYears(30),
                null, "alive", "unknown", family.getId(), housingId);
    }

    @Test
    @DisplayName("does not move a citizen into housing that is already full")
    void updateFullHousing()
    {
        // Arrange
        Housing otherHousing = housingDAO.persist(housing("Other", 2));
        var movingCitizen = citizenService.create(citizenRequest("Moving", otherHousing.getId()));
        citizenService.create(citizenRequest("Resident", fullHousing.getId()));

        // Act
        assertThrows(IllegalArgumentException.class,
                () -> citizenService.update(movingCitizen.id(), citizenRequest("Moving", fullHousing.getId())));

        // Assert
        try (EntityManager em = emf.createEntityManager())
        {
            Citizen row = em.find(Citizen.class, movingCitizen.id());
            assertNotNull(row);
            assertNotNull(row.getHousing());
            org.junit.jupiter.api.Assertions.assertEquals(otherHousing.getId(), row.getHousing().getId());
        }
    }

    @Test
    @DisplayName("preserves a location referenced by housing when deletion fails")
    void deleteReferencedLocation()
    {
        // Arrange
        Long locationId = fullHousing.getLocation().getId();

        // Act
        assertThrows(PersistenceException.class, () -> locationDAO.delete(locationId));

        // Assert
        try (EntityManager em = emf.createEntityManager())
        {
            assertNotNull(em.find(Location.class, locationId));
        }
    }

    @Test
    @DisplayName("rejects deletion of a family referenced by a citizen")
    void deleteReferencedFamily()
    {
        // Arrange
        Citizen citizen = new Citizen();
        citizen.setFirstName("Alex");
        citizen.setLastName("Smith");
        citizen.setBirthDate(LocalDate.now().minusYears(30));
        citizen.setStatus("alive");
        citizen.setFamily(family);
        citizenDAO.persist(citizen);

        // Act / Assert
        assertThrows(PersistenceException.class, () -> familyDAO.delete(family.getId()));
    }
}
