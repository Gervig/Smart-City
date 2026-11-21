package migrator.graph;

import migrator.source.Snapshot;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Turns the relational rows into graph steps: first every node, then every relationship.
 * <ul>
 *     <li>Nodes: Location, Family, Housing, Company, Institution, Citizen, User, Role. The relational id is kept as
 *     the property {@code id} (username / name for User / Role).</li>
 *     <li>Foreign keys become relationships without properties: (Citizen)-[:MEMBER_OF]->(Family),
 *     (Citizen)-[:LIVES_IN]->(Housing), (Housing|Company|Institution)-[:LOCATED_AT]->(Location),
 *     (User)-[:HAS_ROLE]->(Role).</li>
 *     <li>Join tables with their own columns become relationships with properties:
 *     job → (Citizen)-[:WORKS_AT {id, title, salary, startDate, endDate, status}]->(Company|Institution),
 *     education_record → (Citizen)-[:STUDIED_AT {id, startDate, endDate}]->(Institution),
 *     citizen_relationship → (Citizen)-[:RELATED_TO {id, type, startDate, endDate}]->(Citizen).</li>
 * </ul>
 * Neo4j has no decimal type, so the salary (numeric(12,2)) is stored as a float. Null columns are not stored.
 */
public final class GraphMapper
{
    /** Labels the migrator owns: only nodes with these labels are removed before writing */
    public static final List<String> LABELS =
            List.of("Location", "Family", "Housing", "Company", "Institution", "Citizen", "User", "Role");

    /** One Cypher statement, run once with all rows as the parameter $rows */
    public record Step(String name, String cypher, List<Map<String, Object>> rows) {}

    private GraphMapper()
    {
    }

    public static List<Step> map(Snapshot s)
    {
        return List.of(
                nodes("Location", s.locations(), l -> props("id", l.id(), "name", l.name(),
                        "streetAddress", l.streetAddress(), "latitude", l.latitude(), "longitude", l.longitude(),
                        "osmId", l.osmId(), "osmType", l.osmType())),
                nodes("Family", s.families(), f -> props("id", f.id(), "familyName", f.familyName())),
                nodes("Housing", s.housings(), h -> props("id", h.id(), "housingType", h.housingType(),
                        "capacity", h.capacity(), "numberOfLevels", h.numberOfLevels(),
                        "numberOfFlats", h.numberOfFlats(), "height", h.height())),
                nodes("Company", s.companies(), c -> props("id", c.id(), "name", c.name(),
                        "foundedDate", c.foundedDate(), "dissolvedDate", c.dissolvedDate(), "status", c.status(),
                        "cvrNumber", c.cvrNumber(), "productionUnitNumber", c.productionUnitNumber(),
                        "website", c.website(), "phone", c.phone(), "email", c.email(),
                        "category", c.category(), "openingHours", c.openingHours())),
                nodes("Institution", s.institutions(), i -> props("id", i.id(), "name", i.name(),
                        "type", i.type(), "website", i.website(), "phone", i.phone(), "email", i.email())),
                nodes("Citizen", s.citizens(), c -> props("id", c.id(), "firstName", c.firstName(),
                        "lastName", c.lastName(), "birthDate", c.birthDate(), "deathDate", c.deathDate(),
                        "status", c.status(), "gender", c.gender())),
                nodes("User", s.users(), u -> props("username", u.username(), "password", u.password())),
                new Step("Role nodes", "UNWIND $rows AS row CREATE (n:Role) SET n = row",
                        s.users().stream().flatMap(u -> u.roles().stream()).distinct()
                                .map(role -> props("name", role)).toList()),

                link("LOCATED_AT", "Housing", "Location", s.housings(), h -> pair(h.id(), h.locationId())),
                link("LOCATED_AT", "Company", "Location", s.companies(), c -> pair(c.id(), c.locationId())),
                link("LOCATED_AT", "Institution", "Location", s.institutions(), i -> pair(i.id(), i.locationId())),
                link("MEMBER_OF", "Citizen", "Family", s.citizens(), c -> pair(c.id(), c.familyId())),
                link("LIVES_IN", "Citizen", "Housing",
                        s.citizens().stream().filter(c -> c.housingId() != null).toList(),
                        c -> pair(c.id(), c.housingId())),
                new Step("HAS_ROLE", """
                        UNWIND $rows AS row
                        MATCH (u:User {username: row.username}) MATCH (r:Role {name: row.role})
                        CREATE (u)-[:HAS_ROLE]->(r)""",
                        s.users().stream().flatMap(u -> u.roles().stream()
                                .map(role -> props("username", u.username(), "role", role))).toList()),

                linkWithProperties("WORKS_AT", "Citizen", "Company",
                        s.jobs().stream().filter(j -> j.companyId() != null).toList(),
                        j -> pair(j.citizenId(), j.companyId()), GraphMapper::job),
                linkWithProperties("WORKS_AT", "Citizen", "Institution",
                        s.jobs().stream().filter(j -> j.institutionId() != null).toList(),
                        j -> pair(j.citizenId(), j.institutionId()), GraphMapper::job),
                linkWithProperties("STUDIED_AT", "Citizen", "Institution", s.educationRecords(),
                        e -> pair(e.citizenId(), e.institutionId()),
                        e -> props("id", e.id(), "startDate", e.startDate(), "endDate", e.endDate())),
                linkWithProperties("RELATED_TO", "Citizen", "Citizen", s.relationships(),
                        r -> pair(r.citizenId1(), r.citizenId2()),
                        r -> props("id", r.id(), "type", r.type(), "startDate", r.startDate(),
                                "endDate", r.endDate())));
    }

    private static Map<String, Object> job(Snapshot.JobRow j)
    {
        return props("id", j.id(), "title", j.title(), "salary", j.salary().doubleValue(),
                "startDate", j.startDate(), "endDate", j.endDate(), "status", j.status());
    }

    private static <T> Step nodes(String label, List<T> rows, Function<T, Map<String, Object>> properties)
    {
        return new Step(label + " nodes", "UNWIND $rows AS row CREATE (n:" + label + ") SET n = row",
                rows.stream().map(properties).toList());
    }

    private static <T> Step link(String type, String from, String to, List<T> rows, Function<T, long[]> ids)
    {
        return new Step(type + " " + from + "->" + to, """
                UNWIND $rows AS row
                MATCH (a:%s {id: row.from}) MATCH (b:%s {id: row.to})
                CREATE (a)-[:%s]->(b)""".formatted(from, to, type),
                rows.stream().map(ids).map(p -> props("from", p[0], "to", p[1])).toList());
    }

    private static <T> Step linkWithProperties(String type, String from, String to, List<T> rows,
                                               Function<T, long[]> ids, Function<T, Map<String, Object>> properties)
    {
        return new Step(type + " " + from + "->" + to, """
                UNWIND $rows AS row
                MATCH (a:%s {id: row.from}) MATCH (b:%s {id: row.to})
                CREATE (a)-[r:%s]->(b) SET r = row.properties""".formatted(from, to, type),
                rows.stream().map(row ->
                {
                    long[] p = ids.apply(row);
                    return props("from", p[0], "to", p[1], "properties", properties.apply(row));
                }).toList());
    }

    private static long[] pair(long from, long to)
    {
        return new long[]{from, to};
    }

    /** Key/value pairs to a map, leaving out null values (Neo4j does not store nulls) */
    private static Map<String, Object> props(Object... keysAndValues)
    {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2)
        {
            if (keysAndValues[i + 1] != null)
            {
                map.put((String) keysAndValues[i], keysAndValues[i + 1]);
            }
        }
        return map;
    }
}
