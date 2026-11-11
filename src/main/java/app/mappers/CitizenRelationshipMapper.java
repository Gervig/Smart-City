package app.mappers;

import app.dtos.CitizenRelationshipDTO;
import app.entities.Citizen;
import app.entities.CitizenRelationship;

import java.util.Collection;
import java.util.List;

public final class CitizenRelationshipMapper {

    private CitizenRelationshipMapper() {
    }

    public static CitizenRelationshipDTO toDTO(CitizenRelationship relationship) {
        if (relationship == null) return null;
        return CitizenRelationshipDTO.builder()
                .id(relationship.getId())
                .citizen1Id(relationship.getCitizen1() != null ? relationship.getCitizen1().getId() : null)
                .citizen2Id(relationship.getCitizen2() != null ? relationship.getCitizen2().getId() : null)
                .relationshipType(relationship.getRelationshipType())
                .startDate(relationship.getStartDate())
                .endDate(relationship.getEndDate())
                .build();
    }

    public static List<CitizenRelationshipDTO> toDTOList(Collection<CitizenRelationship> relationships) {
        return relationships.stream().map(CitizenRelationshipMapper::toDTO).toList();
    }

    /** Builds a new (unsaved) CitizenRelationship. The two citizens must be looked up by the caller. */
    public static CitizenRelationship toEntity(CitizenRelationshipDTO dto, Citizen citizen1, Citizen citizen2) {
        if (dto == null) return null;
        CitizenRelationship relationship = new CitizenRelationship();
        updateEntity(relationship, dto);
        relationship.setCitizen1(citizen1);
        relationship.setCitizen2(citizen2);
        return relationship;
    }

    /** Copies the plain fields from the DTO onto an existing CitizenRelationship (id and relations untouched). */
    public static void updateEntity(CitizenRelationship relationship, CitizenRelationshipDTO dto) {
        relationship.setRelationshipType(dto.getRelationshipType());
        relationship.setStartDate(dto.getStartDate());
        relationship.setEndDate(dto.getEndDate());
    }
}
