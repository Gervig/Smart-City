package app.services;

import app.daos.GenericDAO;
import app.dtos.InstitutionDTO;
import app.dtos.InstitutionRequestDTO;
import app.entities.Institution;
import app.entities.Location;
import app.mappers.InstitutionMapper;

public class InstitutionService extends AbstractCrudService<Institution, InstitutionDTO, InstitutionRequestDTO> {
    private final GenericDAO<Location> locationDAO;

    public InstitutionService(GenericDAO<Institution> dao, GenericDAO<Location> locationDAO) {
        super(dao, InstitutionMapper.INSTANCE::toDTO, InstitutionMapper.INSTANCE::toEntity, InstitutionMapper.INSTANCE::update);
        this.locationDAO = locationDAO;
    }

    @Override
    protected void resolveReferences(Institution entity, InstitutionRequestDTO request) {
        entity.setLocation(locationDAO.findByIdOrThrow(request.locationId()));
    }
}
