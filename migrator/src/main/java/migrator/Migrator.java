package migrator;

import migrator.document.DocumentMapper;
import migrator.document.MongoWriter;
import migrator.graph.GraphMapper;
import migrator.graph.Neo4jWriter;
import migrator.source.Snapshot;
import migrator.source.SqlReader;
import org.bson.Document;

import java.util.List;
import java.util.Map;

/**
 * One-time migration of the Smart City data from PostgreSQL into MongoDB and Neo4j:
 * read every table, map the rows to documents and to a graph, write both, and compare the counts.
 * PostgreSQL is only read. Data only: MongoDB and Neo4j must be set up first with NoSQL/setup.ps1 (collections,
 * validators, constraints, indexes). Running it again replaces what an earlier run wrote.
 * <p>
 * Run: {@code java -jar target/migrator.jar} (settings: see {@link MigratorConfig}).
 * Exit code 0 when every count matches, 1 when not, 2 when the migration failed.
 */
public final class Migrator
{
    private Migrator()
    {
    }

    public static void main(String[] args)
    {
        try
        {
            MigrationReport report = run(MigratorConfig.fromEnvironment());
            System.out.println(report.format());
            System.exit(report.ok() ? 0 : 1);
        }
        catch (Exception e)
        {
            System.err.println("Migration failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(2);
        }
    }

    public static MigrationReport run(MigratorConfig config) throws Exception
    {
        System.out.println("Reading PostgreSQL " + config.postgresUrl() + " as " + config.postgresUser());
        Snapshot snapshot = new SqlReader(config.postgresUrl(), config.postgresUser(), config.postgresPassword()).read();
        System.out.println("Read " + snapshot.rowCount() + " rows");

        Map<String, List<Document>> documents = DocumentMapper.map(snapshot);
        System.out.println("Writing MongoDB " + config.mongoUri() + "/" + config.mongoDatabase());
        Map<String, Long> mongoCounts = new MongoWriter(config.mongoUri(), config.mongoDatabase()).write(documents);

        List<GraphMapper.Step> steps = GraphMapper.map(snapshot);
        System.out.println("Writing Neo4j " + config.neo4jUri());
        Map<String, Long> neo4jCounts = new Neo4jWriter(config.neo4jUri(), config.neo4jUser(), config.neo4jPassword())
                .write(steps);

        return MigrationReport.of(snapshot, documents, mongoCounts, neo4jCounts);
    }
}
