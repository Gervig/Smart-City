package migrator.document;

import migrator.SampleData;
import org.bson.Document;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentMapperTest
{
    private final Map<String, List<Document>> collections = DocumentMapper.map(SampleData.snapshot());

    @Test
    @DisplayName("creates one collection per aggregate, with one document per row")
    void collections()
    {
        // Arrange

        // Act
        Map<String, Integer> sizes = Map.of(
                "locations", collections.get("locations").size(), "families", collections.get("families").size(),
                "housings", collections.get("housings").size(), "companies", collections.get("companies").size(),
                "institutions", collections.get("institutions").size(), "citizens", collections.get("citizens").size(),
                "users", collections.get("users").size());

        // Assert
        assertEquals(Map.of("locations", 2, "families", 1, "housings", 1, "companies", 1, "institutions", 1,
                "citizens", 3, "users", 2), sizes);
    }

    @Test
    @DisplayName("a citizen embeds the family, jobs with the employer, education and relationships")
    void citizenEmbedsWhatIsReadTogether()
    {
        // Arrange

        // Act
        Document anna = citizen(1);

        // Assert
        assertEquals(new Document("id", 1L).append("familyName", "Jensen"), anna.get("family"));
        assertEquals(1L, anna.get("housingId"));
        Document job = anna.getList("jobs", Document.class).get(0);
        assertEquals("Developer", job.get("title"));
        assertEquals(new BigDecimal("42000.50"), job.get("salary"));
        assertEquals(new Document("type", "company").append("id", 1L).append("name", "Acme ApS"), job.get("employer"));
        Document education = anna.getList("education", Document.class).get(0);
        assertEquals("City School", education.get("institution", Document.class).get("name"));
        Document relationship = anna.getList("relationships", Document.class).get(0);
        assertEquals("marriage", relationship.get("type"));
        assertEquals(2L, relationship.get("citizenId"));
        assertEquals("Bo Jensen", relationship.get("name"));
        assertEquals("outgoing", relationship.get("direction"));
    }

    @Test
    @DisplayName("a relationship is embedded in both citizens, each pointing at the other")
    void relationshipOnBothSides()
    {
        // Arrange

        // Act
        Document fromBo = citizen(2).getList("relationships", Document.class).get(0);

        // Assert
        assertEquals(1L, fromBo.get("citizenId"));
        assertEquals("Anna Jensen", fromBo.get("name"));
        assertEquals("incoming", fromBo.get("direction"));
    }

    @Test
    @DisplayName("a job at an institution names the institution as employer")
    void institutionEmployer()
    {
        // Arrange

        // Act
        Document job = citizen(2).getList("jobs", Document.class).get(0);

        // Assert
        assertEquals(new Document("type", "institution").append("id", 1L).append("name", "City School"),
                job.get("employer"));
    }

    @Test
    @DisplayName("null columns are left out of the document")
    void nullsLeftOut()
    {
        // Arrange

        // Act
        Document carl = citizen(3);

        // Assert
        assertFalse(carl.containsKey("housingId"));
        assertFalse(carl.containsKey("deathDate"));
        assertTrue(carl.getList("jobs", Document.class).isEmpty());
    }

    @Test
    @DisplayName("a housing embeds its location and lists its residents")
    void housingEmbedsLocation()
    {
        // Arrange

        // Act
        Document housing = collections.get("housings").get(0);

        // Assert
        assertEquals("Town Hall", housing.get("location", Document.class).get("name"));
        assertEquals(1L, housing.get("location", Document.class).get("id"));
        assertEquals(List.of(1L, 2L), housing.get("residentIds"));
        assertFalse(housing.containsKey("numberOfLevels"));
    }

    @Test
    @DisplayName("a location with coordinates gets a GeoJSON point in [longitude, latitude] order")
    void geoJsonPoint()
    {
        // Arrange

        // Act
        Document point = collections.get("locations").get(0).get("point", Document.class);

        // Assert
        assertEquals("Point", point.get("type"));
        assertEquals(List.of(12.5683, 55.6761), point.get("coordinates"));
    }

    @Test
    @DisplayName("a location without coordinates has no point")
    void noPointWithoutCoordinates()
    {
        // Arrange

        // Act
        Document school = collections.get("locations").get(1);

        // Assert
        assertFalse(school.containsKey("point"));
        assertFalse(school.containsKey("osm"));
    }

    @Test
    @DisplayName("a family lists its members, a user embeds its roles")
    void familiesAndUsers()
    {
        // Arrange

        // Act
        Document family = collections.get("families").get(0);
        Document admin = collections.get("users").get(0);

        // Assert
        assertEquals(List.of(1L, 2L, 3L), family.get("memberIds"));
        assertEquals("admin", admin.get("_id"));
        assertEquals(List.of("admin"), admin.get("roles"));
    }

    private Document citizen(long id)
    {
        return collections.get("citizens").stream().filter(d -> d.get("_id").equals(id)).findFirst().orElseThrow();
    }
}
