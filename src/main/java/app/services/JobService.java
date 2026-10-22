package app.services;

import app.daos.GenericDAO;
import app.dtos.JobDTO;
import app.dtos.JobRequestDTO;
import app.entities.Citizen;
import app.entities.Company;
import app.entities.Institution;
import app.entities.Job;
import app.mappers.JobMapper;

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
    protected void resolveReferences(Job entity, JobRequestDTO request) {
        if (request.companyId() == null && request.institutionId() == null) {
            throw new IllegalArgumentException("A job needs a company or an institution");
        }
        entity.setCitizen(citizenDAO.findByIdOrThrow(request.citizenId()));
        entity.setCompany(optional(companyDAO, request.companyId()));
        entity.setInstitution(optional(institutionDAO, request.institutionId()));
    }
}
