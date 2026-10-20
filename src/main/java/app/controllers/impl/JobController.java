
package app.controllers.impl;

import app.daos.impl.JobDAO;
import app.entities.Job;

public class JobController extends CrudController<Job> {

    public JobController(JobDAO jobDAO) {
        super(
                jobDAO,
                Job.class,
                "/jobs",
                "Job",
                Job::setId
        );
    }
}