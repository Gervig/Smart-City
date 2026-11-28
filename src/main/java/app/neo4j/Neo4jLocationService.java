package app.neo4j;

import app.dtos.LocationDTO;
import app.dtos.LocationRequestDTO;
import app.nosql.QueryRules.Field;
import app.validators.LocationValidator;
import org.neo4j.driver.Record;

import java.util.Map;
import java.util.Set;

/** (:Location). Housings, companies and institutions point at it with LOCATED_AT; a used location is kept (409). */
public class Neo4jLocationService extends Neo4jCrudService<LocationDTO, LocationRequestDTO>
{
    public Neo4jLocationService(Neo4jStore store)
    {
        super(store, "Location");
    }

    @Override
    protected String match()
    {
        return "MATCH (n:Location)";
    }

    @Override
    protected String variables()
    {
        return "n";
    }

    @Override
    protected String idExpression()
    {
        return "n.id";
    }

    @Override
    protected String projection()
    {
        return "n.id AS id, n.name AS name, n.streetAddress AS streetAddress, n.latitude AS latitude, n.longitude AS longitude";
    }

    @Override
    protected LocationDTO toDTO(Record r)
    {
        Double latitude = r.get("latitude").isNull() ? null : r.get("latitude").asDouble();
        Double longitude = r.get("longitude").isNull() ? null : r.get("longitude").asDouble();

        return new LocationDTO(
                r.get("id").asLong(),
                Neo4jStore.text(r.get("name")),
                Neo4jStore.text(r.get("streetAddress")),
                latitude,
                longitude
        );
    }

    @Override
    protected Map<String, Field> filters()
    {
        return Map.of("name", new Field("n.name", String.class), "streetAddress", new Field("n.streetAddress", String.class));
    }

    @Override
    protected Set<String> sorts()
    {
        return Set.of("id", "name", "streetAddress");
    }

    @Override
    public LocationDTO create(LocationRequestDTO request)
    {
        LocationValidator.validate(request);
        return store.write(tx ->
        {
            long id = Neo4jStore.nextId(tx, "location");
            tx.run("CREATE (n:Location) SET n = $props", Map.of("props",
                    Neo4jStore.properties("id", id, "name", request.name(), "streetAddress", request.streetAddress())));
            return find(tx, id);
        });
    }

    @Override
    public LocationDTO update(Long id, LocationRequestDTO request)
    {
        LocationValidator.validate(request);
        return store.write(tx ->
        {
            find(tx, id);
            tx.run("MATCH (n:Location {id: $id}) SET n.name = $name, n.streetAddress = $streetAddress",
                    Neo4jStore.parameters("id", id, "name", request.name(), "streetAddress", request.streetAddress()));
            return find(tx, id);
        });
    }

    @Override
    public void delete(Long id)
    {
        store.write(tx ->
        {
            find(tx, id);
            restrict(tx, "MATCH (n:Location {id: $id})<-[:LOCATED_AT]-()", id,
                    "Location " + id + " is still used by a housing, company or institution");
            tx.run("MATCH (n:Location {id: $id}) DELETE n", Map.of("id", id));
            return null;
        });
    }
}