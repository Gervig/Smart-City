package app.services;

import app.daos.GenericDAO;
import app.dtos.HousingDTO;
import app.dtos.HousingRequestDTO;
import app.entities.Housing;
import app.entities.Location;
import app.exceptions.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers requirement(s): FR-13
 */
@ExtendWith(MockitoExtension.class)
class HousingServiceTest
{
    @Mock
    private GenericDAO<Housing> housingDAO;
    @Mock
    private GenericDAO<Location> locationDAO;

    private HousingService service;

    @BeforeEach
    void setUp()
    {
        service = new HousingService(housingDAO, locationDAO);
    }

    @Test
    @DisplayName("rejects invalid capacity before looking up location")
    void createInvalidCapacity()
    {
        // Arrange
        HousingRequestDTO request = new HousingRequestDTO(1L, "house", 0);

        // Act
        assertThrows(IllegalArgumentException.class, () -> service.create(request));

        // Assert
        verify(locationDAO, never()).findByIdOrThrow(any());
        verify(housingDAO, never()).persist(any());
    }

    @Test
    @DisplayName("creates valid housing and returns its DTO")
    void create()
    {
        // Arrange
        Location location = new Location();
        location.setId(1L);
        when(locationDAO.findByIdOrThrow(1L)).thenReturn(location);
        when(housingDAO.persist(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        HousingDTO result = service.create(new HousingRequestDTO(1L, "apartment", 2));

        // Assert
        assertEquals("apartment", result.housingType());
        assertEquals(1L, result.locationId());
        verify(housingDAO).persist(any());
    }

    @Test
    @DisplayName("propagates an unknown location without persisting")
    void createUnknownLocation()
    {
        // Arrange
        when(locationDAO.findByIdOrThrow(1L)).thenThrow(new NotFoundException("Location not found"));

        // Act
        assertThrows(NotFoundException.class,
                () -> service.create(new HousingRequestDTO(1L, "house", 1)));

        // Assert
        verify(housingDAO, never()).persist(any());
    }

    @Test
    @DisplayName("updates housing and returns its DTO")
    void update()
    {
        // Arrange
        Housing housing = new Housing();
        housing.setId(4L);
        when(housingDAO.findByIdOrThrow(4L)).thenReturn(housing);
        Location location = new Location();
        location.setId(1L);
        when(locationDAO.findByIdOrThrow(1L)).thenReturn(location);
        when(housingDAO.merge(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        HousingDTO result = service.update(4L, new HousingRequestDTO(1L, "house", 3));

        // Assert
        assertEquals(3, result.capacity());
        assertEquals(1L, result.locationId());
        verify(housingDAO).merge(any());
    }

    @Test
    @DisplayName("does not merge when the housing id is unknown")
    void updateUnknownId()
    {
        // Arrange
        when(housingDAO.findByIdOrThrow(9L)).thenThrow(new NotFoundException("Housing not found"));

        // Act
        assertThrows(NotFoundException.class,
                () -> service.update(9L, new HousingRequestDTO(1L, "house", 1)));

        // Assert
        verify(housingDAO, never()).merge(any());
    }
}
