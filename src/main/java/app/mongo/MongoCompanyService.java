package app.mongo;

import app.dtos.CompanyDTO;
import app.dtos.CompanyRequestDTO;
import app.exceptions.ApiException;
import app.nosql.QueryRules.Field;
import app.validators.CompanyValidator;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import org.bson.Document;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Companies, with their location embedded. The company's name is copied into the jobs of its employees
 * (jobs.employer.name), so a rename is written there too; a company with employees cannot be deleted.
 */
public class MongoCompanyService extends MongoCollectionService<CompanyDTO, CompanyRequestDTO>
{
    public MongoCompanyService(MongoStore store)
    {
        super(store, "companies", "Company", "company");
    }

    @Override
    protected CompanyDTO toDTO(Document d)
    {
        return new CompanyDTO(MongoStore.id(d.get("_id")), MongoStore.id(d.get("location", Document.class).get("id")),
                d.getString("name"), MongoStore.date(d.get("foundedDate")), MongoStore.date(d.get("dissolvedDate")),
                d.getString("status"));
    }

    @Override
    protected void validate(CompanyRequestDTO request)
    {
        CompanyValidator.validate(request);
    }

    @Override
    protected Document fields(CompanyRequestDTO request, Long id)
    {
        return new Document("name", request.name())
                .append("foundedDate", request.foundedDate())
                .append("dissolvedDate", request.dissolvedDate())
                .append("status", request.status())
                .append("location", store.embeddedLocation(request.locationId()));
    }

    @Override
    protected Map<String, Field> filters()
    {
        return Map.of("locationId", new Field("location.id", Long.class),
                "name", new Field("name", String.class),
                "status", new Field("status", String.class),
                "foundedDateFrom", new Field("foundedDate", LocalDate.class),
                "foundedDateTo", new Field("foundedDate", LocalDate.class));
    }

    @Override
    protected Map<String, String> sorts()
    {
        return Map.of("id", "_id", "locationId", "location.id", "name", "name", "foundedDate", "foundedDate",
                "dissolvedDate", "dissolvedDate", "status", "status");
    }

    @Override
    protected void afterUpdate(Document before, long id, Document fields)
    {
        store.collection("citizens").updateMany(employs(id), Updates.set("jobs.$[j].employer.name", fields.get("name")),
                new UpdateOptions().arrayFilters(List.of(
                        new Document("j.employer.type", "company").append("j.employer.id", id))));
    }

    @Override
    protected void beforeDelete(Document existing)
    {
        if (store.collection("citizens").countDocuments(employs((Long) existing.get("_id"))) > 0)
        {
            throw new ApiException(409, "Company " + existing.get("_id") + " still has jobs");
        }
    }

    private static org.bson.conversions.Bson employs(long id)
    {
        return Filters.elemMatch("jobs", Filters.and(Filters.eq("employer.type", "company"), Filters.eq("employer.id", id)));
    }
}
