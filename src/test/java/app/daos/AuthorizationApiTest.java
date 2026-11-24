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
import io.restassured.specification.RequestSpecification;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.Matchers.empty;

/**
 * Covers requirement(s): FR-03 (logout), FR-04 (no valid token gives 401), FR-05 (administrator actions: the
 * seed endpoints, creating jobs and seeing or setting salaries)
 * <p>
 * Starts the real application on a free port against the test database. The users come from the startup
 * populators: admin (role admin) and User1 (role user).
 */
class AuthorizationApiTest
{
    private static final String ADMIN_PASSWORD = "AdminPassword123";
    private static final TokenSettings SETTINGS =
            new TokenSettings("smart-city-test", "1800000", "test-secret-that-is-at-least-32-characters-long");

    private static EntityManagerFactory emf;
    private static Javalin app;

    @BeforeAll
    static void startApp()
    {
        emf = HibernateConfig.getEntityManagerFactoryForTest();
        try (EntityManager em = emf.createEntityManager())
        {
            em.getTransaction().begin();
            em.createNativeQuery("TRUNCATE TABLE user_role, users, roles CASCADE").executeUpdate();
            // Hibernate creates only the tables in the test database; the view behind /active-employment comes from SQL
            em.createNativeQuery(readScript("SQL/views/active_employment.sql")).executeUpdate();
            em.getTransaction().commit();
        }
        StartupPopulator.populate(emf, ADMIN_PASSWORD);
        SecurityController securityController = new SecurityController(SETTINGS, new TokenDenylist(Clock.systemUTC()));
        app = Main.createApp(emf, securityController).start(0);
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = app.port();
    }

    @AfterAll
    static void stopApp()
    {
        try (EntityManager em = emf.createEntityManager())
        {
            em.getTransaction().begin();
            em.createNativeQuery("DROP VIEW IF EXISTS v_active_employment").executeUpdate();
            em.getTransaction().commit();
        }
        app.stop();
        RestAssured.reset();
    }

    @Test
    @DisplayName("reading without a token gives 401")
    void readWithoutToken()
    {
        given().when().get("/locations").then().statusCode(401);
    }

    @Test
    @DisplayName("reading with a garbage token gives 401, not 500")
    void readWithGarbageToken()
    {
        given().header("Authorization", "Bearer not.a.token").when().get("/locations").then().statusCode(401);
    }

    @Test
    @DisplayName("a normal user can read city data")
    void userCanRead()
    {
        as("User1", "Password123").when().get("/locations").then().statusCode(200);
    }

    @Test
    @DisplayName("an admin can read city data")
    void adminCanRead()
    {
        as("admin", ADMIN_PASSWORD).when().get("/citizens").then().statusCode(200);
    }

    @ParameterizedTest(name = "a normal user passes the role check when creating on {0}")
    @ValueSource(strings = {"/locations", "/families", "/housings", "/citizens", "/citizen-relationships",
            "/institutions", "/companies", "/education-records"})
    @DisplayName("a normal user may create on every resource except jobs")
    void userMayCreate(String path)
    {
        // The empty body is rejected by validation, which runs only after the role check has let the user through
        as("User1", "Password123").contentType(ContentType.JSON).body(Map.of()).when().post(path)
                .then().statusCode(allOf(not(401), not(403)));
    }

    @Test
    @DisplayName("a normal user can create, update and delete")
    void userCanCreateUpdateDelete()
    {
        String user = login("User1", "Password123");
        int id = create(user, "/families", Map.of("familyName", "Usersen"));

        withToken(user).contentType(ContentType.JSON).body(Map.of("familyName", "Usersen-Hansen"))
                .when().put("/families/" + id)
                .then().statusCode(200).body("familyName", equalTo("Usersen-Hansen"));

        withToken(user).when().delete("/families/" + id).then().statusCode(204);
    }

    @Test
    @DisplayName("a normal user cannot create a job, because a job needs a salary (403)")
    void userCannotCreateJob()
    {
        Employment employment = employment();

        as("User1", "Password123").contentType(ContentType.JSON).body(jobBody(employment, "Developer", 35000))
                .when().post("/jobs").then().statusCode(403);

        employment.delete();
    }

    @Test
    @DisplayName("a normal user sees jobs without the salary, an admin sees it")
    void salaryHiddenFromUser()
    {
        Employment employment = employment();
        String user = login("User1", "Password123");

        withToken(user).when().get("/jobs/" + employment.jobId)
                .then().statusCode(200).body("title", equalTo("Developer")).body("salary", nullValue());
        withToken(user).when().get("/jobs").then().statusCode(200)
                .body("data", not(empty()))
                .body("data.salary", everyItem(nullValue()));
        assertEquals(0, new BigDecimal("35000").compareTo(salaryAsAdmin(employment.jobId)));

        employment.delete();
    }

    @ParameterizedTest(name = "a normal user gets 403 for {0}")
    @ValueSource(strings = {"/jobs?salaryFrom=1", "/jobs?salaryTo=100000", "/jobs?sort=salary,asc",
            "/jobs?sort=salary,desc", "/jobs?sort=Salary,asc"})
    @DisplayName("a normal user cannot filter or sort jobs by salary, which would reveal the hidden values")
    void userCannotUseSalaryInTheList(String path)
    {
        as("User1", "Password123").when().get(path)
                .then().statusCode(403).body("error", equalTo("Only an administrator can filter or sort by salary"));
    }

    @Test
    @DisplayName("a normal user can still filter and sort jobs by other fields")
    void userCanUseOtherJobParameters()
    {
        as("User1", "Password123").when().get("/jobs?title=dev&sort=title,asc&size=5").then().statusCode(200);
    }

    @Test
    @DisplayName("an admin can filter and sort jobs by salary")
    void adminCanUseSalaryInTheList()
    {
        Employment employment = employment();

        as("admin", ADMIN_PASSWORD).when().get("/jobs?salaryFrom=30000&salaryTo=40000&sort=salary,desc")
                .then().statusCode(200)
                .body("data", not(empty()))
                .body("data.salary", everyItem(not(nullValue())));

        employment.delete();
    }

    @Test
    @DisplayName("active employment: a normal user gets no salaries, an admin gets them")
    void activeEmploymentHidesSalaryFromUser()
    {
        Employment employment = employment();

        as("User1", "Password123").when().get("/active-employment")
                .then().statusCode(200)
                .body("citizen_id", not(empty()))
                .body("salary", everyItem(nullValue()));
        as("admin", ADMIN_PASSWORD).when().get("/active-employment")
                .then().statusCode(200)
                .body("salary", everyItem(not(nullValue())));

        employment.delete();
    }

    @Test
    @DisplayName("a normal user who sends a salary in an update gets 403 and the salary stays")
    void userCannotSetSalary()
    {
        Employment employment = employment();

        as("User1", "Password123").contentType(ContentType.JSON).body(jobBody(employment, "Developer", 1))
                .when().put("/jobs/" + employment.jobId)
                .then().statusCode(403).body("error", equalTo("Only an administrator can set the salary"));

        assertEquals(0, new BigDecimal("35000").compareTo(salaryAsAdmin(employment.jobId)));
        employment.delete();
    }

    @Test
    @DisplayName("a normal user can update a job without the salary, and the stored salary is kept")
    void userUpdateKeepsSalary()
    {
        Employment employment = employment();
        Map<String, Object> body = jobBody(employment, "Senior developer", 0);
        body.remove("salary");

        as("User1", "Password123").contentType(ContentType.JSON).body(body).when().put("/jobs/" + employment.jobId)
                .then().statusCode(200).body("title", equalTo("Senior developer")).body("salary", nullValue());

        assertEquals(0, new BigDecimal("35000").compareTo(salaryAsAdmin(employment.jobId)));
        employment.delete();
    }

    @Test
    @DisplayName("an admin can change the salary")
    void adminCanChangeSalary()
    {
        Employment employment = employment();

        as("admin", ADMIN_PASSWORD).contentType(ContentType.JSON).body(jobBody(employment, "Developer", 45000))
                .when().put("/jobs/" + employment.jobId).then().statusCode(200);

        assertEquals(0, new BigDecimal("45000").compareTo(salaryAsAdmin(employment.jobId)));
        employment.delete();
    }

    @Test
    @DisplayName("a normal user can delete a job")
    void userCanDeleteJob()
    {
        Employment employment = employment();

        as("User1", "Password123").when().delete("/jobs/" + employment.jobId).then().statusCode(204);

        as("admin", ADMIN_PASSWORD).when().get("/jobs/" + employment.jobId).then().statusCode(404);
        employment.jobId = null;
        employment.delete();
    }

    @Test
    @DisplayName("a normal user cannot call the admin seed endpoints (403)")
    void userCannotSeed()
    {
        as("User1", "Password123").when().post("/api/admin/seed/fake").then().statusCode(403);
    }

    @Test
    @DisplayName("writing without a token gives 401")
    void writeWithoutToken()
    {
        given().contentType(ContentType.JSON).body(Map.of("name", "x")).when().post("/locations")
                .then().statusCode(401);
    }

    @Test
    @DisplayName("an admin can create, update and delete")
    void adminCanWrite()
    {
        int id = as("admin", ADMIN_PASSWORD).contentType(ContentType.JSON)
                .body(Map.of("name", "Authorization Test Location", "streetAddress", "Rådhuspladsen 1"))
                .when().post("/locations")
                .then().statusCode(201).extract().path("id");

        as("admin", ADMIN_PASSWORD).contentType(ContentType.JSON)
                .body(Map.of("name", "Renamed Location", "streetAddress", "Rådhuspladsen 1"))
                .when().put("/locations/" + id)
                .then().statusCode(200).body("name", equalTo("Renamed Location"));

        as("admin", ADMIN_PASSWORD).when().delete("/locations/" + id).then().statusCode(204);
    }

    @Test
    @DisplayName("after logout the same token is rejected with 401, and a new login works")
    void logoutRevokesToken()
    {
        String token = login("User1", "Password123");
        withToken(token).when().get("/locations").then().statusCode(200);

        withToken(token).when().post("/api/auth/logout").then().statusCode(200).body("msg", equalTo("Logged out"));

        withToken(token).when().get("/locations").then().statusCode(401);
        withToken(token).when().post("/api/auth/logout").then().statusCode(401);
        withToken(login("User1", "Password123")).when().get("/locations").then().statusCode(200);
    }

    @Test
    @DisplayName("logging out one token leaves the user's other tokens working")
    void logoutOnlyThatToken()
    {
        String first = login("admin", ADMIN_PASSWORD);
        String second = login("admin", ADMIN_PASSWORD);

        withToken(first).when().post("/api/auth/logout").then().statusCode(200);

        withToken(first).when().get("/locations").then().statusCode(401);
        withToken(second).when().get("/locations").then().statusCode(200);
    }

    @Test
    @DisplayName("logout without a token gives 401")
    void logoutWithoutToken()
    {
        given().when().post("/api/auth/logout").then().statusCode(401);
    }

    @Test
    @DisplayName("register and login stay open to anyone")
    void registerAndLoginAreOpen()
    {
        given().contentType(ContentType.JSON).body(Map.of("username", "new_api_user", "password", "Password123"))
                .when().post("/api/auth/register").then().statusCode(201);
        withToken(login("new_api_user", "Password123")).when().get("/families").then().statusCode(200);
        withToken(login("new_api_user", "Password123")).contentType(ContentType.JSON).body(Map.of())
                .when().post("/jobs").then().statusCode(403);
    }

    /**
     * A job (salary 35000) with the location, family, citizen and company it needs, all created by the admin.
     */
    private static Employment employment()
    {
        String admin = login("admin", ADMIN_PASSWORD);
        Employment employment = new Employment();
        employment.admin = admin;
        employment.locationId = create(admin, "/locations",
                Map.of("name", "Salary Test Location", "streetAddress", "Testvej 1"));
        employment.familyId = create(admin, "/families", Map.of("familyName", "Salarysen"));
        employment.citizenId = create(admin, "/citizens", Map.of("firstName", "Bo", "lastName", "Salarysen",
                "birthDate", "1990-05-17", "status", "alive", "gender", "male", "familyId", employment.familyId));
        employment.companyId = create(admin, "/companies", Map.of("locationId", employment.locationId,
                "name", "Salary Ltd", "foundedDate", "2001-03-04", "status", "active"));
        employment.jobId = create(admin, "/jobs", jobBody(employment, "Developer", 35000));
        return employment;
    }

    private static Map<String, Object> jobBody(Employment employment, String title, int salary)
    {
        Map<String, Object> body = new HashMap<>();
        body.put("citizenId", employment.citizenId);
        body.put("companyId", employment.companyId);
        body.put("title", title);
        body.put("salary", salary);
        body.put("startDate", "2015-09-01");
        body.put("status", "active");
        return body;
    }

    private static BigDecimal salaryAsAdmin(int jobId)
    {
        Object salary = as("admin", ADMIN_PASSWORD).when().get("/jobs/" + jobId)
                .then().statusCode(200).extract().path("salary");
        return new BigDecimal(salary.toString());
    }

    private static String readScript(String path)
    {
        try
        {
            return Files.readString(Path.of(path));
        }
        catch (java.io.IOException e)
        {
            throw new IllegalStateException("Cannot read " + path, e);
        }
    }

    private static int create(String token, String path, Map<String, Object> body)
    {
        return withToken(token).contentType(ContentType.JSON).body(body).when().post(path)
                .then().statusCode(201).extract().path("id");
    }

    private static final class Employment
    {
        private String admin;
        private Integer locationId;
        private Integer familyId;
        private Integer citizenId;
        private Integer companyId;
        private Integer jobId;

        // Removes everything in reverse order, so the next test starts from the same state
        void delete()
        {
            if (jobId != null)
            {
                withToken(admin).when().delete("/jobs/" + jobId).then().statusCode(204);
            }
            withToken(admin).when().delete("/citizens/" + citizenId).then().statusCode(204);
            withToken(admin).when().delete("/companies/" + companyId).then().statusCode(204);
            withToken(admin).when().delete("/families/" + familyId).then().statusCode(204);
            withToken(admin).when().delete("/locations/" + locationId).then().statusCode(204);
        }
    }

    private static String login(String username, String password)
    {
        return given().contentType(ContentType.JSON).body(Map.of("username", username, "password", password))
                .when().post("/api/auth/login")
                .then().statusCode(200).extract().path("token");
    }

    private static RequestSpecification as(String username, String password)
    {
        return withToken(login(username, password));
    }

    private static RequestSpecification withToken(String token)
    {
        return given().header("Authorization", "Bearer " + token);
    }
}