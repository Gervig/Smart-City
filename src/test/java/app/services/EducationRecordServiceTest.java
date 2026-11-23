package app.services;

import app.daos.GenericDAO;
import app.dtos.EducationRecordDTO;
import app.dtos.EducationRecordRequestDTO;
import app.entities.Citizen;
import app.entities.EducationRecord;
import app.entities.Institution;
import app.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers requirement(s): Business rule, not yet in the SRS
 */
@ExtendWith(MockitoExtension.class)
class EducationRecordServiceTest
{
    @Mock
    private GenericDAO<EducationRecord> dao;
    @Mock
    private GenericDAO<Citizen> citizenDAO;
    @Mock
    private GenericDAO<Institution> institutionDAO;

    private EducationRecordService service;

    @BeforeEach
    void setUp()
    {
        service = new EducationRecordService(dao, citizenDAO, institutionDAO);
    }

    private EducationRecordRequestDTO request(LocalDate startDate, LocalDate endDate)
    {
        return new EducationRecordRequestDTO(1L, 2L, startDate, endDate);
    }

    @Test
    @DisplayName("creates an education record and returns its DTO")
    void create()
    {
        // Arrange
        Citizen citizen = new Citizen();
        citizen.setId(1L);
        Institution institution = new Institution();
        institution.setId(2L);
        when(citizenDAO.findByIdOrThrow(1L)).thenReturn(citizen);
        when(institutionDAO.findByIdOrThrow(2L)).thenReturn(institution);
        when(dao.persist(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        EducationRecordDTO result = service.create(request(LocalDate.now(), null));

        // Assert
        assertEquals(1L, result.citizenId());
        assertEquals(2L, result.institutionId());
        verify(dao).persist(any());
    }

    @Test
    @DisplayName("rejects an invalid education record before persistence")
    void createInvalid()
    {
        // Arrange
        EducationRecordRequestDTO request = request(null, null);

        // Act
        assertThrows(IllegalArgumentException.class, () -> service.create(request));

        // Assert
        verify(dao, never()).persist(any());
    }

    @Test
    @DisplayName("propagates an unknown citizen without persisting")
    void createUnknownCitizen()
    {
        // Arrange
        when(citizenDAO.findByIdOrThrow(1L)).thenThrow(new NotFoundException("Citizen not found"));

        // Act
        assertThrows(NotFoundException.class, () -> service.create(request(LocalDate.now(), null)));

        // Assert
        verify(dao, never()).persist(any());
    }

    @Test
    @DisplayName("propagates an unknown institution without persisting")
    void createUnknownInstitution()
    {
        // Arrange
        when(citizenDAO.findByIdOrThrow(1L)).thenReturn(new Citizen());
        when(institutionDAO.findByIdOrThrow(2L)).thenThrow(new NotFoundException("Institution not found"));

        // Act
        assertThrows(NotFoundException.class, () -> service.create(request(LocalDate.now(), null)));

        // Assert
        verify(dao, never()).persist(any());
    }

    @Test
    @DisplayName("does not merge when the education record id is unknown")
    void updateUnknownId()
    {
        // Arrange
        when(dao.findByIdOrThrow(9L)).thenThrow(new NotFoundException("Education record not found"));

        // Act
        assertThrows(NotFoundException.class, () -> service.update(9L, request(LocalDate.now(), null)));

        // Assert
        verify(dao, never()).merge(any());
    }
}
