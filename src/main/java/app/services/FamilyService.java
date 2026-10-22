package app.services;

import app.daos.GenericDAO;
import app.dtos.FamilyDTO;
import app.dtos.FamilyRequestDTO;
import app.entities.Family;
import app.mappers.FamilyMapper;

public class FamilyService extends AbstractCrudService<Family, FamilyDTO, FamilyRequestDTO> {
    public FamilyService(GenericDAO<Family> dao) {
        super(dao, FamilyMapper.INSTANCE::toDTO, FamilyMapper.INSTANCE::toEntity, FamilyMapper.INSTANCE::update);
    }
}
