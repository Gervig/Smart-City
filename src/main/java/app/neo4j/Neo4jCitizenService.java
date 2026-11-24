package app.neo4j;

import app.dtos.CitizenDTO;
import app.dtos.CitizenRequestDTO;
import app.nosql.QueryRules.Field;
import app.validators.CitizenValidator;
import org.neo4j.driver.Record;
import org.neo4j.driver.TransactionContext;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

/**
 * (:Citizen)-[:MEMBER_OF]->(:Family), (:Citizen)-[:LIVES_IN]->(:Housing). Family and housing are relationships, not
 * properties, so changing them replaces a relationship. Everything for one write runs in one transaction, including
 * the capacity check and the death cleanup (trg_citizen_post_death_cleanup in PostgreSQL).
 */
public class Neo4jCitizenService extends Neo4jCrudService<CitizenDTO, CitizenRequestDTO>
{
    public Neo4jCitizenService(Neo4jStore store)
    {
        super(store, "Citizen");
    }

    @Override
    protected String match()
    {
        return "MATCH (n:Citizen) OPTIONAL MATCH (n)-[:MEMBER_OF]->(family:Family) "
                + "OPTIONAL MATCH (n)-[:LIVES_IN]->(housing:Housing)";
    }

    @Override
    protected String variables()
    {
        return "n, family, housing";
    }

    @Override
    protected String idExpression()
    {
        return "n.id";
    }

    @Override
    protected String projection()
    {
        return "n.id AS id, n.firstName AS firstName, n.lastName AS lastName, n.birthDate AS birthDate, "
                + "n.deathDate AS deathDate, n.status AS status, n.gender AS gender, family.id AS familyId, "
                + "housing.id AS housingId";
    }

    @Override
    protected CitizenDTO toDTO(Record r)
    {
        return new CitizenDTO(r.get("id").asLong(), Neo4jStore.text(r.get("firstName")),
                Neo4jStore.text(r.get("lastName")), Neo4jStore.date(r.get("birthDate")),
                Neo4jStore.date(r.get("deathDate")), Neo4jStore.text(r.get("status")),
                Neo4jStore.text(r.get("gender")), Neo4jStore.id(r.get("familyId")), Neo4jStore.id(r.get("housingId")));
    }

    @Override
    protected Map<String, Field> filters()
    {
        return Map.of("status", new Field("n.status", String.class),
                "lastName", new Field("n.lastName", String.class),
                "birthDateFrom", new Field("n.birthDate", LocalDate.class),
                "birthDateTo", new Field("n.birthDate", LocalDate.class),
                "familyId", new Field("family.id", Long.class),
                "housingId", new Field("housing.id", Long.class));
    }

    @Override
    protected Set<String> sorts()
    {
        return Set.of("id", "firstName", "lastName", "birthDate", "deathDate", "status", "gender", "familyId",
                "housingId");
    }

    @Override
    public CitizenDTO create(CitizenRequestDTO request)
    {
        CitizenValidator.validate(request);
        return store.write(tx ->
        {
            checkReferences(tx, request, null);
            long id = Neo4jStore.nextId(tx, "citizen");
            tx.run("CREATE (n:Citizen) SET n = $props", Map.of("props", Neo4jStore.properties("id", id,
                    "firstName", request.firstName(), "lastName", request.lastName(), "birthDate", request.birthDate(),
                    "deathDate", request.deathDate(), "status", request.status(), "gender", request.gender())));
            link(tx, id, request);
            return find(tx, id);
        });
    }

    @Override
    public CitizenDTO update(Long id, CitizenRequestDTO request)
    {
        CitizenValidator.validate(request);
        return store.write(tx ->
        {
            CitizenDTO before = find(tx, id);
            checkReferences(tx, request, id);
            tx.run("MATCH (n:Citizen {id: $id}) SET n.firstName = $firstName, n.lastName = $lastName, "
                            + "n.birthDate = $birthDate, n.deathDate = $deathDate, n.status = $status, n.gender = $gender",
                    Neo4jStore.parameters("id", id, "firstName", request.firstName(), "lastName", request.lastName(),
                            "birthDate", request.birthDate(), "deathDate", request.deathDate(),
                            "status", request.status(), "gender", request.gender()));
            tx.run("MATCH (n:Citizen {id: $id})-[r:MEMBER_OF|LIVES_IN]->() DELETE r", Map.of("id", id));
            link(tx, id, request);
            if (request.deathDate() != null && !request.deathDate().equals(before.deathDate()))
            {
                afterDeath(tx, id, request.deathDate());
            }
            return find(tx, id);
        });
    }

    // Family must exist (404); the housing too, and it must have room (400), not counting this citizen
    private static void checkReferences(TransactionContext tx, CitizenRequestDTO request, Long id)
    {
        requireNode(tx, "Family", request.familyId());
        if (request.housingId() != null)
        {
            requireNode(tx, "Housing", request.housingId());
            Record room = tx.run("MATCH (h:Housing {id: $housingId}) OPTIONAL MATCH (c:Citizen)-[:LIVES_IN]->(h) "
                            + "WHERE c.id <> $id RETURN h.capacity AS capacity, count(c) AS occupants",
                    Neo4jStore.parameters("housingId", request.housingId(), "id", id == null ? -1L : id)).single();
            if (room.get("occupants").asLong() >= room.get("capacity").asLong())
            {
                throw new IllegalArgumentException("housing is full");
            }
        }
    }

    private static void link(TransactionContext tx, long id, CitizenRequestDTO request)
    {
        tx.run("MATCH (n:Citizen {id: $id}), (f:Family {id: $familyId}) CREATE (n)-[:MEMBER_OF]->(f)",
                Map.of("id", id, "familyId", request.familyId()));
        if (request.housingId() != null)
        {
            tx.run("MATCH (n:Citizen {id: $id}), (h:Housing {id: $housingId}) CREATE (n)-[:LIVES_IN]->(h)",
                    Map.of("id", id, "housingId", request.housingId()));
        }
    }

    // trg_citizen_post_death_cleanup: end open jobs and relationships, leave the housing
    private static void afterDeath(TransactionContext tx, long id, LocalDate death)
    {
        Map<String, Object> parameters = Map.of("id", id, "death", death);
        tx.run("MATCH (n:Citizen {id: $id})-[r:WORKS_AT]->() WHERE r.endDate IS NULL OR r.endDate > $death "
                + "SET r.endDate = $death, r.status = 'inactive'", parameters);
        tx.run("MATCH (n:Citizen {id: $id})-[r:LIVES_IN]->() DELETE r", parameters);
        tx.run("MATCH (n:Citizen {id: $id})-[r:RELATED_TO]-() WHERE r.endDate IS NULL SET r.endDate = $death",
                parameters);
    }

    @Override
    public void delete(Long id)
    {
        store.write(tx ->
        {
            find(tx, id);
            restrict(tx, "MATCH (n:Citizen {id: $id})-[:WORKS_AT|STUDIED_AT|RELATED_TO]-()", id,
                    "Citizen " + id + " still has jobs, education records or relationships");
            tx.run("MATCH (n:Citizen {id: $id}) DETACH DELETE n", Map.of("id", id));
            return null;
        });
    }
}
