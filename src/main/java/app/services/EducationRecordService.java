package app.services;

import app.daos.GenericDAO;
import app.dtos.EducationRecordDTO;
import app.dtos.EducationRecordRequestDTO;
import app.entities.Citizen;
import app.entities.EducationRecord;
import app.entities.Institution;
import app.mappers.EducationRecordMapper;
import app.validators.EducationRecordValidator;

import java.util.Map;

public class EducationRecordService extends AbstractCrudService<EducationRecord, EducationRecordDTO, EducationRecordRequestDTO> {
    private final GenericDAO<Citizen> citizenDAO;
    private final GenericDAO<Institution> institutionDAO;

    public EducationRecordService(GenericDAO<EducationRecord> dao, GenericDAO<Citizen> citizenDAO, GenericDAO<Institution> institutionDAO) {
        super(dao, EducationRecordMapper.INSTANCE::toDTO, EducationRecordMapper.INSTANCE::toEntity, EducationRecordMapper.INSTANCE::update);
        this.citizenDAO = citizenDAO;
        this.institutionDAO = institutionDAO;
    }

    @Override
    protected Map<String, String> allowedSortFields() {
        return Map.of(
                "id", "id",
                "citizenId", "citizen.id",
                "institutionId", "institution.id",
                "startDate", "startDate",
                "endDate", "endDate"
        );
    }

    @Override
    protected Map<String, String> allowedFilters() {
        return Map.of(
                "citizenId", "citizen.id",
                "institutionId", "institution.id",
                "startDateFrom", "startDate",
                "startDateTo", "startDate",
                "endDateFrom", "endDate",
                "endDateTo", "endDate"
        );
    }

    @Override
    protected void validate(EducationRecordRequestDTO request) {
        EducationRecordValidator.validate(request);
    }

    @Override
    protected void resolveReferences(EducationRecord entity, EducationRecordRequestDTO request) {
        entity.setCitizen(citizenDAO.findByIdOrThrow(request.citizenId()));
        entity.setInstitution(institutionDAO.findByIdOrThrow(request.institutionId()));
    }
}
