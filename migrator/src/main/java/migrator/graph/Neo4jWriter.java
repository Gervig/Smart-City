package migrator.graph;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes the graph. Data only: the constraints and indexes come from NoSQL/neo4j/01_constraints.cypher.
 * Nodes with the migrator's labels are removed first (other data in the database is left alone), so a second run gives the same graph instead of duplicates. Everything is written in one transaction: if a step
 * fails, nothing of the new graph is kept.
 */
public class Neo4jWriter
{
    private static final List<String> RELATIONSHIP_TYPES =
            List.of("LOCATED_AT", "MEMBER_OF", "LIVES_IN", "HAS_ROLE", "WORKS_AT", "STUDIED_AT", "RELATED_TO");

    private final String uri;
    private final String user;
    private final String password;

    public Neo4jWriter(String uri, String user, String password)
    {
        this.uri = uri;
        this.user = user;
        this.password = password;
    }

    /** The uniqueness constraints made by NoSQL/neo4j/01_constraints.cypher, one per node label */
    static final List<String> CONSTRAINTS = List.of("citizen_id", "family_id", "housing_id", "location_id",
            "company_id", "institution_id", "user_username", "role_name");

    // Data only: the constraints come from the setup script, like the SQL tables from the schema script. Without them
    // ids would not be unique and the relationship lookups (MATCH by id) would scan every node.
    private static void requireStructure(Session session)
    {
        List<String> existing = session.run("SHOW CONSTRAINTS YIELD name RETURN collect(name) AS names").single()
                .get("names").asList(value -> value.asString());
        List<String> missing = CONSTRAINTS.stream().filter(name -> !existing.contains(name)).toList();
        if (!missing.isEmpty())
        {
            throw new IllegalStateException("Neo4j is not set up, missing constraints " + missing
                    + ". Run NoSQL/neo4j/01_constraints.cypher first (NoSQL/setup.ps1).");
        }
    }

    /** @return number of nodes per label and relationships per type, counted in Neo4j after writing */
    public Map<String, Long> write(List<GraphMapper.Step> steps)
    {
        return write(steps, Map.of());
    }

    /**
     * @param sequences the highest id per entity, stored as (:Sequence {name, value}) so the CRUD application
     *                  continues after the migrated ids
     */
    public Map<String, Long> write(List<GraphMapper.Step> steps, Map<String, Long> sequences)
    {
        try (Driver driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
             Session session = driver.session())
        {
            requireStructure(session);

            session.executeWriteWithoutResult(tx ->
            {
                tx.run("MATCH (n) WHERE any(label IN labels(n) WHERE label IN $labels) DETACH DELETE n",
                        Map.of("labels", GraphMapper.LABELS)).consume();
                for (GraphMapper.Step step : steps)
                {
                    if (!step.rows().isEmpty())
                    {
                        tx.run(step.cypher(), Map.of("rows", step.rows())).consume();
                    }
                }
                List<Map<String, Object>> rows = sequences.entrySet().stream()
                        .map(e -> Map.<String, Object>of("name", e.getKey(), "value", e.getValue())).toList();
                tx.run("UNWIND $rows AS row MERGE (s:Sequence {name: row.name}) SET s.value = row.value",
                        Map.of("rows", rows)).consume();
            });

            Map<String, Long> counts = new LinkedHashMap<>();
            for (String label : GraphMapper.LABELS)
            {
                counts.put(label, session.run("MATCH (n:" + label + ") RETURN count(n) AS c").single().get("c").asLong());
            }
            for (String type : RELATIONSHIP_TYPES)
            {
                counts.put(type, session.run("MATCH ()-[r:" + type + "]->() RETURN count(r) AS c").single().get("c").asLong());
            }
            return counts;
        }
    }
}
