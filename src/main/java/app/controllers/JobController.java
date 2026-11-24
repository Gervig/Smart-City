package app.controllers;

import app.dtos.JobDTO;
import app.dtos.JobRequestDTO;
import app.exceptions.ApiException;
import app.services.CrudService;
import dk.bugelhartmann.UserDTO;
import io.javalin.http.Context;

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
    public void getAll(Context ctx) {
        if (!isAdmin(ctx) && usesSalary(ctx)) {
            throw new ApiException(403, "Only an administrator can filter or sort by salary");
        }
        super.getAll(ctx);
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