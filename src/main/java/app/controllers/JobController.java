package app.controllers;

import app.dtos.JobDTO;
import app.dtos.JobRequestDTO;
import app.exceptions.ApiException;
import app.services.CrudService;
import dk.bugelhartmann.UserDTO;
import io.javalin.http.Context;

import java.math.BigDecimal;

/**
 * CRUD for jobs where the salary is for administrators only. This is the API counterpart of the database user
 * smartcity_restricted, which can read every job column except salary (SQL/users/08_users.sql).
 * <ul>
 *     <li>A normal user gets jobs with {@code salary: null}.</li>
 *     <li>A normal user may update a job but not its salary: the request must leave salary out (or null), and the
 *     stored salary is kept. Sending a salary gives 403.</li>
 *     <li>Creating a job needs a salary, so the create route is for administrators only (see Routes).</li>
 * </ul>
 */
public class JobController extends CrudController<JobDTO, JobRequestDTO> {

    public JobController(CrudService<JobDTO, JobRequestDTO> service) {
        super(service, JobRequestDTO.class);
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

    private static boolean isAdmin(Context ctx) {
        UserDTO user = ctx.attribute("user");
        return user != null && user.getRoles().stream().anyMatch(role -> role.equalsIgnoreCase("admin"));
    }
}
