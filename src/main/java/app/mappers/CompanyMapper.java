package app.mappers;

import app.dtos.CompanyDTO;
import app.dtos.CompanyRequestDTO;
import app.entities.Company;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper(config = MapperSettings.class)
public interface CompanyMapper {
    CompanyMapper INSTANCE = Mappers.getMapper(CompanyMapper.class);

    @Mapping(source = "location.id", target = "locationId")
    CompanyDTO toDTO(Company entity);

    Company toEntity(CompanyRequestDTO request);
    void update(CompanyRequestDTO request, @MappingTarget Company entity);
}
