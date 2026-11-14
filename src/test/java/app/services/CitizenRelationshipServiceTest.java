package app.services;

import app.daos.GenericDAO;
import app.dtos.CitizenRelationshipDTO;
import app.dtos.CitizenRelationshipRequestDTO;
import app.entities.Citizen;
import app.entities.CitizenRelationship;
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
class CitizenRelationshipServiceTest
{
    @Mock
    private GenericDAO<CitizenRelationship> dao;
    @Mock
    private GenericDAO<Citizen> citizenDAO;

    private CitizenRelationshipService service;

    @BeforeEach
    void setUp()
    {
        service = new CitizenRelationshipService(dao, citizenDAO);
    }

    private CitizenRelationshipRequestDTO request()
    {
        return new CitizenRelationshipRequestDTO(1L, 2L, "marriage", LocalDate.now(), null);
    }

    @Test
    @DisplayName("creates a relationship and returns its DTO")
    void create()
    {
        // Arrange
        Citizen first = new Citizen();
        first.setId(1L);
        Citizen second = new Citizen();
        second.setId(2L);
        when(citizenDAO.findByIdOrThrow(1L)).thenReturn(first);
        when(citizenDAO.findByIdOrThrow(2L)).thenReturn(second);
        when(dao.persist(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CitizenRelationshipDTO result = service.create(request());

        // Assert
        assertEquals(1L, result.citizenId1());
        assertEquals(2L, result.citizenId2());
        verify(dao).persist(any());
    }

    @Test
    @DisplayName("rejects an invalid relationship before persistence")
    void createInvalid()
    {
        // Arrange
        CitizenRelationshipRequestDTO request =
                new CitizenRelationshipRequestDTO(1L, 1L, "marriage", LocalDate.now(), null);

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
        assertThrows(NotFoundException.class, () -> service.create(request()));

        // Assert
        verify(dao, never()).persist(any());
    }

    @Test
    @DisplayName("does not merge when the relationship id is unknown")
    void updateUnknownId()
    {
        // Arrange
        when(dao.findByIdOrThrow(9L)).thenThrow(new NotFoundException("Relationship not found"));

        // Act
        assertThrows(NotFoundException.class, () -> service.update(9L, request()));

        // Assert
        verify(dao, never()).merge(any());
    }
}
