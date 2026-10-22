package app.mappers;

import app.dtos.JobDTO;
import app.dtos.JobRequestDTO;
import app.entities.Job;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.factory.Mappers;

@Mapper(config = MapperSettings.class)
public interface JobMapper {
    JobMapper INSTANCE = Mappers.getMapper(JobMapper.class);

    @Mapping(source = "citizen.id", target = "citizenId")
    @Mapping(source = "company.id", target = "companyId")
    @Mapping(source = "institution.id", target = "institutionId")
    JobDTO toDTO(Job entity);

    Job toEntity(JobRequestDTO request);
    void update(JobRequestDTO request, @MappingTarget Job entity);
}
