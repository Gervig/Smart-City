package app.mongo;

import app.dtos.EducationRecordDTO;
import app.dtos.EducationRecordRequestDTO;
import app.nosql.QueryRules.Field;
import app.validators.EducationRecordValidator;
import com.mongodb.client.model.Aggregates;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Education records, embedded in the citizen (citizens.education[]) with a copy of the institution's id, name and
 * type.
 */
public class MongoEducationRecordService extends MongoEmbeddedService<EducationRecordDTO, EducationRecordRequestDTO>
{
    public MongoEducationRecordService(MongoStore store)
    {
        super(store, "education", "EducationRecord", "education_record");
    }

    @Override
    protected List<Bson> rowStages()
    {
        return List.of(Aggregates.replaceWith(new Document("id", "$education.id")
                .append("citizenId", "$_id")
                .append("institutionId", "$education.institution.id")
                .append("startDate", "$education.startDate")
                .append("endDate", "$education.endDate")));
    }

    @Override
    protected EducationRecordDTO toDTO(Document row)
    {
        return new EducationRecordDTO(MongoStore.id(row.get("id")), MongoStore.id(row.get("citizenId")),
                MongoStore.id(row.get("institutionId")), MongoStore.date(row.get("startDate")),
                MongoStore.date(row.get("endDate")));
    }

    @Override
    protected void validate(EducationRecordRequestDTO request)
    {
        EducationRecordValidator.validate(request);
    }

    @Override
    protected Map<Long, Document> place(EducationRecordRequestDTO request, long id)
    {
        citizen(request.citizenId());
        Document institution = store.require("institutions", "Institution", request.institutionId());
        return Map.of(request.citizenId(), element("id", id,
                "institution", new Document("id", institution.get("_id")).append("name", institution.get("name"))
                        .append("type", institution.get("type")),
                "startDate", request.startDate(), "endDate", request.endDate()));
    }

    @Override
    protected Map<String, Field> filters()
    {
        return Map.of("citizenId", new Field("citizenId", Long.class),
                "institutionId", new Field("institutionId", Long.class),
                "startDateFrom", new Field("startDate", LocalDate.class),
                "startDateTo", new Field("startDate", LocalDate.class),
                "endDateFrom", new Field("endDate", LocalDate.class),
                "endDateTo", new Field("endDate", LocalDate.class));
    }

    @Override
    protected Map<String, String> sorts()
    {
        return Map.of("id", "id", "citizenId", "citizenId", "institutionId", "institutionId",
                "startDate", "startDate", "endDate", "endDate");
    }
}
