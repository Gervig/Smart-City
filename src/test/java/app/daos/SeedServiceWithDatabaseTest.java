package app.daos;

import app.config.HibernateConfig;
import app.entities.Citizen;
import app.entities.CitizenRelationship;
import app.entities.Company;
import app.entities.EducationRecord;
import app.entities.Family;
import app.entities.Housing;
import app.entities.Institution;
import app.entities.Job;
import app.entities.Location;
import app.external.overpass.OsmElement;
import app.external.overpass.OverpassException;
import app.external.overpass.OverpassImportClient;
import app.services.SeedReport;
import app.services.SeedService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Business rule, not yet in the SRS
 * <p>
 * Uses real PostgreSQL, Hibernate and DAOs; only the unmanaged Overpass service is a hand-written fake to keep tests offline.
 */
class SeedServiceWithDatabaseTest
{
    private static EntityManagerFactory emf;
    private GenericDAO<Location> locationDAO;
    private GenericDAO<Family> familyDAO;
    private GenericDAO<Housing> housingDAO;
    private GenericDAO<Citizen> citizenDAO;
    private GenericDAO<Institution> institutionDAO;
    private GenericDAO<Company> companyDAO;
    private GenericDAO<EducationRecord> educationRecordDAO;
    private GenericDAO<Job> jobDAO;
    private GenericDAO<CitizenRelationship> citizenRelationshipDAO;

    @BeforeAll
    static void initDatabase()
    {
        emf = HibernateConfig.getEntityManagerFactoryForTest();
    }

    @BeforeEach
    void setUp()
    {
        locationDAO = new GenericDAO<>(emf, Location.class);
        familyDAO = new GenericDAO<>(emf, Family.class);
        housingDAO = new GenericDAO<>(emf, Housing.class);
        citizenDAO = new GenericDAO<>(emf, Citizen.class);
        institutionDAO = new GenericDAO<>(emf, Institution.class);
        companyDAO = new GenericDAO<>(emf, Company.class);
        educationRecordDAO = new GenericDAO<>(emf, EducationRecord.class);
        jobDAO = new GenericDAO<>(emf, Job.class);
        citizenRelationshipDAO = new GenericDAO<>(emf, CitizenRelationship.class);
        try (EntityManager em = emf.createEntityManager())
        {
            em.getTransaction().begin();
            em.createNativeQuery("TRUNCATE TABLE citizen_relationship, job, education_record, citizen, company, institution, housing, family, location RESTART IDENTITY CASCADE")
                    .executeUpdate();
            em.getTransaction().commit();
        }
    }

    @Test
    @DisplayName("imports the same OSM locations idempotently")
    void seedLocationsFromOsmTwice()
    {
        // Arrange
        FakeOverpassClient overpass = new FakeOverpassClient();
        overpass.areaResult = List.of(element(1, "node", "Hospital"), element(2, "way", "Clinic"));
        SeedService service = service(overpass);

        // Act
        SeedReport first = service.seedLocationsFromOsm(55.66, 12.55, 55.70, 12.62, "hospital", 20);
        SeedReport second = service.seedLocationsFromOsm(55.66, 12.55, 55.70, 12.62, "hospital", 20);

        // Assert
        assertEquals(2, first.counts().get("imported"));
        assertEquals(0, second.counts().get("imported"));
        assertEquals(2, locationDAO.findAll().size());
    }

    @Test
    @DisplayName("seeds valid related data without exceeding housing capacity")
    void seedFakeData()
    {
        // Arrange
        locationDAO.persist(location(100, "node", "Seed location"));
        SeedService service = service(new FakeOverpassClient());

        // Act
        SeedReport report = service.seedFakeData(50, 1);

        // Assert
        assertEquals(50, report.counts().get("citizens"));
        assertEquals(50, citizenDAO.findAll().size());
        assertFalse(familyDAO.findAll().isEmpty());
        assertFalse(housingDAO.findAll().isEmpty());
        assertFalse(institutionDAO.findAll().isEmpty());
        assertFalse(companyDAO.findAll().isEmpty());
        assertEquals(50, educationRecordDAO.findAll().size());
        assertEquals(50, jobDAO.findAll().size());
        assertTrue(locationDAO.findAll().stream().allMatch(location -> location.getName() != null
                && !location.getName().isBlank()));
        assertTrue(familyDAO.findAll().stream().allMatch(family -> family.getFamilyName() != null
                && !family.getFamilyName().isBlank()));
        assertTrue(institutionDAO.findAll().stream().allMatch(institution ->
                List.of("high_school", "college", "hospital").contains(institution.getType())));

        for (Housing housing : housingDAO.findAll())
        {
            assertTrue(citizenDAO.countWhere("housing.id", housing.getId()) <= housing.getCapacity());
            assertTrue(List.of("house", "apartment").contains(housing.getHousingType()));
            assertTrue(housing.getCapacity() >= 1 && housing.getCapacity() <= 6);
        }
        for (Citizen citizen : citizenDAO.findAll())
        {
            assertNotNull(citizen.getFamily());
            if ("dead".equals(citizen.getStatus()))
            {
                assertNotNull(citizen.getDeathDate());
                assertFalse(citizen.getDeathDate().isBefore(citizen.getBirthDate()));
                assertNull(citizen.getHousing());
            }
            else
            {
                assertEquals("alive", citizen.getStatus());
                assertNull(citizen.getDeathDate());
            }
        }
        for (Company company : companyDAO.findAll())
        {
            if ("dissolved".equals(company.getStatus()))
            {
                assertNotNull(company.getDissolvedDate());
                assertFalse(company.getDissolvedDate().isBefore(company.getFoundedDate()));
            }
            else
            {
                assertEquals("active", company.getStatus());
                assertNull(company.getDissolvedDate());
            }
        }
        for (Job job : jobsWithCitizen())
        {
            assertNotNull(job.getStartDate());
            assertTrue(job.getSalary().compareTo(BigDecimal.ZERO) >= 0);
            assertTrue((job.getCompany() == null) ^ (job.getInstitution() == null));
            assertFalse(job.getStartDate().isBefore(job.getCitizen().getBirthDate()));
            if ("active".equals(job.getStatus()))
            {
                assertNull(job.getEndDate());
            }
            else
            {
                assertEquals("inactive", job.getStatus());
                assertNotNull(job.getEndDate());
                assertFalse(job.getEndDate().isBefore(job.getStartDate()));
            }
            if (job.getCitizen().getDeathDate() != null)
            {
                assertFalse(job.getStartDate().isAfter(job.getCitizen().getDeathDate()));
                assertNotNull(job.getEndDate());
                assertFalse(job.getEndDate().isAfter(job.getCitizen().getDeathDate()));
            }
        }
        for (EducationRecord record : educationRecordsWithCitizen())
        {
            assertFalse(record.getStartDate().isBefore(record.getCitizen().getBirthDate()));
            if (record.getEndDate() != null)
            {
                assertFalse(record.getEndDate().isBefore(record.getStartDate()));
            }
            if (record.getCitizen().getDeathDate() != null)
            {
                assertNotNull(record.getEndDate());
                assertFalse(record.getEndDate().isAfter(record.getCitizen().getDeathDate()));
            }
        }
        assertTrue(companyDAO.findAll().stream().allMatch(company -> company.getFoundedDate().getYear() >= 1950
                && company.getFoundedDate().getYear() <= 2020));
        assertTrue(citizenDAO.findAll().stream().allMatch(citizen -> citizen.getBirthDate().getYear() >= 1930
                && citizen.getBirthDate().getYear() <= 2020 && !citizen.getBirthDate().isAfter(LocalDate.now())));
    }

    @Test
    @DisplayName("fills missing institution contacts and preserves existing values")
    void enrichFromOsm()
    {
        // Arrange
        Location location = locationDAO.persist(location(300, "node", "School"));
        Institution institution = new Institution();
        institution.setLocation(location);
        institution.setName("Existing School");
        institution.setType("high_school");
        institution.setWebsite("keep.example");
        institutionDAO.persist(institution);
        FakeOverpassClient overpass = new FakeOverpassClient();
        overpass.byIds = (type, ids) -> List.of(new OsmElement(300, "node", "School", 55.67, 12.56,
                Map.of("website", "replace.example", "phone", "12345", "email", "school@example.test")));

        // Act
        SeedReport report = service(overpass).enrichFromOsm();

        // Assert
        assertTrue(report.completed());
        Institution saved = institutionDAO.findAll().get(0);
        assertEquals("keep.example", saved.getWebsite());
        assertEquals("12345", saved.getPhone());
        assertEquals("school@example.test", saved.getEmail());
    }

    @Test
    @DisplayName("keeps the first enrichment batch committed when the next batch fails")
    void enrichFromOsmSecondBatchFails()
    {
        // Arrange
        for (int i = 0; i < 51; i++)
        {
            Location location = locationDAO.persist(location(1000 + i, "node", "Place " + i));
            Institution institution = new Institution();
            institution.setLocation(location);
            institution.setName("Institution " + i);
            institution.setType("college");
            institutionDAO.persist(institution);
        }
        FakeOverpassClient overpass = new FakeOverpassClient();
        overpass.byIds = new FunctionWithCalls();

        // Act
        SeedReport report = service(overpass).enrichFromOsm();

        // Assert
        assertFalse(report.completed());
        assertEquals(50, report.counts().get("updated"));
        assertEquals(50L, institutionDAO.countWhere("phone", "555"));
        assertEquals(1L, institutionDAO.findAll().stream().filter(institution -> institution.getPhone() == null).count());
    }

    private SeedService service(OverpassImportClient client)
    {
        return new SeedService(locationDAO, familyDAO, housingDAO, citizenDAO, institutionDAO, companyDAO,
                educationRecordDAO, jobDAO, citizenRelationshipDAO, client, new Random(11));
    }

    private Location location(long osmId, String type, String name)
    {
        Location location = new Location();
        location.setOsmId(osmId);
        location.setOsmType(type);
        location.setName(name);
        location.setLatitude(55.67);
        location.setLongitude(12.56);
        return location;
    }

    private OsmElement element(long id, String type, String name)
    {
        return new OsmElement(id, type, name, 55.67, 12.56, Map.of());
    }

    private List<Job> jobsWithCitizen()
    {
        try (EntityManager em = emf.createEntityManager())
        {
            return em.createQuery("SELECT j FROM Job j JOIN FETCH j.citizen", Job.class).getResultList();
        }
    }

    private List<EducationRecord> educationRecordsWithCitizen()
    {
        try (EntityManager em = emf.createEntityManager())
        {
            return em.createQuery("SELECT e FROM EducationRecord e JOIN FETCH e.citizen", EducationRecord.class)
                    .getResultList();
        }
    }

    private static class FakeOverpassClient implements OverpassImportClient
    {
        private List<OsmElement> areaResult = List.of();
        private BiFunction<String, List<Long>, List<OsmElement>> byIds = (type, ids) -> List.of();

        @Override
        public List<OsmElement> findInArea(double south, double west, double north, double east, String amenity, int limit)
        {
            return areaResult;
        }

        @Override
        public List<OsmElement> findByIds(String osmType, List<Long> ids)
        {
            return byIds.apply(osmType, ids);
        }
    }

    private static class FunctionWithCalls implements BiFunction<String, List<Long>, List<OsmElement>>
    {
        private int calls;

        @Override
        public List<OsmElement> apply(String type, List<Long> ids)
        {
            calls++;
            if (calls == 2)
            {
                throw new OverpassException("second batch unavailable");
            }
            return ids.stream().map(id -> new OsmElement(id, type, "", 55.67, 12.56, Map.of("phone", "555"))).toList();
        }
    }
}
