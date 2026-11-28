package app.daos;

import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.exceptions.AuthenticationException;
import org.neo4j.driver.exceptions.ClientException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers requirement(s): the final project's database user privileges (an application user with least privilege, an
 * admin, a read-only user and a restricted reader without sensitive data), for MongoDB and Neo4j.
 * <p>
 * MongoDB runs WITH authentication here (the image's root user switches it on), set up with NoSQL/mongodb/01 and 02;
 * each user connects with its own password and is allowed or refused (error 13, Unauthorized). Neo4j Community has
 * users but no roles: the test shows that the users exist and can log in, and that GRANT is refused.
 */
class NoSqlDatabaseUsersTest
{
    private static final String ROOT = "mongodb://root:root-test-password@%s:%d/smart_city?authSource=admin";
    private static final String NEO4J_PASSWORD = "users-test";

    private static final GenericContainer<?> MONGO = new GenericContainer<>("mongo:7")
            .withExposedPorts(27017)
            .withEnv("MONGO_INITDB_ROOT_USERNAME", "root")
            .withEnv("MONGO_INITDB_ROOT_PASSWORD", "root-test-password")
            .waitingFor(Wait.forLogMessage(".*Waiting for connections.*", 2));
    private static final Neo4jContainer<?> NEO4J = new Neo4jContainer<>("neo4j:5").withAdminPassword(NEO4J_PASSWORD);

    @BeforeAll
    static void setUp() throws Exception
    {
        MONGO.start();
        NEO4J.start();
        String root = String.format(ROOT, "localhost", 27017);
        for (String script : List.of("01_collections.js", "02_users.js"))
        {
            run(MONGO, "NoSQL/mongodb/" + script, "mongosh", root, "--quiet", "--file", "/tmp/" + script);
        }
        run(NEO4J, "NoSQL/neo4j/02_users.cypher", "cypher-shell", "-u", "neo4j", "-p", NEO4J_PASSWORD, "-d", "system",
                "-f", "/tmp/02_users.cypher");

        // One citizen with a salary, one family and one login user, written as root
        try (MongoClient client = MongoClients.create(String.format(ROOT, MONGO.getHost(), MONGO.getMappedPort(27017))))
        {
            MongoDatabase db = client.getDatabase("smart_city");
            db.getCollection("families").insertOne(new Document("_id", 1L).append("familyName", "Jensen")
                    .append("memberIds", List.of(1L)));
            db.getCollection("citizens").insertOne(new Document("_id", 1L).append("firstName", "Anna")
                    .append("lastName", "Jensen").append("birthDate", LocalDate.of(1990, 5, 17)).append("status", "alive")
                    .append("family", new Document("id", 1L).append("familyName", "Jensen"))
                    .append("jobs", List.of(new Document("id", 1L).append("title", "Developer")
                            .append("salary", new BigDecimal("42000.50")).append("startDate", LocalDate.of(2015, 9, 1))
                            .append("status", "active").append("employer",
                                    new Document("type", "company").append("id", 1L).append("name", "Acme"))))
                    .append("education", List.of()).append("relationships", List.of()));
            db.getCollection("users").insertOne(new Document("_id", "admin").append("password", "$2a$10$hash")
                    .append("roles", List.of("admin")));
        }
    }

    private static void run(GenericContainer<?> container, String script, String... command) throws Exception
    {
        container.copyFileToContainer(MountableFile.forHostPath(Path.of(script)), "/tmp/" + Path.of(script).getFileName());
        var result = container.execInContainer(command);
        assertEquals(0, result.getExitCode(), script + ": " + result.getStderr() + result.getStdout());
    }

    @AfterAll
    static void tearDown()
    {
        MONGO.stop();
        NEO4J.stop();
    }

    private static MongoClient as(String user, String password)
    {
        return MongoClients.create(String.format("mongodb://%s:%s@%s:%d/smart_city?authSource=smart_city",
                user, password, MONGO.getHost(), MONGO.getMappedPort(27017)));
    }

    private static void assertUnauthorized(Runnable action)
    {
        MongoException e = assertThrows(MongoException.class, action::run);
        assertEquals(13, e.getCode(), e.getMessage()); // 13 = Unauthorized
    }

    // ---------- MongoDB ----------

    @Test
    @DisplayName("MongoDB app user: reads and writes documents, including the login users")
    void appUserCrud()
    {
        try (MongoClient client = as("smartcity_app", "app-dev-password"))
        {
            // Arrange
            var families = client.getDatabase("smart_city").getCollection("families");

            // Act and Assert
            assertDoesNotThrow(() -> families.insertOne(new Document("_id", 50L).append("familyName", "App")
                    .append("memberIds", List.of())));
            assertDoesNotThrow(() -> families.updateOne(Filters.eq("_id", 50L), Updates.set("familyName", "App2")));
            assertDoesNotThrow(() -> families.deleteOne(Filters.eq("_id", 50L)));
            assertNotNull(client.getDatabase("smart_city").getCollection("users").find().first());
        }
    }

    @Test
    @DisplayName("MongoDB app user: may not change the structure (drop, index, validators)")
    void appUserCannotChangeStructure()
    {
        try (MongoClient client = as("smartcity_app", "app-dev-password"))
        {
            // Arrange
            MongoDatabase db = client.getDatabase("smart_city");

            // Act and Assert
            assertUnauthorized(() -> db.getCollection("families").drop());
            assertUnauthorized(() -> db.getCollection("families").createIndex(Indexes.ascending("familyName")));
            assertUnauthorized(() -> db.runCommand(new Document("collMod", "families").append("validator", new Document())));
        }
    }

    @Test
    @DisplayName("MongoDB admin user: may change the structure")
    void adminUser()
    {
        try (MongoClient client = as("smartcity_admin", "admin-dev-password"))
        {
            // Arrange
            MongoDatabase db = client.getDatabase("smart_city");

            // Act and Assert
            assertDoesNotThrow(() -> db.createCollection("admin_scratch"));
            assertDoesNotThrow(() -> db.getCollection("admin_scratch").drop());
        }
    }

    @Test
    @DisplayName("MongoDB read-only user: reads everything, writes nothing")
    void readOnlyUser()
    {
        try (MongoClient client = as("smartcity_readonly", "readonly-dev-password"))
        {
            // Arrange
            MongoDatabase db = client.getDatabase("smart_city");

            // Act and Assert
            assertNotNull(db.getCollection("citizens").find().first());
            assertUnauthorized(() -> db.getCollection("families").insertOne(new Document("_id", 60L)
                    .append("familyName", "No").append("memberIds", List.of())));
        }
    }

    @Test
    @DisplayName("MongoDB restricted user: no salaries and no login users, through the view citizens_restricted")
    void restrictedUser()
    {
        try (MongoClient client = as("smartcity_restricted", "restricted-dev-password"))
        {
            // Arrange
            MongoDatabase db = client.getDatabase("smart_city");

            // Act
            Document anna = db.getCollection("citizens_restricted").find().first();

            // Assert
            Document job = anna.getList("jobs", Document.class).get(0);
            assertEquals("Developer", job.get("title"));
            assertFalse(job.containsKey("salary"));
            assertNotNull(db.getCollection("families").find().first());
            assertUnauthorized(() -> db.getCollection("citizens").find().first());
            assertUnauthorized(() -> db.getCollection("users").find().first());
            assertUnauthorized(() -> db.getCollection("families").deleteMany(new Document()));
        }
    }

    @Test
    @DisplayName("MongoDB: a wrong password is refused")
    void wrongPassword()
    {
        try (MongoClient client = as("smartcity_app", "not-the-password"))
        {
            assertThrows(MongoException.class, () -> client.getDatabase("smart_city").getCollection("families").find().first());
        }
    }

    @Test
    @DisplayName("MongoDB: the root user still sees the salary (the source data is unchanged)")
    void sourceKeepsSalary()
    {
        try (MongoClient client = MongoClients.create(String.format(ROOT, MONGO.getHost(), MONGO.getMappedPort(27017))))
        {
            Document anna = client.getDatabase("smart_city").getCollection("citizens").find().first();
            assertEquals(new Decimal128(new BigDecimal("42000.50")),
                    anna.getList("jobs", Document.class).get(0).get("salary"));
        }
    }

    // ---------- Neo4j Community ----------

    @Test
    @DisplayName("Neo4j: the four users exist and can log in with their password")
    void neo4jUsers()
    {
        // Arrange
        List<String> users;
        try (Driver driver = GraphDatabase.driver(NEO4J.getBoltUrl(), AuthTokens.basic("neo4j", NEO4J_PASSWORD));
             Session session = driver.session(SessionConfig.forDatabase("system")))
        {
            // Act
            users = session.run("SHOW USERS YIELD user RETURN user").list(r -> r.get(0).asString());
        }

        // Assert
        assertTrue(users.containsAll(List.of("smartcity_app", "smartcity_admin", "smartcity_readonly",
                "smartcity_restricted")));
        try (Driver app = GraphDatabase.driver(NEO4J.getBoltUrl(), AuthTokens.basic("smartcity_app", "app-dev-password")))
        {
            assertDoesNotThrow(app::verifyConnectivity);
        }
        try (Driver wrong = GraphDatabase.driver(NEO4J.getBoltUrl(), AuthTokens.basic("smartcity_app", "wrong")))
        {
            assertThrows(AuthenticationException.class, wrong::verifyConnectivity);
        }
    }

    @Test
    @DisplayName("Neo4j Community refuses roles and GRANT (Enterprise only): restrictions are kept by the application")
    void neo4jCommunityHasNoRoles()
    {
        try (Driver driver = GraphDatabase.driver(NEO4J.getBoltUrl(), AuthTokens.basic("neo4j", NEO4J_PASSWORD));
             Session session = driver.session(SessionConfig.forDatabase("system")))
        {
            ClientException e = assertThrows(ClientException.class,
                    () -> session.run("GRANT ROLE reader TO smartcity_readonly").consume());
            assertTrue(e.getMessage().contains("Unsupported administration command"), e.getMessage());
        }
    }
}
