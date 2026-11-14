package app.mappers;

import app.dtos.HousingRequestDTO;
import app.entities.Housing;
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
class HousingMapperTest
{
    private HousingMapper mapper;

    @BeforeEach
    void setUp()
    {
        mapper = HousingMapper.INSTANCE;
    }

    @Test
    @DisplayName("maps the location relation to its id")
    void toDTO()
    {
        // Arrange
        Location location = new Location();
        location.setId(8L);
        Housing housing = new Housing();
        housing.setId(3L);
        housing.setLocation(location);
        housing.setHousingType("house");
        housing.setCapacity(2);

        // Act
        var result = mapper.toDTO(housing);

        // Assert
        assertEquals(8L, result.locationId());
        assertEquals("house", result.housingType());
        assertEquals(2, result.capacity());
    }

    @Test
    @DisplayName("maps plain fields and leaves location unset")
    void toEntity()
    {
        // Arrange
        HousingRequestDTO request = new HousingRequestDTO(8L, "apartment", 4);

        // Act
        Housing result = mapper.toEntity(request);

        // Assert
        assertEquals("apartment", result.getHousingType());
        assertEquals(4, result.getCapacity());
        assertNull(result.getLocation());
    }

    @Test
    @DisplayName("updates plain fields and preserves the location relation")
    void update()
    {
        // Arrange
        Location location = new Location();
        Housing housing = new Housing();
        housing.setLocation(location);
        HousingRequestDTO request = new HousingRequestDTO(8L, "apartment", 4);

        // Act
        mapper.update(request, housing);

        // Assert
        assertEquals("apartment", housing.getHousingType());
        assertEquals(4, housing.getCapacity());
        assertSame(location, housing.getLocation());
    }
}
