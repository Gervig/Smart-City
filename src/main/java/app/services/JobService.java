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

import java.util.Map;

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
    protected Map<String, String> allowedSortFields() {
        return Map.of(
                "id", "id",
                "citizenId", "citizen.id",
                "companyId", "company.id",
                "institutionId", "institution.id",
                "title", "title",
                "salary", "salary",
                "startDate", "startDate",
                "endDate", "endDate",
                "status", "status"
        );
    }

    @Override
    protected Map<String, String> allowedFilters() {
        return Map.ofEntries(
                Map.entry("citizenId", "citizen.id"),
                Map.entry("companyId", "company.id"),
                Map.entry("institutionId", "institution.id"),
                Map.entry("title", "title"),
                Map.entry("status", "status"),
                Map.entry("salaryFrom", "salary"),
                Map.entry("salaryTo", "salary"),
                Map.entry("startDateFrom", "startDate"),
                Map.entry("startDateTo", "startDate"),
                Map.entry("endDateFrom", "endDate"),
                Map.entry("endDateTo", "endDate")
        );
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
