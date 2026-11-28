package app.mongo;

import app.dtos.CitizenRelationshipDTO;
import app.dtos.CitizenRelationshipRequestDTO;
import app.nosql.QueryRules.Field;
import app.validators.CitizenRelationshipValidator;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * Relationships between two citizens. Each one is embedded in BOTH citizens (citizens.relationships[]), so either can
 * be read on its own: citizen_id_1 holds it with direction "outgoing", citizen_id_2 with "incoming", and each copy
 * names the other citizen. The list reads only the outgoing copies, so every relationship appears once.
 */
public class MongoCitizenRelationshipService
        extends MongoEmbeddedService<CitizenRelationshipDTO, CitizenRelationshipRequestDTO>
{
    public MongoCitizenRelationshipService(MongoStore store)
    {
        super(store, "relationships", "CitizenRelationship", "citizen_relationship");
    }

    @Override
    protected List<Bson> rowStages()
    {
        return List.of(Aggregates.match(Filters.eq("relationships.direction", "outgoing")),
                Aggregates.replaceWith(new Document("id", "$relationships.id")
                        .append("citizenId1", "$_id")
                        .append("citizenId2", "$relationships.citizenId")
                        .append("relationshipType", "$relationships.type")
                        .append("startDate", "$relationships.startDate")
                        .append("endDate", "$relationships.endDate")));
    }

    @Override
    protected CitizenRelationshipDTO toDTO(Document row)
    {
        return new CitizenRelationshipDTO(MongoStore.id(row.get("id")), MongoStore.id(row.get("citizenId1")),
                MongoStore.id(row.get("citizenId2")), row.getString("relationshipType"),
                MongoStore.date(row.get("startDate")), MongoStore.date(row.get("endDate")));
    }

    @Override
    protected void validate(CitizenRelationshipRequestDTO request)
    {
        CitizenRelationshipValidator.validate(request);
    }

    @Override
    protected Map<Long, Document> place(CitizenRelationshipRequestDTO request, long id)
    {
        Document first = citizen(request.citizenId1());
        Document second = citizen(request.citizenId2());
        return Map.of(request.citizenId1(), copy(request, id, second, "outgoing"),
                request.citizenId2(), copy(request, id, first, "incoming"));
    }

    private static Document copy(CitizenRelationshipRequestDTO request, long id, Document other, String direction)
    {
        return element("id", id, "type", request.relationshipType(), "citizenId", other.get("_id"),
                "name", other.get("firstName") + " " + other.get("lastName"), "direction", direction,
                "startDate", request.startDate(), "endDate", request.endDate());
    }

    @Override
    protected Map<String, Field> filters()
    {
        return Map.of("citizenId1", new Field("citizenId1", Long.class),
                "citizenId2", new Field("citizenId2", Long.class),
                "relationshipType", new Field("relationshipType", String.class),
                "startDateFrom", new Field("startDate", LocalDate.class),
                "startDateTo", new Field("startDate", LocalDate.class),
                "endDateFrom", new Field("endDate", LocalDate.class),
                "endDateTo", new Field("endDate", LocalDate.class));
    }

    @Override
    protected Map<String, String> sorts()
    {
        return Map.of("id", "id", "citizenId1", "citizenId1", "citizenId2", "citizenId2",
                "relationshipType", "relationshipType", "startDate", "startDate", "endDate", "endDate");
    }
}
