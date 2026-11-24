package app.services;

import app.daos.DatabaseViewsDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DatabaseViewsServiceTest {

    @Mock
    private DatabaseViewsDAO dao;

    private DatabaseViewsService service;

    @BeforeEach
    void setUp() {
        service = new DatabaseViewsService(dao);
    }

    @Test
    void getCitizenDetailsDelegatesToDAO() {
        // Arrange
        List<Map<String, Object>> expected = List.of(
                Map.of("citizen_id", 1L, "first_name", "Alex")
        );

        when(dao.getCitizenDetails()).thenReturn(expected);

        // Act
        List<Map<String, Object>> result = service.getCitizenDetails();

        // Assert
        assertEquals(expected, result);
        verify(dao).getCitizenDetails();
    }

    @Test
    void getHousingOccupancyDelegatesToDAO() {
        // Arrange
        List<Map<String, Object>> expected = List.of(
                Map.of("housing_id", 1L, "capacity", 5)
        );

        when(dao.getHousingOccupancy()).thenReturn(expected);

        // Act
        List<Map<String, Object>> result = service.getHousingOccupancy();

        // Assert
        assertEquals(expected, result);
        verify(dao).getHousingOccupancy();
    }

    @Test
    void getCompanyFinancialSummaryDelegatesToDAO() {
        // Arrange
        List<Map<String, Object>> expected = List.of(
                Map.of("company_id", 1L, "company_name", "Test Company")
        );

        when(dao.getCompanyFinancialSummary()).thenReturn(expected);

        // Act
        List<Map<String, Object>> result =
                service.getCompanyFinancialSummary();

        // Assert
        assertEquals(expected, result);
        verify(dao).getCompanyFinancialSummary();
    }

    @Test
    void getActiveEmploymentDelegatesToDAO() {
        // Arrange
        List<Map<String, Object>> expected = List.of(
                Map.of("citizen_id", 1L, "job_title", "Developer")
        );

        when(dao.getActiveEmployment()).thenReturn(expected);

        // Act
        List<Map<String, Object>> result = service.getActiveEmployment();

        // Assert
        assertEquals(expected, result);
        verify(dao).getActiveEmployment();
    }

    @Test
    void getCitizenRelationshipsDelegatesToDAO() {
        // Arrange
        List<Map<String, Object>> expected = List.of(
                Map.of("relationship_id", 1L, "relationship_type", "Friend")
        );

        when(dao.getCitizenRelationships()).thenReturn(expected);

        // Act
        List<Map<String, Object>> result =
                service.getCitizenRelationships();

        // Assert
        assertEquals(expected, result);
        verify(dao).getCitizenRelationships();
    }

    @Test
    void getTotalCitizenSalaryReturnsSalaryFromDAO() {
        // Arrange
        Long citizenId = 1L;
        BigDecimal expected = new BigDecimal("45000.00");

        when(dao.getTotalCitizenSalary(citizenId)).thenReturn(expected);

        // Act
        BigDecimal result = service.getTotalCitizenSalary(citizenId);

        // Assert
        assertEquals(expected, result);
        verify(dao).getTotalCitizenSalary(citizenId);
    }

    @Test
    void getTotalCitizenSalaryRejectsNullId() {
        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> service.getTotalCitizenSalary(null)
        );

        verifyNoInteractions(dao);
    }

    @Test
    void getTotalCitizenSalaryRejectsZeroId() {
        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> service.getTotalCitizenSalary(0L)
        );

        verifyNoInteractions(dao);
    }

    @Test
    void getTotalCitizenSalaryRejectsNegativeId() {
        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> service.getTotalCitizenSalary(-1L)
        );

        verifyNoInteractions(dao);
    }
}
