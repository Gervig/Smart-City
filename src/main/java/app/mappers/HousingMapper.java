package app.mappers;

import app.dtos.HousingDTO;
import app.entities.Housing;
import app.entities.Location;

import java.util.Collection;
import java.util.List;

public final class HousingMapper {

    private HousingMapper() {
    }

    public static HousingDTO toDTO(Housing housing) {
        if (housing == null) return null;
        return HousingDTO.builder()
                .id(housing.getId())
                .locationId(housing.getLocation() != null ? housing.getLocation().getId() : null)
                .housingType(housing.getHousingType())
                .capacity(housing.getCapacity())
                .build();
    }

    public static List<HousingDTO> toDTOList(Collection<Housing> housings) {
        return housings.stream().map(HousingMapper::toDTO).toList();
    }

    /** Builds a new (unsaved) Housing. The Location must be looked up by the caller. */
    public static Housing toEntity(HousingDTO dto, Location location) {
        if (dto == null) return null;
        Housing housing = new Housing();
        updateEntity(housing, dto);
        housing.setLocation(location);
        return housing;
    }

    /** Copies the plain fields from the DTO onto an existing Housing (id and relations untouched). */
    public static void updateEntity(Housing housing, HousingDTO dto) {
        housing.setHousingType(dto.getHousingType());
        housing.setCapacity(dto.getCapacity());
    }
}
