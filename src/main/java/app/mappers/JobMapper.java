package app.mappers;

import app.dtos.JobDTO;
import app.entities.Citizen;
import app.entities.Company;
import app.entities.Institution;
import app.entities.Job;

import java.util.Collection;
import java.util.List;

public final class JobMapper {

    private JobMapper() {
    }

    public static JobDTO toDTO(Job job) {
        if (job == null) return null;
        return JobDTO.builder()
                .id(job.getId())
                .citizenId(job.getCitizen() != null ? job.getCitizen().getId() : null)
                .companyId(job.getCompany() != null ? job.getCompany().getId() : null)
                .institutionId(job.getInstitution() != null ? job.getInstitution().getId() : null)
                .title(job.getTitle())
                .salary(job.getSalary())
                .startDate(job.getStartDate())
                .endDate(job.getEndDate())
                .status(job.getStatus())
                .build();
    }

    public static List<JobDTO> toDTOList(Collection<Job> jobs) {
        return jobs.stream().map(JobMapper::toDTO).toList();
    }

    /**
     * Builds a new (unsaved) Job. A job belongs to either a Company or an Institution,
     * so pass null for the one that does not apply. Related entities must be looked up by the caller.
     */
    public static Job toEntity(JobDTO dto, Citizen citizen, Company company, Institution institution) {
        if (dto == null) return null;
        Job job = new Job();
        updateEntity(job, dto);
        job.setCitizen(citizen);
        job.setCompany(company);
        job.setInstitution(institution);
        return job;
    }

    /** Copies the plain fields from the DTO onto an existing Job (id and relations untouched). */
    public static void updateEntity(Job job, JobDTO dto) {
        job.setTitle(dto.getTitle());
        job.setSalary(dto.getSalary());
        job.setStartDate(dto.getStartDate());
        job.setEndDate(dto.getEndDate());
        if (dto.getStatus() != null) job.setStatus(dto.getStatus());
    }
}
