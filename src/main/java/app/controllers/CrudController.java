package app.controllers;

import app.services.CrudService;
import io.javalin.http.Context;

public class CrudController<D, R> {
    private final CrudService<D, R> service;
    private final Class<R> requestType;

    public CrudController(CrudService<D, R> service, Class<R> requestType) {
        this.service = service;
        this.requestType = requestType;
    }

    public void getAll(Context ctx) {
        ctx.json(service.getAll().stream().map(dto -> present(ctx, dto)).toList());
    }

    public void getById(Context ctx) {
        ctx.json(present(ctx, service.getById(id(ctx))));
    }

    public void create(Context ctx) {
        ctx.status(201).json(present(ctx, service.create(ctx.bodyAsClass(requestType))));
    }

    public void update(Context ctx) {
        Long id = id(ctx);
        ctx.json(present(ctx, service.update(id, prepareUpdate(ctx, id, ctx.bodyAsClass(requestType)))));
    }

    public void delete(Context ctx) {
        service.delete(id(ctx));
        ctx.status(204);
    }

    /**
     * Hook for a subclass to change what the caller gets back, for example to hide a field from some roles.
     */
    protected D present(Context ctx, D dto) {
        return dto;
    }

    /**
     * Hook for a subclass to check or change an update request before it is saved.
     */
    protected R prepareUpdate(Context ctx, Long id, R request) {
        return request;
    }

    protected CrudService<D, R> service() {
        return service;
    }

    private Long id(Context ctx) {
        return ctx.pathParamAsClass("id", Long.class).get();
    }
}
