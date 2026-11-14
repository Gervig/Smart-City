package app.services;

import app.daos.GenericDAO;
import app.dtos.JobDTO;
import app.dtos.JobRequestDTO;
import app.entities.Citizen;
import app.entities.Company;
import app.entities.Institution;
import app.entities.Job;
import app.mappers.JobMapper;
import app.validators.JobValidator;

public class JobService extends AbstractCrudService<Job, JobDTO, JobRequestDTO> {
    private final GenericDAO<Citizen> citizenDAO;
    private final GenericDAO<Company> companyDAO;
    private final GenericDAO<Institution> institutionDAO;

    public JobService(GenericDAO<Job> dao, GenericDAO<Citizen> citizenDAO, GenericDAO<Company> companyDAO, GenericDAO<Institution> institutionDAO) {
        super(dao, JobMapper.INSTANCE::toDTO, JobMapper.INSTANCE::toEntity, JobMapper.INSTANCE::update);
        this.citizenDAO = citizenDAO;
        this.companyDAO = companyDAO;
        this.institutionDAO = institutionDAO;
    }

    @Override
    protected void validate(JobRequestDTO request) {
        JobValidator.validate(request);
    }

    @Override
    protected void resolveReferences(Job entity, JobRequestDTO request) {
        entity.setCitizen(citizenDAO.findByIdOrThrow(request.citizenId()));
        if (request.startDate().isBefore(entity.getCitizen().getBirthDate())) {
            throw new IllegalArgumentException("startDate must not be before the citizen's birthDate");
        }
        if (entity.getCitizen().getDeathDate() != null
                && request.startDate().isAfter(entity.getCitizen().getDeathDate())) {
            throw new IllegalArgumentException("startDate must not be after the citizen's deathDate");
        }
        entity.setCompany(optional(companyDAO, request.companyId()));
        entity.setInstitution(optional(institutionDAO, request.institutionId()));
    }
}
