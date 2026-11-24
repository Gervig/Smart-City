package app.neo4j;

import app.dtos.FamilyDTO;
import app.dtos.FamilyRequestDTO;
import app.nosql.QueryRules.Field;
import app.validators.FamilyValidator;
import org.neo4j.driver.Record;

import java.util.Map;
import java.util.Set;

/** (:Family). Citizens point at it with MEMBER_OF; a family with members is kept (409). */
public class Neo4jFamilyService extends Neo4jCrudService<FamilyDTO, FamilyRequestDTO>
{
    public Neo4jFamilyService(Neo4jStore store)
    {
        super(store, "Family");
    }

    @Override
    protected String match()
    {
        return "MATCH (n:Family)";
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
        return "n.id AS id, n.familyName AS familyName";
    }

    @Override
    protected FamilyDTO toDTO(Record r)
    {
        return new FamilyDTO(r.get("id").asLong(), Neo4jStore.text(r.get("familyName")));
    }

    @Override
    protected Map<String, Field> filters()
    {
        return Map.of("familyName", new Field("n.familyName", String.class));
    }

    @Override
    protected Set<String> sorts()
    {
        return Set.of("id", "familyName");
    }

    @Override
    public FamilyDTO create(FamilyRequestDTO request)
    {
        FamilyValidator.validate(request);
        return store.write(tx ->
        {
            long id = Neo4jStore.nextId(tx, "family");
            tx.run("CREATE (n:Family {id: $id, familyName: $familyName})",
                    Map.of("id", id, "familyName", request.familyName()));
            return find(tx, id);
        });
    }

    @Override
    public FamilyDTO update(Long id, FamilyRequestDTO request)
    {
        FamilyValidator.validate(request);
        return store.write(tx ->
        {
            find(tx, id);
            tx.run("MATCH (n:Family {id: $id}) SET n.familyName = $familyName",
                    Map.of("id", id, "familyName", request.familyName()));
            return find(tx, id);
        });
    }

    @Override
    public void delete(Long id)
    {
        store.write(tx ->
        {
            find(tx, id);
            restrict(tx, "MATCH (n:Family {id: $id})<-[:MEMBER_OF]-()", id, "Family " + id + " still has members");
            tx.run("MATCH (n:Family {id: $id}) DELETE n", Map.of("id", id));
            return null;
        });
    }
}
