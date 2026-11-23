package app.daos;

import app.config.HibernateConfig;
import app.dtos.PageRequest;
import app.entities.Citizen;
import app.entities.CitizenRelationship;
import app.entities.Company;
import app.entities.EducationRecord;
import app.entities.Family;
import app.entities.Housing;
import app.entities.Institution;
import app.entities.Job;
import app.entities.Location;
import app.exceptions.NotFoundException;
import app.services.CitizenRelationshipService;
import app.services.CitizenService;
import app.services.CompanyService;
import app.services.EducationRecordService;
import app.services.FamilyService;
import app.services.HousingService;
import app.services.InstitutionService;
import app.services.JobService;
import app.services.LocationService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.time.LocalDate;

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
    private GenericDAO<Housing> housingDAO;

    @BeforeEach
    void setUp()
    {
        emf = HibernateConfig.getEntityManagerFactoryForTest();
        locationDAO = new GenericDAO<>(emf, Location.class);
        familyDAO = new GenericDAO<>(emf, Family.class);
        citizenDAO = new GenericDAO<>(emf, Citizen.class);
        housingDAO = new GenericDAO<>(emf, Housing.class);
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

    @Test
    @DisplayName("rolls back all entities when a batch persist fails")
    void persistAllRollsBackOnFailure()
    {
        // Arrange
        Family family = new Family();
        family.setFamilyName("Smith");
        familyDAO.persist(family);

        Citizen validCitizen = new Citizen();
        validCitizen.setFirstName("Alex");
        validCitizen.setLastName("Smith");
        validCitizen.setFamily(family);

        Citizen invalidCitizen = new Citizen();
        invalidCitizen.setFirstName("Jamie");
        invalidCitizen.setLastName("Smith");

        // Arrange (add at the end of the existing arrange part)
        List<Citizen> batch = List.of(validCitizen, invalidCitizen);

        // Act / Assert
        assertThrows(PersistenceException.class, () -> citizenDAO.persistAll(batch));
    }

    @Test
    @DisplayName("returns an empty result for an empty merge batch")
    void mergeAllEntitiesEmpty()
    {
        // Arrange

        // Act
        List<Location> merged = locationDAO.mergeAllEntities(List.of());

        // Assert
        assertEquals(0, merged.size());
    }

    @Test
    @DisplayName("rolls back all entities when a batch merge fails")
    void mergeAllEntitiesRollsBackOnFailure()
    {
        // Arrange
        Location validLocation = location("Batch location");
        Citizen invalidCitizen = new Citizen();
        invalidCitizen.setFirstName("Alex");
        invalidCitizen.setLastName("Smith");

        // Arrange (add at the end of the existing arrange part)
        List<Object> batch = List.of(validLocation, invalidCitizen);

        // Act / Assert
        assertThrows(PersistenceException.class, () -> locationDAO.mergeAllEntities(batch));
    }

    @Test
    @DisplayName("rejects a null id when deleting")
    void deleteNull()
    {
        // Arrange / Act / Assert
        assertThrows(IllegalArgumentException.class, () -> locationDAO.delete(null));
    }

    @Test
    @DisplayName("pages filtered citizens and reports the total matching count")
    void findPageAndCountFiltered()
    {
        Family family = new Family();
        family.setFamilyName("Page test");
        family = familyDAO.persist(family);
        citizenDAO.persist(citizen("Alex", "Smith", LocalDate.of(1990, 1, 1), family));
        citizenDAO.persist(citizen("Jamie", "Smithson", LocalDate.of(1995, 6, 1), family));
        citizenDAO.persist(citizen("Taylor", "Jones", LocalDate.of(1995, 6, 1), family));

        Map<String, String> allowedFilters = Map.of(
                "lastName", "lastName",
                "birthDateFrom", "birthDate",
                "birthDateTo", "birthDate"
        );
        Map<String, String> filters = Map.of(
                "lastName", "SMITH",
                "birthDateFrom", "1990-01-01",
                "birthDateTo", "1999-12-31"
        );

        List<Citizen> firstPage = citizenDAO.findPage(
                0, 1, "id", "asc", filters, allowedFilters, Map.of("id", "id"));
        List<Citizen> secondPage = citizenDAO.findPage(
                1, 1, "id", "asc", filters, allowedFilters, Map.of("id", "id"));
        List<Citizen> beyondLastPage = citizenDAO.findPage(
                3, 1, "id", "asc", filters, allowedFilters, Map.of("id", "id"));

        assertEquals(2L, citizenDAO.countFiltered(filters, allowedFilters));
        assertEquals(1, firstPage.size());
        assertEquals("Alex", firstPage.get(0).getFirstName());
        assertEquals("Jamie", secondPage.get(0).getFirstName());
        assertEquals(0, beyondLastPage.size());
    }

    @Test
    @DisplayName("sorts by an allowlisted relationship id and uses the entity id as a tie-breaker")
    void findPageSortsByRelationshipId()
    {
        Family earlierFamily = new Family();
        earlierFamily.setFamilyName("A");
        earlierFamily = familyDAO.persist(earlierFamily);
        Family laterFamily = new Family();
        laterFamily.setFamilyName("B");
        laterFamily = familyDAO.persist(laterFamily);

        Citizen firstInserted = citizenDAO.persist(
                citizen("Later family", "Smith", LocalDate.of(1990, 1, 1), laterFamily));
        Citizen secondInserted = citizenDAO.persist(
                citizen("Earlier family 1", "Smith", LocalDate.of(1990, 1, 1), earlierFamily));
        Citizen thirdInserted = citizenDAO.persist(
                citizen("Earlier family 2", "Smith", LocalDate.of(1990, 1, 1), earlierFamily));

        List<Citizen> rows = citizenDAO.findPage(
                0, 3, "familyId", "asc", Map.of(), Map.of(),
                Map.of("familyId", "family.id"));

        assertEquals(List.of(secondInserted.getId(), thirdInserted.getId(), firstInserted.getId()),
                rows.stream().map(Citizen::getId).toList());

        List<Citizen> descendingRows = citizenDAO.findPage(
                0, 3, "familyId", "desc", Map.of(), Map.of(),
                Map.of("familyId", "family.id"));
        assertEquals(List.of(firstInserted.getId(), secondInserted.getId(), thirdInserted.getId()),
                descendingRows.stream().map(Citizen::getId).toList());
    }

    @Test
    @DisplayName("rejects unsupported sort and filter fields, including blank filters")
    void rejectsUnsupportedPageFields()
    {
        assertThrows(IllegalArgumentException.class, () ->
                citizenDAO.findPage(0, 10, "unknown", "asc", Map.of(), Map.of(),
                        Map.of("id", "id")));
        assertThrows(IllegalArgumentException.class, () ->
                citizenDAO.countFiltered(Map.of("unknown", ""), Map.of()));
        assertThrows(IllegalArgumentException.class, () ->
                citizenDAO.findPage(0, 10, "id", "asc", Map.of(), Map.of(),
                        Map.of("id", "family.unknown")));
    }

    @Test
    @DisplayName("rejects malformed dates and range filters on string attributes")
    void rejectsInvalidFilterValues()
    {
        Family family = new Family();
        family.setFamilyName("Invalid values");
        familyDAO.persist(family);

        assertThrows(IllegalArgumentException.class, () ->
                citizenDAO.countFiltered(
                        Map.of("birthDateFrom", "not-a-date"),
                        Map.of("birthDateFrom", "birthDate")));
        assertThrows(IllegalArgumentException.class, () ->
                citizenDAO.countFiltered(
                        Map.of("lastNameFrom", "A"),
                        Map.of("lastNameFrom", "lastName")));
    }

    @Test
    @DisplayName("all CRUD services map their allowlisted sort fields to valid entity paths")
    void allServiceSortFieldsResolveAgainstTheMetamodel()
    {
        GenericDAO<Company> companyDAO = new GenericDAO<>(emf, Company.class);
        GenericDAO<Institution> institutionDAO = new GenericDAO<>(emf, Institution.class);
        GenericDAO<Job> jobDAO = new GenericDAO<>(emf, Job.class);
        GenericDAO<EducationRecord> educationDAO = new GenericDAO<>(emf, EducationRecord.class);
        GenericDAO<CitizenRelationship> relationshipDAO = new GenericDAO<>(emf, CitizenRelationship.class);

        assertSortable(new FamilyService(familyDAO), "id", "familyName");
        assertSortable(new LocationService(locationDAO), "id", "name", "streetAddress");
        assertSortable(new HousingService(housingDAO, locationDAO),
                "id", "locationId", "housingType", "capacity");
        assertSortable(new CitizenService(citizenDAO, familyDAO, housingDAO),
                "id", "firstName", "lastName", "birthDate", "deathDate", "status",
                "gender", "familyId", "housingId");
        assertSortable(new CitizenRelationshipService(relationshipDAO, citizenDAO),
                "id", "citizenId1", "citizenId2", "relationshipType", "startDate", "endDate");
        assertSortable(new InstitutionService(institutionDAO, locationDAO),
                "id", "locationId", "name", "type");
        assertSortable(new CompanyService(companyDAO, locationDAO),
                "id", "locationId", "name", "foundedDate", "dissolvedDate", "status");
        assertSortable(new EducationRecordService(educationDAO, citizenDAO, institutionDAO),
                "id", "citizenId", "institutionId", "startDate", "endDate");
        assertSortable(new JobService(jobDAO, citizenDAO, companyDAO, institutionDAO),
                "id", "citizenId", "companyId", "institutionId", "title", "salary",
                "startDate", "endDate", "status");
    }

    @Test
    @DisplayName("combines numeric range filters with relationship ID filters")
    void filtersNumericRangesAndRelationshipIds()
    {
        Location location = locationDAO.persist(location("Range test"));
        housingDAO.persist(housing(location, 2));
        housingDAO.persist(housing(location, 4));
        housingDAO.persist(housing(location, 6));

        Map<String, String> allowedFilters = Map.of(
                "capacityFrom", "capacity",
                "capacityTo", "capacity",
                "locationId", "location.id"
        );
        Map<String, String> filters = Map.of(
                "capacityFrom", "3",
                "capacityTo", "5",
                "locationId", location.getId().toString()
        );

        List<Housing> rows = housingDAO.findPage(
                0, 10, "capacity", "desc", filters, allowedFilters,
                Map.of("capacity", "capacity"));

        assertEquals(1L, housingDAO.countFiltered(filters, allowedFilters));
        assertEquals(List.of(4), rows.stream().map(Housing::getCapacity).toList());
        assertThrows(IllegalArgumentException.class, () ->
                housingDAO.countFiltered(
                        Map.of("capacityFrom", "not-a-number"),
                        Map.of("capacityFrom", "capacity")));
    }

    private Citizen citizen(String firstName, String lastName, LocalDate birthDate, Family family)
    {
        Citizen citizen = new Citizen();
        citizen.setFirstName(firstName);
        citizen.setLastName(lastName);
        citizen.setBirthDate(birthDate);
        citizen.setFamily(family);
        return citizen;
    }

    private Housing housing(Location location, int capacity)
    {
        Housing housing = new Housing();
        housing.setLocation(location);
        housing.setHousingType("house");
        housing.setCapacity(capacity);
        return housing;
    }

    private void assertSortable(app.services.CrudService<?, ?> service, String... fields)
    {
        for (String field : fields) {
            service.getAll(new PageRequest(0, 10, field, "asc", Map.of()));
        }
    }
}
