package app.services;

import app.daos.GenericDAO;
import app.dtos.LocationDTO;
import app.dtos.LocationRequestDTO;
import app.entities.Location;
import app.mappers.LocationMapper;

public class LocationService extends AbstractCrudService<Location, LocationDTO, LocationRequestDTO> {
    public LocationService(GenericDAO<Location> dao) {
        super(dao, LocationMapper.INSTANCE::toDTO, LocationMapper.INSTANCE::toEntity, LocationMapper.INSTANCE::update);
    }
}
