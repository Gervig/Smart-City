package app.services;

import app.daos.GenericDAO;
import app.dtos.CompanyDTO;
import app.dtos.CompanyRequestDTO;
import app.entities.Company;
import app.entities.Location;
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
class CompanyServiceTest
{
    @Mock
    private GenericDAO<Company> dao;
    @Mock
    private GenericDAO<Location> locationDAO;

    private CompanyService service;

    @BeforeEach
    void setUp()
    {
        service = new CompanyService(dao, locationDAO);
    }

    private CompanyRequestDTO request(String name, LocalDate foundedDate)
    {
        return new CompanyRequestDTO(1L, name, foundedDate, null, "active");
    }

    @Test
    @DisplayName("creates a company and returns its DTO")
    void create()
    {
        // Arrange
        Location location = new Location();
        location.setId(1L);
        when(locationDAO.findByIdOrThrow(1L)).thenReturn(location);
        when(dao.persist(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CompanyDTO result = service.create(request("Acme", LocalDate.now()));

        // Assert
        assertEquals("Acme", result.name());
        assertEquals(1L, result.locationId());
        verify(dao).persist(any());
    }

    @Test
    @DisplayName("rejects an invalid company before persistence")
    void createInvalid()
    {
        // Arrange
        CompanyRequestDTO request = request(" ", LocalDate.now());

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
        assertThrows(NotFoundException.class, () -> service.create(request("Acme", LocalDate.now())));

        // Assert
        verify(dao, never()).persist(any());
    }

    @Test
    @DisplayName("does not merge when the company id is unknown")
    void updateUnknownId()
    {
        // Arrange
        when(dao.findByIdOrThrow(9L)).thenThrow(new NotFoundException("Company not found"));

        // Act
        assertThrows(NotFoundException.class, () -> service.update(9L, request("Acme", LocalDate.now())));

        // Assert
        verify(dao, never()).merge(any());
    }
}
