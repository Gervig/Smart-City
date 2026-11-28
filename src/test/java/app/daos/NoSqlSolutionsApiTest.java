package app.daos;

import app.Main;
import app.Main.Solution;
import app.config.HibernateConfig;
import app.mongo.MongoStore;
import app.mongo.MongoUserStore;
import app.neo4j.Neo4jStore;
import app.neo4j.Neo4jUserStore;
import app.routes.CrudServices;
import app.security.rest.SecurityController;
import app.security.rest.TokenDenylist;
import app.security.rest.TokenSettings;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mindrot.jbcrypt.BCrypt;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.containers.Neo4jContainer;
import org.testcontainers.utility.MountableFile;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers requirement(s): the final project's "the document and graph database solutions must support the same core
 * business functionality as the relational solution" (CRUD, querying, filtering, authentication/authorization,
 * key business workflows), plus FR-04/FR-05 for the MongoDB and Neo4j routes.
 * <p>
 * Starts the real application with a real MongoDB and Neo4j in Docker, set up with the scripts in NoSQL/, and runs
 * every scenario against BOTH /mongodb and /neo4j, so the two solutions are shown to behave the same.
 */
class NoSqlSolutionsApiTest
{
    private static final String NEO4J_PASSWORD = "nosql-test";
    private static final String ADMIN_PASSWORD = "AdminPassword123";
    private static final TokenSettings SETTINGS =
            new TokenSettings("smart-city-test", "1800000", "test-secret-that-is-at-least-32-characters-long");

    private static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7");
    private static final Neo4jContainer<?> NEO4J = new Neo4jContainer<>("neo4j:5").withAdminPassword(NEO4J_PASSWORD);

    private static MongoStore mongo;
    private static Neo4jStore neo4j;
    private static Javalin app;

    @BeforeAll
    static void startApp() throws Exception
    {
        MONGO.start();
        NEO4J.start();
        run(MONGO, "NoSQL/mongodb/01_collections.js", "mongosh", "smart_city_test", "--quiet", "--file",
                "/tmp/01_collections.js");
        run(NEO4J, "NoSQL/neo4j/01_constraints.cypher", "cypher-shell", "-u", "neo4j", "-p", NEO4J_PASSWORD,
                "-f", "/tmp/01_constraints.cypher");

        mongo = new MongoStore(MONGO.getReplicaSetUrl(), "smart_city_test");
        neo4j = new Neo4jStore(NEO4J.getBoltUrl(), "neo4j", NEO4J_PASSWORD);

        // An admin in each database, as the migrator would copy it from PostgreSQL
        String hash = BCrypt.hashpw(ADMIN_PASSWORD, BCrypt.gensalt());
        mongo.collection("users").insertOne(new Document("_id", "admin").append("password", hash)
                .append("roles", List.of("admin")));
        try (Driver driver = GraphDatabase.driver(NEO4J.getBoltUrl(), AuthTokens.basic("neo4j", NEO4J_PASSWORD));
             Session session = driver.session())
        {
            session.run("CREATE (:User {username: 'admin', password: $hash})-[:HAS_ROLE]->(:Role {name: 'admin'})",
                    Map.of("hash", hash)).consume();
        }

        TokenDenylist denylist = new TokenDenylist(Clock.systemUTC());
        app = Main.createApp(HibernateConfig.getEntityManagerFactoryForTest(), new SecurityController(SETTINGS, denylist),
                new Solution("/mongodb", CrudServices.mongo(mongo),
                        new SecurityController(SETTINGS, denylist, new MongoUserStore(mongo))),
                new Solution("/neo4j", CrudServices.neo4j(neo4j),
                        new SecurityController(SETTINGS, denylist, new Neo4jUserStore(neo4j))))
                .start(0);
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = app.port();
    }

    private static void run(GenericContainer<?> container, String script, String... command) throws Exception
    {
        container.copyFileToContainer(MountableFile.forHostPath(Path.of(script)), "/tmp/" + Path.of(script).getFileName());
        var result = container.execInContainer(command);
        assertEquals(0, result.getExitCode(), script + ": " + result.getStderr() + result.getStdout());
    }

    @AfterAll
    static void stopApp()
    {
        app.stop();
        mongo.close();
        neo4j.close();
        MONGO.stop();
        NEO4J.stop();
        RestAssured.reset();
    }

    // ---------- login and roles ----------

    @ParameterizedTest(name = "{0}: register and login use the solution's own users")
    @ValueSource(strings = {"/mongodb", "/neo4j"})
    void registerAndLogin(String db)
    {
        // Arrange
        String username = "user_" + UUID.randomUUID().toString().substring(0, 8);

        // Act
        given().contentType(ContentType.JSON).body(Map.of("username", username, "password", "Password123"))
                .when().post(db + "/api/auth/register").then().statusCode(201).body("roles", equalTo(List.of("user")));

        // Assert
        withToken(login(db, username, "Password123")).when().get(db + "/families").then().statusCode(200);
        given().contentType(ContentType.JSON).body(Map.of("username", username, "password", "Password123"))
                .when().post(db + "/api/auth/register").then().statusCode(409);
        given().contentType(ContentType.JSON).body(Map.of("username", username, "password", "wrong-password"))
                .when().post(db + "/api/auth/login").then().statusCode(401);
    }

    @ParameterizedTest(name = "{0}: no token gives 401")
    @ValueSource(strings = {"/mongodb", "/neo4j"})
    void noToken(String db)
    {
        given().when().get(db + "/citizens").then().statusCode(401);
    }

    @ParameterizedTest(name = "{0}: a normal user cannot create a job and sees no salary")
    @ValueSource(strings = {"/mongodb", "/neo4j"})
    void salaryIsAdminOnly(String db)
    {
        // Arrange
        City city = city(db);
        String user = newUser(db);

        // Act and Assert
        withToken(user).contentType(ContentType.JSON).body(Map.of()).when().post(db + "/jobs").then().statusCode(403);
        withToken(user).when().get(db + "/jobs/" + city.jobId).then().statusCode(200).body("salary", nullValue());
        withToken(user).when().get(db + "/jobs?citizenId=" + city.citizenId)
                .then().statusCode(200).body("data.salary", everyItem(nullValue()));
        assertEquals(0, new BigDecimal("35000.50").compareTo(salary(db, city.jobId)));
    }

    // ---------- CRUD, paging, filters, sorting ----------

    @ParameterizedTest(name = "{0}: every resource can be created and read back")
    @ValueSource(strings = {"/mongodb", "/neo4j"})
    void createAndRead(String db)
    {
        // Arrange and Act
        City city = city(db);
        String admin = adminToken(db);

        // Assert
        withToken(admin).when().get(db + "/citizens/" + city.citizenId).then().statusCode(200)
                .body("firstName", equalTo("Anna")).body("familyId", equalTo((int) city.familyId))
                .body("housingId", equalTo((int) city.housingId));
        withToken(admin).when().get(db + "/housings/" + city.housingId).then().statusCode(200)
                .body("locationId", equalTo((int) city.locationId)).body("capacity", equalTo(2));
        withToken(admin).when().get(db + "/jobs/" + city.jobId).then().statusCode(200)
                .body("citizenId", equalTo((int) city.citizenId)).body("companyId", equalTo((int) city.companyId))
                .body("institutionId", nullValue()).body("title", equalTo("Developer"));
        withToken(admin).when().get(db + "/education-records/" + city.educationId).then().statusCode(200)
                .body("institutionId", equalTo((int) city.institutionId));
        withToken(admin).when().get(db + "/citizen-relationships/" + city.relationshipId).then().statusCode(200)
                .body("citizenId1", equalTo((int) city.citizenId)).body("citizenId2", equalTo((int) city.partnerId))
                .body("relationshipType", equalTo("marriage"));
    }

    @ParameterizedTest(name = "{0}: a list is paged, filtered and sorted like in PostgreSQL")
    @ValueSource(strings = {"/mongodb", "/neo4j"})
    void pagingFilteringSorting(String db)
    {
        // Arrange
        City city = city(db);
        String admin = adminToken(db);

        // Act and Assert: a text filter matches part of the name, case-insensitively
        withToken(admin).when().get(db + "/citizens?familyId=" + city.familyId + "&sort=firstName,desc&size=1")
                .then().statusCode(200).body("data", hasSize(1)).body("data[0].firstName", equalTo("Bo"))
                .body("totalElements", equalTo(2)).body("totalPages", equalTo(2)).body("size", equalTo(1));
        withToken(admin).when().get(db + "/families?familyName=" + city.familyName.substring(3, 9).toUpperCase())
                .then().statusCode(200).body("totalElements", equalTo(1));
        withToken(admin).when().get(db + "/jobs?citizenId=" + city.citizenId + "&salaryFrom=35000&salaryTo=36000")
                .then().statusCode(200).body("totalElements", equalTo(1));
        withToken(admin).when().get(db + "/jobs?citizenId=" + city.citizenId + "&salaryFrom=40000")
                .then().statusCode(200).body("totalElements", equalTo(0));
    }

    @ParameterizedTest(name = "{0}: unknown filters, sort fields and ids give 400 / 404")
    @ValueSource(strings = {"/mongodb", "/neo4j"})
    void badRequests(String db)
    {
        String admin = adminToken(db);
        withToken(admin).when().get(db + "/citizens?shoeSize=42").then().statusCode(400)
                .body("error", equalTo("Unsupported filter: shoeSize"));
        withToken(admin).when().get(db + "/citizens?sort=shoeSize,asc").then().statusCode(400)
                .body("error", equalTo("Unsupported sort field: shoeSize"));
        withToken(admin).when().get(db + "/citizens/999999").then().statusCode(404);
        withToken(admin).contentType(ContentType.JSON).body(Map.of("familyName", "")).when().post(db + "/families")
                .then().statusCode(400);
    }

    @ParameterizedTest(name = "{0}: an update changes the row, a rename is visible everywhere")
    @ValueSource(strings = {"/mongodb", "/neo4j"})
    void update(String db)
    {
        // Arrange
        City city = city(db);
        String admin = adminToken(db);

        // Act
        withToken(admin).contentType(ContentType.JSON)
                .body(Map.of("locationId", city.locationId, "name", "Renamed Ltd", "foundedDate", "2001-03-04",
                        "status", "active"))
                .when().put(db + "/companies/" + city.companyId).then().statusCode(200).body("name", equalTo("Renamed Ltd"));

        // Assert: the job still points at the company; in MongoDB its copied name was updated too
        withToken(admin).when().get(db + "/jobs?companyId=" + city.companyId).then().statusCode(200)
                .body("totalElements", equalTo(1));
        if (db.equals("/mongodb"))
        {
            Document anna = mongo.collection("citizens").find(new Document("_id", city.citizenId)).first();
            assertEquals("Renamed Ltd",
                    anna.getList("jobs", Document.class).get(0).get("employer", Document.class).get("name"));
        }
    }

    // ---------- business rules ----------

    @ParameterizedTest(name = "{0}: references and business rules are enforced")
    @ValueSource(strings = {"/mongodb", "/neo4j"})
    void businessRules(String db)
    {
        // Arrange
        City city = city(db);
        String admin = adminToken(db);

        // Act and Assert: the housing (capacity 2) is full
        withToken(admin).contentType(ContentType.JSON).body(citizen("Carl", city.familyId, city.housingId, null))
                .when().post(db + "/citizens").then().statusCode(400).body("error", equalTo("housing is full"));
        // unknown family
        withToken(admin).contentType(ContentType.JSON).body(citizen("Dan", 999999L, null, null))
                .when().post(db + "/citizens").then().statusCode(404);
        // a job cannot start before the citizen was born
        Map<String, Object> early = job(city.citizenId, city.companyId);
        early.put("startDate", "1980-01-01");
        withToken(admin).contentType(ContentType.JSON).body(early).when().post(db + "/jobs").then().statusCode(400);
        // still referenced: like ON DELETE RESTRICT
        withToken(admin).when().delete(db + "/families/" + city.familyId).then().statusCode(409);
        withToken(admin).when().delete(db + "/companies/" + city.companyId).then().statusCode(409);
        withToken(admin).when().delete(db + "/citizens/" + city.citizenId).then().statusCode(409);
    }

    @ParameterizedTest(name = "{0}: deleting a housing leaves its residents without one (SET NULL)")
    @ValueSource(strings = {"/mongodb", "/neo4j"})
    void deleteHousing(String db)
    {
        // Arrange
        City city = city(db);
        String admin = adminToken(db);

        // Act
        withToken(admin).when().delete(db + "/housings/" + city.housingId).then().statusCode(204);

        // Assert
        withToken(admin).when().get(db + "/citizens/" + city.citizenId).then().statusCode(200)
                .body("housingId", nullValue());
        withToken(admin).when().get(db + "/housings/" + city.housingId).then().statusCode(404);
    }

    @ParameterizedTest(name = "{0}: a death date ends the citizen's open jobs and relationships")
    @ValueSource(strings = {"/mongodb", "/neo4j"})
    void deathCleanup(String db)
    {
        // Arrange (CitizenValidator: a dead citizen has no housing)
        City city = city(db);
        String admin = adminToken(db);
        Map<String, Object> dead = citizen("Anna", city.familyId, null, "2024-05-01");
        dead.put("status", "deceased");

        // Act
        withToken(admin).contentType(ContentType.JSON).body(dead).when().put(db + "/citizens/" + city.citizenId)
                .then().statusCode(200).body("housingId", nullValue());

        // Assert
        withToken(admin).when().get(db + "/jobs/" + city.jobId).then().statusCode(200)
                .body("endDate", equalTo(List.of(2024, 5, 1))).body("status", equalTo("inactive"));
        withToken(admin).when().get(db + "/citizen-relationships/" + city.relationshipId).then().statusCode(200)
                .body("endDate", equalTo(List.of(2024, 5, 1)));
    }

    @ParameterizedTest(name = "{0}: rows are deleted again in dependency order")
    @ValueSource(strings = {"/mongodb", "/neo4j"})
    void deleteEverything(String db)
    {
        // Arrange
        City city = city(db);
        String admin = adminToken(db);

        // Act and Assert
        for (String path : List.of("/jobs/" + city.jobId, "/education-records/" + city.educationId,
                "/citizen-relationships/" + city.relationshipId, "/citizens/" + city.citizenId,
                "/citizens/" + city.partnerId, "/housings/" + city.housingId, "/companies/" + city.companyId,
                "/institutions/" + city.institutionId, "/families/" + city.familyId, "/locations/" + city.locationId))
        {
            withToken(admin).when().delete(db + path).then().statusCode(204);
            withToken(admin).when().get(db + path).then().statusCode(404);
        }
    }

    @Test
    @DisplayName("the PostgreSQL routes are still there next to the new ones")
    void postgresStillServed()
    {
        given().when().get("/citizens").then().statusCode(401);
        given().when().get("/mongodb/citizens").then().statusCode(401);
        given().when().get("/neo4j/citizens").then().statusCode(401);
    }

    @Test
    @DisplayName("OpenAPI documents CRUD and authentication paths for each NoSQL solution only")
    void openApiDocumentsSolutionPaths() throws Exception
    {
        JsonNode paths = new ObjectMapper().readTree(given().when().get("/openapi").then().statusCode(200)
                .extract().asString()).path("paths");

        for (String prefix : List.of("/mongodb", "/neo4j"))
        {
            assertTrue(paths.has(prefix + "/citizens"));
            assertTrue(paths.has(prefix + "/citizens/{id}"));
            assertTrue(paths.has(prefix + "/api/auth/login"));
            assertTrue(paths.has(prefix + "/api/auth/register"));
            assertFalse(paths.has(prefix + "/citizen-details"));
            assertFalse(paths.has(prefix + "/locations/{id}/nearby"));
            assertFalse(paths.has(prefix + "/citizens/{id}/total-salary"));
            assertFalse(paths.has(prefix + "/api/admin/seed/locations"));
        }

        JsonNode postgresList = paths.path("/citizens").path("get");
        JsonNode mongoList = paths.path("/mongodb/citizens").path("get");
        JsonNode neo4jList = paths.path("/neo4j/citizens").path("get");
        assertNotEquals(postgresList.path("operationId").asText(), mongoList.path("operationId").asText());
        assertNotEquals(mongoList.path("operationId").asText(), neo4jList.path("operationId").asText());
        assertTrue(mongoList.path("summary").asText().endsWith(" (MongoDB)"));
        assertTrue(neo4jList.path("summary").asText().endsWith(" (Neo4j)"));
        assertTrue(mongoList.path("tags").get(0).asText().endsWith(" (MongoDB)"));
        assertTrue(neo4jList.path("tags").get(0).asText().endsWith(" (Neo4j)"));
    }

    // ---------- a small city per test, made through the API ----------

    private record City(long locationId, long familyId, String familyName, long housingId, long citizenId,
                        long partnerId, long companyId, long institutionId, long jobId, long educationId,
                        long relationshipId)
    {
    }

    /** Anna and Bo (married) live in a housing for 2; Anna works at a company (35000.50) and studied at a college */
    private static City city(String db)
    {
        String admin = adminToken(db);
        String familyName = "Family_" + UUID.randomUUID().toString().substring(0, 8);
        long location = create(admin, db + "/locations", Map.of("name", "Town Hall", "streetAddress", "Square 1"));
        long family = create(admin, db + "/families", Map.of("familyName", familyName));
        long housing = create(admin, db + "/housings",
                Map.of("locationId", location, "housingType", "apartment", "capacity", 2));
        long anna = create(admin, db + "/citizens", citizen("Anna", family, housing, null));
        long bo = create(admin, db + "/citizens", citizen("Bo", family, housing, null));
        long company = create(admin, db + "/companies",
                Map.of("locationId", location, "name", "Acme ApS", "foundedDate", "2001-03-04", "status", "active"));
        long institution = create(admin, db + "/institutions",
                Map.of("locationId", location, "name", "City College", "type", "college"));
        long job = create(admin, db + "/jobs", job(anna, company));
        long education = create(admin, db + "/education-records",
                Map.of("citizenId", anna, "institutionId", institution, "startDate", "2008-08-15",
                        "endDate", "2011-06-20"));
        long relationship = create(admin, db + "/citizen-relationships",
                Map.of("citizenId1", anna, "citizenId2", bo, "relationshipType", "marriage", "startDate", "2018-06-01"));
        return new City(location, family, familyName, housing, anna, bo, company, institution, job, education,
                relationship);
    }

    private static Map<String, Object> citizen(String firstName, Long familyId, Long housingId, String deathDate)
    {
        Map<String, Object> body = new HashMap<>();
        body.put("firstName", firstName);
        body.put("lastName", "Jensen");
        body.put("birthDate", "1990-05-17");
        body.put("deathDate", deathDate);
        body.put("status", "alive");
        body.put("gender", "female");
        body.put("familyId", familyId);
        body.put("housingId", housingId);
        return body;
    }

    private static Map<String, Object> job(long citizenId, long companyId)
    {
        Map<String, Object> body = new HashMap<>();
        body.put("citizenId", citizenId);
        body.put("companyId", companyId);
        body.put("title", "Developer");
        body.put("salary", 35000.50);
        body.put("startDate", "2015-09-01");
        body.put("status", "active");
        return body;
    }

    private static long create(String token, String path, Map<String, Object> body)
    {
        return ((Number) withToken(token).contentType(ContentType.JSON).body(body).when().post(path)
                .then().statusCode(201).extract().path("id")).longValue();
    }

    private static BigDecimal salary(String db, long jobId)
    {
        Object salary = withToken(adminToken(db)).when().get(db + "/jobs/" + jobId).then().statusCode(200)
                .extract().path("salary");
        return new BigDecimal(salary.toString());
    }

    private static String adminToken(String db)
    {
        return login(db, "admin", ADMIN_PASSWORD);
    }

    private static String newUser(String db)
    {
        String username = "user_" + UUID.randomUUID().toString().substring(0, 8);
        given().contentType(ContentType.JSON).body(Map.of("username", username, "password", "Password123"))
                .when().post(db + "/api/auth/register").then().statusCode(201);
        return login(db, username, "Password123");
    }

    private static String login(String db, String username, String password)
    {
        return given().contentType(ContentType.JSON).body(Map.of("username", username, "password", password))
                .when().post(db + "/api/auth/login").then().statusCode(200).extract().path("token");
    }

    private static RequestSpecification withToken(String token)
    {
        return given().header("Authorization", "Bearer " + token);
    }
}
