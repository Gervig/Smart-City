package app.mappers;

import app.dtos.CompanyRequestDTO;
import app.entities.Company;
import app.entities.Location;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Covers requirement(s): Business rule, not yet in the SRS
 */
class CompanyMapperTest
{
    private CompanyMapper mapper;

    @BeforeEach
    void setUp()
    {
        mapper = CompanyMapper.INSTANCE;
    }

    @Test
    @DisplayName("maps the location relation to its id")
    void toDTO()
    {
        // Arrange
        Location location = new Location();
        location.setId(7L);
        Company company = new Company();
        company.setLocation(location);
        company.setName("Acme");

        // Act
        var result = mapper.toDTO(company);

        // Assert
        assertEquals(7L, result.locationId());
        assertEquals("Acme", result.name());
    }

    @Test
    @DisplayName("maps plain fields and leaves location unset")
    void toEntity()
    {
        // Arrange
        LocalDate date = LocalDate.now().minusYears(5);
        CompanyRequestDTO request = new CompanyRequestDTO(7L, "Acme", date, null, "active");

        // Act
        Company result = mapper.toEntity(request);

        // Assert
        assertEquals("Acme", result.getName());
        assertEquals(date, result.getFoundedDate());
        assertNull(result.getLocation());
    }

    @Test
    @DisplayName("updates plain fields and preserves the location relation")
    void update()
    {
        // Arrange
        Location location = new Location();
        Company company = new Company();
        company.setLocation(location);
        LocalDate date = LocalDate.now().minusYears(5);
        CompanyRequestDTO request = new CompanyRequestDTO(8L, "New name", date, null, "active");

        // Act
        mapper.update(request, company);

        // Assert
        assertEquals("New name", company.getName());
        assertEquals(date, company.getFoundedDate());
        assertSame(location, company.getLocation());
    }
}
