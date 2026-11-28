package app.daos;

import app.Main;
import app.config.HibernateConfig;
import app.populators.StartupPopulator;
import app.security.rest.SecurityController;
import app.security.rest.TokenDenylist;
import app.security.rest.TokenSettings;
import io.javalin.Javalin;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.Session;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Covers requirement(s): the final project's auditing, end to end: a PUT that changes a citizen's status is logged
 * with the API user who made it (not only the database user), because GenericDAO passes it to the trigger.
 */
class AuditApiTest
{
    private static final TokenSettings SETTINGS =
            new TokenSettings("smart-city-test", "1800000", "test-secret-that-is-at-least-32-characters-long");

    private static EntityManagerFactory emf;
    private static Javalin app;

    @BeforeAll
    static void startApp() throws Exception
    {
        emf = HibernateConfig.getEntityManagerFactoryForTest();
        String audit = Files.readString(Path.of("SQL/audit/09_citizen_status_audit.sql"));
        try (EntityManager em = emf.createEntityManager())
        {
            em.getTransaction().begin();
            em.createNativeQuery("TRUNCATE TABLE user_role, users, roles CASCADE").executeUpdate();
            em.getTransaction().commit();
            em.unwrap(Session.class).doWork(connection -> connection.createStatement().execute(audit));
        }
        StartupPopulator.populate(emf, "AdminPassword123");
        app = Main.createApp(emf, new SecurityController(SETTINGS, new TokenDenylist(Clock.systemUTC()))).start(0);
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = app.port();
    }

    @AfterAll
    static void stopApp()
    {
        app.stop();
        RestAssured.reset();
    }

    @Test
    @DisplayName("PUT /citizens/{id} that makes a citizen deceased is logged with the API user")
    void statusChangeThroughTheApi()
    {
        // Arrange
        String user = login("User1", "Password123");
        int family = given().header("Authorization", "Bearer " + user).contentType(ContentType.JSON)
                .body(Map.of("familyName", "Audited")).when().post("/families")
                .then().statusCode(201).extract().path("id");
        int citizen = given().header("Authorization", "Bearer " + user).contentType(ContentType.JSON)
                .body(citizen(family, "alive", null)).when().post("/citizens")
                .then().statusCode(201).extract().path("id");

        // Act
        given().header("Authorization", "Bearer " + user).contentType(ContentType.JSON)
                .body(citizen(family, "deceased", "2024-05-01")).when().put("/citizens/" + citizen)
                .then().statusCode(200);

        // Assert
        assertEquals(List.of("alive -> deceased | 2024-05-01 | User1"), history(citizen));
    }

    @Test
    @DisplayName("a PUT that keeps the status is not logged")
    void otherChangesNotLogged()
    {
        // Arrange
        String admin = login("admin", "AdminPassword123");
        int family = given().header("Authorization", "Bearer " + admin).contentType(ContentType.JSON)
                .body(Map.of("familyName", "Unchanged")).when().post("/families")
                .then().statusCode(201).extract().path("id");
        int citizen = given().header("Authorization", "Bearer " + admin).contentType(ContentType.JSON)
                .body(citizen(family, "alive", null)).when().post("/citizens")
                .then().statusCode(201).extract().path("id");
        Map<String, Object> renamed = citizen(family, "alive", null);
        renamed.put("firstName", "Renamed");

        // Act
        given().header("Authorization", "Bearer " + admin).contentType(ContentType.JSON)
                .body(renamed).when().put("/citizens/" + citizen).then().statusCode(200);

        // Assert
        assertEquals(List.of(), history(citizen));
    }

    private static Map<String, Object> citizen(int familyId, String status, String deathDate)
    {
        Map<String, Object> body = new HashMap<>();
        body.put("firstName", "Anna");
        body.put("lastName", "Jensen");
        body.put("birthDate", "1950-05-17");
        body.put("deathDate", deathDate);
        body.put("status", status);
        body.put("gender", "female");
        body.put("familyId", familyId);
        return body;
    }

    private static List<String> history(long citizenId)
    {
        List<String> rows = new ArrayList<>();
        try (EntityManager em = emf.createEntityManager())
        {
            em.unwrap(Session.class).doWork(connection ->
            {
                ResultSet rs = connection.createStatement().executeQuery("SELECT old_status, new_status, "
                        + "new_death_date, app_user FROM citizen_status_audit WHERE citizen_id = " + citizenId
                        + " ORDER BY id");
                while (rs.next())
                {
                    rows.add(rs.getString(1) + " -> " + rs.getString(2) + " | " + rs.getString(3) + " | "
                            + rs.getString(4));
                }
            });
        }
        return rows;
    }

    private static String login(String username, String password)
    {
        return given().contentType(ContentType.JSON).body(Map.of("username", username, "password", password))
                .when().post("/api/auth/login").then().statusCode(200).extract().path("token");
    }
}
