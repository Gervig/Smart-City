package app.neo4j;

import app.dtos.CitizenRelationshipDTO;
import app.dtos.CitizenRelationshipRequestDTO;
import app.nosql.QueryRules.Field;
import app.validators.CitizenRelationshipValidator;
import org.neo4j.driver.Record;
import org.neo4j.driver.TransactionContext;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

/**
 * A relationship between two citizens is a graph relationship: (:Citizen)-[:RELATED_TO {id, type, startDate,
 * endDate}]->(:Citizen), from citizen_id_1 to citizen_id_2. Queries that do not care about the direction match
 * -[:RELATED_TO]- and find it from both sides, so it is stored once (the document model needs two copies).
 */
public class Neo4jCitizenRelationshipService
        extends Neo4jCrudService<CitizenRelationshipDTO, CitizenRelationshipRequestDTO>
{
    public Neo4jCitizenRelationshipService(Neo4jStore store)
    {
        super(store, "CitizenRelationship");
    }

    @Override
    protected String match()
    {
        return "MATCH (a:Citizen)-[r:RELATED_TO]->(b:Citizen)";
    }

    @Override
    protected String variables()
    {
        return "a, r, b";
    }

    @Override
    protected String idExpression()
    {
        return "r.id";
    }

    @Override
    protected String projection()
    {
        return "r.id AS id, a.id AS citizenId1, b.id AS citizenId2, r.type AS relationshipType, "
                + "r.startDate AS startDate, r.endDate AS endDate";
    }

    @Override
    protected CitizenRelationshipDTO toDTO(Record r)
    {
        return new CitizenRelationshipDTO(r.get("id").asLong(), r.get("citizenId1").asLong(),
                r.get("citizenId2").asLong(), Neo4jStore.text(r.get("relationshipType")),
                Neo4jStore.date(r.get("startDate")), Neo4jStore.date(r.get("endDate")));
    }

    @Override
    protected Map<String, Field> filters()
    {
        return Map.of("citizenId1", new Field("a.id", Long.class),
                "citizenId2", new Field("b.id", Long.class),
                "relationshipType", new Field("r.type", String.class),
                "startDateFrom", new Field("r.startDate", LocalDate.class),
                "startDateTo", new Field("r.startDate", LocalDate.class),
                "endDateFrom", new Field("r.endDate", LocalDate.class),
                "endDateTo", new Field("r.endDate", LocalDate.class));
    }

    @Override
    protected Set<String> sorts()
    {
        return Set.of("id", "citizenId1", "citizenId2", "relationshipType", "startDate", "endDate");
    }

    @Override
    public CitizenRelationshipDTO create(CitizenRelationshipRequestDTO request)
    {
        CitizenRelationshipValidator.validate(request);
        return store.write(tx ->
        {
            check(tx, request);
            long id = Neo4jStore.nextId(tx, "citizen_relationship");
            relate(tx, id, request);
            return find(tx, id);
        });
    }

    @Override
    public CitizenRelationshipDTO update(Long id, CitizenRelationshipRequestDTO request)
    {
        CitizenRelationshipValidator.validate(request);
        return store.write(tx ->
        {
            find(tx, id);
            check(tx, request);
            tx.run("MATCH ()-[r:RELATED_TO {id: $id}]->() DELETE r", Map.of("id", id));
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
            tx.run("MATCH ()-[r:RELATED_TO {id: $id}]->() DELETE r", Map.of("id", id));
            return null;
        });
    }

    private static void check(TransactionContext tx, CitizenRelationshipRequestDTO request)
    {
        requireNode(tx, "Citizen", request.citizenId1());
        requireNode(tx, "Citizen", request.citizenId2());
    }

    private static void relate(TransactionContext tx, long id, CitizenRelationshipRequestDTO request)
    {
        tx.run("MATCH (a:Citizen {id: $citizenId1}), (b:Citizen {id: $citizenId2}) "
                        + "CREATE (a)-[r:RELATED_TO]->(b) SET r = $props",
                Map.of("citizenId1", request.citizenId1(), "citizenId2", request.citizenId2(),
                        "props", Neo4jStore.properties("id", id, "type", request.relationshipType(),
                                "startDate", request.startDate(), "endDate", request.endDate())));
    }
}
