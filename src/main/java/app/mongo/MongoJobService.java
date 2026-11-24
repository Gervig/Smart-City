package app.mongo;

import app.dtos.JobDTO;
import app.dtos.JobRequestDTO;
import app.nosql.QueryRules.Field;
import app.validators.JobValidator;
import com.mongodb.client.model.Aggregates;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Jobs, embedded in the citizen (citizens.jobs[]) with a copy of the employer's name. The employer is a company or
 * an institution: {type, id, name}.
 */
public class MongoJobService extends MongoEmbeddedService<JobDTO, JobRequestDTO>
{
    public MongoJobService(MongoStore store)
    {
        super(store, "jobs", "Job", "job");
    }

    @Override
    protected List<Bson> rowStages()
    {
        return List.of(Aggregates.replaceWith(new Document("id", "$jobs.id")
                .append("citizenId", "$_id")
                .append("companyId", employerId("company"))
                .append("institutionId", employerId("institution"))
                .append("title", "$jobs.title")
                .append("salary", "$jobs.salary")
                .append("startDate", "$jobs.startDate")
                .append("endDate", "$jobs.endDate")
                .append("status", "$jobs.status")));
    }

    private static Document employerId(String type)
    {
        return new Document("$cond", List.of(new Document("$eq", List.of("$jobs.employer.type", type)),
                "$jobs.employer.id", "$$REMOVE"));
    }

    @Override
    protected JobDTO toDTO(Document row)
    {
        return new JobDTO(MongoStore.id(row.get("id")), MongoStore.id(row.get("citizenId")),
                MongoStore.id(row.get("companyId")), MongoStore.id(row.get("institutionId")), row.getString("title"),
                MongoStore.decimal(row.get("salary")), MongoStore.date(row.get("startDate")),
                MongoStore.date(row.get("endDate")), row.getString("status"));
    }

    @Override
    protected void validate(JobRequestDTO request)
    {
        JobValidator.validate(request);
    }

    @Override
    protected Map<Long, Document> place(JobRequestDTO request, long id)
    {
        Document citizen = citizen(request.citizenId());
        LocalDate birth = MongoStore.date(citizen.get("birthDate"));
        LocalDate death = MongoStore.date(citizen.get("deathDate"));
        if (request.startDate().isBefore(birth))
        {
            throw new IllegalArgumentException("startDate must not be before the citizen's birthDate");
        }
        if (death != null && request.startDate().isAfter(death))
        {
            throw new IllegalArgumentException("startDate must not be after the citizen's deathDate");
        }
        Document employer = request.companyId() != null
                ? employer("company", store.require("companies", "Company", request.companyId()))
                : employer("institution", store.require("institutions", "Institution", request.institutionId()));
        return Map.of(request.citizenId(), element("id", id, "title", request.title(), "salary", request.salary(),
                "startDate", request.startDate(), "endDate", request.endDate(), "status", request.status(),
                "employer", employer));
    }

    private static Document employer(String type, Document employer)
    {
        return new Document("type", type).append("id", employer.get("_id")).append("name", employer.get("name"));
    }

    @Override
    protected Map<String, Field> filters()
    {
        return Map.ofEntries(Map.entry("citizenId", new Field("citizenId", Long.class)),
                Map.entry("companyId", new Field("companyId", Long.class)),
                Map.entry("institutionId", new Field("institutionId", Long.class)),
                Map.entry("title", new Field("title", String.class)),
                Map.entry("status", new Field("status", String.class)),
                Map.entry("salaryFrom", new Field("salary", BigDecimal.class)),
                Map.entry("salaryTo", new Field("salary", BigDecimal.class)),
                Map.entry("startDateFrom", new Field("startDate", LocalDate.class)),
                Map.entry("startDateTo", new Field("startDate", LocalDate.class)),
                Map.entry("endDateFrom", new Field("endDate", LocalDate.class)),
                Map.entry("endDateTo", new Field("endDate", LocalDate.class)));
    }

    @Override
    protected Map<String, String> sorts()
    {
        return Map.of("id", "id", "citizenId", "citizenId", "companyId", "companyId", "institutionId",
                "institutionId", "title", "title", "salary", "salary", "startDate", "startDate", "endDate", "endDate",
                "status", "status");
    }
}
