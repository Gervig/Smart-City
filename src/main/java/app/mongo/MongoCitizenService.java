package app.mongo;

import app.dtos.CitizenDTO;
import app.dtos.CitizenRequestDTO;
import app.exceptions.ApiException;
import app.nosql.QueryRules.Field;
import app.validators.CitizenValidator;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import org.bson.Document;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Citizens: the aggregate root of the document model. Family (id + name), jobs, education and relationships live
 * inside the citizen; the housing is a reference (housingId), because several citizens share one.
 * <p>
 * The service does what PostgreSQL does with foreign keys and triggers: it checks family and housing (404), the
 * housing's capacity (400), keeps families.memberIds and housings.residentIds in step, copies a new name into the
 * partners' relationships, and repeats trg_citizen_post_death_cleanup when a death date is set.
 */
public class MongoCitizenService extends MongoCollectionService<CitizenDTO, CitizenRequestDTO>
{
    private final MongoCollection<Document> families;
    private final MongoCollection<Document> housings;

    public MongoCitizenService(MongoStore store)
    {
        super(store, "citizens", "Citizen", "citizen");
        this.families = store.collection("families");
        this.housings = store.collection("housings");
    }

    @Override
    protected CitizenDTO toDTO(Document d)
    {
        return new CitizenDTO(MongoStore.id(d.get("_id")), d.getString("firstName"), d.getString("lastName"),
                MongoStore.date(d.get("birthDate")), MongoStore.date(d.get("deathDate")), d.getString("status"),
                d.getString("gender"), MongoStore.id(d.get("family", Document.class).get("id")),
                MongoStore.id(d.get("housingId")));
    }

    @Override
    protected void validate(CitizenRequestDTO request)
    {
        CitizenValidator.validate(request);
    }

    @Override
    protected Document fields(CitizenRequestDTO request, Long id)
    {
        Document family = store.require("families", "Family", request.familyId());
        if (request.housingId() != null)
        {
            Document housing = store.require("housings", "Housing", request.housingId());
            long occupants = collection.countDocuments(Filters.and(Filters.eq("housingId", request.housingId()),
                    Filters.ne("_id", id)));
            if (occupants >= housing.getInteger("capacity"))
            {
                throw new IllegalArgumentException("housing is full");
            }
        }
        return new Document("firstName", request.firstName())
                .append("lastName", request.lastName())
                .append("birthDate", request.birthDate())
                .append("deathDate", request.deathDate())
                .append("status", request.status())
                .append("gender", request.gender())
                .append("family", new Document("id", family.get("_id")).append("familyName", family.get("familyName")))
                .append("housingId", request.housingId());
    }

    @Override
    protected Document newDocumentExtras()
    {
        return new Document("jobs", List.of()).append("education", List.of()).append("relationships", List.of());
    }

    @Override
    protected Map<String, Field> filters()
    {
        return Map.of("status", new Field("status", String.class),
                "lastName", new Field("lastName", String.class),
                "birthDateFrom", new Field("birthDate", LocalDate.class),
                "birthDateTo", new Field("birthDate", LocalDate.class),
                "familyId", new Field("family.id", Long.class),
                "housingId", new Field("housingId", Long.class));
    }

    @Override
    protected Map<String, String> sorts()
    {
        return Map.ofEntries(Map.entry("id", "_id"), Map.entry("firstName", "firstName"),
                Map.entry("lastName", "lastName"), Map.entry("birthDate", "birthDate"),
                Map.entry("deathDate", "deathDate"), Map.entry("status", "status"), Map.entry("gender", "gender"),
                Map.entry("familyId", "family.id"), Map.entry("housingId", "housingId"));
    }

    @Override
    protected void afterCreate(long id, Document fields)
    {
        families.updateOne(Filters.eq("_id", familyId(fields)), Updates.addToSet("memberIds", id));
        if (fields.get("housingId") != null)
        {
            housings.updateOne(Filters.eq("_id", fields.get("housingId")), Updates.addToSet("residentIds", id));
        }
    }

    @Override
    protected void afterUpdate(Document before, long id, Document fields)
    {
        Object oldFamily = before.get("family", Document.class).get("id");
        if (!Objects.equals(oldFamily, familyId(fields)))
        {
            families.updateOne(Filters.eq("_id", oldFamily), Updates.pull("memberIds", id));
            families.updateOne(Filters.eq("_id", familyId(fields)), Updates.addToSet("memberIds", id));
        }
        Object oldHousing = before.get("housingId");
        if (!Objects.equals(oldHousing, fields.get("housingId")))
        {
            housings.updateOne(Filters.eq("_id", oldHousing), Updates.pull("residentIds", id));
            housings.updateOne(Filters.eq("_id", fields.get("housingId")), Updates.addToSet("residentIds", id));
        }

        String name = fields.get("firstName") + " " + fields.get("lastName");
        if (!name.equals(before.get("firstName") + " " + before.get("lastName")))
        {
            collection.updateMany(Filters.eq("relationships.citizenId", id),
                    Updates.set("relationships.$[r].name", name),
                    new UpdateOptions().arrayFilters(List.of(new Document("r.citizenId", id))));
        }

        LocalDate death = (LocalDate) fields.get("deathDate");
        if (death != null && !death.equals(MongoStore.date(before.get("deathDate"))))
        {
            afterDeath(id, death);
        }
    }

    // The same as trg_citizen_post_death_cleanup in PostgreSQL: end open jobs and relationships, leave the housing
    private void afterDeath(long id, LocalDate death)
    {
        collection.updateOne(Filters.eq("_id", id),
                Updates.combine(Updates.set("jobs.$[j].endDate", death), Updates.set("jobs.$[j].status", "inactive")),
                new UpdateOptions().arrayFilters(List.of(new Document("$or", List.of(
                        new Document("j.endDate", new Document("$exists", false)),
                        new Document("j.endDate", new Document("$gt", death)))))));
        Document current = require(id);
        if (current.get("housingId") != null)
        {
            housings.updateOne(Filters.eq("_id", current.get("housingId")), Updates.pull("residentIds", id));
            collection.updateOne(Filters.eq("_id", id), Updates.unset("housingId"));
        }
        Document openRelationship = new Document("r.endDate", new Document("$exists", false));
        collection.updateOne(Filters.eq("_id", id), Updates.set("relationships.$[r].endDate", death),
                new UpdateOptions().arrayFilters(List.of(openRelationship)));
        collection.updateMany(Filters.eq("relationships.citizenId", id), Updates.set("relationships.$[r].endDate", death),
                new UpdateOptions().arrayFilters(List.of(new Document(openRelationship).append("r.citizenId", id))));
    }

    @Override
    protected void beforeDelete(Document existing)
    {
        for (String array : List.of("jobs", "education", "relationships"))
        {
            if (!existing.getList(array, Document.class, List.of()).isEmpty())
            {
                throw new ApiException(409, "Citizen " + existing.get("_id") + " still has " + array);
            }
        }
    }

    @Override
    protected void afterDelete(Document existing)
    {
        Object id = existing.get("_id");
        families.updateOne(Filters.eq("_id", existing.get("family", Document.class).get("id")), Updates.pull("memberIds", id));
        if (existing.get("housingId") != null)
        {
            housings.updateOne(Filters.eq("_id", existing.get("housingId")), Updates.pull("residentIds", id));
        }
    }

    private static Object familyId(Document fields)
    {
        return fields.get("family", Document.class).get("id");
    }
}
