package app.mappers;

import app.dtos.FamilyDTO;
import app.dtos.FamilyRequestDTO;
import app.entities.Family;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper(config = MapperSettings.class)
public interface FamilyMapper {
    FamilyMapper INSTANCE = Mappers.getMapper(FamilyMapper.class);

    FamilyDTO toDTO(Family entity);
    Family toEntity(FamilyRequestDTO request);
    void update(FamilyRequestDTO request, @MappingTarget Family entity);
}
