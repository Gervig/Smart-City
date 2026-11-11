package app.mappers;

import app.dtos.CompanyDTO;
import app.entities.Company;
import app.entities.Location;

import java.util.Collection;
import java.util.List;

public final class CompanyMapper {

    private CompanyMapper() {
    }

    public static CompanyDTO toDTO(Company company) {
        if (company == null) return null;
        return CompanyDTO.builder()
                .id(company.getId())
                .locationId(company.getLocation() != null ? company.getLocation().getId() : null)
                .name(company.getName())
                .foundedDate(company.getFoundedDate())
                .dissolvedDate(company.getDissolvedDate())
                .status(company.getStatus())
                .build();
    }

    public static List<CompanyDTO> toDTOList(Collection<Company> companies) {
        return companies.stream().map(CompanyMapper::toDTO).toList();
    }

    /** Builds a new (unsaved) Company. The Location must be looked up by the caller. */
    public static Company toEntity(CompanyDTO dto, Location location) {
        if (dto == null) return null;
        Company company = new Company();
        updateEntity(company, dto);
        company.setLocation(location);
        return company;
    }

    /** Copies the plain fields from the DTO onto an existing Company (id and relations untouched). */
    public static void updateEntity(Company company, CompanyDTO dto) {
        company.setName(dto.getName());
        company.setFoundedDate(dto.getFoundedDate());
        company.setDissolvedDate(dto.getDissolvedDate());
        if (dto.getStatus() != null) company.setStatus(dto.getStatus());
    }
}
