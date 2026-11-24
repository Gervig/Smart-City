package app.neo4j;

import app.dtos.EducationRecordDTO;
import app.dtos.EducationRecordRequestDTO;
import app.nosql.QueryRules.Field;
import app.validators.EducationRecordValidator;
import org.neo4j.driver.Record;
import org.neo4j.driver.TransactionContext;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

/** An education record is a relationship: (:Citizen)-[:STUDIED_AT {id, startDate, endDate}]->(:Institution). */
public class Neo4jEducationRecordService extends Neo4jCrudService<EducationRecordDTO, EducationRecordRequestDTO>
{
    public Neo4jEducationRecordService(Neo4jStore store)
    {
        super(store, "EducationRecord");
    }

    @Override
    protected String match()
    {
        return "MATCH (c:Citizen)-[r:STUDIED_AT]->(i:Institution)";
    }

    @Override
    protected String variables()
    {
        return "c, r, i";
    }

    @Override
    protected String idExpression()
    {
        return "r.id";
    }

    @Override
    protected String projection()
    {
        return "r.id AS id, c.id AS citizenId, i.id AS institutionId, r.startDate AS startDate, r.endDate AS endDate";
    }

    @Override
    protected EducationRecordDTO toDTO(Record r)
    {
        return new EducationRecordDTO(r.get("id").asLong(), r.get("citizenId").asLong(), r.get("institutionId").asLong(),
                Neo4jStore.date(r.get("startDate")), Neo4jStore.date(r.get("endDate")));
    }

    @Override
    protected Map<String, Field> filters()
    {
        return Map.of("citizenId", new Field("c.id", Long.class),
                "institutionId", new Field("i.id", Long.class),
                "startDateFrom", new Field("r.startDate", LocalDate.class),
                "startDateTo", new Field("r.startDate", LocalDate.class),
                "endDateFrom", new Field("r.endDate", LocalDate.class),
                "endDateTo", new Field("r.endDate", LocalDate.class));
    }

    @Override
    protected Set<String> sorts()
    {
        return Set.of("id", "citizenId", "institutionId", "startDate", "endDate");
    }

    @Override
    public EducationRecordDTO create(EducationRecordRequestDTO request)
    {
        EducationRecordValidator.validate(request);
        return store.write(tx ->
        {
            check(tx, request);
            long id = Neo4jStore.nextId(tx, "education_record");
            relate(tx, id, request);
            return find(tx, id);
        });
    }

    @Override
    public EducationRecordDTO update(Long id, EducationRecordRequestDTO request)
    {
        EducationRecordValidator.validate(request);
        return store.write(tx ->
        {
            find(tx, id);
            check(tx, request);
            tx.run("MATCH ()-[r:STUDIED_AT {id: $id}]->() DELETE r", Map.of("id", id));
            relate(tx, id, request);
            return find(tx, id);
        });
    }

    @Override
    public void delete(Long id)
    {
        store.write(tx ->
        {
            find(tx, id);
            tx.run("MATCH ()-[r:STUDIED_AT {id: $id}]->() DELETE r", Map.of("id", id));
            return null;
        });
    }

    private static void check(TransactionContext tx, EducationRecordRequestDTO request)
    {
        requireNode(tx, "Citizen", request.citizenId());
        requireNode(tx, "Institution", request.institutionId());
    }

    private static void relate(TransactionContext tx, long id, EducationRecordRequestDTO request)
    {
        tx.run("MATCH (c:Citizen {id: $citizenId}), (i:Institution {id: $institutionId}) "
                        + "CREATE (c)-[r:STUDIED_AT]->(i) SET r = $props",
                Map.of("citizenId", request.citizenId(), "institutionId", request.institutionId(),
                        "props", Neo4jStore.properties("id", id, "startDate", request.startDate(),
                                "endDate", request.endDate())));
    }
}
