package app.neo4j;

import app.dtos.InstitutionDTO;
import app.dtos.InstitutionRequestDTO;
import app.nosql.QueryRules.Field;
import app.validators.InstitutionValidator;
import org.neo4j.driver.Record;

import java.util.Map;
import java.util.Set;

/**
 * (:Institution)-[:LOCATED_AT]->(:Location). Citizens point at it with WORKS_AT and STUDIED_AT; an institution with
 * either is kept (409).
 */
public class Neo4jInstitutionService extends Neo4jCrudService<InstitutionDTO, InstitutionRequestDTO>
{
    public Neo4jInstitutionService(Neo4jStore store)
    {
        super(store, "Institution");
    }

    @Override
    protected String match()
    {
        return "MATCH (n:Institution) OPTIONAL MATCH (n)-[:LOCATED_AT]->(location:Location)";
    }

    @Override
    protected String variables()
    {
        return "n, location";
    }

    @Override
    protected String idExpression()
    {
        return "n.id";
    }

    @Override
    protected String projection()
    {
        return "n.id AS id, location.id AS locationId, n.name AS name, n.type AS type";
    }

    @Override
    protected InstitutionDTO toDTO(Record r)
    {
        return new InstitutionDTO(r.get("id").asLong(), Neo4jStore.id(r.get("locationId")),
                Neo4jStore.text(r.get("name")), Neo4jStore.text(r.get("type")));
    }

    @Override
    protected Map<String, Field> filters()
    {
        return Map.of("locationId", new Field("location.id", Long.class),
                "name", new Field("n.name", String.class),
                "type", new Field("n.type", String.class));
    }

    @Override
    protected Set<String> sorts()
    {
        return Set.of("id", "locationId", "name", "type");
    }

    @Override
    public InstitutionDTO create(InstitutionRequestDTO request)
    {
        InstitutionValidator.validate(request);
        return store.write(tx ->
        {
            requireNode(tx, "Location", request.locationId());
            long id = Neo4jStore.nextId(tx, "institution");
            tx.run("MATCH (l:Location {id: $locationId}) "
                            + "CREATE (n:Institution {id: $id, name: $name, type: $type})-[:LOCATED_AT]->(l)",
                    Map.of("id", id, "locationId", request.locationId(), "name", request.name(), "type", request.type()));
            return find(tx, id);
        });
    }

    @Override
    public InstitutionDTO update(Long id, InstitutionRequestDTO request)
    {
        InstitutionValidator.validate(request);
        return store.write(tx ->
        {
            find(tx, id);
            requireNode(tx, "Location", request.locationId());
            tx.run("MATCH (n:Institution {id: $id}) SET n.name = $name, n.type = $type "
                            + "WITH n OPTIONAL MATCH (n)-[old:LOCATED_AT]->() DELETE old "
                            + "WITH DISTINCT n MATCH (l:Location {id: $locationId}) CREATE (n)-[:LOCATED_AT]->(l)",
                    Map.of("id", id, "locationId", request.locationId(), "name", request.name(), "type", request.type()));
            return find(tx, id);
        });
    }

    @Override
    public void delete(Long id)
    {
        store.write(tx ->
        {
            find(tx, id);
            restrict(tx, "MATCH (n:Institution {id: $id})<-[:WORKS_AT|STUDIED_AT]-()", id,
                    "Institution " + id + " still has jobs or education records");
            tx.run("MATCH (n:Institution {id: $id}) DETACH DELETE n", Map.of("id", id));
            return null;
        });
    }
}
