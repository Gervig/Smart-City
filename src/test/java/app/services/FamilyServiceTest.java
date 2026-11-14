package app.services;

import app.daos.GenericDAO;
import app.dtos.FamilyDTO;
import app.dtos.FamilyRequestDTO;
import app.entities.Family;
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
class FamilyServiceTest
{
    @Mock
    private GenericDAO<Family> dao;

    private FamilyService service;

    @BeforeEach
    void setUp()
    {
        service = new FamilyService(dao);
    }

    @Test
    @DisplayName("creates a family and returns its DTO")
    void create()
    {
        // Arrange
        when(dao.persist(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        FamilyDTO result = service.create(new FamilyRequestDTO("Smith"));

        // Assert
        assertEquals("Smith", result.familyName());
        verify(dao).persist(any());
    }

    @Test
    @DisplayName("rejects an invalid family before persistence")
    void createInvalid()
    {
        // Arrange
        FamilyRequestDTO request = new FamilyRequestDTO(" ");

        // Act
        assertThrows(IllegalArgumentException.class, () -> service.create(request));

        // Assert
        verify(dao, never()).persist(any());
    }

    @Test
    @DisplayName("does not merge when the family id is unknown")
    void updateUnknownId()
    {
        // Arrange
        when(dao.findByIdOrThrow(9L)).thenThrow(new NotFoundException("Family not found"));

        // Act
        assertThrows(NotFoundException.class, () -> service.update(9L, new FamilyRequestDTO("Smith")));

        // Assert
        verify(dao, never()).merge(any());
    }
}
