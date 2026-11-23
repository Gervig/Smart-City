package app.services;

import java.util.Map;

import app.daos.GenericDAO;
import app.dtos.FamilyDTO;
import app.dtos.FamilyRequestDTO;
import app.entities.Family;
import app.mappers.FamilyMapper;
import app.validators.FamilyValidator;

public class FamilyService extends AbstractCrudService<Family, FamilyDTO, FamilyRequestDTO> {
    public FamilyService(GenericDAO<Family> dao) {
        super(dao, FamilyMapper.INSTANCE::toDTO, FamilyMapper.INSTANCE::toEntity, FamilyMapper.INSTANCE::update);
    }

    @Override
    protected Map<String, String> allowedSortFields() {
        return Map.of(
                "id", "id",
                "familyName", "familyName"
        );
    }

    @Override
    protected Map<String, String> allowedFilters() {
        return Map.of("familyName", "familyName");
    }

    @Override
    protected void validate(FamilyRequestDTO request) {
        FamilyValidator.validate(request);
    }
}
