package app.mappers;

import app.dtos.EducationRecordDTO;
import app.dtos.EducationRecordRequestDTO;
import app.entities.EducationRecord;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper(config = MapperSettings.class)
public interface EducationRecordMapper {
    EducationRecordMapper INSTANCE = Mappers.getMapper(EducationRecordMapper.class);

    @Mapping(source = "citizen.id", target = "citizenId")
    @Mapping(source = "institution.id", target = "institutionId")
    EducationRecordDTO toDTO(EducationRecord entity);

    EducationRecord toEntity(EducationRecordRequestDTO request);
    void update(EducationRecordRequestDTO request, @MappingTarget EducationRecord entity);
}
