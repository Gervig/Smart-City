package app.services;

import app.daos.GenericDAO;
import app.dtos.CitizenRelationshipDTO;
import app.dtos.CitizenRelationshipRequestDTO;
import app.entities.Citizen;
import app.entities.CitizenRelationship;
import app.mappers.CitizenRelationshipMapper;

public class CitizenRelationshipService extends AbstractCrudService<CitizenRelationship, CitizenRelationshipDTO, CitizenRelationshipRequestDTO> {
    private final GenericDAO<Citizen> citizenDAO;

    public CitizenRelationshipService(GenericDAO<CitizenRelationship> dao, GenericDAO<Citizen> citizenDAO) {
        super(dao, CitizenRelationshipMapper.INSTANCE::toDTO, CitizenRelationshipMapper.INSTANCE::toEntity, CitizenRelationshipMapper.INSTANCE::update);
        this.citizenDAO = citizenDAO;
    }

    @Override
    protected void resolveReferences(CitizenRelationship entity, CitizenRelationshipRequestDTO request) {
        entity.setCitizen1(citizenDAO.findByIdOrThrow(request.citizenId1()));
        entity.setCitizen2(citizenDAO.findByIdOrThrow(request.citizenId2()));
    }
}
