package app.controllers;

import app.services.DatabaseViewsService;
import io.javalin.http.Context;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * The view contains the salary of every active job. A normal user gets the rows with {@code salary: null}, the
     * same as on /jobs; an administrator gets the real values.
     */
    public void getActiveEmployment(Context ctx) {
        List<Map<String, Object>> rows = service.getActiveEmployment();
        if (JobController.isAdmin(ctx)) {
            ctx.json(rows);
            return;
        }
        ctx.json(rows.stream().map(DatabaseViewsController::withoutSalary).toList());
    }

    public void getCitizenRelationships(Context ctx) {
        ctx.json(service.getCitizenRelationships());
    }

    public void getTotalCitizenSalary(Context ctx) {
        Long citizenId = ctx.pathParamAsClass("id", Long.class).get();

        ctx.json(service.getTotalCitizenSalary(citizenId));
    }

    private static Map<String, Object> withoutSalary(Map<String, Object> row) {
        Map<String, Object> copy = new LinkedHashMap<>(row);
        copy.put("salary", null);
        return copy;
    }
}
