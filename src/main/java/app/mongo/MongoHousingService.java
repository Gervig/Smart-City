package app.mongo;

import app.dtos.HousingDTO;
import app.dtos.HousingRequestDTO;
import app.nosql.QueryRules.Field;
import app.validators.HousingValidator;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;

import java.util.List;
import java.util.Map;

/**
 * Housings, with their location embedded. Deleting a housing removes it from its residents (like ON DELETE SET NULL
 * on citizen.housing_id). residentIds is kept by the citizen service.
 */
public class MongoHousingService extends MongoCollectionService<HousingDTO, HousingRequestDTO>
{
    public MongoHousingService(MongoStore store)
    {
        super(store, "housings", "Housing", "housing");
    }

    @Override
    protected HousingDTO toDTO(Document d)
    {
        return new HousingDTO(MongoStore.id(d.get("_id")), MongoStore.id(d.get("location", Document.class).get("id")),
                d.getString("housingType"), d.getInteger("capacity"));
    }

    @Override
    protected void validate(HousingRequestDTO request)
    {
        HousingValidator.validate(request);
    }

    @Override
    protected Document fields(HousingRequestDTO request, Long id)
    {
        return new Document("housingType", request.housingType())
                .append("capacity", request.capacity())
                .append("location", store.embeddedLocation(request.locationId()));
    }

    @Override
    protected Document newDocumentExtras()
    {
        return new Document("residentIds", List.of());
    }

    @Override
    protected Map<String, Field> filters()
    {
        return Map.of("locationId", new Field("location.id", Long.class),
                "housingType", new Field("housingType", String.class),
                "capacityFrom", new Field("capacity", Integer.class),
                "capacityTo", new Field("capacity", Integer.class));
    }

    @Override
    protected Map<String, String> sorts()
    {
        return Map.of("id", "_id", "locationId", "location.id", "housingType", "housingType", "capacity", "capacity");
    }

    @Override
    protected void beforeDelete(Document existing)
    {
        store.collection("citizens").updateMany(Filters.eq("housingId", existing.get("_id")), Updates.unset("housingId"));
    }
}
