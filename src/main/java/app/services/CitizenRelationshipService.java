package app.services;

import app.daos.GenericDAO;
import app.dtos.CitizenRelationshipDTO;
import app.dtos.CitizenRelationshipRequestDTO;
import app.entities.Citizen;
import app.entities.CitizenRelationship;
import app.mappers.CitizenRelationshipMapper;
import app.validators.CitizenRelationshipValidator;

import java.util.Map;

public class CitizenRelationshipService extends AbstractCrudService<CitizenRelationship, CitizenRelationshipDTO, CitizenRelationshipRequestDTO> {
    private final GenericDAO<Citizen> citizenDAO;

    public CitizenRelationshipService(GenericDAO<CitizenRelationship> dao, GenericDAO<Citizen> citizenDAO) {
        super(dao, CitizenRelationshipMapper.INSTANCE::toDTO, CitizenRelationshipMapper.INSTANCE::toEntity, CitizenRelationshipMapper.INSTANCE::update);
        this.citizenDAO = citizenDAO;
    }

    @Override
    protected Map<String, String> allowedSortFields() {
        return Map.of(
                "id", "id",
                "citizenId1", "citizen1.id",
                "citizenId2", "citizen2.id",
                "relationshipType", "relationshipType",
                "startDate", "startDate",
                "endDate", "endDate"
        );
    }

    @Override
    protected Map<String, String> allowedFilters() {
        return Map.of(
                "citizenId1", "citizen1.id",
                "citizenId2", "citizen2.id",
                "relationshipType", "relationshipType",
                "startDateFrom", "startDate",
                "startDateTo", "startDate",
                "endDateFrom", "endDate",
                "endDateTo", "endDate"
        );
    }

    @Override
    protected void validate(CitizenRelationshipRequestDTO request) {
        CitizenRelationshipValidator.validate(request);
    }

    @Override
    protected void resolveReferences(CitizenRelationship entity, CitizenRelationshipRequestDTO request) {
        entity.setCitizen1(citizenDAO.findByIdOrThrow(request.citizenId1()));
        entity.setCitizen2(citizenDAO.findByIdOrThrow(request.citizenId2()));
    }
}
