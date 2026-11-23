package app.mappers;

import app.dtos.EducationRecordRequestDTO;
import app.entities.Citizen;
import app.entities.EducationRecord;
import app.entities.Institution;
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
class EducationRecordMapperTest
{
    private EducationRecordMapper mapper;

    @BeforeEach
    void setUp()
    {
        mapper = EducationRecordMapper.INSTANCE;
    }

    @Test
    @DisplayName("maps citizen and institution relations to ids")
    void toDTO()
    {
        // Arrange
        Citizen citizen = new Citizen();
        citizen.setId(1L);
        Institution institution = new Institution();
        institution.setId(2L);
        EducationRecord record = new EducationRecord();
        record.setCitizen(citizen);
        record.setInstitution(institution);

        // Act
        var result = mapper.toDTO(record);

        // Assert
        assertEquals(1L, result.citizenId());
        assertEquals(2L, result.institutionId());
    }

    @Test
    @DisplayName("maps plain fields and leaves relations unset")
    void toEntity()
    {
        // Arrange
        LocalDate date = LocalDate.now();
        EducationRecordRequestDTO request = new EducationRecordRequestDTO(1L, 2L, date, null);

        // Act
        EducationRecord result = mapper.toEntity(request);

        // Assert
        assertEquals(date, result.getStartDate());
        assertNull(result.getCitizen());
        assertNull(result.getInstitution());
    }

    @Test
    @DisplayName("updates plain fields and preserves relations")
    void update()
    {
        // Arrange
        Citizen citizen = new Citizen();
        Institution institution = new Institution();
        EducationRecord record = new EducationRecord();
        record.setCitizen(citizen);
        record.setInstitution(institution);
        LocalDate date = LocalDate.now();
        EducationRecordRequestDTO request = new EducationRecordRequestDTO(3L, 4L, date, date);

        // Act
        mapper.update(request, record);

        // Assert
        assertEquals(date, record.getStartDate());
        assertEquals(date, record.getEndDate());
        assertSame(citizen, record.getCitizen());
        assertSame(institution, record.getInstitution());
    }
}
