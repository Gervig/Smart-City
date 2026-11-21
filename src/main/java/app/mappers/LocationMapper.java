package app.mappers;

import app.dtos.LocationDTO;
import app.dtos.LocationRequestDTO;
import app.entities.Location;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper(config = MapperSettings.class)
public interface LocationMapper {
    LocationMapper INSTANCE = Mappers.getMapper(LocationMapper.class);

    LocationDTO toDTO(Location entity);
    Location toEntity(LocationRequestDTO request);
    void update(LocationRequestDTO request, @MappingTarget Location entity);
}
