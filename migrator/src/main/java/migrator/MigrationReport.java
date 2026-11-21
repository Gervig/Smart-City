package migrator;

import migrator.source.Snapshot;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Compares what was read from PostgreSQL with what is in MongoDB and Neo4j after the migration. Every line is
 * expected (from the SQL rows) against actual (counted in the target database).
 */
public final class MigrationReport
{
    public record Check(String target, String what, long expected, long actual)
    {
        public boolean ok()
        {
            return expected == actual;
        }
    }

    private final List<Check> checks = new ArrayList<>();

    public static MigrationReport of(Snapshot s, Map<String, List<Document>> documents, Map<String, Long> mongo,
                                     Map<String, Long> neo4j)
    {
        MigrationReport report = new MigrationReport();
        report.mongo("locations", s.locations().size(), mongo);
        report.mongo("families", s.families().size(), mongo);
        report.mongo("housings", s.housings().size(), mongo);
        report.mongo("companies", s.companies().size(), mongo);
        report.mongo("institutions", s.institutions().size(), mongo);
        report.mongo("citizens", s.citizens().size(), mongo);
        report.mongo("users", s.users().size(), mongo);
        List<Document> citizens = documents.get("citizens");
        report.add("MongoDB", "citizens.jobs (embedded)", s.jobs().size(), embedded(citizens, "jobs"));
        report.add("MongoDB", "citizens.education (embedded)", s.educationRecords().size(), embedded(citizens, "education"));
        // each relationship is embedded in both citizens
        report.add("MongoDB", "citizens.relationships (embedded, 2 per row)", 2L * s.relationships().size(),
                embedded(citizens, "relationships"));

        report.neo4j("Location", s.locations().size(), neo4j);
        report.neo4j("Family", s.families().size(), neo4j);
        report.neo4j("Housing", s.housings().size(), neo4j);
        report.neo4j("Company", s.companies().size(), neo4j);
        report.neo4j("Institution", s.institutions().size(), neo4j);
        report.neo4j("Citizen", s.citizens().size(), neo4j);
        report.neo4j("User", s.users().size(), neo4j);
        report.neo4j("Role", s.users().stream().flatMap(u -> u.roles().stream()).distinct().count(), neo4j);
        report.neo4j("LOCATED_AT", s.housings().size() + s.companies().size() + s.institutions().size(), neo4j);
        report.neo4j("MEMBER_OF", s.citizens().size(), neo4j);
        report.neo4j("LIVES_IN", s.citizens().stream().filter(c -> c.housingId() != null).count(), neo4j);
        report.neo4j("HAS_ROLE", s.users().stream().mapToLong(u -> u.roles().size()).sum(), neo4j);
        report.neo4j("WORKS_AT", s.jobs().size(), neo4j);
        report.neo4j("STUDIED_AT", s.educationRecords().size(), neo4j);
        report.neo4j("RELATED_TO", s.relationships().size(), neo4j);
        return report;
    }

    public List<Check> checks()
    {
        return List.copyOf(checks);
    }

    public boolean ok()
    {
        return checks.stream().allMatch(Check::ok);
    }

    public String format()
    {
        StringBuilder out = new StringBuilder(String.format("%-8s %-46s %9s %9s%n", "Target", "What", "Expected", "Actual"));
        for (Check c : checks)
        {
            out.append(String.format("%-8s %-46s %9d %9d %s%n", c.target(), c.what(), c.expected(), c.actual(),
                    c.ok() ? "OK" : "MISMATCH"));
        }
        out.append(ok() ? "Migration complete: every count matches." : "Migration finished WITH MISMATCHES.");
        return out.toString();
    }

    private void mongo(String collection, long expected, Map<String, Long> counts)
    {
        add("MongoDB", collection, expected, counts.getOrDefault(collection, 0L));
    }

    private void neo4j(String name, long expected, Map<String, Long> counts)
    {
        add("Neo4j", name, expected, counts.getOrDefault(name, 0L));
    }

    private void add(String target, String what, long expected, long actual)
    {
        checks.add(new Check(target, what, expected, actual));
    }

    private static long embedded(List<Document> documents, String field)
    {
        return documents.stream().mapToLong(d -> d.getList(field, Document.class, List.of()).size()).sum();
    }
}
