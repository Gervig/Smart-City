package migrator.document;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import org.bson.Document;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes the documents. Data only: the collections, their validators and the indexes are created by
 * NoSQL/mongodb/01_collections.js, like the PostgreSQL tables by SQL/schema/database_schema.sql.
 * <p>
 * Each collection is emptied (not dropped, which would remove its validator) and then filled, so running the
 * migration again gives the same result instead of duplicates. MongoDB checks every document against the
 * collection's validator, so a document that does not fit the design is rejected.
 */
public class MongoWriter
{
    private final String uri;
    private final String databaseName;

    public MongoWriter(String uri, String databaseName)
    {
        this.uri = uri;
        this.databaseName = databaseName;
    }

    /** @return number of documents per collection, counted in MongoDB after writing */
    public Map<String, Long> write(Map<String, List<Document>> collections)
    {
        return write(collections, Map.of());
    }

    /**
     * @param sequences the highest id per entity, stored in the collection "counters" ({_id: name, seq: id}) so the
     *                  CRUD application continues after the migrated ids
     */
    public Map<String, Long> write(Map<String, List<Document>> collections, Map<String, Long> sequences)
    {
        try (MongoClient client = MongoClients.create(uri))
        {
            MongoDatabase db = client.getDatabase(databaseName);
            requireStructure(db, collections.keySet());

            collections.forEach((name, documents) ->
            {
                db.getCollection(name).deleteMany(new Document());
                if (!documents.isEmpty())
                {
                    db.getCollection(name).insertMany(documents);
                }
            });
            sequences.forEach((name, max) -> db.getCollection("counters").updateOne(new Document("_id", name),
                    new Document("$set", new Document("seq", max)), new UpdateOptions().upsert(true)));

            Map<String, Long> counts = new LinkedHashMap<>();
            collections.keySet().forEach(name -> counts.put(name, db.getCollection(name).countDocuments()));
            return counts;
        }
    }

    private static void requireStructure(MongoDatabase db, Iterable<String> needed)
    {
        List<String> existing = db.listCollectionNames().into(new ArrayList<>());
        List<String> missing = new ArrayList<>();
        needed.forEach(name ->
        {
            if (!existing.contains(name))
            {
                missing.add(name);
            }
        });
        if (!missing.isEmpty())
        {
            throw new IllegalStateException("MongoDB database " + db.getName() + " is not set up, missing collections "
                    + missing + ". Run NoSQL/mongodb/01_collections.js first (NoSQL/setup.ps1).");
        }
    }
}
