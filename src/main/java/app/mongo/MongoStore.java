package app.mongo;

import app.exceptions.NotFoundException;
import app.nosql.QueryRules.Condition;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.Decimal128;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The connection to the MongoDB solution and the helpers its services share. The structure (collections,
 * $jsonSchema validators, indexes) comes from NoSQL/mongodb/01_collections.js and the data from the migrator; the
 * services only read and write documents.
 */
public class MongoStore implements AutoCloseable
{
    private final MongoClient client;
    private final MongoDatabase database;

    public MongoStore(String uri, String databaseName)
    {
        this.client = MongoClients.create(uri);
        this.database = client.getDatabase(databaseName);
    }

    public MongoCollection<Document> collection(String name)
    {
        return database.getCollection(name);
    }

    /**
     * MongoDB has no sequences, so ids come from the collection "counters": one document per entity, increased
     * atomically ($inc in findOneAndUpdate). The migrator sets each counter to the highest migrated id.
     */
    public long nextId(String sequence)
    {
        Document counter = collection("counters").findOneAndUpdate(Filters.eq("_id", sequence),
                Updates.inc("seq", 1L),
                new FindOneAndUpdateOptions().upsert(true).returnDocument(ReturnDocument.AFTER));
        return ((Number) counter.get("seq")).longValue();
    }

    /** The document with this _id, or 404 with the entity's name */
    public Document require(String collection, String entity, Long id)
    {
        Document document = collection(collection).find(Filters.eq("_id", id)).first();
        if (document == null)
        {
            throw new NotFoundException(entity + " " + id + " not found");
        }
        return document;
    }

    /**
     * A copy of a location to embed in a housing, company or institution (denormalisation: the address is read
     * with its owner, so it is stored with it).
     */
    public Document embeddedLocation(Long locationId)
    {
        Document location = require("locations", "Location", locationId);
        Document copy = new Document("id", location.get("_id"));
        location.forEach((key, value) ->
        {
            if (!key.equals("_id"))
            {
                copy.put(key, value);
            }
        });
        return copy;
    }

    /** The shared filter rules as a MongoDB query; text uses a case-insensitive regex of the quoted value */
    public static Bson filter(List<Condition> conditions)
    {
        if (conditions.isEmpty())
        {
            return new Document();
        }
        return Filters.and(conditions.stream().map(c -> switch (c.operator())
        {
            case CONTAINS -> Filters.regex(c.path(),
                    Pattern.compile(Pattern.quote((String) c.value()), Pattern.CASE_INSENSITIVE));
            case EQUALS -> Filters.eq(c.path(), c.value());
            case AT_LEAST -> Filters.gte(c.path(), c.value());
            case AT_MOST -> Filters.lte(c.path(), c.value());
        }).toList());
    }

    public static LocalDate date(Object value)
    {
        if (value instanceof Date date)
        {
            return date.toInstant().atZone(ZoneOffset.UTC).toLocalDate();
        }
        return (LocalDate) value;
    }

    public static BigDecimal decimal(Object value)
    {
        if (value instanceof Decimal128 decimal)
        {
            return decimal.bigDecimalValue();
        }
        return value == null ? null : new BigDecimal(value.toString());
    }

    public static Long id(Object value)
    {
        return value == null ? null : ((Number) value).longValue();
    }

    @Override
    public void close()
    {
        client.close();
    }
}
