package app.daos;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers requirement(s): the final project's auditing ("audit structure implemented with triggers"): a change of a
 * citizen's status is logged in citizen_status_audit.
 * <p>
 * A PostgreSQL container gets the real scripts in their real order: the schema (with its own triggers, such as
 * update_citizen_status, which sets the status from the death date), the users (08_users.sql) and the audit
 * (09_citizen_status_audit.sql). Every test makes its own citizen, so the tests do not depend on each other.
 */
class CitizenStatusAuditTest
{
    private static final String PERMISSION_DENIED = "42501";
    private static PostgreSQLContainer<?> postgres;
    private static long familyId;

    @BeforeAll
    static void startDatabase() throws SQLException
    {
        postgres = new PostgreSQLContainer<>("postgres:16.2")
                .withDatabaseName("smart_city")
                .withUsername("postgres")
                .withPassword("postgres")
                .withCopyFileToContainer(MountableFile.forHostPath("SQL/schema/database_schema.sql"),
                        "/docker-entrypoint-initdb.d/01_schema.sql")
                .withCopyFileToContainer(MountableFile.forHostPath("SQL/users/08_users.sql"),
                        "/docker-entrypoint-initdb.d/02_users.sql")
                .withCopyFileToContainer(MountableFile.forHostPath("SQL/audit/09_citizen_status_audit.sql"),
                        "/docker-entrypoint-initdb.d/03_audit.sql");
        postgres.start();
        try (Connection connection = admin())
        {
            familyId = id(connection, "INSERT INTO family (family_name) VALUES ('Audit') RETURNING id");
        }
    }

    @AfterAll
    static void stopDatabase()
    {
        postgres.stop();
    }

    private static Connection connect(String user, String password) throws SQLException
    {
        return DriverManager.getConnection(postgres.getJdbcUrl(), user, password);
    }

    private static Connection admin() throws SQLException
    {
        return connect("postgres", "postgres");
    }

    private static long id(Connection connection, String insert) throws SQLException
    {
        try (Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(insert))
        {
            rs.next();
            return rs.getLong(1);
        }
    }

    private static long newCitizen() throws SQLException
    {
        try (Connection connection = admin())
        {
            return id(connection, "INSERT INTO citizen (first_name, last_name, birth_date, status, family_id) "
                    + "VALUES ('Anna', 'Jensen', '1950-05-17', 'alive', " + familyId + ") RETURNING id");
        }
    }

    private static void execute(String user, String password, String... statements) throws SQLException
    {
        try (Connection connection = connect(user, password); Statement statement = connection.createStatement())
        {
            connection.setAutoCommit(false);
            for (String sql : statements)
            {
                statement.execute(sql);
            }
            connection.commit();
        }
    }

    /** The audit rows of one citizen, oldest first: "old -> new | old date -> new date | db user | app user" */
    private static List<String> history(long citizenId) throws SQLException
    {
        try (Connection connection = admin(); Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT old_status, new_status, old_death_date, new_death_date, "
                     + "db_user, app_user FROM citizen_status_audit WHERE citizen_id = " + citizenId + " ORDER BY id"))
        {
            List<String> rows = new ArrayList<>();
            while (rs.next())
            {
                rows.add(rs.getString(1) + " -> " + rs.getString(2) + " | " + rs.getString(3) + " -> "
                        + rs.getString(4) + " | " + rs.getString(5) + " | " + rs.getString(6));
            }
            return rows;
        }
    }

    @Test
    @DisplayName("a status change is logged with old and new status, the death date and the API user")
    void statusChangeIsLogged() throws SQLException
    {
        // Arrange
        long id = newCitizen();

        // Act: what the backend does for PUT /citizens/{id} (GenericDAO sets the API user first)
        execute("smartcity_app", "app-dev-password",
                "SELECT set_config('smartcity.app_user', 'User1', true)",
                "UPDATE citizen SET status = 'deceased', death_date = '2024-05-01' WHERE id = " + id);

        // Assert
        assertEquals(List.of("alive -> deceased | null -> 2024-05-01 | smartcity_app | User1"), history(id));
    }

    @Test
    @DisplayName("a status set by the schema's own trigger (from the death date) is logged too")
    void statusSetByTriggerIsLogged() throws SQLException
    {
        // Arrange
        long id = newCitizen();

        // Act: only the death date; update_citizen_status turns the status into "deceased"
        execute("postgres", "postgres", "UPDATE citizen SET death_date = '2023-01-02' WHERE id = " + id);

        // Assert
        assertEquals(List.of("alive -> deceased | null -> 2023-01-02 | postgres | null"), history(id));
    }

    @Test
    @DisplayName("a change back to alive is logged as well")
    void backToAlive() throws SQLException
    {
        // Arrange
        long id = newCitizen();
        execute("postgres", "postgres", "UPDATE citizen SET death_date = '2023-01-02' WHERE id = " + id);

        // Act
        execute("postgres", "postgres", "UPDATE citizen SET status = 'alive', death_date = NULL WHERE id = " + id);

        // Assert
        assertEquals("deceased -> alive | 2023-01-02 -> null | postgres | null", history(id).get(1));
    }

    @Test
    @DisplayName("updates that do not change the status, and inserts, are not logged")
    void otherChangesAreNotLogged() throws SQLException
    {
        // Arrange
        long id = newCitizen();

        // Act
        execute("postgres", "postgres",
                "UPDATE citizen SET first_name = 'Anne' WHERE id = " + id,
                "UPDATE citizen SET status = 'alive' WHERE id = " + id);

        // Assert
        assertEquals(List.of(), history(id));
    }

    @Test
    @DisplayName("the API user is only kept for its own transaction")
    void appUserDoesNotLeak() throws SQLException
    {
        // Arrange
        long id = newCitizen();
        try (Connection connection = admin(); Statement statement = connection.createStatement())
        {
            connection.setAutoCommit(false);
            statement.execute("SELECT set_config('smartcity.app_user', 'User1', true)");
            statement.execute("UPDATE citizen SET death_date = '2023-01-02' WHERE id = " + id);
            connection.commit();

            // Act: the next transaction on the same connection sets no user
            statement.execute("UPDATE citizen SET status = 'alive', death_date = NULL WHERE id = " + id);
            connection.commit();
        }

        // Assert
        List<String> rows = history(id);
        assertTrue(rows.get(0).endsWith("| User1"), rows.get(0));
        assertTrue(rows.get(1).endsWith("| null"), rows.get(1));
    }

    // ---------- security ----------

    @Test
    @DisplayName("the application user cannot read, write, change or delete the history")
    void appUserCannotTouchTheLog() throws SQLException
    {
        try (Connection connection = connect("smartcity_app", "app-dev-password"))
        {
            for (String sql : List.of("SELECT * FROM citizen_status_audit",
                    "INSERT INTO citizen_status_audit (citizen_id, old_status, new_status) VALUES (1, 'alive', 'deceased')",
                    "UPDATE citizen_status_audit SET app_user = 'x'", "DELETE FROM citizen_status_audit"))
            {
                SQLException e = assertThrows(SQLException.class, () -> connection.createStatement().execute(sql));
                assertEquals(PERMISSION_DENIED, e.getSQLState(), sql);
            }
        }
    }

    @Test
    @DisplayName("the history is append-only, even for the admin user")
    void appendOnlyForEveryone() throws SQLException
    {
        // Arrange
        long id = newCitizen();
        execute("postgres", "postgres", "UPDATE citizen SET death_date = '2023-01-02' WHERE id = " + id);

        try (Connection connection = connect("smartcity_admin", "admin-dev-password"))
        {
            // Act and Assert: reading is allowed, rewriting is not
            assertDoesNotThrow(() -> connection.createStatement().executeQuery("SELECT * FROM citizen_status_audit"));
            for (String sql : List.of("UPDATE citizen_status_audit SET app_user = 'x'", "DELETE FROM citizen_status_audit"))
            {
                SQLException e = assertThrows(SQLException.class, () -> connection.createStatement().execute(sql));
                assertEquals(PERMISSION_DENIED, e.getSQLState(), sql);
            }
        }
        try (Connection connection = admin())
        {
            SQLException e = assertThrows(SQLException.class,
                    () -> connection.createStatement().execute("TRUNCATE citizen_status_audit"));
            assertTrue(e.getMessage().contains("append-only"), e.getMessage());
        }
    }

    @Test
    @DisplayName("the restricted user cannot read the history, the read-only user can")
    void readersOfTheLog() throws SQLException
    {
        try (Connection restricted = connect("smartcity_restricted", "restricted-dev-password");
             Connection readonly = connect("smartcity_readonly", "readonly-dev-password"))
        {
            SQLException e = assertThrows(SQLException.class,
                    () -> restricted.createStatement().executeQuery("SELECT * FROM citizen_status_audit"));
            assertEquals(PERMISSION_DENIED, e.getSQLState());
            assertDoesNotThrow(() -> readonly.createStatement().executeQuery("SELECT * FROM citizen_status_audit"));
        }
    }
}
