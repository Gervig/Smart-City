package app.mappers;

import app.dtos.InstitutionRequestDTO;
import app.entities.Institution;
import app.entities.Location;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Covers requirement(s): Business rule, not yet in the SRS
 */
class InstitutionMapperTest
{
    private InstitutionMapper mapper;

    @BeforeEach
    void setUp()
    {
        mapper = InstitutionMapper.INSTANCE;
    }

    @Test
    @DisplayName("maps the location relation to its id")
    void toDTO()
    {
        // Arrange
        Location location = new Location();
        location.setId(7L);
        Institution institution = new Institution();
        institution.setLocation(location);
        institution.setName("School");
        institution.setType("college");

        // Act
        var result = mapper.toDTO(institution);

        // Assert
        assertEquals(7L, result.locationId());
        assertEquals("School", result.name());
    }

    @Test
    @DisplayName("maps plain fields and leaves location unset")
    void toEntity()
    {
        // Arrange
        InstitutionRequestDTO request = new InstitutionRequestDTO(7L, "School", "college");

        // Act
        Institution result = mapper.toEntity(request);

        // Assert
        assertEquals("School", result.getName());
        assertEquals("college", result.getType());
        assertNull(result.getLocation());
    }

    @Test
    @DisplayName("updates plain fields and preserves the location relation")
    void update()
    {
        // Arrange
        Location location = new Location();
        Institution institution = new Institution();
        institution.setLocation(location);
        InstitutionRequestDTO request = new InstitutionRequestDTO(8L, "Hospital", "hospital");

        // Act
        mapper.update(request, institution);

        // Assert
        assertEquals("Hospital", institution.getName());
        assertEquals("hospital", institution.getType());
        assertSame(location, institution.getLocation());
    }
}
