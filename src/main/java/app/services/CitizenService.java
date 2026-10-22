package app.services;

import app.daos.GenericDAO;
import app.dtos.CitizenDTO;
import app.dtos.CitizenRequestDTO;
import app.entities.Citizen;
import app.entities.Family;
import app.entities.Housing;
import app.mappers.CitizenMapper;

public class CitizenService extends AbstractCrudService<Citizen, CitizenDTO, CitizenRequestDTO> {
    private final GenericDAO<Family> familyDAO;
    private final GenericDAO<Housing> housingDAO;

    public CitizenService(GenericDAO<Citizen> dao, GenericDAO<Family> familyDAO, GenericDAO<Housing> housingDAO) {
        super(dao, CitizenMapper.INSTANCE::toDTO, CitizenMapper.INSTANCE::toEntity, CitizenMapper.INSTANCE::update);
        this.familyDAO = familyDAO;
        this.housingDAO = housingDAO;
    }

    @Override
    protected void resolveReferences(Citizen entity, CitizenRequestDTO request) {
        entity.setFamily(familyDAO.findByIdOrThrow(request.familyId()));
        entity.setHousing(optional(housingDAO, request.housingId()));
    }
}
