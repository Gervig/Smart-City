package app.services;

import app.daos.GenericDAO;
import app.dtos.HousingRequestDTO;
import app.entities.Housing;
import app.entities.Location;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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
}
