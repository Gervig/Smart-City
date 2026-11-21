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
        ctx.json(service.getAll());
    }

    public void getById(Context ctx) {
        ctx.json(service.getById(id(ctx)));
    }

    public void create(Context ctx) {
        ctx.status(201).json(service.create(ctx.bodyAsClass(requestType)));
    }

    public void update(Context ctx) {
        ctx.json(service.update(id(ctx), ctx.bodyAsClass(requestType)));
    }

    public void delete(Context ctx) {
        service.delete(id(ctx));
        ctx.status(204);
    }

    private Long id(Context ctx) {
        return ctx.pathParamAsClass("id", Long.class).get();
    }
}
