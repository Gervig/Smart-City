package app.mongo;

import app.dtos.PageRequest;
import app.dtos.PageResponse;
import app.nosql.QueryRules;
import app.services.CrudService;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * CRUD on one top-level collection, with the same contract as the PostgreSQL services: same DTOs, validators,
 * paging, filters and sort fields, 404 for an unknown id and 409 when a delete would break a reference.
 * <p>
 * A subclass says how a request becomes fields ({@link #fields}), and which other documents to keep in step after a
 * write. MongoDB has no foreign keys or cascades, so the services enforce references themselves and update the
 * copies that denormalisation spread over other documents.
 */
public abstract class MongoCollectionService<D, R> implements CrudService<D, R>
{
    protected final MongoStore store;
    protected final MongoCollection<Document> collection;
    private final String collectionName;
    private final String entity;
    private final String sequence;

    protected MongoCollectionService(MongoStore store, String collectionName, String entity, String sequence)
    {
        this.store = store;
        this.collection = store.collection(collectionName);
        this.collectionName = collectionName;
        this.entity = entity;
        this.sequence = sequence;
    }

    protected abstract D toDTO(Document document);

    protected abstract void validate(R request);

    /**
     * The fields the request sets, with references checked (404) and copies embedded. A null value removes the
     * field: the collection's validator does not accept null for a typed field.
     *
     * @param id null on create
     */
    protected abstract Document fields(R request, Long id);

    protected abstract Map<String, QueryRules.Field> filters();

    /** API sort field to document path; "id" is always "_id" */
    protected abstract Map<String, String> sorts();

    /** Fields only a new document gets, for example an empty list of members */
    protected Document newDocumentExtras()
    {
        return new Document();
    }

    protected void afterCreate(long id, Document fields)
    {
    }

    protected void afterUpdate(Document before, long id, Document fields)
    {
    }

    /** Throw 409 here when other documents still refer to this one (like ON DELETE RESTRICT) */
    protected void beforeDelete(Document existing)
    {
    }

    protected void afterDelete(Document existing)
    {
    }

    @Override
    public List<D> getAll()
    {
        return collection.find().sort(Sorts.ascending("_id")).map(this::toDTO).into(new ArrayList<>());
    }

    @Override
    public PageResponse<D> getAll(PageRequest request)
    {
        Bson filter = MongoStore.filter(QueryRules.conditions(request.filters(), filters()));
        String path = QueryRules.sortPath(request.sortField(), sorts());
        Bson order = request.sortDirection().equals("desc") ? Sorts.descending(path) : Sorts.ascending(path);
        Bson sort = path.equals("_id") ? order : Sorts.orderBy(order, Sorts.ascending("_id"));
        List<D> data = collection.find(filter).sort(sort)
                .skip(request.page() * request.size()).limit(request.size())
                .map(this::toDTO).into(new ArrayList<>());
        return PageResponse.of(data, request.page(), request.size(), collection.countDocuments(filter));
    }

    @Override
    public D getById(Long id)
    {
        return toDTO(require(id));
    }

    protected Document require(Long id)
    {
        return store.require(collectionName, entity, id);
    }

    @Override
    public D create(R request)
    {
        validate(request);
        Document fields = fields(request, null);
        long id = store.nextId(sequence);
        Document document = new Document("_id", id);
        fields.forEach((key, value) ->
        {
            if (value != null)
            {
                document.put(key, value);
            }
        });
        document.putAll(newDocumentExtras());
        collection.insertOne(document);
        afterCreate(id, fields);
        return getById(id);
    }

    @Override
    public D update(Long id, R request)
    {
        validate(request);
        Document before = require(id);
        Document fields = fields(request, id);
        List<Bson> updates = new ArrayList<>();
        fields.forEach((key, value) -> updates.add(value == null ? Updates.unset(key) : Updates.set(key, value)));
        collection.updateOne(Filters.eq("_id", id), Updates.combine(updates));
        afterUpdate(before, id, fields);
        return getById(id);
    }

    @Override
    public void delete(Long id)
    {
        Document existing = require(id);
        beforeDelete(existing);
        collection.deleteOne(Filters.eq("_id", id));
        afterDelete(existing);
    }
}
