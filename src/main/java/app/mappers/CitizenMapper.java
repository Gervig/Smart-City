package app.mappers;

import app.dtos.CitizenDTO;
import app.dtos.CitizenRequestDTO;
import app.entities.Citizen;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper(config = MapperSettings.class)
public interface CitizenMapper {
    CitizenMapper INSTANCE = Mappers.getMapper(CitizenMapper.class);

    @Mapping(source = "family.id", target = "familyId")
    @Mapping(source = "housing.id", target = "housingId")
    CitizenDTO toDTO(Citizen entity);

    Citizen toEntity(CitizenRequestDTO request);
    void update(CitizenRequestDTO request, @MappingTarget Citizen entity);
}
