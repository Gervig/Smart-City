package migrator;

import migrator.source.Snapshot;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * A small city used by all tests. The SQL inserts and the snapshot hold the same data:
 * 2 locations (only the first has coordinates), 1 family, 1 housing, 1 company, 1 institution,
 * 3 citizens (Anna and Bo live in the housing, Carl has no housing), Anna works at the company and Bo at the
 * institution, Anna studied at the institution, Anna and Bo are married (type marriage), and 2 users.
 */
public final class SampleData
{
    public static final List<String> SQL_INSERTS = List.of(
            "INSERT INTO location (id, name, street_address, latitude, longitude, osm_id, osm_type) OVERRIDING SYSTEM VALUE VALUES (1, 'Town Hall', 'Rådhuspladsen 1', 55.6761, 12.5683, 123, 'node')",
            "INSERT INTO location (id, name, street_address) OVERRIDING SYSTEM VALUE VALUES (2, 'School Street', 'Skolevej 2')",
            "INSERT INTO family (id, family_name) OVERRIDING SYSTEM VALUE VALUES (1, 'Jensen')",
            "INSERT INTO housing (id, location_id, housing_type, capacity) OVERRIDING SYSTEM VALUE VALUES (1, 1, 'apartment', 3)",
            "INSERT INTO company (id, location_id, name, founded_date, status) OVERRIDING SYSTEM VALUE VALUES (1, 1, 'Acme ApS', '2001-03-04', 'active')",
            "INSERT INTO institution (id, location_id, name, type) OVERRIDING SYSTEM VALUE VALUES (1, 2, 'City School', 'high_school')",
            "INSERT INTO citizen (id, first_name, last_name, birth_date, status, gender, family_id, housing_id) OVERRIDING SYSTEM VALUE VALUES (1, 'Anna', 'Jensen', '1990-05-17', 'alive', 'female', 1, 1)",
            "INSERT INTO citizen (id, first_name, last_name, birth_date, status, gender, family_id, housing_id) OVERRIDING SYSTEM VALUE VALUES (2, 'Bo', 'Jensen', '1988-01-02', 'alive', 'male', 1, 1)",
            "INSERT INTO citizen (id, first_name, last_name, birth_date, status, gender, family_id) OVERRIDING SYSTEM VALUE VALUES (3, 'Carl', 'Jensen', '2015-09-30', 'alive', 'male', 1)",
            "INSERT INTO job (id, citizen_id, company_id, title, salary, start_date, status) OVERRIDING SYSTEM VALUE VALUES (1, 1, 1, 'Developer', 42000.50, '2015-09-01', 'active')",
            "INSERT INTO job (id, citizen_id, institution_id, title, salary, start_date, status) OVERRIDING SYSTEM VALUE VALUES (2, 2, 1, 'Teacher', 38000.00, '2012-08-01', 'active')",
            "INSERT INTO education_record (id, citizen_id, institution_id, start_date, end_date) OVERRIDING SYSTEM VALUE VALUES (1, 1, 1, '2006-08-15', '2009-06-20')",
            "INSERT INTO citizen_relationship (id, citizen_id_1, citizen_id_2, relationship_type, start_date) OVERRIDING SYSTEM VALUE VALUES (1, 1, 2, 'marriage', '2018-06-01')",
            "INSERT INTO roles (name) VALUES ('user'), ('admin')",
            "INSERT INTO users (username, password) VALUES ('admin', '$2a$10$hashA'), ('User1', '$2a$10$hashU')",
            "INSERT INTO user_role (user_id, role_id) VALUES ('admin', 'admin'), ('User1', 'user')");

    private SampleData()
    {
    }

    public static Snapshot snapshot()
    {
        return new Snapshot(
                List.of(new LocationRow(1, "Town Hall", "Rådhuspladsen 1", 55.6761, 12.5683, 123L, "node"),
                        new LocationRow(2, "School Street", "Skolevej 2", null, null, null, null)),
                List.of(new FamilyRow(1, "Jensen")),
                List.of(new HousingRow(1, 1, "apartment", 3, null, null, null)),
                List.of(new CompanyRow(1, 1, "Acme ApS", LocalDate.of(2001, 3, 4), null, "active",
                        null, null, null, null, null, null, null)),
                List.of(new InstitutionRow(1, 2, "City School", "high_school", null, null, null)),
                List.of(new CitizenRow(1, "Anna", "Jensen", LocalDate.of(1990, 5, 17), null, "alive", "female", 1, 1L),
                        new CitizenRow(2, "Bo", "Jensen", LocalDate.of(1988, 1, 2), null, "alive", "male", 1, 1L),
                        new CitizenRow(3, "Carl", "Jensen", LocalDate.of(2015, 9, 30), null, "alive", "male", 1, null)),
                List.of(new JobRow(1, 1, 1L, null, "Developer", new BigDecimal("42000.50"),
                                LocalDate.of(2015, 9, 1), null, "active"),
                        new JobRow(2, 2, null, 1L, "Teacher", new BigDecimal("38000.00"),
                                LocalDate.of(2012, 8, 1), null, "active")),
                List.of(new EducationRow(1, 1, 1, LocalDate.of(2006, 8, 15), LocalDate.of(2009, 6, 20))),
                List.of(new RelationshipRow(1, 1, 2, "marriage", LocalDate.of(2018, 6, 1), null)),
                List.of(new UserRow("admin", "$2a$10$hashA", List.of("admin")),
                        new UserRow("User1", "$2a$10$hashU", List.of("user"))));
    }
}
