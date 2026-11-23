package app.services;

import app.daos.GenericDAO;
import app.dtos.HousingDTO;
import app.dtos.HousingRequestDTO;
import app.entities.Housing;
import app.entities.Location;
import app.mappers.HousingMapper;
import app.validators.HousingValidator;

import java.util.Map;

public class HousingService extends AbstractCrudService<Housing, HousingDTO, HousingRequestDTO> {
    private final GenericDAO<Location> locationDAO;

    public HousingService(GenericDAO<Housing> dao, GenericDAO<Location> locationDAO) {
        super(dao, HousingMapper.INSTANCE::toDTO, HousingMapper.INSTANCE::toEntity, HousingMapper.INSTANCE::update);
        this.locationDAO = locationDAO;
    }

    @Override
    protected Map<String, String> allowedSortFields() {
        return Map.of(
                "id", "id",
                "locationId", "location.id",
                "housingType", "housingType",
                "capacity", "capacity"
        );
    }

    @Override
    protected Map<String, String> allowedFilters() {
        return Map.of(
                "locationId", "location.id",
                "housingType", "housingType",
                "capacityFrom", "capacity",
                "capacityTo", "capacity"
        );
    }

    @Override
    protected void validate(HousingRequestDTO request) {
        HousingValidator.validate(request);
    }

    @Override
    protected void resolveReferences(Housing entity, HousingRequestDTO request) {
        entity.setLocation(locationDAO.findByIdOrThrow(request.locationId()));
    }
}
