package app.controllers;

import app.services.DatabaseViewsService;
import io.javalin.http.Context;

public class DatabaseViewsController {

    private final DatabaseViewsService service;

    public DatabaseViewsController(DatabaseViewsService service) {
        this.service = service;
    }

    public void getCitizenDetails(Context ctx) {
        ctx.json(service.getCitizenDetails());
    }

    public void getHousingOccupancy(Context ctx) {
        ctx.json(service.getHousingOccupancy());
    }

    public void getCompanyFinancialSummary(Context ctx) {
        ctx.json(service.getCompanyFinancialSummary());
    }

    public void getActiveEmployment(Context ctx) {
        ctx.json(service.getActiveEmployment());
    }

    public void getCitizenRelationships(Context ctx) {
        ctx.json(service.getCitizenRelationships());
    }

    public void getTotalCitizenSalary(Context ctx) {
        Long citizenId = ctx.pathParamAsClass("id", Long.class).get();

        ctx.json(service.getTotalCitizenSalary(citizenId));
    }
}
