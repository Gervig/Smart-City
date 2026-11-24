package app.neo4j;

import app.dtos.PageRequest;
import app.dtos.PageResponse;
import app.exceptions.ApiException;
import app.exceptions.NotFoundException;
import app.nosql.QueryRules;
import app.services.CrudService;
import org.neo4j.driver.Record;
import org.neo4j.driver.TransactionContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The read side shared by all Neo4j services: list with paging, filters and sorting, and get by id, with the same
 * contract as the PostgreSQL services. A subclass gives its MATCH pattern and how a row becomes a DTO; the write side
 * (create, update, delete) is written per service, because nodes and relationships are written differently.
 * <p>
 * Read query: {@code <match> WITH <variables> WHERE <filters> RETURN <projection> ORDER BY <sort> SKIP LIMIT}. The
 * projection's aliases are the DTO's field names, so a sort field is a column of the result.
 */
public abstract class Neo4jCrudService<D, R> implements CrudService<D, R>
{
    protected final Neo4jStore store;
    private final String entity;

    protected Neo4jCrudService(Neo4jStore store, String entity)
    {
        this.store = store;
        this.entity = entity;
    }

    /** For example "MATCH (n:Housing) OPTIONAL MATCH (n)-[:LOCATED_AT]->(location)" */
    protected abstract String match();

    /** The variables the WHERE and RETURN use, for example "n, location" */
    protected abstract String variables();

    /** The expression of the id, for example "n.id" (a node) or "r.id" (a relationship) */
    protected abstract String idExpression();

    /** For example "n.id AS id, location.id AS locationId, n.capacity AS capacity" */
    protected abstract String projection();

    protected abstract D toDTO(Record row);

    /** API filter name to a Cypher expression over the variables */
    protected abstract Map<String, QueryRules.Field> filters();

    /** API sort fields: aliases of the projection */
    protected abstract Set<String> sorts();

    @Override
    public List<D> getAll()
    {
        return store.read(tx -> tx.run(match() + " WITH " + variables() + " RETURN " + projection() + " ORDER BY id")
                .list(this::toDTO));
    }

    @Override
    public PageResponse<D> getAll(PageRequest request)
    {
        Map<String, Object> parameters = new HashMap<>();
        String where = Neo4jStore.where(QueryRules.conditions(request.filters(), filters()), parameters);
        String sort = request.sortField();
        if (!sorts().contains(sort))
        {
            throw new IllegalArgumentException("Unsupported sort field: " + sort);
        }
        String direction = request.sortDirection().equals("desc") ? " DESC" : " ASC";
        String order = " ORDER BY " + sort + direction + (sort.equals("id") ? "" : ", id ASC");
        parameters.put("skip", (long) request.page() * request.size());
        parameters.put("limit", (long) request.size());

        String base = match() + " WITH " + variables() + where;
        return store.read(tx ->
        {
            List<D> data = tx.run(base + " RETURN " + projection() + order + " SKIP $skip LIMIT $limit", parameters)
                    .list(this::toDTO);
            long total = tx.run(base + " RETURN count(*) AS total", parameters).single().get("total").asLong();
            return PageResponse.of(data, request.page(), request.size(), total);
        });
    }

    @Override
    public D getById(Long id)
    {
        return store.read(tx -> find(tx, id));
    }

    /** The row with this id inside a running transaction, or 404 */
    protected D find(TransactionContext tx, Long id)
    {
        var result = tx.run(match() + " WITH " + variables() + " WHERE " + idExpression() + " = $id RETURN "
                + projection(), Map.of("id", id));
        if (!result.hasNext())
        {
            throw new NotFoundException(entity + " " + id + " not found");
        }
        return toDTO(result.next());
    }

    /** 404 unless a node with this label and id exists */
    protected static void requireNode(TransactionContext tx, String label, Long id)
    {
        if (id == null || !Neo4jStore.exists(tx, "MATCH (n:" + label + " {id: $id}) RETURN n", Map.of("id", id)))
        {
            throw new NotFoundException(label + " " + id + " not found");
        }
    }

    /** 409 when the pattern still finds something (the graph's ON DELETE RESTRICT) */
    protected static void restrict(TransactionContext tx, String pattern, Long id, String message)
    {
        if (Neo4jStore.exists(tx, pattern + " RETURN 1 LIMIT 1", Map.of("id", id)))
        {
            throw new ApiException(409, message);
        }
    }
}
