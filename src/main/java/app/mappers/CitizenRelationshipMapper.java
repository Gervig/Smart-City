package app.mappers;

import app.dtos.CitizenRelationshipDTO;
import app.dtos.CitizenRelationshipRequestDTO;
import app.entities.CitizenRelationship;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper(config = MapperSettings.class)
public interface CitizenRelationshipMapper {
    CitizenRelationshipMapper INSTANCE = Mappers.getMapper(CitizenRelationshipMapper.class);

    @Mapping(source = "citizen1.id", target = "citizenId1")
    @Mapping(source = "citizen2.id", target = "citizenId2")
    CitizenRelationshipDTO toDTO(CitizenRelationship entity);

    CitizenRelationship toEntity(CitizenRelationshipRequestDTO request);
    void update(CitizenRelationshipRequestDTO request, @MappingTarget CitizenRelationship entity);
}
