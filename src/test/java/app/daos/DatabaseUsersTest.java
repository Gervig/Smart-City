package app.daos;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Checks the database users from SQL/users/08_users.sql against a real PostgreSQL container.
 * PostgreSQL reports "permission denied" with SQLSTATE 42501.
 */
class DatabaseUsersTest
{
    private static final String PERMISSION_DENIED = "42501";
    private static PostgreSQLContainer<?> postgres;

    @BeforeAll
    static void startDatabase()
    {
        postgres = new PostgreSQLContainer<>("postgres:16.2")
                .withDatabaseName("smart_city")
                .withUsername("postgres")   // the scripts refer to the role postgres
                .withPassword("postgres")
                .withCopyFileToContainer(MountableFile.forHostPath("SQL/schema/database_schema.sql"),
                        "/docker-entrypoint-initdb.d/01_schema.sql")
                .withCopyFileToContainer(MountableFile.forHostPath("SQL/users/08_users.sql"),
                        "/docker-entrypoint-initdb.d/02_users.sql");
        postgres.start();
    }

    @AfterAll
    static void stopDatabase()
    {
        postgres.stop();
    }

    private void executeAs(String user, String password, String sql) throws SQLException
    {
        try (Connection connection = DriverManager.getConnection(postgres.getJdbcUrl(), user, password);
             Statement statement = connection.createStatement())
        {
            statement.execute(sql);
        }
    }

    private String sqlStateOf(SQLException exception)
    {
        return exception.getSQLState();
    }

    @Test
    void appUserCanChangeRows() throws SQLException
    {
        // Arrange
        String sql = "INSERT INTO family (family_name) VALUES ('Hansen')";

        // Act and assert
        assertDoesNotThrow(() -> executeAs("smartcity_app", "app-dev-password", sql));
    }

    @Test
    void appUserCannotDropTables()
    {
        // Arrange
        String sql = "DROP TABLE job";

        // Act
        SQLException exception = assertThrows(SQLException.class,
                () -> executeAs("smartcity_app", "app-dev-password", sql));

        // Assert
        assertEquals(PERMISSION_DENIED, sqlStateOf(exception));
    }

    @Test
    void appUserCannotCreateTables()
    {
        // Arrange
        String sql = "CREATE TABLE extra (id INT)";

        // Act
        SQLException exception = assertThrows(SQLException.class,
                () -> executeAs("smartcity_app", "app-dev-password", sql));

        // Assert
        assertEquals(PERMISSION_DENIED, sqlStateOf(exception));
    }

    @Test
    void readOnlyUserCanReadButNotWrite()
    {
        // Arrange
        String read = "SELECT count(*) FROM citizen";
        String write = "DELETE FROM citizen";

        // Act
        SQLException exception = assertThrows(SQLException.class,
                () -> executeAs("smartcity_readonly", "readonly-dev-password", write));

        // Assert
        assertDoesNotThrow(() -> executeAs("smartcity_readonly", "readonly-dev-password", read));
        assertEquals(PERMISSION_DENIED, sqlStateOf(exception));
    }

    @Test
    void restrictedUserCannotReadSalary()
    {
        // Arrange
        String sql = "SELECT salary FROM job";

        // Act
        SQLException exception = assertThrows(SQLException.class,
                () -> executeAs("smartcity_restricted", "restricted-dev-password", sql));

        // Assert
        assertEquals(PERMISSION_DENIED, sqlStateOf(exception));
    }

    @Test
    void restrictedUserCanReadOtherJobColumns()
    {
        // Arrange
        String sql = "SELECT id, title FROM job";

        // Act and assert
        assertDoesNotThrow(() -> executeAs("smartcity_restricted", "restricted-dev-password", sql));
    }

    @Test
    void restrictedUserCannotReadUsersOrSalaryViews()
    {
        // Arrange
        String users = "SELECT * FROM users";
        String view = "SELECT * FROM v_active_employment";

        // Act
        SQLException usersError = assertThrows(SQLException.class,
                () -> executeAs("smartcity_restricted", "restricted-dev-password", users));
        SQLException viewError = assertThrows(SQLException.class,
                () -> executeAs("smartcity_restricted", "restricted-dev-password", view));

        // Assert
        assertEquals(PERMISSION_DENIED, sqlStateOf(usersError));
        assertEquals(PERMISSION_DENIED, sqlStateOf(viewError));
    }

    @Test
    void adminUserCanChangeTheSchema()
    {
        // Arrange
        String create = "CREATE TABLE admin_check (id INT)";
        String drop = "DROP TABLE admin_check";

        // Act and assert
        assertDoesNotThrow(() -> executeAs("smartcity_admin", "admin-dev-password", create));
        assertDoesNotThrow(() -> executeAs("smartcity_admin", "admin-dev-password", drop));
    }
}
