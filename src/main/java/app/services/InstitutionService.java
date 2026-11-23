package app.services;

import app.daos.GenericDAO;
import app.dtos.InstitutionDTO;
import app.dtos.InstitutionRequestDTO;
import app.entities.Institution;
import app.entities.Location;
import app.mappers.InstitutionMapper;
import app.validators.InstitutionValidator;

import java.util.Map;

public class InstitutionService extends AbstractCrudService<Institution, InstitutionDTO, InstitutionRequestDTO> {
    private final GenericDAO<Location> locationDAO;

    public InstitutionService(GenericDAO<Institution> dao, GenericDAO<Location> locationDAO) {
        super(dao, InstitutionMapper.INSTANCE::toDTO, InstitutionMapper.INSTANCE::toEntity, InstitutionMapper.INSTANCE::update);
        this.locationDAO = locationDAO;
    }

    @Override
    protected Map<String, String> allowedSortFields() {
        return Map.of(
                "id", "id",
                "locationId", "location.id",
                "name", "name",
                "type", "type"
        );
    }

    @Override
    protected Map<String, String> allowedFilters() {
        return Map.of(
                "locationId", "location.id",
                "name", "name",
                "type", "type"
        );
    }

    @Override
    protected void validate(InstitutionRequestDTO request) {
        InstitutionValidator.validate(request);
    }

    @Override
    protected void resolveReferences(Institution entity, InstitutionRequestDTO request) {
        entity.setLocation(locationDAO.findByIdOrThrow(request.locationId()));
    }
}
