package migrator.source;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToLongFunction;

/**
 * The highest migrated id per entity. MongoDB and Neo4j have no sequences like PostgreSQL's identity columns, so the
 * CRUD application counts on its own (MongoDB: collection "counters", Neo4j: :Sequence nodes). The migrator sets each
 * counter to the highest id it copied, so the next id the application hands out is new.
 * The names match the ones the application uses (app.mongo.MongoStore, app.neo4j.Neo4jStore).
 */
public final class Sequences
{
    private Sequences()
    {
    }

    public static Map<String, Long> of(Snapshot s)
    {
        Map<String, Long> sequences = new LinkedHashMap<>();
        sequences.put("location", max(s.locations(), Snapshot.LocationRow::id));
        sequences.put("family", max(s.families(), Snapshot.FamilyRow::id));
        sequences.put("housing", max(s.housings(), Snapshot.HousingRow::id));
        sequences.put("company", max(s.companies(), Snapshot.CompanyRow::id));
        sequences.put("institution", max(s.institutions(), Snapshot.InstitutionRow::id));
        sequences.put("citizen", max(s.citizens(), Snapshot.CitizenRow::id));
        sequences.put("job", max(s.jobs(), Snapshot.JobRow::id));
        sequences.put("education_record", max(s.educationRecords(), Snapshot.EducationRow::id));
        sequences.put("citizen_relationship", max(s.relationships(), Snapshot.RelationshipRow::id));
        return sequences;
    }

    private static <T> long max(List<T> rows, ToLongFunction<T> id)
    {
        return rows.stream().mapToLong(id).max().orElse(0);
    }
}
