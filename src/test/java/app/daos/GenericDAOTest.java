package app.daos;

import app.config.HibernateConfig;
import app.entities.Citizen;
import app.entities.Family;
import app.entities.Location;
import app.exceptions.NotFoundException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Covers requirement(s): Business rule, not yet in the SRS
 */
class GenericDAOTest
{
    private EntityManagerFactory emf;
    private GenericDAO<Location> locationDAO;
    private GenericDAO<Family> familyDAO;
    private GenericDAO<Citizen> citizenDAO;

    @BeforeEach
    void setUp()
    {
        emf = HibernateConfig.getEntityManagerFactoryForTest();
        locationDAO = new GenericDAO<>(emf, Location.class);
        familyDAO = new GenericDAO<>(emf, Family.class);
        citizenDAO = new GenericDAO<>(emf, Citizen.class);
        try (EntityManager em = emf.createEntityManager())
        {
            em.getTransaction().begin();
            em.createNativeQuery("TRUNCATE TABLE citizen_relationship, job, education_record, citizen, company, institution, housing, family, location RESTART IDENTITY CASCADE")
                    .executeUpdate();
            em.getTransaction().commit();
        }
    }

    private Location location(String name)
    {
        Location location = new Location();
        location.setName(name);
        return location;
    }

    @Test
    @DisplayName("persists a location with a generated id")
    void persist()
    {
        // Arrange
        Location location = location("Central");

        // Act
        Location saved = locationDAO.persist(location);

        // Assert
        assertNotNull(saved.getId());
        try (EntityManager em = emf.createEntityManager())
        {
            Location row = em.find(Location.class, saved.getId());
            assertNotNull(row);
            assertEquals("Central", row.getName());
        }
    }

    @Test
    @DisplayName("merges and saves a changed location field")
    void merge()
    {
        // Arrange
        Location location = locationDAO.persist(location("Before"));
        location.setName("After");

        // Act
        locationDAO.merge(location);

        // Assert
        try (EntityManager em = emf.createEntityManager())
        {
            assertEquals("After", em.find(Location.class, location.getId()).getName());
        }
    }

    @Test
    @DisplayName("finds a location by id")
    void findByIdOrThrowFound()
    {
        // Arrange
        Location location = locationDAO.persist(location("Found"));

        // Act
        Location found = locationDAO.findByIdOrThrow(location.getId());

        // Assert
        assertEquals("Found", found.getName());
    }

    @Test
    @DisplayName("throws for an unknown location id")
    void findByIdOrThrowUnknown()
    {
        // Arrange / Act / Assert
        assertThrows(NotFoundException.class, () -> locationDAO.findByIdOrThrow(999L));
    }

    @Test
    @DisplayName("rejects a null location id")
    void findByIdOrThrowNull()
    {
        // Arrange / Act / Assert
        assertThrows(IllegalArgumentException.class, () -> locationDAO.findByIdOrThrow(null));
    }

    @Test
    @DisplayName("returns no rows when the table is empty")
    void findAllEmpty()
    {
        // Arrange / Act
        var rows = locationDAO.findAll();

        // Assert
        assertEquals(0, rows.size());
    }

    @Test
    @DisplayName("returns all persisted rows")
    void findAll()
    {
        // Arrange
        locationDAO.persist(location("One"));
        locationDAO.persist(location("Two"));

        // Act
        var rows = locationDAO.findAll();

        // Assert
        assertEquals(2, rows.size());
    }

    @Test
    @DisplayName("deletes a location row")
    void delete()
    {
        // Arrange
        Location location = locationDAO.persist(location("Remove"));

        // Act
        locationDAO.delete(location.getId());

        // Assert
        try (EntityManager em = emf.createEntityManager())
        {
            assertNull(em.find(Location.class, location.getId()));
        }
    }

    @Test
    @DisplayName("throws when deleting an unknown location")
    void deleteUnknown()
    {
        // Arrange / Act / Assert
        assertThrows(NotFoundException.class, () -> locationDAO.delete(999L));
    }

    @Test
    @DisplayName("counts only families matching the requested field")
    void countWhere()
    {
        // Arrange
        Family smith = new Family();
        smith.setFamilyName("Smith");
        Family jones = new Family();
        jones.setFamilyName("Jones");
        familyDAO.persist(smith);
        familyDAO.persist(jones);
        Family anotherSmith = new Family();
        anotherSmith.setFamilyName("Smith");
        familyDAO.persist(anotherSmith);

        // Act
        long count = familyDAO.countWhere("familyName", "Smith");

        // Assert
        assertEquals(2L, count);
    }

    @Test
    @DisplayName("rejects a citizen with no family")
    void persistCitizenWithoutFamily()
    {
        // Arrange
        Citizen citizen = new Citizen();
        citizen.setFirstName("Alex");
        citizen.setLastName("Smith");

        // Act / Assert
        assertThrows(PersistenceException.class, () -> citizenDAO.persist(citizen));
    }
}
