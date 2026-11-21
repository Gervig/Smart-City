package migrator.source;

import migrator.source.Snapshot.CitizenRow;
import migrator.source.Snapshot.CompanyRow;
import migrator.source.Snapshot.EducationRow;
import migrator.source.Snapshot.FamilyRow;
import migrator.source.Snapshot.HousingRow;
import migrator.source.Snapshot.InstitutionRow;
import migrator.source.Snapshot.JobRow;
import migrator.source.Snapshot.LocationRow;
import migrator.source.Snapshot.RelationshipRow;
import migrator.source.Snapshot.UserRow;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads every table in one read-only REPEATABLE READ transaction, so all tables are seen as they were at the same
 * moment even if the backend writes meanwhile. The queries are fixed strings without user input.
 */
public class SqlReader
{
    private final String url;
    private final String user;
    private final String password;

    public SqlReader(String url, String user, String password)
    {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public Snapshot read() throws SQLException
    {
        try (Connection connection = DriverManager.getConnection(url, user, password))
        {
            connection.setAutoCommit(false);
            connection.setReadOnly(true);
            connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
            try (Statement statement = connection.createStatement())
            {
                Snapshot snapshot = new Snapshot(
                        query(statement, "SELECT * FROM location ORDER BY id", rs -> new LocationRow(
                                rs.getLong("id"), rs.getString("name"), rs.getString("street_address"),
                                rs.getObject("latitude", Double.class), rs.getObject("longitude", Double.class),
                                rs.getObject("osm_id", Long.class), rs.getString("osm_type"))),
                        query(statement, "SELECT * FROM family ORDER BY id", rs -> new FamilyRow(
                                rs.getLong("id"), rs.getString("family_name"))),
                        query(statement, "SELECT * FROM housing ORDER BY id", rs -> new HousingRow(
                                rs.getLong("id"), rs.getLong("location_id"), rs.getString("housing_type"),
                                rs.getInt("capacity"), rs.getObject("number_of_levels", Integer.class),
                                rs.getObject("number_of_flats", Integer.class), rs.getObject("height", Double.class))),
                        query(statement, "SELECT * FROM company ORDER BY id", rs -> new CompanyRow(
                                rs.getLong("id"), rs.getLong("location_id"), rs.getString("name"),
                                date(rs, "founded_date"), date(rs, "dissolved_date"), rs.getString("status"),
                                rs.getString("cvr_number"), rs.getString("production_unit_number"),
                                rs.getString("website"), rs.getString("phone"), rs.getString("email"),
                                rs.getString("category"), rs.getString("opening_hours"))),
                        query(statement, "SELECT * FROM institution ORDER BY id", rs -> new InstitutionRow(
                                rs.getLong("id"), rs.getLong("location_id"), rs.getString("name"),
                                rs.getString("type"), rs.getString("website"), rs.getString("phone"),
                                rs.getString("email"))),
                        query(statement, "SELECT * FROM citizen ORDER BY id", rs -> new CitizenRow(
                                rs.getLong("id"), rs.getString("first_name"), rs.getString("last_name"),
                                date(rs, "birth_date"), date(rs, "death_date"), rs.getString("status"),
                                rs.getString("gender"), rs.getLong("family_id"), rs.getObject("housing_id", Long.class))),
                        query(statement, "SELECT * FROM job ORDER BY id", rs -> new JobRow(
                                rs.getLong("id"), rs.getLong("citizen_id"), rs.getObject("company_id", Long.class),
                                rs.getObject("institution_id", Long.class), rs.getString("title"),
                                rs.getBigDecimal("salary"), date(rs, "start_date"), date(rs, "end_date"),
                                rs.getString("status"))),
                        query(statement, "SELECT * FROM education_record ORDER BY id", rs -> new EducationRow(
                                rs.getLong("id"), rs.getLong("citizen_id"), rs.getLong("institution_id"),
                                date(rs, "start_date"), date(rs, "end_date"))),
                        query(statement, "SELECT * FROM citizen_relationship ORDER BY id", rs -> new RelationshipRow(
                                rs.getLong("id"), rs.getLong("citizen_id_1"), rs.getLong("citizen_id_2"),
                                rs.getString("relationship_type"), date(rs, "start_date"), date(rs, "end_date"))),
                        users(statement));
                connection.commit();
                return snapshot;
            }
        }
    }

    private static List<UserRow> users(Statement statement) throws SQLException
    {
        Map<String, String> passwords = new LinkedHashMap<>();
        Map<String, List<String>> roles = new LinkedHashMap<>();
        try (ResultSet rs = statement.executeQuery("SELECT username, password FROM users ORDER BY username"))
        {
            while (rs.next())
            {
                passwords.put(rs.getString("username"), rs.getString("password"));
                roles.put(rs.getString("username"), new ArrayList<>());
            }
        }
        try (ResultSet rs = statement.executeQuery("SELECT user_id, role_id FROM user_role ORDER BY user_id, role_id"))
        {
            while (rs.next())
            {
                roles.get(rs.getString("user_id")).add(rs.getString("role_id"));
            }
        }
        List<UserRow> users = new ArrayList<>();
        passwords.forEach((username, hash) -> users.add(new UserRow(username, hash, List.copyOf(roles.get(username)))));
        return users;
    }

    private static <T> List<T> query(Statement statement, String sql, RowMapper<T> mapper) throws SQLException
    {
        List<T> rows = new ArrayList<>();
        try (ResultSet rs = statement.executeQuery(sql))
        {
            while (rs.next())
            {
                rows.add(mapper.map(rs));
            }
        }
        return rows;
    }

    private static LocalDate date(ResultSet rs, String column) throws SQLException
    {
        return rs.getObject(column, LocalDate.class);
    }

    @FunctionalInterface
    private interface RowMapper<T>
    {
        T map(ResultSet rs) throws SQLException;
    }
}
