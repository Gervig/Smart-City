package app.mappers;

import app.dtos.InstitutionDTO;
import app.dtos.InstitutionRequestDTO;
import app.entities.Institution;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper(config = MapperSettings.class)
public interface InstitutionMapper {
    InstitutionMapper INSTANCE = Mappers.getMapper(InstitutionMapper.class);

    @Mapping(source = "location.id", target = "locationId")
    InstitutionDTO toDTO(Institution entity);

    Institution toEntity(InstitutionRequestDTO request);
    void update(InstitutionRequestDTO request, @MappingTarget Institution entity);
}
