package app.mongo;

import app.dtos.LocationDTO;
import app.dtos.LocationRequestDTO;
import app.exceptions.ApiException;
import app.nosql.QueryRules.Field;
import app.validators.LocationValidator;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;

import java.util.List;
import java.util.Map;

/**
 - Locations. A location is also copied into every housing, company and institution at that place, so a change is
 - written to those copies too, and a location that is still used cannot be deleted.
 */
public class MongoLocationService extends MongoCollectionService<LocationDTO, LocationRequestDTO>
{
    private static final List<String> USED_BY = List.of("housings", "companies", "institutions");

    public MongoLocationService(MongoStore store)
    {
        super(store, "locations", "Location", "location");
    }

    @Override
    protected LocationDTO toDTO(Document d)
    {
        return new LocationDTO(
                MongoStore.id(d.get("_id")),
                d.getString("name"),
                d.getString("streetAddress"),
                d.getDouble("latitude"),
                d.getDouble("longitude")
        );
    }

    @Override
    protected void validate(LocationRequestDTO request)
    {
        LocationValidator.validate(request);
    }

    @Override
    protected Document fields(LocationRequestDTO request, Long id)
    {
        return new Document("name", request.name()).append("streetAddress", request.streetAddress());
    }

    @Override
    protected Map<String, Field> filters()
    {
        return Map.of("name", new Field("name", String.class), "streetAddress", new Field("streetAddress", String.class));
    }

    @Override
    protected Map<String, String> sorts()
    {
        return Map.of("id", "_id", "name", "name", "streetAddress", "streetAddress");
    }

    @Override
    protected void afterUpdate(Document before, long id, Document fields)
    {
        for (String collection : USED_BY)
        {
            store.collection(collection).updateMany(Filters.eq("location.id", id), Updates.combine(
                    Updates.set("location.name", fields.get("name")),
                    fields.get("streetAddress") == null ? Updates.unset("location.streetAddress")
                            : Updates.set("location.streetAddress", fields.get("streetAddress"))));
        }
    }

    @Override
    protected void beforeDelete(Document existing)
    {
        for (String collection : USED_BY)
        {
            if (store.collection(collection).countDocuments(Filters.eq("location.id", existing.get("_id"))) > 0)
            {
                throw new ApiException(409, "Location " + existing.get("_id") + " is still used by " + collection);
            }
        }
    }
}