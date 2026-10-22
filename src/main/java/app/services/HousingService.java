package app.services;

import app.daos.GenericDAO;
import app.dtos.HousingDTO;
import app.dtos.HousingRequestDTO;
import app.entities.Housing;
import app.entities.Location;
import app.mappers.HousingMapper;

public class HousingService extends AbstractCrudService<Housing, HousingDTO, HousingRequestDTO> {
    private final GenericDAO<Location> locationDAO;

    public HousingService(GenericDAO<Housing> dao, GenericDAO<Location> locationDAO) {
        super(dao, HousingMapper.INSTANCE::toDTO, HousingMapper.INSTANCE::toEntity, HousingMapper.INSTANCE::update);
        this.locationDAO = locationDAO;
    }

    @Override
    protected void resolveReferences(Housing entity, HousingRequestDTO request) {
        entity.setLocation(locationDAO.findByIdOrThrow(request.locationId()));
    }
}
