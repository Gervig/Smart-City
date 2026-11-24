package app.mongo;

import app.dtos.PageRequest;
import app.dtos.PageResponse;
import app.exceptions.NotFoundException;
import app.nosql.QueryRules;
import app.services.CrudService;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Facet;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * CRUD on rows that are embedded in the citizens' documents: jobs, education records and relationships. The API is
 * the same as for PostgreSQL (/jobs, /jobs/{id}, ...), even though there is no jobs collection.
 * <p>
 * Reading uses an aggregation pipeline: $unwind the array (one row per embedded element), $replaceWith a document in
 * the DTO's shape (so filters and sort fields are the API's names), $match the filters, and $facet for the page and
 * the total count in one round trip. Writing is $push / $pull on the citizen's array.
 */
public abstract class MongoEmbeddedService<D, R> implements CrudService<D, R>
{
    protected final MongoStore store;
    protected final MongoCollection<Document> citizens;
    private final String array;
    private final String entity;
    private final String sequence;

    protected MongoEmbeddedService(MongoStore store, String array, String entity, String sequence)
    {
        this.store = store;
        this.citizens = store.collection("citizens");
        this.array = array;
        this.entity = entity;
        this.sequence = sequence;
    }

    /** Stages after the $unwind that turn one citizen + one element into a row in the DTO's shape */
    protected abstract List<Bson> rowStages();

    protected abstract D toDTO(Document row);

    protected abstract void validate(R request);

    /**
     * Checks the references (404) and business rules, and returns which citizen gets which embedded document. A
     * relationship is embedded in both of its citizens.
     */
    protected abstract Map<Long, Document> place(R request, long id);

    protected abstract Map<String, QueryRules.Field> filters();

    protected abstract Map<String, String> sorts();

    private List<Bson> rows()
    {
        List<Bson> pipeline = new ArrayList<>();
        pipeline.add(Aggregates.unwind("$" + array));
        pipeline.addAll(rowStages());
        return pipeline;
    }

    @Override
    public List<D> getAll()
    {
        List<Bson> pipeline = rows();
        pipeline.add(Aggregates.sort(Sorts.ascending("id")));
        return citizens.aggregate(pipeline).map(this::toDTO).into(new ArrayList<>());
    }

    @Override
    public PageResponse<D> getAll(PageRequest request)
    {
        Bson filter = MongoStore.filter(QueryRules.conditions(request.filters(), filters()));
        String path = QueryRules.sortPath(request.sortField(), sorts());
        Bson order = request.sortDirection().equals("desc") ? Sorts.descending(path) : Sorts.ascending(path);
        Bson sort = path.equals("id") ? order : Sorts.orderBy(order, Sorts.ascending("id"));

        List<Bson> pipeline = rows();
        pipeline.add(Aggregates.match(filter));
        pipeline.add(Aggregates.facet(
                new Facet("data", Aggregates.sort(sort), Aggregates.skip(request.page() * request.size()),
                        Aggregates.limit(request.size())),
                new Facet("total", Aggregates.count("n"))));
        Document result = citizens.aggregate(pipeline).first();

        List<D> data = result.getList("data", Document.class).stream().map(this::toDTO).toList();
        List<Document> total = result.getList("total", Document.class);
        long count = total.isEmpty() ? 0 : ((Number) total.get(0).get("n")).longValue();
        return PageResponse.of(data, request.page(), request.size(), count);
    }

    @Override
    public D getById(Long id)
    {
        List<Bson> pipeline = rows();
        pipeline.add(Aggregates.match(Filters.eq("id", id)));
        Document row = citizens.aggregate(pipeline).first();
        if (row == null)
        {
            throw new NotFoundException(entity + " " + id + " not found");
        }
        return toDTO(row);
    }

    @Override
    public D create(R request)
    {
        validate(request);
        long id = store.nextId(sequence);
        push(place(request, id));
        return getById(id);
    }

    /**
     * Moves the element to the citizen(s) the request names. Without multi-document transactions (a standalone
     * MongoDB has none) the $pull and $push are two writes; each one is atomic for its document.
     */
    @Override
    public D update(Long id, R request)
    {
        validate(request);
        getById(id);
        Map<Long, Document> placement = place(request, id);
        pull(id);
        push(placement);
        return getById(id);
    }

    @Override
    public void delete(Long id)
    {
        getById(id);
        pull(id);
    }

    private void push(Map<Long, Document> placement)
    {
        placement.forEach((citizenId, element) ->
                citizens.updateOne(Filters.eq("_id", citizenId), Updates.push(array, element)));
    }

    private void pull(Long id)
    {
        citizens.updateMany(Filters.eq(array + ".id", id), Updates.pull(array, new Document("id", id)));
    }

    /** The citizen a row belongs to, or 404 */
    protected Document citizen(Long id)
    {
        return store.require("citizens", "Citizen", id);
    }

    /** Leaves out null values: the validator does not accept null for a typed field */
    protected static Document element(Object... keysAndValues)
    {
        Document document = new Document();
        for (int i = 0; i < keysAndValues.length; i += 2)
        {
            if (keysAndValues[i + 1] != null)
            {
                document.put((String) keysAndValues[i], keysAndValues[i + 1]);
            }
        }
        return document;
    }
}
