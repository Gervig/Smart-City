package migrator.graph;

import migrator.SampleData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GraphMapperTest
{
    private final List<GraphMapper.Step> steps = GraphMapper.map(SampleData.snapshot());

    @Test
    @DisplayName("every node step comes before every relationship step")
    void nodesFirst()
    {
        // Arrange

        // Act
        int lastNodeStep = 0;
        int firstRelationshipStep = Integer.MAX_VALUE;
        for (int i = 0; i < steps.size(); i++)
        {
            if (steps.get(i).name().endsWith("nodes")) lastNodeStep = i;
            else firstRelationshipStep = Math.min(firstRelationshipStep, i);
        }

        // Assert
        assertTrue(lastNodeStep < firstRelationshipStep);
    }

    @Test
    @DisplayName("one node per row, and each role only once")
    void nodeCounts()
    {
        // Arrange

        // Act
        Map<String, Integer> counts = Map.of("Location", rows("Location nodes").size(),
                "Citizen", rows("Citizen nodes").size(), "User", rows("User nodes").size(),
                "Role", rows("Role nodes").size());

        // Assert
        assertEquals(Map.of("Location", 2, "Citizen", 3, "User", 2, "Role", 2), counts);
    }

    @Test
    @DisplayName("a job becomes WORKS_AT to the company or the institution, with the salary as a number")
    void jobsBecomeWorksAt()
    {
        // Arrange

        // Act
        List<Map<String, Object>> atCompany = rows("WORKS_AT Citizen->Company");
        List<Map<String, Object>> atInstitution = rows("WORKS_AT Citizen->Institution");

        // Assert
        assertEquals(1, atCompany.size());
        assertEquals(1L, atCompany.get(0).get("from"));
        assertEquals(1L, atCompany.get(0).get("to"));
        Map<?, ?> properties = (Map<?, ?>) atCompany.get(0).get("properties");
        assertEquals("Developer", properties.get("title"));
        assertEquals(42000.5, properties.get("salary"));
        assertEquals(1, atInstitution.size());
        assertEquals(2L, atInstitution.get(0).get("from"));
    }

    @Test
    @DisplayName("LIVES_IN is only created for citizens with a housing")
    void livesInSkipsHomeless()
    {
        // Arrange

        // Act
        List<Map<String, Object>> livesIn = rows("LIVES_IN Citizen->Housing");

        // Assert
        assertEquals(2, livesIn.size());
        assertTrue(livesIn.stream().noneMatch(row -> row.get("from").equals(3L)));
    }

    @Test
    @DisplayName("null columns are not stored as properties")
    void nullsNotStored()
    {
        // Arrange

        // Act
        Map<String, Object> carl = rows("Citizen nodes").get(2);

        // Assert
        assertEquals("Carl", carl.get("firstName"));
        assertFalse(carl.containsKey("deathDate"));
    }

    @Test
    @DisplayName("values go in as parameters, not into the Cypher text")
    void parametersOnly()
    {
        // Arrange

        // Act
        boolean anyValueInCypher = steps.stream().anyMatch(step -> step.cypher().contains("Jensen"));

        // Assert
        assertFalse(anyValueInCypher);
        assertTrue(steps.stream().allMatch(step -> step.cypher().contains("$rows")));
    }

    private List<Map<String, Object>> rows(String stepName)
    {
        return steps.stream().filter(s -> s.name().equals(stepName)).findFirst().orElseThrow().rows();
    }
}
