package app.mongo;

import app.dtos.FamilyDTO;
import app.dtos.FamilyRequestDTO;
import app.exceptions.ApiException;
import app.nosql.QueryRules.Field;
import app.validators.FamilyValidator;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;

import java.util.List;
import java.util.Map;

/**
 * Families. The family's name is also copied into each member's citizen document (family.familyName), so a rename is
 * written there too. memberIds is kept by the citizen service.
 */
public class MongoFamilyService extends MongoCollectionService<FamilyDTO, FamilyRequestDTO>
{
    public MongoFamilyService(MongoStore store)
    {
        super(store, "families", "Family", "family");
    }

    @Override
    protected FamilyDTO toDTO(Document d)
    {
        return new FamilyDTO(MongoStore.id(d.get("_id")), d.getString("familyName"));
    }

    @Override
    protected void validate(FamilyRequestDTO request)
    {
        FamilyValidator.validate(request);
    }

    @Override
    protected Document fields(FamilyRequestDTO request, Long id)
    {
        return new Document("familyName", request.familyName());
    }

    @Override
    protected Document newDocumentExtras()
    {
        return new Document("memberIds", List.of());
    }

    @Override
    protected Map<String, Field> filters()
    {
        return Map.of("familyName", new Field("familyName", String.class));
    }

    @Override
    protected Map<String, String> sorts()
    {
        return Map.of("id", "_id", "familyName", "familyName");
    }

    @Override
    protected void afterUpdate(Document before, long id, Document fields)
    {
        store.collection("citizens").updateMany(Filters.eq("family.id", id),
                Updates.set("family.familyName", fields.get("familyName")));
    }

    @Override
    protected void beforeDelete(Document existing)
    {
        if (store.collection("citizens").countDocuments(Filters.eq("family.id", existing.get("_id"))) > 0)
        {
            throw new ApiException(409, "Family " + existing.get("_id") + " still has members");
        }
    }
}
