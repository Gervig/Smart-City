package app.services;

import app.daos.GenericDAO;
import app.dtos.CitizenDTO;
import app.dtos.CitizenRequestDTO;
import app.entities.Citizen;
import app.entities.Family;
import app.entities.Housing;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 - Covers requirement(s): FR-08, FR-09, FR-13
 */
@ExtendWith(MockitoExtension.class)
class CitizenServiceTest
{
    @Mock
    private GenericDAO<Citizen> citizenDAO;
    @Mock
    private GenericDAO<Family> familyDAO;
    @Mock
    private GenericDAO<Housing> housingDAO;

    private CitizenService service;
    private Family family;
    private Housing housing;

    @BeforeEach
    void setUp()
    {
        service = new CitizenService(citizenDAO, familyDAO, housingDAO);
        family = new Family();
        family.setId(1L);
        housing = new Housing();
        housing.setId(2L);
        housing.setCapacity(1);
    }

    private CitizenRequestDTO request(LocalDate birthDate, LocalDate deathDate, String status, Long housingId)
    {
        return new CitizenRequestDTO("Alex", "Smith", birthDate, deathDate, status, "unknown", 1L, housingId);
    }

    @Test
    @DisplayName("creates a valid citizen once")
    void create()
    {
        // Arrange
        when(familyDAO.findByIdOrThrow(1L)).thenReturn(family);
        when(citizenDAO.persist(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDate birthDate = LocalDate.now();

        // Act
        CitizenDTO result = service.create(request(birthDate, null, "alive", null));

        // Assert
        ArgumentCaptor<Citizen> saved = ArgumentCaptor.forClass(Citizen.class);
        verify(citizenDAO).persist(saved.capture());
        assertEquals("Alex", saved.getValue().getFirstName());
        assertEquals("Smith", saved.getValue().getLastName());
        assertEquals(birthDate, saved.getValue().getBirthDate());
        assertEquals("alive", saved.getValue().getStatus());
        assertSame(family, saved.getValue().getFamily());
        assertNull(saved.getValue().getHousing());
        assertEquals("Alex", result.firstName());
        assertEquals(1L, result.familyId());
        assertNull(result.housingId());
    }

    @Test
    @DisplayName("[EP-02] rejects an invalid citizen before persistence")
    void createInvalid()
    {
        // Arrange
        CitizenRequestDTO request = request(LocalDate.now().plusDays(1), null, "alive", null);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.create(request));

        // Assert
        assertEquals("birthDate must not be in the future", exception.getMessage());
        verify(citizenDAO, never()).persist(any());
    }

    @Test
    @DisplayName("[EP-21, BVA-16] rejects a citizen when housing is full")
    void createHousingFull()
    {
        // Arrange
        when(familyDAO.findByIdOrThrow(1L)).thenReturn(family);
        when(housingDAO.findByIdOrThrow(2L)).thenReturn(housing);
        when(citizenDAO.countWhere("housing.id", 2L)).thenReturn(1L);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.create(request(LocalDate.now(), null, "alive", 2L)));

        // Assert
        assertTrue(exception.getMessage().contains("full"));
        verify(citizenDAO, never()).persist(any());
    }

    @Test
    @DisplayName("[EP-20, BVA-15] accepts a citizen when housing has capacity")
    void createHousingAvailable()
    {
        // Arrange
        when(familyDAO.findByIdOrThrow(1L)).thenReturn(family);
        when(housingDAO.findByIdOrThrow(2L)).thenReturn(housing);
        when(citizenDAO.countWhere("housing.id", 2L)).thenReturn(0L);
        when(citizenDAO.persist(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CitizenDTO result = service.create(request(LocalDate.now(), null, "alive", 2L));

        // Assert
        ArgumentCaptor<Citizen> saved = ArgumentCaptor.forClass(Citizen.class);
        verify(citizenDAO).persist(saved.capture());
        assertSame(housing, saved.getValue().getHousing());
        assertEquals(2L, result.housingId());
    }

    @Test
    @DisplayName("[EP-22] does not count the updated citizen twice in its current housing")
    void updateKeepsFullHousing()
    {
        // Arrange
        Citizen citizen = new Citizen();
        citizen.setId(3L);
        citizen.setFamily(family);
        citizen.setHousing(housing);
        when(citizenDAO.findByIdOrThrow(3L)).thenReturn(citizen);
        when(familyDAO.findByIdOrThrow(1L)).thenReturn(family);
        when(housingDAO.findByIdOrThrow(2L)).thenReturn(housing);
        when(citizenDAO.countWhere("housing.id", 2L)).thenReturn(1L);
        when(citizenDAO.merge(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        CitizenDTO result = service.update(3L, request(LocalDate.now(), null, "alive", 2L));

        // Assert
        verify(citizenDAO).merge(citizen);
        assertSame(housing, citizen.getHousing());
        assertEquals(3L, result.id());
        assertEquals(2L, result.housingId());
    }

    @Test
    @DisplayName("[EP-10] rejects housing for a deceased citizen")
    void createDeceasedCitizenWithHousing()
    {
        // Arrange
        LocalDate date = LocalDate.now().minusYears(10);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.create(request(date, date, "deceased", 2L)));

        // Assert
        assertEquals("housingId must be null for a deceased citizen", exception.getMessage());
        verify(citizenDAO, never()).persist(any());
    }
}