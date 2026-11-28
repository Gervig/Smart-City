package app.controllers;

import app.dtos.JobDTO;
import app.dtos.JobRequestDTO;
import app.exceptions.ApiException;
import app.services.CrudService;
import dk.bugelhartmann.UserDTO;
import io.javalin.http.Context;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiContentProperty;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiRequestBody;
import io.javalin.openapi.OpenApiResponse;
import io.javalin.openapi.OpenApiSecurity;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * CRUD for jobs where the salary is for administrators only. This is the API counterpart of the database user
 * smartcity_restricted, which can read every job column except salary (SQL/users/08_users.sql).
 * <ul>
 *     <li>A normal user gets jobs with {@code salary: null}.</li>
 *     <li>A normal user may update a job but not its salary: the request must leave salary out (or null), and the
 *     stored salary is kept. Sending a salary gives 403.</li>
 *     <li>Creating a job needs a salary, so the create route is for administrators only (see Routes).</li>
 *     <li>A normal user cannot filter or sort the list by salary (403), because the order or a range would reveal
 *     the hidden values.</li>
 * </ul>
 */
public class JobController extends CrudController<JobDTO, JobRequestDTO> {

    public JobController(CrudService<JobDTO, JobRequestDTO> service) {
        super(service, JobRequestDTO.class);
    }

    @Override
    @OpenApi(path = "/jobs", methods = HttpMethod.GET, operationId = "listJobs",
            summary = "List jobs", tags = "Jobs",
            description = "Requires a valid bearer token and the USER or ADMIN role. String filters use case-insensitive contains matching; date and salary ranges are inclusive. USER responses hide salary, and USER requests cannot filter or sort by salary.",
            security = @OpenApiSecurity(name = "BearerAuth"),
            queryParams = {
                    @OpenApiParam(name = "page", type = Integer.class, description = "Zero-based page index; defaults to 0."),
                    @OpenApiParam(name = "size", type = Integer.class, description = "Page size from 1 to 100; defaults to 20."),
                    @OpenApiParam(name = "sort", description = "Sort field and direction as field,asc|desc. Fields: id, citizenId, companyId, institutionId, title, salary, startDate, endDate, status; defaults to id,asc. Salary sorting requires ADMIN."),
                    @OpenApiParam(name = "citizenId", type = Long.class, description = "Filter by citizen ID."),
                    @OpenApiParam(name = "companyId", type = Long.class, description = "Filter by company ID."),
                    @OpenApiParam(name = "institutionId", type = Long.class, description = "Filter by institution ID."),
                    @OpenApiParam(name = "title", description = "Optional substring filter."),
                    @OpenApiParam(name = "status", description = "Filter by status."),
                    @OpenApiParam(name = "salaryFrom", type = BigDecimal.class, description = "Inclusive minimum salary; ADMIN only."),
                    @OpenApiParam(name = "salaryTo", type = BigDecimal.class, description = "Inclusive maximum salary; ADMIN only."),
                    @OpenApiParam(name = "startDateFrom", description = "Inclusive lower date bound (YYYY-MM-DD)."),
                    @OpenApiParam(name = "startDateTo", description = "Inclusive upper date bound (YYYY-MM-DD)."),
                    @OpenApiParam(name = "endDateFrom", description = "Inclusive lower date bound (YYYY-MM-DD)."),
                    @OpenApiParam(name = "endDateTo", description = "Inclusive upper date bound (YYYY-MM-DD).")
            },
            responses = {
                    @OpenApiResponse(status = "200", description = "Paginated job results. Salary is null for USER; ADMIN receives salary values.", content = @OpenApiContent(mimeType = "application/json", type = "object", properties = {
                            @OpenApiContentProperty(name = "data", from = JobDTO.class, isArray = true),
                            @OpenApiContentProperty(name = "page", type = "integer", format = "int32"),
                            @OpenApiContentProperty(name = "size", type = "integer", format = "int32"),
                            @OpenApiContentProperty(name = "totalElements", type = "integer", format = "int64"),
                            @OpenApiContentProperty(name = "totalPages", type = "integer", format = "int64")
                    })),
                    @OpenApiResponse(status = "400", description = "Invalid pagination, sort, or filter parameter."),
                    @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                    @OpenApiResponse(status = "403", description = "A USER attempted a salary filter or sort."),
                    @OpenApiResponse(status = "500", description = "Unexpected server error.")
            })
    public void getAll(Context ctx) {
        if (!isAdmin(ctx) && usesSalary(ctx)) {
            throw new ApiException(403, "Only an administrator can filter or sort by salary");
        }
        super.getAll(ctx);
    }

    @Override
    @OpenApi(path = "/jobs/{id}", methods = HttpMethod.GET, operationId = "getJob",
            summary = "Get a job", tags = "Jobs",
            description = "Requires a valid bearer token and the USER or ADMIN role. Salary is hidden from USER and returned as null.",
            security = @OpenApiSecurity(name = "BearerAuth"),
            pathParams = @OpenApiParam(name = "id", type = Long.class, required = true, description = "Job ID."),
            responses = {
                    @OpenApiResponse(status = "200", description = "Job details.", content = @OpenApiContent(from = JobDTO.class)),
                    @OpenApiResponse(status = "400", description = "The ID is not a valid integer."),
                    @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                    @OpenApiResponse(status = "404", description = "Job not found."),
                    @OpenApiResponse(status = "500", description = "Unexpected server error.")
            })
    public void getById(Context ctx) {
        super.getById(ctx);
    }

    @Override
    @OpenApi(path = "/jobs", methods = HttpMethod.POST, operationId = "createJob",
            summary = "Create a job", tags = "Jobs",
            description = "Requires a valid bearer token and the ADMIN role. A salary is required when creating a job.",
            security = @OpenApiSecurity(name = "BearerAuth"),
            requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = JobRequestDTO.class), required = true),
            responses = {
                    @OpenApiResponse(status = "201", description = "Job created.", content = @OpenApiContent(from = JobDTO.class)),
                    @OpenApiResponse(status = "400", description = "Invalid job data or references."),
                    @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                    @OpenApiResponse(status = "403", description = "The authenticated user does not have the ADMIN role."),
                    @OpenApiResponse(status = "409", description = "A database constraint was violated."),
                    @OpenApiResponse(status = "500", description = "Unexpected server error.")
            })
    public void create(Context ctx) {
        super.create(ctx);
    }

    @Override
    @OpenApi(path = "/jobs/{id}", methods = HttpMethod.PUT, operationId = "updateJob",
            summary = "Update a job", tags = "Jobs",
            description = "Requires a valid bearer token and the USER or ADMIN role. ADMIN may update salary. USER must omit or send null for salary; the stored value is retained.",
            security = @OpenApiSecurity(name = "BearerAuth"),
            pathParams = @OpenApiParam(name = "id", type = Long.class, required = true, description = "Job ID."),
            requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = JobRequestDTO.class), required = true),
            responses = {
                    @OpenApiResponse(status = "200", description = "Updated job. Salary is hidden from USER and returned as null.", content = @OpenApiContent(from = JobDTO.class)),
                    @OpenApiResponse(status = "400", description = "Invalid ID or job data."),
                    @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                    @OpenApiResponse(status = "403", description = "A USER attempted to set a salary."),
                    @OpenApiResponse(status = "404", description = "Job not found."),
                    @OpenApiResponse(status = "409", description = "A database constraint was violated."),
                    @OpenApiResponse(status = "500", description = "Unexpected server error.")
            })
    public void update(Context ctx) {
        super.update(ctx);
    }

    @Override
    @OpenApi(path = "/jobs/{id}", methods = HttpMethod.DELETE, operationId = "deleteJob",
            summary = "Delete a job", tags = "Jobs",
            description = "Requires a valid bearer token and the USER or ADMIN role.",
            security = @OpenApiSecurity(name = "BearerAuth"),
            pathParams = @OpenApiParam(name = "id", type = Long.class, required = true, description = "Job ID."),
            responses = {
                    @OpenApiResponse(status = "204", description = "Job deleted."),
                    @OpenApiResponse(status = "400", description = "The ID is not a valid integer."),
                    @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                    @OpenApiResponse(status = "404", description = "Job not found."),
                    @OpenApiResponse(status = "409", description = "A database constraint prevents deletion."),
                    @OpenApiResponse(status = "500", description = "Unexpected server error.")
            })
    public void delete(Context ctx) {
        super.delete(ctx);
    }

    @Override
    protected JobDTO present(Context ctx, JobDTO job) {
        if (isAdmin(ctx)) {
            return job;
        }
        return new JobDTO(job.id(), job.citizenId(), job.companyId(), job.institutionId(), job.title(), null,
                job.startDate(), job.endDate(), job.status());
    }

    @Override
    protected JobRequestDTO prepareUpdate(Context ctx, Long id, JobRequestDTO request) {
        if (isAdmin(ctx)) {
            return request;
        }
        if (request.salary() != null) {
            throw new ApiException(403, "Only an administrator can set the salary");
        }
        BigDecimal storedSalary = service().getById(id).salary();
        return new JobRequestDTO(request.citizenId(), request.companyId(), request.institutionId(), request.title(),
                storedSalary, request.startDate(), request.endDate(), request.status());
    }

    /**
     * True when the request filters on salary (salaryFrom, salaryTo, or any parameter starting with salary) or sorts
     * by it. The check ignores case and spaces, so a variant spelling cannot slip past it.
     */
    private static boolean usesSalary(Context ctx) {
        boolean filter = ctx.queryParamMap().keySet().stream()
                .anyMatch(name -> name.trim().toLowerCase(Locale.ROOT).startsWith("salary"));
        boolean sort = ctx.queryParams("sort").stream()
                .anyMatch(value -> value.trim().toLowerCase(Locale.ROOT).startsWith("salary"));
        return filter || sort;
    }

    /**
     * Shared with the controllers for the database views, so there is one definition of who is an administrator.
     */
    static boolean isAdmin(Context ctx) {
        UserDTO user = ctx.attribute("user");
        return user != null && user.getRoles().stream().anyMatch(role -> role.equalsIgnoreCase("admin"));
    }
}