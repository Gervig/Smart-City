package app.mappers;

import app.dtos.CitizenRequestDTO;
import app.entities.Citizen;
import app.entities.Family;
import app.entities.Housing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Covers requirement(s): FR-08, FR-09, FR-13
 */
class CitizenMapperTest
{
    private CitizenMapper mapper;

    @BeforeEach
    void setUp()
    {
        mapper = CitizenMapper.INSTANCE;
    }

    @Test
    @DisplayName("maps family and housing relations to ids")
    void toDTO()
    {
        // Arrange
        Family family = new Family();
        family.setId(4L);
        Housing housing = new Housing();
        housing.setId(5L);
        Citizen citizen = new Citizen();
        citizen.setFamily(family);
        citizen.setHousing(housing);

        // Act
        var result = mapper.toDTO(citizen);

        // Assert
        assertEquals(4L, result.familyId());
        assertEquals(5L, result.housingId());
    }

    @Test
    @DisplayName("maps a null housing relation to a null id")
    void toDTONullHousing()
    {
        // Arrange
        Citizen citizen = new Citizen();
        Family family = new Family();
        family.setId(4L);
        citizen.setFamily(family);

        // Act
        var result = mapper.toDTO(citizen);

        // Assert
        assertEquals(4L, result.familyId());
        assertNull(result.housingId());
    }

    @Test
    @DisplayName("maps plain fields and leaves relations unset")
    void toEntity()
    {
        // Arrange
        LocalDate birthDate = LocalDate.now().minusYears(30);
        CitizenRequestDTO request = new CitizenRequestDTO("Alex", "Smith", birthDate, null,
                "alive", "unknown", 4L, 5L);

        // Act
        Citizen result = mapper.toEntity(request);

        // Assert
        assertEquals("Alex", result.getFirstName());
        assertEquals("Smith", result.getLastName());
        assertEquals(birthDate, result.getBirthDate());
        assertEquals("alive", result.getStatus());
        assertNull(result.getFamily());
        assertNull(result.getHousing());
    }

    @Test
    @DisplayName("updates plain fields and preserves existing relations")
    void update()
    {
        // Arrange
        Family family = new Family();
        Housing housing = new Housing();
        Citizen citizen = new Citizen();
        citizen.setFamily(family);
        citizen.setHousing(housing);
        CitizenRequestDTO request = new CitizenRequestDTO("Alex", "Smith", LocalDate.now().minusYears(20),
                null, "alive", "unknown", 4L, 5L);

        // Act
        mapper.update(request, citizen);

        // Assert
        assertEquals("Alex", citizen.getFirstName());
        assertEquals("Smith", citizen.getLastName());
        assertSame(family, citizen.getFamily());
        assertSame(housing, citizen.getHousing());
    }
}
