package app.mappers;

import app.dtos.HousingDTO;
import app.dtos.HousingRequestDTO;
import app.entities.Housing;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper(config = MapperSettings.class)
public interface HousingMapper {
    HousingMapper INSTANCE = Mappers.getMapper(HousingMapper.class);

    @Mapping(source = "location.id", target = "locationId")
    HousingDTO toDTO(Housing entity);

    Housing toEntity(HousingRequestDTO request);
    void update(HousingRequestDTO request, @MappingTarget Housing entity);
}
