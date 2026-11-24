package app.neo4j;

import app.dtos.JobDTO;
import app.dtos.JobRequestDTO;
import app.nosql.QueryRules.Field;
import app.validators.JobValidator;
import org.neo4j.driver.Record;
import org.neo4j.driver.TransactionContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Set;

/**
 * A job is a relationship with properties: (:Citizen)-[:WORKS_AT {id, title, salary, startDate, endDate, status}]
 * ->(:Company|:Institution). A relationship's ends cannot be changed, so an update deletes and recreates it in the
 * same transaction, keeping its id. The salary is a float here (Neo4j has no decimal type).
 */
public class Neo4jJobService extends Neo4jCrudService<JobDTO, JobRequestDTO>
{
    public Neo4jJobService(Neo4jStore store)
    {
        super(store, "Job");
    }

    @Override
    protected String match()
    {
        return "MATCH (c:Citizen)-[r:WORKS_AT]->(e)";
    }

    @Override
    protected String variables()
    {
        return "c, r, e";
    }

    @Override
    protected String idExpression()
    {
        return "r.id";
    }

    @Override
    protected String projection()
    {
        return "r.id AS id, c.id AS citizenId, CASE WHEN e:Company THEN e.id END AS companyId, "
                + "CASE WHEN e:Institution THEN e.id END AS institutionId, r.title AS title, r.salary AS salary, "
                + "r.startDate AS startDate, r.endDate AS endDate, r.status AS status";
    }

    @Override
    protected JobDTO toDTO(Record r)
    {
        return new JobDTO(r.get("id").asLong(), r.get("citizenId").asLong(), Neo4jStore.id(r.get("companyId")),
                Neo4jStore.id(r.get("institutionId")), Neo4jStore.text(r.get("title")),
                Neo4jStore.decimal(r.get("salary")), Neo4jStore.date(r.get("startDate")),
                Neo4jStore.date(r.get("endDate")), Neo4jStore.text(r.get("status")));
    }

    @Override
    protected Map<String, Field> filters()
    {
        return Map.ofEntries(Map.entry("citizenId", new Field("c.id", Long.class)),
                Map.entry("companyId", new Field("CASE WHEN e:Company THEN e.id END", Long.class)),
                Map.entry("institutionId", new Field("CASE WHEN e:Institution THEN e.id END", Long.class)),
                Map.entry("title", new Field("r.title", String.class)),
                Map.entry("status", new Field("r.status", String.class)),
                Map.entry("salaryFrom", new Field("r.salary", BigDecimal.class)),
                Map.entry("salaryTo", new Field("r.salary", BigDecimal.class)),
                Map.entry("startDateFrom", new Field("r.startDate", LocalDate.class)),
                Map.entry("startDateTo", new Field("r.startDate", LocalDate.class)),
                Map.entry("endDateFrom", new Field("r.endDate", LocalDate.class)),
                Map.entry("endDateTo", new Field("r.endDate", LocalDate.class)));
    }

    @Override
    protected Set<String> sorts()
    {
        return Set.of("id", "citizenId", "companyId", "institutionId", "title", "salary", "startDate", "endDate",
                "status");
    }

    @Override
    public JobDTO create(JobRequestDTO request)
    {
        JobValidator.validate(request);
        return store.write(tx ->
        {
            check(tx, request);
            long id = Neo4jStore.nextId(tx, "job");
            relate(tx, id, request);
            return find(tx, id);
        });
    }

    @Override
    public JobDTO update(Long id, JobRequestDTO request)
    {
        JobValidator.validate(request);
        return store.write(tx ->
        {
            find(tx, id);
            check(tx, request);
            tx.run("MATCH ()-[r:WORKS_AT {id: $id}]->() DELETE r", Map.of("id", id));
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
            tx.run("MATCH ()-[r:WORKS_AT {id: $id}]->() DELETE r", Map.of("id", id));
            return null;
        });
    }

    // The same rules as JobService: citizen and employer exist (404), the start lies within the citizen's life (400)
    private static void check(TransactionContext tx, JobRequestDTO request)
    {
        requireNode(tx, "Citizen", request.citizenId());
        Record life = tx.run("MATCH (c:Citizen {id: $id}) RETURN c.birthDate AS birth, c.deathDate AS death",
                Map.of("id", request.citizenId())).single();
        if (request.startDate().isBefore(life.get("birth").asLocalDate()))
        {
            throw new IllegalArgumentException("startDate must not be before the citizen's birthDate");
        }
        LocalDate death = Neo4jStore.date(life.get("death"));
        if (death != null && request.startDate().isAfter(death))
        {
            throw new IllegalArgumentException("startDate must not be after the citizen's deathDate");
        }
        if (request.companyId() != null)
        {
            requireNode(tx, "Company", request.companyId());
        }
        else
        {
            requireNode(tx, "Institution", request.institutionId());
        }
    }

    private static void relate(TransactionContext tx, long id, JobRequestDTO request)
    {
        String employer = request.companyId() != null ? "Company" : "Institution";
        Long employerId = request.companyId() != null ? request.companyId() : request.institutionId();
        tx.run("MATCH (c:Citizen {id: $citizenId}), (e:" + employer + " {id: $employerId}) "
                        + "CREATE (c)-[r:WORKS_AT]->(e) SET r = $props",
                Map.of("citizenId", request.citizenId(), "employerId", employerId, "props", Neo4jStore.properties(
                        "id", id, "title", request.title(), "salary", request.salary(),
                        "startDate", request.startDate(), "endDate", request.endDate(), "status", request.status())));
    }
}
