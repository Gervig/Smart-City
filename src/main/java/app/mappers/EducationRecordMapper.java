package app.mappers;

import app.dtos.EducationRecordDTO;
import app.entities.Citizen;
import app.entities.EducationRecord;
import app.entities.Institution;

import java.util.Collection;
import java.util.List;

public final class EducationRecordMapper {

    private EducationRecordMapper() {
    }

    public static EducationRecordDTO toDTO(EducationRecord record) {
        if (record == null) return null;
        return EducationRecordDTO.builder()
                .id(record.getId())
                .citizenId(record.getCitizen() != null ? record.getCitizen().getId() : null)
                .institutionId(record.getInstitution() != null ? record.getInstitution().getId() : null)
                .startDate(record.getStartDate())
                .endDate(record.getEndDate())
                .build();
    }

    public static List<EducationRecordDTO> toDTOList(Collection<EducationRecord> records) {
        return records.stream().map(EducationRecordMapper::toDTO).toList();
    }

    /** Builds a new (unsaved) EducationRecord. The Citizen and Institution must be looked up by the caller. */
    public static EducationRecord toEntity(EducationRecordDTO dto, Citizen citizen, Institution institution) {
        if (dto == null) return null;
        EducationRecord record = new EducationRecord();
        updateEntity(record, dto);
        record.setCitizen(citizen);
        record.setInstitution(institution);
        return record;
    }

    /** Copies the plain fields from the DTO onto an existing EducationRecord (id and relations untouched). */
    public static void updateEntity(EducationRecord record, EducationRecordDTO dto) {
        record.setStartDate(dto.getStartDate());
        record.setEndDate(dto.getEndDate());
    }
}
