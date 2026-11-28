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
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Covers requirement(s): the final project's auditing, end to end: a change made through the API is logged with the
 * API user who made it (not only the database user), because GenericDAO passes it to the trigger.
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
        String audit = Files.readString(Path.of("SQL/audit/09_audit_log.sql"));
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
    @DisplayName("create, update and delete through the API are logged with the API user")
    void apiChangesAreLoggedWithTheUser()
    {
        // Arrange
        String user = login("User1", "Password123");

        // Act
        int id = given().header("Authorization", "Bearer " + user).contentType(ContentType.JSON)
                .body(Map.of("familyName", "Audited")).when().post("/families")
                .then().statusCode(201).extract().path("id");
        given().header("Authorization", "Bearer " + user).contentType(ContentType.JSON)
                .body(Map.of("familyName", "Audited Again")).when().put("/families/" + id).then().statusCode(200);
        given().header("Authorization", "Bearer " + login("admin", "AdminPassword123"))
                .when().delete("/families/" + id).then().statusCode(204);

        // Assert
        assertEquals(List.of("INSERT User1", "UPDATE User1", "DELETE admin"), history(id));
    }

    @Test
    @DisplayName("a change without a logged-in API user has no app user")
    void directChangeHasNoAppUser()
    {
        // Arrange
        long id;
        try (EntityManager em = emf.createEntityManager())
        {
            em.getTransaction().begin();

            // Act
            id = ((Number) em.createNativeQuery("INSERT INTO family (family_name) VALUES ('Direct') RETURNING id")
                    .getSingleResult()).longValue();
            em.getTransaction().commit();
        }

        // Assert
        assertEquals(List.of("INSERT null"), history(id));
    }

    private static List<String> history(long familyId)
    {
        List<String> rows = new ArrayList<>();
        try (EntityManager em = emf.createEntityManager())
        {
            em.unwrap(Session.class).doWork(connection ->
            {
                ResultSet rs = connection.createStatement().executeQuery("SELECT operation, app_user FROM audit_log "
                        + "WHERE table_name = 'family' AND row_id = " + familyId + " ORDER BY id");
                while (rs.next())
                {
                    rows.add(rs.getString(1) + " " + rs.getString(2));
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
