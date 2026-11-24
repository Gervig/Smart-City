package app.mongo;

import app.dtos.InstitutionDTO;
import app.dtos.InstitutionRequestDTO;
import app.exceptions.ApiException;
import app.nosql.QueryRules.Field;
import app.validators.InstitutionValidator;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.List;
import java.util.Map;

/**
 * Institutions, with their location embedded. Name and type are copied into the jobs (employer) and education records
 * of citizens, so changes are written there too; an institution with jobs or students cannot be deleted.
 */
public class MongoInstitutionService extends MongoCollectionService<InstitutionDTO, InstitutionRequestDTO>
{
    public MongoInstitutionService(MongoStore store)
    {
        super(store, "institutions", "Institution", "institution");
    }

    @Override
    protected InstitutionDTO toDTO(Document d)
    {
        return new InstitutionDTO(MongoStore.id(d.get("_id")),
                MongoStore.id(d.get("location", Document.class).get("id")), d.getString("name"), d.getString("type"));
    }

    @Override
    protected void validate(InstitutionRequestDTO request)
    {
        InstitutionValidator.validate(request);
    }

    @Override
    protected Document fields(InstitutionRequestDTO request, Long id)
    {
        return new Document("name", request.name())
                .append("type", request.type())
                .append("location", store.embeddedLocation(request.locationId()));
    }

    @Override
    protected Map<String, Field> filters()
    {
        return Map.of("locationId", new Field("location.id", Long.class),
                "name", new Field("name", String.class),
                "type", new Field("type", String.class));
    }

    @Override
    protected Map<String, String> sorts()
    {
        return Map.of("id", "_id", "locationId", "location.id", "name", "name", "type", "type");
    }

    @Override
    protected void afterUpdate(Document before, long id, Document fields)
    {
        var citizens = store.collection("citizens");
        citizens.updateMany(employs(id), Updates.set("jobs.$[j].employer.name", fields.get("name")),
                new UpdateOptions().arrayFilters(List.of(
                        new Document("j.employer.type", "institution").append("j.employer.id", id))));
        citizens.updateMany(Filters.eq("education.institution.id", id), Updates.combine(
                        Updates.set("education.$[e].institution.name", fields.get("name")),
                        Updates.set("education.$[e].institution.type", fields.get("type"))),
                new UpdateOptions().arrayFilters(List.of(new Document("e.institution.id", id))));
    }

    @Override
    protected void beforeDelete(Document existing)
    {
        long id = (Long) existing.get("_id");
        if (store.collection("citizens").countDocuments(
                Filters.or(employs(id), Filters.eq("education.institution.id", id))) > 0)
        {
            throw new ApiException(409, "Institution " + id + " still has jobs or education records");
        }
    }

    private static Bson employs(long id)
    {
        return Filters.elemMatch("jobs",
                Filters.and(Filters.eq("employer.type", "institution"), Filters.eq("employer.id", id)));
    }
}
