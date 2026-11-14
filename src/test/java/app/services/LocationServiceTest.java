package app.services;

import app.daos.GenericDAO;
import app.dtos.LocationDTO;
import app.dtos.LocationRequestDTO;
import app.entities.Location;
import app.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;

/**
 * Covers requirement(s): Business rule, not yet in the SRS
 */
@ExtendWith(MockitoExtension.class)
class LocationServiceTest
{
    @Mock
    private GenericDAO<Location> dao;

    private LocationService service;

    @BeforeEach
    void setUp()
    {
        service = new LocationService(dao);
    }

    @Test
    @DisplayName("returns all locations from the DAO")
    void getAll()
    {
        // Arrange
        Location location = new Location();
        location.setName("Central");
        when(dao.findAll()).thenReturn(List.of(location));

        // Act
        List<LocationDTO> result = service.getAll();

        // Assert
        assertEquals(1, result.size());
        assertEquals("Central", result.get(0).name());
        verify(dao).findAll();
    }

    @Test
    @DisplayName("returns a location by id")
    void getById()
    {
        // Arrange
        Location location = new Location();
        location.setId(7L);
        location.setName("Central");
        when(dao.findByIdOrThrow(7L)).thenReturn(location);

        // Act
        LocationDTO result = service.getById(7L);

        // Assert
        assertEquals(7L, result.id());
        assertEquals("Central", result.name());
        verify(dao).findByIdOrThrow(7L);
    }

    @Test
    @DisplayName("creates a location through the DAO")
    void create()
    {
        // Arrange
        when(dao.persist(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        LocationDTO result = service.create(new LocationRequestDTO("Central", null));

        // Assert
        assertEquals("Central", result.name());
        verify(dao).persist(any());
    }

    @Test
    @DisplayName("updates a location through the DAO")
    void update()
    {
        // Arrange
        Location location = new Location();
        location.setId(7L);
        location.setName("Old");
        when(dao.findByIdOrThrow(7L)).thenReturn(location);
        when(dao.merge(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        LocationDTO result = service.update(7L, new LocationRequestDTO("New", null));

        // Assert
        assertEquals("New", result.name());
        verify(dao).merge(any());
    }

    @Test
    @DisplayName("propagates unknown id on update without merging")
    void updateUnknownId()
    {
        // Arrange
        when(dao.findByIdOrThrow(99L)).thenThrow(new NotFoundException("Location 99 not found"));

        // Act
        assertThrows(NotFoundException.class,
                () -> service.update(99L, new LocationRequestDTO("New", null)));

        // Assert
        verify(dao, never()).merge(any());
    }

    @Test
    @DisplayName("deletes a location through the DAO")
    void delete()
    {
        // Arrange
        doNothing().when(dao).delete(7L);

        // Act
        service.delete(7L);

        // Assert
        verify(dao).delete(7L);
    }
}
