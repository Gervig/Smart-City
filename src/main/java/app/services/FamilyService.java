package app.services;

import java.util.Set;

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
    protected Set<String> allowedSortFields() {
        return Set.of(
                "id",
                "familyName"
        );
    }

    @Override
    protected void validate(FamilyRequestDTO request) {
        FamilyValidator.validate(request);
    }
}
