package app.services;

import app.daos.GenericDAO;
import app.dtos.CompanyDTO;
import app.dtos.CompanyRequestDTO;
import app.entities.Company;
import app.entities.Location;
import app.mappers.CompanyMapper;

public class CompanyService extends AbstractCrudService<Company, CompanyDTO, CompanyRequestDTO> {
    private final GenericDAO<Location> locationDAO;

    public CompanyService(GenericDAO<Company> dao, GenericDAO<Location> locationDAO) {
        super(dao, CompanyMapper.INSTANCE::toDTO, CompanyMapper.INSTANCE::toEntity, CompanyMapper.INSTANCE::update);
        this.locationDAO = locationDAO;
    }

    @Override
    protected void resolveReferences(Company entity, CompanyRequestDTO request) {
        entity.setLocation(locationDAO.findByIdOrThrow(request.locationId()));
    }
}
