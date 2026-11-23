package app.services;

import app.daos.GenericDAO;
import app.dtos.LocationDTO;
import app.dtos.LocationRequestDTO;
import app.entities.Location;
import app.mappers.LocationMapper;
import app.validators.LocationValidator;

import java.util.Map;

public class LocationService extends AbstractCrudService<Location, LocationDTO, LocationRequestDTO> {
    public LocationService(GenericDAO<Location> dao) {
        super(dao, LocationMapper.INSTANCE::toDTO, LocationMapper.INSTANCE::toEntity, LocationMapper.INSTANCE::update);
    }

    @Override
    protected Map<String, String> allowedSortFields() {
        return Map.of(
                "id", "id",
                "name", "name",
                "streetAddress", "streetAddress"
        );
    }

    @Override
    protected Map<String, String> allowedFilters() {
        return Map.of(
                "name", "name",
                "streetAddress", "streetAddress"
        );
    }

    @Override
    protected void validate(LocationRequestDTO request) {
        LocationValidator.validate(request);
    }
}
