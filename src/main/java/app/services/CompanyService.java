package app.services;

import app.daos.GenericDAO;
import app.dtos.CompanyDTO;
import app.dtos.CompanyRequestDTO;
import app.entities.Company;
import app.entities.Location;
import app.mappers.CompanyMapper;
import app.validators.CompanyValidator;

import java.util.Map;

public class CompanyService extends AbstractCrudService<Company, CompanyDTO, CompanyRequestDTO> {
    private final GenericDAO<Location> locationDAO;

    public CompanyService(GenericDAO<Company> dao, GenericDAO<Location> locationDAO) {
        super(dao, CompanyMapper.INSTANCE::toDTO, CompanyMapper.INSTANCE::toEntity, CompanyMapper.INSTANCE::update);
        this.locationDAO = locationDAO;
    }

    @Override
    protected Map<String, String> allowedSortFields() {
        return Map.of(
                "id", "id",
                "locationId", "location.id",
                "name", "name",
                "foundedDate", "foundedDate",
                "dissolvedDate", "dissolvedDate",
                "status", "status"
        );
    }

    @Override
    protected Map<String, String> allowedFilters() {
        return Map.of(
                "locationId", "location.id",
                "name", "name",
                "status", "status",
                "foundedDateFrom", "foundedDate",
                "foundedDateTo", "foundedDate"
        );
    }

    @Override
    protected void validate(CompanyRequestDTO request) {
        CompanyValidator.validate(request);
    }

    @Override
    protected void resolveReferences(Company entity, CompanyRequestDTO request) {
        entity.setLocation(locationDAO.findByIdOrThrow(request.locationId()));
    }
}
