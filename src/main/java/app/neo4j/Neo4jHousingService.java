package app.neo4j;

import app.dtos.HousingDTO;
import app.dtos.HousingRequestDTO;
import app.nosql.QueryRules.Field;
import app.validators.HousingValidator;
import org.neo4j.driver.Record;

import java.util.Map;
import java.util.Set;

/**
 * (:Housing)-[:LOCATED_AT]->(:Location). Deleting a housing also deletes the LIVES_IN relationships to it, which
 * leaves its residents without a housing (like ON DELETE SET NULL on citizen.housing_id).
 */
public class Neo4jHousingService extends Neo4jCrudService<HousingDTO, HousingRequestDTO>
{
    public Neo4jHousingService(Neo4jStore store)
    {
        super(store, "Housing");
    }

    @Override
    protected String match()
    {
        return "MATCH (n:Housing) OPTIONAL MATCH (n)-[:LOCATED_AT]->(location:Location)";
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
        return "n.id AS id, location.id AS locationId, n.housingType AS housingType, n.capacity AS capacity";
    }

    @Override
    protected HousingDTO toDTO(Record r)
    {
        return new HousingDTO(r.get("id").asLong(), Neo4jStore.id(r.get("locationId")),
                Neo4jStore.text(r.get("housingType")), r.get("capacity").asInt());
    }

    @Override
    protected Map<String, Field> filters()
    {
        return Map.of("locationId", new Field("location.id", Long.class),
                "housingType", new Field("n.housingType", String.class),
                "capacityFrom", new Field("n.capacity", Integer.class),
                "capacityTo", new Field("n.capacity", Integer.class));
    }

    @Override
    protected Set<String> sorts()
    {
        return Set.of("id", "locationId", "housingType", "capacity");
    }

    @Override
    public HousingDTO create(HousingRequestDTO request)
    {
        HousingValidator.validate(request);
        return store.write(tx ->
        {
            requireNode(tx, "Location", request.locationId());
            long id = Neo4jStore.nextId(tx, "housing");
            tx.run("MATCH (l:Location {id: $locationId}) "
                            + "CREATE (n:Housing {id: $id, housingType: $housingType, capacity: $capacity})-[:LOCATED_AT]->(l)",
                    Map.of("id", id, "locationId", request.locationId(), "housingType", request.housingType(),
                            "capacity", request.capacity()));
            return find(tx, id);
        });
    }

    @Override
    public HousingDTO update(Long id, HousingRequestDTO request)
    {
        HousingValidator.validate(request);
        return store.write(tx ->
        {
            find(tx, id);
            requireNode(tx, "Location", request.locationId());
            tx.run("MATCH (n:Housing {id: $id}) SET n.housingType = $housingType, n.capacity = $capacity "
                            + "WITH n OPTIONAL MATCH (n)-[old:LOCATED_AT]->() DELETE old "
                            + "WITH DISTINCT n MATCH (l:Location {id: $locationId}) CREATE (n)-[:LOCATED_AT]->(l)",
                    Map.of("id", id, "locationId", request.locationId(), "housingType", request.housingType(),
                            "capacity", request.capacity()));
            return find(tx, id);
        });
    }

    @Override
    public void delete(Long id)
    {
        store.write(tx ->
        {
            find(tx, id);
            tx.run("MATCH (n:Housing {id: $id}) DETACH DELETE n", Map.of("id", id));
            return null;
        });
    }
}
