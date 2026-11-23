package app.services;

import app.daos.GenericDAO;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Business rule, not yet in the SRS
 * <p>
 * Mocks every DAO and the unmanaged Overpass client; SeedService and its generators are real for deterministic unit coverage.
 */
@ExtendWith(MockitoExtension.class)
class SeedServiceTest
{
    @Mock
    private GenericDAO<Location> locationDAO;
    @Mock
    private GenericDAO<Family> familyDAO;
    @Mock
    private GenericDAO<Housing> housingDAO;
    @Mock
    private GenericDAO<Citizen> citizenDAO;
    @Mock
    private GenericDAO<Institution> institutionDAO;
    @Mock
    private GenericDAO<Company> companyDAO;
    @Mock
    private GenericDAO<EducationRecord> educationRecordDAO;
    @Mock
    private GenericDAO<Job> jobDAO;
    @Mock
    private GenericDAO<CitizenRelationship> citizenRelationshipDAO;
    @Mock
    private OverpassImportClient overpassClient;
    @Captor
    private ArgumentCaptor<List<Location>> locationsCaptor;
    @Captor
    private ArgumentCaptor<List<Citizen>> citizensCaptor;

    private SeedService service;

    @BeforeEach
    void setUp()
    {
        service = new SeedService(locationDAO, familyDAO, housingDAO, citizenDAO, institutionDAO, companyDAO,
                educationRecordDAO, jobDAO, citizenRelationshipDAO, overpassClient, new Random(7));
        lenient().when(locationDAO.mergeAllEntities(anyList())).thenAnswer(invocation ->
                invocation.getArgument(0));
    }

    @Test
    @DisplayName("imports named locations and skips existing OSM ids")
    void seedLocationsFromOsm()
    {
        // Arrange
        Location existing = location(1L, "node", "Already imported");
        when(locationDAO.findAll()).thenReturn(List.of(existing));
        when(overpassClient.findInArea(55.66, 12.55, 55.70, 12.62, "hospital", 20)).thenReturn(List.of(
                element(1L, "node", "Duplicate", Map.of()),
                element(2L, "way", "New Hospital", Map.of("addr:street", "Main Street", "addr:housenumber", "4")),
                element(3L, "node", null, Map.of()),
                element(4L, "node", " ", Map.of())));

        // Act
        SeedReport report = service.seedLocationsFromOsm(55.66, 12.55, 55.70, 12.62, "hospital", 20);

        // Assert
        assertEquals(1, report.counts().get("imported"));
        assertEquals(3, report.counts().get("skipped"));
        verify(locationDAO).persistAll(locationsCaptor.capture());
        assertEquals("Main Street 4", locationsCaptor.getValue().get(0).getStreetAddress());
        assertEquals(2L, locationsCaptor.getValue().get(0).getOsmId());
    }

    @Test
    @DisplayName("imports locations when existing rows have incomplete OSM identity")
    void seedLocationsFromOsmIncompleteExistingIdentity()
    {
        // Arrange
        Location missingId = new Location();
        missingId.setOsmType("node");
        Location missingType = new Location();
        missingType.setOsmId(2L);
        when(locationDAO.findAll()).thenReturn(List.of(missingId, missingType));
        when(overpassClient.findInArea(55.66, 12.55, 55.70, 12.62, "hospital", 20)).thenReturn(List.of(
                element(1L, "node", "First", Map.of()),
                element(2L, "way", "Second", Map.of())));

        // Act
        SeedReport report = service.seedLocationsFromOsm(55.66, 12.55, 55.70, 12.62, "hospital", 20);

        // Assert
        assertEquals(2, report.counts().get("imported"));
        assertEquals(0, report.counts().get("skipped"));
        verify(locationDAO).persistAll(locationsCaptor.capture());
        assertEquals(List.of(1L, 2L), locationsCaptor.getValue().stream().map(Location::getOsmId).toList());
    }

    @Test
    @DisplayName("formats street addresses with optional streets and house numbers")
    void seedLocationsFromOsmStreetAddress()
    {
        // Arrange
        when(locationDAO.findAll()).thenReturn(List.of());
        when(overpassClient.findInArea(55.66, 12.55, 55.70, 12.62, "hospital", 20)).thenReturn(List.of(
                element(1L, "node", "No address", Map.of()),
                element(2L, "node", "Number only", Map.of("addr:housenumber", "8")),
                element(3L, "node", "Street only", Map.of("addr:street", "Main Street")),
                element(4L, "node", "Blank address", Map.of("addr:street", " ", "addr:housenumber", " ")),
                element(5L, "node", "Blank street", Map.of("addr:street", " ", "addr:housenumber", "9"))));

        // Act
        service.seedLocationsFromOsm(55.66, 12.55, 55.70, 12.62, "hospital", 20);

        // Assert
        verify(locationDAO).persistAll(locationsCaptor.capture());
        List<Location> locations = locationsCaptor.getValue();
        assertNull(locations.get(0).getStreetAddress());
        assertEquals("8", locations.get(1).getStreetAddress());
        assertEquals("Main Street", locations.get(2).getStreetAddress());
        assertNull(locations.get(3).getStreetAddress());
        assertEquals("9", locations.get(4).getStreetAddress());
    }

    @Test
    @DisplayName("rejects fake citizen counts outside the allowed range")
    void seedFakeDataInvalidCount()
    {
        // Arrange / Act / Assert
        assertThrows(IllegalArgumentException.class, () -> service.seedFakeData(0, 42));
        assertThrows(IllegalArgumentException.class, () -> service.seedFakeData(1001, 42));
        verify(locationDAO, never()).findAll();
    }

    @Test
    @DisplayName("requires imported locations before generating fake data")
    void seedFakeDataWithoutLocations()
    {
        // Arrange
        when(locationDAO.findAll()).thenReturn(List.of());

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.seedFakeData(10, 42));

        // Assert
        assertEquals("no locations: run the OSM import first", exception.getMessage());
    }

    @Test
    @DisplayName("leaves citizens unhoused when all generated housing is full")
    void seedFakeDataWhenHousingIsFull()
    {
        // Arrange
        when(locationDAO.findAll()).thenReturn(List.of(location(1L, "node", "Central")));
        Random alwaysFirstAndAlive = new Random(7)
        {
            @Override
            public int nextInt(int bound)
            {
                return 0;
            }

            @Override
            public boolean nextBoolean()
            {
                return false;
            }
        };
        service = new SeedService(locationDAO, familyDAO, housingDAO, citizenDAO, institutionDAO, companyDAO,
                educationRecordDAO, jobDAO, citizenRelationshipDAO, overpassClient, alwaysFirstAndAlive);

        // Act
        SeedReport report = service.seedFakeData(10, 42);

        // Assert
        assertTrue(report.completed());
        verify(citizenDAO).persistAll(citizensCaptor.capture());
        assertTrue(citizensCaptor.getValue().stream().anyMatch(citizen -> citizen.getHousing() == null));
    }

    @Test
    @DisplayName("fills only missing institution contacts")
    void enrichFromOsmDoesNotOverwrite()
    {
        // Arrange
        Location location = location(10L, "node", "School");
        Institution institution = new Institution();
        institution.setLocation(location);
        institution.setWebsite("existing.example");
        when(institutionDAO.findAllWithLocation()).thenReturn(List.of(institution));
        when(companyDAO.findAllWithLocation()).thenReturn(List.of());
        when(housingDAO.findAllWithLocation()).thenReturn(List.of());
        when(overpassClient.findByIds("node", List.of(10L))).thenReturn(List.of(element(10L, "node", "School",
                Map.of("website", "new.example", "contact:phone", "123", "email", "school@example.test"))));

        // Act
        SeedReport report = service.enrichFromOsm();

        // Assert
        assertTrue(report.completed());
        assertEquals("existing.example", institution.getWebsite());
        assertEquals("123", institution.getPhone());
        assertEquals("school@example.test", institution.getEmail());
        verify(locationDAO).mergeAllEntities(anyList());
    }

    @Test
    @DisplayName("stops after the second failed batch and keeps the first batch updated")
    void enrichFromOsmSecondBatchFails()
    {
        // Arrange
        List<Institution> institutions = new ArrayList<>();
        for (long id = 1; id <= 51; id++)
        {
            Institution institution = new Institution();
            institution.setLocation(location(id, "node", "Institution " + id));
            institutions.add(institution);
        }
        when(institutionDAO.findAllWithLocation()).thenReturn(institutions);
        when(companyDAO.findAllWithLocation()).thenReturn(List.of());
        when(housingDAO.findAllWithLocation()).thenReturn(List.of());
        List<OsmElement> firstBatch = LongStream.rangeClosed(1, 50)
                .mapToObj(id -> element(id, "node", "Institution", Map.of("phone", "555"))).toList();
        when(overpassClient.findByIds("node", LongStream.rangeClosed(1, 50).boxed().toList())).thenReturn(firstBatch);
        when(overpassClient.findByIds("node", List.of(51L))).thenThrow(new OverpassException("server unavailable"));

        // Act
        SeedReport report = service.enrichFromOsm();

        // Assert
        assertFalse(report.completed());
        assertEquals(50, report.counts().get("updated"));
        assertEquals("555", institutions.get(0).getPhone());
        assertNull(institutions.get(50).getPhone());
        verify(locationDAO).mergeAllEntities(anyList());
    }

    @Test
    @DisplayName("skips invalid numeric tags without losing valid housing enrichment")
    void enrichFromOsmInvalidNumbers()
    {
        // Arrange
        Housing housing = new Housing();
        housing.setLocation(location(20L, "way", "Building"));
        when(institutionDAO.findAllWithLocation()).thenReturn(List.of());
        when(companyDAO.findAllWithLocation()).thenReturn(List.of());
        when(housingDAO.findAllWithLocation()).thenReturn(List.of(housing));
        when(overpassClient.findByIds("way", List.of(20L))).thenReturn(List.of(element(20L, "way", "Building",
                Map.of("building:levels", "unknown", "building:flats", "5", "height", "too high"))));

        // Act
        SeedReport report = service.enrichFromOsm();

        // Assert
        assertTrue(report.completed());
        assertNull(housing.getNumberOfLevels());
        assertEquals(5, housing.getNumberOfFlats());
        assertNull(housing.getHeight());
    }

    @Test
    @DisplayName("enriches company fields and skips unchanged or missing OSM elements")
    void enrichFromOsmCompanies()
    {
        // Arrange
        Company enriched = new Company();
        enriched.setLocation(location(30L, "way", "Company"));
        Company unchanged = new Company();
        unchanged.setLocation(location(31L, "way", "Existing company"));
        unchanged.setWebsite("existing.example");
        Company missingElement = new Company();
        missingElement.setLocation(location(32L, "way", "Not returned"));
        when(institutionDAO.findAllWithLocation()).thenReturn(List.of());
        when(companyDAO.findAllWithLocation()).thenReturn(List.of(enriched, unchanged, missingElement));
        when(housingDAO.findAllWithLocation()).thenReturn(List.of());
        when(overpassClient.findByIds("way", List.of(30L, 31L, 32L))).thenReturn(List.of(
                element(30L, "way", "Company", Map.of(
                        "contact:website", "company.example", "phone", "555-0100", "email", "info@example.test",
                        "amenity", " ",
                        "ref:DK:cvr", "12345678", "ref:DK:pnr", "87654321", "shop", "cafe",
                        "opening_hours", "Mo-Fr 09:00-17:00")),
                element(31L, "way", "Existing company", Map.of("opening_hours", " "))));

        // Act
        SeedReport report = service.enrichFromOsm();

        // Assert
        assertEquals(1, report.counts().get("updated"));
        assertEquals("company.example", enriched.getWebsite());
        assertEquals("555-0100", enriched.getPhone());
        assertEquals("info@example.test", enriched.getEmail());
        assertEquals("12345678", enriched.getCvrNumber());
        assertEquals("87654321", enriched.getProductionUnitNumber());
        assertEquals("cafe", enriched.getCategory());
        assertEquals("Mo-Fr 09:00-17:00", enriched.getOpeningHours());
        assertEquals("existing.example", unchanged.getWebsite());
        verify(locationDAO).mergeAllEntities(anyList());
    }

    @Test
    @DisplayName("ignores enrichment targets without complete OSM identity")
    void enrichFromOsmMissingIdentity()
    {
        // Arrange
        Institution noLocation = new Institution();
        Company noId = new Company();
        noId.setLocation(new Location());
        Housing noType = new Housing();
        noType.setLocation(location(40L, null, "Missing type"));
        when(institutionDAO.findAllWithLocation()).thenReturn(List.of(noLocation));
        when(companyDAO.findAllWithLocation()).thenReturn(List.of(noId));
        when(housingDAO.findAllWithLocation()).thenReturn(List.of(noType));

        // Act
        SeedReport report = service.enrichFromOsm();

        // Assert
        assertTrue(report.completed());
        assertEquals(0, report.counts().get("updated"));
        verify(overpassClient, never()).findByIds(org.mockito.ArgumentMatchers.anyString(), anyList());
    }

    @Test
    @DisplayName("parses valid and invalid housing measurements")
    void enrichFromOsmHousingMeasurements()
    {
        // Arrange
        Housing validMeasurements = new Housing();
        validMeasurements.setLocation(location(50L, "way", "Valid"));
        Housing invalidMeasurements = new Housing();
        invalidMeasurements.setLocation(location(51L, "way", "Invalid"));
        Housing missingMeasurements = new Housing();
        missingMeasurements.setLocation(location(52L, "way", "Missing"));
        Housing nonFiniteHeight = new Housing();
        nonFiniteHeight.setLocation(location(54L, "way", "Non-finite"));
        Housing existingMeasurements = new Housing();
        existingMeasurements.setLocation(location(53L, "way", "Existing"));
        existingMeasurements.setNumberOfLevels(7);
        existingMeasurements.setNumberOfFlats(8);
        existingMeasurements.setHeight(9.5);
        when(institutionDAO.findAllWithLocation()).thenReturn(List.of());
        when(companyDAO.findAllWithLocation()).thenReturn(List.of());
        when(housingDAO.findAllWithLocation()).thenReturn(List.of(
                validMeasurements, invalidMeasurements, missingMeasurements, existingMeasurements, nonFiniteHeight));
        when(overpassClient.findByIds("way", List.of(50L, 51L, 52L, 53L, 54L))).thenReturn(List.of(
                element(50L, "way", "Valid", Map.of("building:levels", "3", "building:flats", "6", "height", "12.5")),
                element(51L, "way", "Invalid", Map.of("building:levels", "-1", "building:flats", "-2", "height", "-2.5")),
                element(52L, "way", "Missing", Map.of()),
                element(53L, "way", "Existing", Map.of("building:levels", "2", "building:flats", "4", "height", "5.5")),
                element(54L, "way", "Non-finite", Map.of("height", "NaN"))));

        // Act
        SeedReport report = service.enrichFromOsm();

        // Assert
        assertEquals(1, report.counts().get("updated"));
        assertEquals(3, validMeasurements.getNumberOfLevels());
        assertEquals(6, validMeasurements.getNumberOfFlats());
        assertEquals(12.5, validMeasurements.getHeight());
        assertNull(invalidMeasurements.getNumberOfLevels());
        assertNull(invalidMeasurements.getNumberOfFlats());
        assertNull(invalidMeasurements.getHeight());
        assertNull(missingMeasurements.getHeight());
        assertEquals(7, existingMeasurements.getNumberOfLevels());
        assertEquals(8, existingMeasurements.getNumberOfFlats());
        assertEquals(9.5, existingMeasurements.getHeight());
        assertNull(nonFiniteHeight.getHeight());
    }

    private Location location(long osmId, String osmType, String name)
    {
        Location location = new Location();
        location.setOsmId(osmId);
        location.setOsmType(osmType);
        location.setName(name);
        return location;
    }

    private OsmElement element(long id, String type, String name, Map<String, String> tags)
    {
        return new OsmElement(id, type, name, 55.67, 12.56, tags);
    }
}
