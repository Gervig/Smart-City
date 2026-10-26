package app.controllers.impl;

import app.daos.IDAO;

import io.javalin.apibuilder.EndpointGroup;
import io.javalin.http.BadRequestResponse;
import io.javalin.http.NotFoundResponse;

import java.util.function.BiConsumer;

import static io.javalin.apibuilder.ApiBuilder.*;

public abstract class CrudController<T>
        implements EndpointGroup {

    private final IDAO<T, Long> dao;
    private final Class<T> entityClass;
    private final String route;
    private final String entityName;
    private final BiConsumer<T, Long> setId;

    protected CrudController(
            IDAO<T, Long> dao,
            Class<T> entityClass,
            String route,
            String entityName,
            BiConsumer<T, Long> setId) {

        this.dao = dao;
        this.entityClass = entityClass;
        this.route = route;
        this.entityName = entityName;
        this.setId = setId;
    }

    @Override
    public void addEndpoints() {
        path(route, () -> {

            // GET: Read all records
            get(ctx -> ctx.json(dao.readAll()));

            // GET: Read one record
            get("/{id}", ctx -> {
                Long id = parseId(ctx.pathParam("id"));
                T entity = dao.read(id);

                if (entity == null) {
                    throw new NotFoundResponse(
                            entityName + " not found");
                }

                ctx.json(entity);
            });

            // POST: Create a record
            post(ctx -> {
                T entity = ctx.bodyAsClass(entityClass);
                T created = dao.create(entity);

                ctx.status(201).json(created);
            });

            // PUT: Update a record
            put("/{id}", ctx -> {
                Long id = parseId(ctx.pathParam("id"));

                if (dao.read(id) == null) {
                    throw new NotFoundResponse(
                            entityName + " not found");
                }

                T entity = ctx.bodyAsClass(entityClass);

                setId.accept(entity, id);

                ctx.json(dao.update(entity));
            });

            // DELETE: Delete a record
            delete("/{id}", ctx -> {
                Long id = parseId(ctx.pathParam("id"));

                if (dao.read(id) == null) {
                    throw new NotFoundResponse(
                            entityName + " not found");
                }

                dao.delete(id);

                ctx.status(204);
            });
        });
    }

    private Long parseId(String value) {
        try {
            long id = Long.parseLong(value);

            if (id <= 0) {
                throw new NumberFormatException();
            }

            return id;
        } catch (NumberFormatException e) {
            throw new BadRequestResponse(
                    "ID must be a positive integer");
        }
    }
}