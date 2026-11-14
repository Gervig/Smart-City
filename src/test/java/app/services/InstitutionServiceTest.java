package app.services;

import app.daos.GenericDAO;
import app.dtos.InstitutionDTO;
import app.dtos.InstitutionRequestDTO;
import app.entities.Institution;
import app.entities.Location;
import app.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
class InstitutionServiceTest
{
    @Mock
    private GenericDAO<Institution> dao;
    @Mock
    private GenericDAO<Location> locationDAO;

    private InstitutionService service;

    @BeforeEach
    void setUp()
    {
        service = new InstitutionService(dao, locationDAO);
    }

    private InstitutionRequestDTO request(String name, String type)
    {
        return new InstitutionRequestDTO(1L, name, type);
    }

    @Test
    @DisplayName("creates an institution and returns its DTO")
    void create()
    {
        // Arrange
        Location location = new Location();
        location.setId(1L);
        when(locationDAO.findByIdOrThrow(1L)).thenReturn(location);
        when(dao.persist(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        InstitutionDTO result = service.create(request("Central Hospital", "hospital"));

        // Assert
        assertEquals("Central Hospital", result.name());
        assertEquals(1L, result.locationId());
        verify(dao).persist(any());
    }

    @Test
    @DisplayName("rejects an invalid institution before persistence")
    void createInvalid()
    {
        // Arrange
        InstitutionRequestDTO request = request("School", "university");

        // Act
        assertThrows(IllegalArgumentException.class, () -> service.create(request));

        // Assert
        verify(dao, never()).persist(any());
    }

    @Test
    @DisplayName("propagates an unknown location without persisting")
    void createUnknownLocation()
    {
        // Arrange
        when(locationDAO.findByIdOrThrow(1L)).thenThrow(new NotFoundException("Location not found"));

        // Act
        assertThrows(NotFoundException.class, () -> service.create(request("Hospital", "hospital")));

        // Assert
        verify(dao, never()).persist(any());
    }

    @Test
    @DisplayName("does not merge when the institution id is unknown")
    void updateUnknownId()
    {
        // Arrange
        when(dao.findByIdOrThrow(9L)).thenThrow(new NotFoundException("Institution not found"));

        // Act
        assertThrows(NotFoundException.class, () -> service.update(9L, request("Hospital", "hospital")));

        // Assert
        verify(dao, never()).merge(any());
    }
}
