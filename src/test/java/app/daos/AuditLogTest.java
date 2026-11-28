package app.daos;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

import java.sql.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers requirement(s): the final project's auditing ("audit structure implemented with triggers").
 * <p>
 * A PostgreSQL container gets the real scripts in their real order: the schema, the users (08_users.sql) and the
 * audit (09_audit_log.sql). Every test makes its own family, so the tests do not depend on each other.
 */
class AuditLogTest
{
    private static final String PERMISSION_DENIED = "42501";
    private static PostgreSQLContainer<?> postgres;

    @BeforeAll
    static void startDatabase()
    {
        postgres = new PostgreSQLContainer<>("postgres:16.2")
                .withDatabaseName("smart_city")
                .withUsername("postgres")
                .withPassword("postgres")
                .withCopyFileToContainer(MountableFile.forHostPath("SQL/schema/database_schema.sql"),
                        "/docker-entrypoint-initdb.d/01_schema.sql")
                .withCopyFileToContainer(MountableFile.forHostPath("SQL/users/08_users.sql"),
                        "/docker-entrypoint-initdb.d/02_users.sql")
                .withCopyFileToContainer(MountableFile.forHostPath("SQL/audit/09_audit_log.sql"),
                        "/docker-entrypoint-initdb.d/03_audit.sql");
        postgres.start();
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

    private static long newFamily(Connection connection, String name) throws SQLException
    {
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("INSERT INTO family (family_name) VALUES ('" + name + "') RETURNING id"))
        {
            rs.next();
            return rs.getLong(1);
        }
    }

    /** The audit rows of one family, oldest first: operation | old name | new name | changed | db user | app user */
    private static List<String[]> history(long familyId) throws SQLException
    {
        try (Connection connection = admin(); Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery("SELECT operation, old_data ->> 'family_name', "
                     + "new_data ->> 'family_name', changed_fields, db_user, app_user FROM audit_log "
                     + "WHERE table_name = 'family' AND row_id = " + familyId + " ORDER BY id"))
        {
            List<String[]> rows = new java.util.ArrayList<>();
            while (rs.next())
            {
                Array changed = rs.getArray(4);
                rows.add(new String[]{rs.getString(1), rs.getString(2), rs.getString(3),
                        changed == null ? null : Arrays.toString((Object[]) changed.getArray()),
                        rs.getString(5), rs.getString(6)});
            }
            return rows;
        }
    }

    @Test
    @DisplayName("an insert is logged with the new row, the database user and the time")
    void insertIsLogged() throws SQLException
    {
        // Arrange
        long id;

        // Act
        try (Connection connection = admin())
        {
            id = newFamily(connection, "Inserted");
        }

        // Assert
        List<String[]> rows = history(id);
        assertEquals(1, rows.size());
        assertEquals("INSERT", rows.get(0)[0]);
        assertNull(rows.get(0)[1]);
        assertEquals("Inserted", rows.get(0)[2]);
        assertEquals("postgres", rows.get(0)[4]);
        assertNull(rows.get(0)[5]); // not made through the API
    }

    @Test
    @DisplayName("an update is logged with before, after, the changed columns and the API user")
    void updateIsLogged() throws SQLException
    {
        // Arrange
        long id;
        try (Connection connection = admin())
        {
            id = newFamily(connection, "Before");
        }

        // Act: what GenericDAO does at the start of a transaction
        try (Connection connection = connect("smartcity_app", "app-dev-password"); Statement statement = connection.createStatement())
        {
            connection.setAutoCommit(false);
            statement.execute("SELECT set_config('smartcity.app_user', 'User1', true)");
            statement.execute("UPDATE family SET family_name = 'After' WHERE id = " + id);
            connection.commit();
        }

        // Assert
        String[] update = history(id).get(1);
        assertEquals("UPDATE", update[0]);
        assertEquals("Before", update[1]);
        assertEquals("After", update[2]);
        assertEquals("[family_name]", update[3]);
        assertEquals("smartcity_app", update[4]);
        assertEquals("User1", update[5]);
    }

    @Test
    @DisplayName("an update that changes nothing is not logged")
    void unchangedUpdateIsNotLogged() throws SQLException
    {
        // Arrange
        long id;
        try (Connection connection = admin())
        {
            id = newFamily(connection, "Same");

            // Act
            connection.createStatement().execute("UPDATE family SET family_name = 'Same' WHERE id = " + id);
        }

        // Assert
        assertEquals(1, history(id).size());
    }

    @Test
    @DisplayName("a delete is logged with the row as it was")
    void deleteIsLogged() throws SQLException
    {
        // Arrange
        long id;
        try (Connection connection = admin())
        {
            id = newFamily(connection, "Deleted");

            // Act
            connection.createStatement().execute("DELETE FROM family WHERE id = " + id);
        }

        // Assert
        String[] delete = history(id).get(1);
        assertEquals("DELETE", delete[0]);
        assertEquals("Deleted", delete[1]);
        assertNull(delete[2]);
    }

    @Test
    @DisplayName("the API user is only kept for its own transaction")
    void appUserDoesNotLeak() throws SQLException
    {
        // Arrange
        long id;
        try (Connection connection = admin(); Statement statement = connection.createStatement())
        {
            id = newFamily(connection, "Leak");
            connection.setAutoCommit(false);
            statement.execute("SELECT set_config('smartcity.app_user', 'User1', true)");
            statement.execute("UPDATE family SET family_name = 'Leak 1' WHERE id = " + id);
            connection.commit();

            // Act: the next transaction on the same connection sets no user
            statement.execute("UPDATE family SET family_name = 'Leak 2' WHERE id = " + id);
            connection.commit();
        }

        // Assert
        List<String[]> rows = history(id);
        assertEquals("User1", rows.get(1)[5]);
        assertNull(rows.get(2)[5]);
    }

    @Test
    @DisplayName("every main table has an audit trigger, and TRUNCATE is logged too")
    void allTablesAudited() throws SQLException
    {
        try (Connection connection = admin(); Statement statement = connection.createStatement())
        {
            // Arrange
            ResultSet triggers = statement.executeQuery("SELECT count(DISTINCT event_object_table) FROM "
                    + "information_schema.triggers WHERE trigger_name LIKE 'trg_audit_%' AND event_object_table <> 'audit_log'");
            triggers.next();
            assertEquals(9, triggers.getInt(1));

            // Act
            statement.execute("TRUNCATE citizen_relationship");

            // Assert
            ResultSet truncate = statement.executeQuery("SELECT count(*) FROM audit_log "
                    + "WHERE table_name = 'citizen_relationship' AND operation = 'TRUNCATE' AND row_id IS NULL");
            truncate.next();
            assertEquals(1, truncate.getInt(1));
        }
    }

    // ---------- security ----------

    @Test
    @DisplayName("the application user cannot read, change or delete the history")
    void appUserCannotTouchTheLog() throws SQLException
    {
        try (Connection connection = connect("smartcity_app", "app-dev-password"))
        {
            for (String sql : List.of("SELECT * FROM audit_log", "UPDATE audit_log SET app_user = 'x'",
                    "DELETE FROM audit_log", "INSERT INTO audit_log (table_name, operation, row_id) VALUES ('family', 'INSERT', 1)"))
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
        try (Connection connection = admin())
        {
            newFamily(connection, "History");
        }

        try (Connection connection = connect("smartcity_admin", "admin-dev-password"))
        {
            // Act and Assert: reading is allowed, rewriting is not
            assertDoesNotThrow(() -> connection.createStatement().executeQuery("SELECT * FROM audit_log"));
            for (String sql : List.of("UPDATE audit_log SET app_user = 'x'", "DELETE FROM audit_log"))
            {
                SQLException e = assertThrows(SQLException.class, () -> connection.createStatement().execute(sql));
                assertEquals(PERMISSION_DENIED, e.getSQLState(), sql);
            }
        }
        try (Connection connection = admin())
        {
            SQLException e = assertThrows(SQLException.class,
                    () -> connection.createStatement().execute("TRUNCATE audit_log"));
            assertTrue(e.getMessage().contains("append-only"), e.getMessage());
        }
    }

    @Test
    @DisplayName("the restricted user cannot read the history (it holds salaries), the read-only user can")
    void readersOfTheLog() throws SQLException
    {
        try (Connection restricted = connect("smartcity_restricted", "restricted-dev-password");
             Connection readonly = connect("smartcity_readonly", "readonly-dev-password"))
        {
            SQLException e = assertThrows(SQLException.class,
                    () -> restricted.createStatement().executeQuery("SELECT * FROM audit_log"));
            assertEquals(PERMISSION_DENIED, e.getSQLState());
            assertDoesNotThrow(() -> readonly.createStatement().executeQuery("SELECT * FROM audit_log"));
        }
    }
}
