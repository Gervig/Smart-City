package migrator;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Session;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The whole migration against real databases in Docker: PostgreSQL with the project's schema
 * (SQL/schema/database_schema.sql) and the sample city, then MongoDB and Neo4j as targets.
 */
@Testcontainers
class MigrationIntegrationTest
{
    private static final String NEO4J_PASSWORD = "migrator-test";

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16.2")
            .withDatabaseName("smart_city")
            .withUsername("postgres") // the schema script sets OWNER TO postgres
            .withCopyFileToContainer(MountableFile.forHostPath(Path.of("../SQL/schema/database_schema.sql")),
                    "/docker-entrypoint-initdb.d/01_schema.sql");

    @Container
    private static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7");

    @Container
    private static final Neo4jContainer<?> NEO4J = new Neo4jContainer<>("neo4j:5").withAdminPassword(NEO4J_PASSWORD);

    private static MigrationReport report;

    @BeforeAll
    static void migrate() throws Exception
    {
        try (Connection connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(),
                POSTGRES.getPassword()); Statement statement = connection.createStatement())
        {
            for (String insert : SampleData.SQL_INSERTS)
            {
                statement.execute(insert);
            }
            // Like smartcity_readonly in SQL/users/08_users.sql: the migrator only gets SELECT
            statement.execute("CREATE ROLE reader LOGIN PASSWORD 'reader-pw'");
            statement.execute("GRANT SELECT ON ALL TABLES IN SCHEMA public TO reader");
        }
        // The structure comes from the setup scripts, like the PostgreSQL tables from the schema script
        run(MONGO, "../NoSQL/mongodb/01_collections.js", "mongosh", "smart_city_test", "--quiet", "--file",
                "/tmp/01_collections.js");
        run(NEO4J, "../NoSQL/neo4j/01_constraints.cypher", "cypher-shell", "-u", "neo4j", "-p", NEO4J_PASSWORD,
                "-f", "/tmp/01_constraints.cypher");
        report = Migrator.run(config("reader", "reader-pw"));
    }

    private static void run(org.testcontainers.containers.GenericContainer<?> container, String script,
                            String... command) throws Exception
    {
        String target = "/tmp/" + Path.of(script).getFileName();
        container.copyFileToContainer(MountableFile.forHostPath(Path.of(script)), target);
        var result = container.execInContainer(command);
        assertEquals(0, result.getExitCode(), script + " failed: " + result.getStderr() + result.getStdout());
    }

    @Test
    @DisplayName("setup script: MongoDB has all 7 collections, each with a validator, and the indexes")
    void mongoStructure()
    {
        // Arrange
        try (MongoClient client = MongoClients.create(MONGO.getReplicaSetUrl()))
        {
            MongoDatabase db = client.getDatabase("smart_city_test");

            // Act
            List<Document> collections = db.listCollections().into(new java.util.ArrayList<>());
            List<String> citizenIndexes = db.getCollection("citizens").listIndexes()
                    .map(index -> index.getString("name")).into(new java.util.ArrayList<>());

            // Assert
            List<String> validated = List.of("locations", "families", "housings", "companies", "institutions",
                    "citizens", "users");
            assertTrue(collections.stream().map(c -> c.getString("name")).toList().containsAll(validated));
            assertTrue(collections.stream().filter(c -> validated.contains(c.getString("name"))).allMatch(c ->
                    c.get("options", Document.class).containsKey("validator")));
            assertTrue(citizenIndexes.containsAll(List.of("citizens_name", "citizens_family", "citizens_housing",
                    "citizens_employer")));
        }
    }

    @Test
    @DisplayName("setup script: the validator rejects a citizen without birthDate and with an unknown status")
    void mongoValidatorRejectsBadDocument()
    {
        // Arrange
        Document bad = new Document("_id", 999L).append("firstName", "No").append("lastName", "Birthday")
                .append("status", "missing").append("family", new Document("id", 1L).append("familyName", "X"))
                .append("jobs", List.of()).append("education", List.of()).append("relationships", List.of());
        try (MongoClient client = MongoClients.create(MONGO.getReplicaSetUrl()))
        {
            // Act and Assert
            com.mongodb.MongoWriteException e = assertThrows(com.mongodb.MongoWriteException.class, () ->
                    client.getDatabase("smart_city_test").getCollection("citizens").insertOne(bad));
            assertEquals(121, e.getError().getCode()); // 121 = DocumentValidationFailure
        }
    }

    @Test
    @DisplayName("setup script: Neo4j has the 9 uniqueness constraints (8 labels + Sequence) and the indexes")
    void neo4jStructure()
    {
        // Arrange
        try (Driver driver = GraphDatabase.driver(NEO4J.getBoltUrl(), AuthTokens.basic("neo4j", NEO4J_PASSWORD));
             Session session = driver.session())
        {
            // Act
            List<String> constraints = session.run("SHOW CONSTRAINTS YIELD name RETURN name").list(r -> r.get(0).asString());
            List<String> indexes = session.run("SHOW INDEXES YIELD name RETURN name").list(r -> r.get(0).asString());

            // Assert
            assertEquals(9, constraints.size());
            assertTrue(indexes.containsAll(List.of("citizen_name", "related_to_type", "works_at_status",
                    "works_at_id", "studied_at_id", "related_to_id")));
        }
    }

    @Test
    @DisplayName("the id counters for the CRUD application start after the highest migrated ids")
    void countersStartAfterMigratedIds()
    {
        // Arrange
        try (MongoClient client = MongoClients.create(MONGO.getReplicaSetUrl());
             Driver driver = GraphDatabase.driver(NEO4J.getBoltUrl(), AuthTokens.basic("neo4j", NEO4J_PASSWORD));
             Session session = driver.session())
        {
            // Act
            Document citizenCounter = client.getDatabase("smart_city_test").getCollection("counters")
                    .find(new Document("_id", "citizen")).first();
            long jobSequence = session.run("MATCH (s:Sequence {name: 'job'}) RETURN s.value AS v").single()
                    .get("v").asLong();

            // Assert: the sample city has citizens 1-3 and jobs 1-2
            assertEquals(3L, ((Number) citizenCounter.get("seq")).longValue());
            assertEquals(2L, jobSequence);
        }
    }

    @Test
    @DisplayName("the migrator only writes data: without the setup it stops and says what is missing")
    void migratorNeedsTheSetup()
    {
        // Arrange
        MigratorConfig notSetUp = new MigratorConfig(POSTGRES.getJdbcUrl(), "reader", "reader-pw",
                MONGO.getReplicaSetUrl(), "never_set_up", NEO4J.getBoltUrl(), "neo4j", NEO4J_PASSWORD);

        // Act
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> Migrator.run(notSetUp));

        // Assert
        assertTrue(e.getMessage().contains("01_collections.js"), e.getMessage());
    }

    @Test
    @DisplayName("every count in the report matches")
    void reportMatches()
    {
        // Arrange

        // Act
        boolean ok = report.ok();

        // Assert
        assertTrue(ok, report.format());
        // 7 collections + 3 embedded lists in MongoDB, 8 node labels + 7 relationship types in Neo4j
        assertEquals(25, report.checks().size());
    }

    @Test
    @DisplayName("MongoDB: Anna's document has her job with the exact salary and the employer's name")
    void mongoCitizenDocument()
    {
        // Arrange
        try (MongoClient client = MongoClients.create(MONGO.getReplicaSetUrl()))
        {
            MongoDatabase db = client.getDatabase("smart_city_test");

            // Act
            Document anna = db.getCollection("citizens").find(new Document("firstName", "Anna")).first();

            // Assert
            Document job = anna.getList("jobs", Document.class).get(0);
            assertEquals(new Decimal128(new BigDecimal("42000.50")), job.get("salary"));
            assertEquals("Acme ApS", job.get("employer", Document.class).get("name"));
            assertEquals("Jensen", anna.get("family", Document.class).get("familyName"));
            assertEquals(1, anna.getList("relationships", Document.class).size());
        }
    }

    @Test
    @DisplayName("MongoDB: a geo query finds the location near the Town Hall")
    void mongoGeoQuery()
    {
        // Arrange
        Document near = Document.parse("{point: {$near: {$geometry: {type: 'Point', coordinates: [12.568, 55.676]}, "
                + "$maxDistance: 500}}}");
        try (MongoClient client = MongoClients.create(MONGO.getReplicaSetUrl()))
        {
            // Act
            List<Document> found = client.getDatabase("smart_city_test").getCollection("locations").find(near)
                    .into(new java.util.ArrayList<>());

            // Assert
            assertEquals(1, found.size());
            assertEquals("Town Hall", found.get(0).get("name"));
        }
    }

    @Test
    @DisplayName("Neo4j: Anna works at Acme and is married to Bo, who teaches at the school she studied at")
    void neo4jTraversal()
    {
        // Arrange
        String cypher = """
                MATCH (anna:Citizen {firstName: 'Anna'})-[w:WORKS_AT]->(company:Company),
                      (anna)-[m:RELATED_TO {type: 'marriage'}]->(bo:Citizen)-[:WORKS_AT]->(school:Institution),
                      (anna)-[s:STUDIED_AT]->(school)
                RETURN company.name AS company, w.salary AS salary, bo.firstName AS spouse,
                       school.name AS school, s.endDate AS graduated""";
        try (Driver driver = GraphDatabase.driver(NEO4J.getBoltUrl(), AuthTokens.basic("neo4j", NEO4J_PASSWORD));
             Session session = driver.session())
        {
            // Act
            Record row = session.run(cypher).single();

            // Assert
            assertEquals("Acme ApS", row.get("company").asString());
            assertEquals(42000.5, row.get("salary").asDouble());
            assertEquals("Bo", row.get("spouse").asString());
            assertEquals("City School", row.get("school").asString());
            assertEquals(LocalDate.of(2009, 6, 20), row.get("graduated").asLocalDate());
        }
    }

    @Test
    @DisplayName("running the migration again replaces the data instead of duplicating it")
    void runningTwiceGivesTheSameResult() throws Exception
    {
        // Arrange

        // Act
        MigrationReport second = Migrator.run(config("reader", "reader-pw"));

        // Assert
        assertTrue(second.ok(), second.format());
        assertEquals(report.checks(), second.checks());
    }

    @Test
    @DisplayName("the migrator only reads: the read-only user cannot change PostgreSQL")
    void sourceIsOnlyRead()
    {
        // Arrange

        // Act and Assert
        assertThrows(java.sql.SQLException.class, () ->
        {
            try (Connection connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), "reader", "reader-pw");
                 Statement statement = connection.createStatement())
            {
                statement.execute("DELETE FROM citizen");
            }
        });
    }

    private static MigratorConfig config(String user, String password)
    {
        return new MigratorConfig(POSTGRES.getJdbcUrl(), user, password, MONGO.getReplicaSetUrl(), "smart_city_test",
                NEO4J.getBoltUrl(), "neo4j", NEO4J_PASSWORD);
    }
}
