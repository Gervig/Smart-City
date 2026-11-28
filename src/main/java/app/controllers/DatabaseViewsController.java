package app.controllers;

import app.services.DatabaseViewsService;
import io.javalin.http.Context;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiResponse;
import io.javalin.openapi.OpenApiSecurity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DatabaseViewsController {

    private final DatabaseViewsService service;

    public DatabaseViewsController(DatabaseViewsService service) {
        this.service = service;
    }

    @OpenApi(path = "/citizen-details", methods = HttpMethod.GET, operationId = "getCitizenDetails",
            summary = "View citizen details", tags = "Database Views",
            description = "Requires a valid bearer token and the USER or ADMIN role. Each result contains citizen_id, first_name, last_name, birth_date, age, gender, status, death_date, family_name, housing_id, housing_type, housing_capacity, and housing_location.",
            security = @OpenApiSecurity(name = "BearerAuth"),
            responses = {
                    @OpenApiResponse(status = "200", description = "Rows from the citizen details database view.", content = @OpenApiContent(from = Map[].class)),
                    @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                    @OpenApiResponse(status = "500", description = "Unexpected server error.")
            })
    public void getCitizenDetails(Context ctx) {
        ctx.json(service.getCitizenDetails());
    }

    @OpenApi(path = "/housing-occupancy", methods = HttpMethod.GET, operationId = "getHousingOccupancy",
            summary = "View housing occupancy", tags = "Database Views",
            description = "Requires a valid bearer token and the USER or ADMIN role. Each result contains housing_id, location_name, street_address, housing_type, capacity, current_occupants, and available_spots.",
            security = @OpenApiSecurity(name = "BearerAuth"),
            responses = {
                    @OpenApiResponse(status = "200", description = "Rows from the housing occupancy view.", content = @OpenApiContent(from = Map[].class)),
                    @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                    @OpenApiResponse(status = "500", description = "Unexpected server error.")
            })
    public void getHousingOccupancy(Context ctx) {
        ctx.json(service.getHousingOccupancy());
    }

    @OpenApi(path = "/company-financial-summary", methods = HttpMethod.GET, operationId = "getCompanyFinancialSummary",
            summary = "View company financial summary", tags = "Database Views",
            description = "Requires a valid bearer token and the ADMIN role. Each result contains company_id, company_name, active_job_count, and total_active_salaries.",
            security = @OpenApiSecurity(name = "BearerAuth"),
            responses = {
                    @OpenApiResponse(status = "200", description = "Rows from the company financial summary view.", content = @OpenApiContent(from = Map[].class)),
                    @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                    @OpenApiResponse(status = "403", description = "The authenticated user does not have the ADMIN role."),
                    @OpenApiResponse(status = "500", description = "Unexpected server error.")
            })
    public void getCompanyFinancialSummary(Context ctx) {
        ctx.json(service.getCompanyFinancialSummary());
    }

    /**
     * The view contains the salary of every active job. A normal user gets the rows with {@code salary: null}, the
     * same as on /jobs; an administrator gets the real values.
     */
    @OpenApi(path = "/active-employment", methods = HttpMethod.GET, operationId = "getActiveEmployment",
            summary = "View active employment", tags = "Database Views",
            description = "Requires a valid bearer token and the USER or ADMIN role. Each result contains citizen_id, citizen_name, job_title, salary, company_id, company_name, and start_date. The salary field is null for USER and visible to ADMIN.",
            security = @OpenApiSecurity(name = "BearerAuth"),
            responses = {
                    @OpenApiResponse(status = "200", description = "Rows from the active employment view; salary is hidden from USER.", content = @OpenApiContent(from = Map[].class)),
                    @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                    @OpenApiResponse(status = "500", description = "Unexpected server error.")
            })
    public void getActiveEmployment(Context ctx) {
        List<Map<String, Object>> rows = service.getActiveEmployment();
        if (JobController.isAdmin(ctx)) {
            ctx.json(rows);
            return;
        }
        ctx.json(rows.stream().map(DatabaseViewsController::withoutSalary).toList());
    }

    @OpenApi(path = "/citizen-relationships-view", methods = HttpMethod.GET, operationId = "getCitizenRelationshipsView",
            summary = "View citizen relationships", tags = "Database Views",
            description = "Requires a valid bearer token and the USER or ADMIN role. Each result contains relationship_id, citizen_1_id, citizen_1_name, relationship_type, citizen_2_id, citizen_2_name, start_date, and end_date.",
            security = @OpenApiSecurity(name = "BearerAuth"),
            responses = {
                    @OpenApiResponse(status = "200", description = "Rows from the citizen relationships view.", content = @OpenApiContent(from = Map[].class)),
                    @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                    @OpenApiResponse(status = "500", description = "Unexpected server error.")
            })
    public void getCitizenRelationships(Context ctx) {
        ctx.json(service.getCitizenRelationships());
    }

    @OpenApi(path = "/citizens/{id}/total-salary", methods = HttpMethod.GET, operationId = "getTotalCitizenSalary",
            summary = "Get a citizen's total salary", tags = "Jobs",
            description = "Requires a valid bearer token and the ADMIN role. Returns the stored function's total salary as a JSON number.",
            security = @OpenApiSecurity(name = "BearerAuth"),
            pathParams = @OpenApiParam(name = "id", type = Long.class, required = true, description = "Citizen ID."),
            responses = {
                    @OpenApiResponse(status = "200", description = "Total salary for the citizen.", content = @OpenApiContent(from = java.math.BigDecimal.class)),
                    @OpenApiResponse(status = "400", description = "Invalid citizen ID."),
                    @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                    @OpenApiResponse(status = "403", description = "The authenticated user does not have the ADMIN role."),
                    @OpenApiResponse(status = "500", description = "Unexpected server error.")
            })
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
