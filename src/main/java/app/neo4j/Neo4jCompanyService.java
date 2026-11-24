package app.neo4j;

import app.dtos.CompanyDTO;
import app.dtos.CompanyRequestDTO;
import app.nosql.QueryRules.Field;
import app.validators.CompanyValidator;
import org.neo4j.driver.Record;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

/**
 * (:Company)-[:LOCATED_AT]->(:Location). Employees point at it with WORKS_AT; a company with jobs is kept (409).
 * Unlike the document model, the name lives in one place only, so a rename changes nothing else.
 */
public class Neo4jCompanyService extends Neo4jCrudService<CompanyDTO, CompanyRequestDTO>
{
    public Neo4jCompanyService(Neo4jStore store)
    {
        super(store, "Company");
    }

    @Override
    protected String match()
    {
        return "MATCH (n:Company) OPTIONAL MATCH (n)-[:LOCATED_AT]->(location:Location)";
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
        return "n.id AS id, location.id AS locationId, n.name AS name, n.foundedDate AS foundedDate, "
                + "n.dissolvedDate AS dissolvedDate, n.status AS status";
    }

    @Override
    protected CompanyDTO toDTO(Record r)
    {
        return new CompanyDTO(r.get("id").asLong(), Neo4jStore.id(r.get("locationId")), Neo4jStore.text(r.get("name")),
                Neo4jStore.date(r.get("foundedDate")), Neo4jStore.date(r.get("dissolvedDate")),
                Neo4jStore.text(r.get("status")));
    }

    @Override
    protected Map<String, Field> filters()
    {
        return Map.of("locationId", new Field("location.id", Long.class),
                "name", new Field("n.name", String.class),
                "status", new Field("n.status", String.class),
                "foundedDateFrom", new Field("n.foundedDate", LocalDate.class),
                "foundedDateTo", new Field("n.foundedDate", LocalDate.class));
    }

    @Override
    protected Set<String> sorts()
    {
        return Set.of("id", "locationId", "name", "foundedDate", "dissolvedDate", "status");
    }

    private static Map<String, Object> parameters(long id, CompanyRequestDTO request)
    {
        return Neo4jStore.parameters("id", id, "locationId", request.locationId(), "name", request.name(),
                "foundedDate", request.foundedDate(), "dissolvedDate", request.dissolvedDate(),
                "status", request.status());
    }

    @Override
    public CompanyDTO create(CompanyRequestDTO request)
    {
        CompanyValidator.validate(request);
        return store.write(tx ->
        {
            requireNode(tx, "Location", request.locationId());
            long id = Neo4jStore.nextId(tx, "company");
            tx.run("MATCH (l:Location {id: $locationId}) CREATE (n:Company {id: $id})-[:LOCATED_AT]->(l) "
                    + "SET n.name = $name, n.foundedDate = $foundedDate, n.dissolvedDate = $dissolvedDate, "
                    + "n.status = $status", parameters(id, request));
            return find(tx, id);
        });
    }

    @Override
    public CompanyDTO update(Long id, CompanyRequestDTO request)
    {
        CompanyValidator.validate(request);
        return store.write(tx ->
        {
            find(tx, id);
            requireNode(tx, "Location", request.locationId());
            tx.run("MATCH (n:Company {id: $id}) SET n.name = $name, n.foundedDate = $foundedDate, "
                    + "n.dissolvedDate = $dissolvedDate, n.status = $status "
                    + "WITH n OPTIONAL MATCH (n)-[old:LOCATED_AT]->() DELETE old "
                    + "WITH DISTINCT n MATCH (l:Location {id: $locationId}) CREATE (n)-[:LOCATED_AT]->(l)",
                    parameters(id, request));
            return find(tx, id);
        });
    }

    @Override
    public void delete(Long id)
    {
        store.write(tx ->
        {
            find(tx, id);
            restrict(tx, "MATCH (n:Company {id: $id})<-[:WORKS_AT]-()", id, "Company " + id + " still has jobs");
            tx.run("MATCH (n:Company {id: $id}) DETACH DELETE n", Map.of("id", id));
            return null;
        });
    }
}
