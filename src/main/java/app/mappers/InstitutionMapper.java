package app.mappers;

import app.dtos.InstitutionDTO;
import app.entities.Institution;
import app.entities.Location;

import java.util.Collection;
import java.util.List;

public final class InstitutionMapper {

    private InstitutionMapper() {
    }

    public static InstitutionDTO toDTO(Institution institution) {
        if (institution == null) return null;
        return InstitutionDTO.builder()
                .id(institution.getId())
                .locationId(institution.getLocation() != null ? institution.getLocation().getId() : null)
                .name(institution.getName())
                .type(institution.getType())
                .build();
    }

    public static List<InstitutionDTO> toDTOList(Collection<Institution> institutions) {
        return institutions.stream().map(InstitutionMapper::toDTO).toList();
    }

    /** Builds a new (unsaved) Institution. The Location must be looked up by the caller. */
    public static Institution toEntity(InstitutionDTO dto, Location location) {
        if (dto == null) return null;
        Institution institution = new Institution();
        updateEntity(institution, dto);
        institution.setLocation(location);
        return institution;
    }

    /** Copies the plain fields from the DTO onto an existing Institution (id and relations untouched). */
    public static void updateEntity(Institution institution, InstitutionDTO dto) {
        institution.setName(dto.getName());
        institution.setType(dto.getType());
    }
}
