package app.mappers;

import app.dtos.CitizenRelationshipRequestDTO;
import app.entities.Citizen;
import app.entities.CitizenRelationship;
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
class CitizenRelationshipMapperTest
{
    private CitizenRelationshipMapper mapper;

    @BeforeEach
    void setUp()
    {
        mapper = CitizenRelationshipMapper.INSTANCE;
    }

    @Test
    @DisplayName("maps both citizen relations to ids")
    void toDTO()
    {
        // Arrange
        Citizen first = new Citizen();
        first.setId(1L);
        Citizen second = new Citizen();
        second.setId(2L);
        CitizenRelationship relationship = new CitizenRelationship();
        relationship.setCitizen1(first);
        relationship.setCitizen2(second);

        // Act
        var result = mapper.toDTO(relationship);

        // Assert
        assertEquals(1L, result.citizenId1());
        assertEquals(2L, result.citizenId2());
    }

    @Test
    @DisplayName("maps plain fields and leaves citizen relations unset")
    void toEntity()
    {
        // Arrange
        LocalDate date = LocalDate.now();
        CitizenRelationshipRequestDTO request =
                new CitizenRelationshipRequestDTO(1L, 2L, "marriage", date, null);

        // Act
        CitizenRelationship result = mapper.toEntity(request);

        // Assert
        assertEquals("marriage", result.getRelationshipType());
        assertEquals(date, result.getStartDate());
        assertNull(result.getCitizen1());
        assertNull(result.getCitizen2());
    }

    @Test
    @DisplayName("updates plain fields and preserves citizen relations")
    void update()
    {
        // Arrange
        Citizen first = new Citizen();
        Citizen second = new Citizen();
        CitizenRelationship relationship = new CitizenRelationship();
        relationship.setCitizen1(first);
        relationship.setCitizen2(second);
        CitizenRelationshipRequestDTO request =
                new CitizenRelationshipRequestDTO(3L, 4L, "parent_child", LocalDate.now(), null);

        // Act
        mapper.update(request, relationship);

        // Assert
        assertEquals("parent_child", relationship.getRelationshipType());
        assertSame(first, relationship.getCitizen1());
        assertSame(second, relationship.getCitizen2());
    }
}
