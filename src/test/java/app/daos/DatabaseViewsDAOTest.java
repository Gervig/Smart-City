package app.daos;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DatabaseViewsDAOTest {

    private EntityManagerFactory emf;
    private EntityManager em;
    private Query query;

    private DatabaseViewsDAO dao;

    @BeforeEach
    void setUp() {
        emf = mock(EntityManagerFactory.class);
        em = mock(EntityManager.class);
        query = mock(Query.class);

        dao = new DatabaseViewsDAO(emf);

        when(emf.createEntityManager()).thenReturn(em);
    }

    @Test
    void getHousingOccupancyMapsResultsCorrectly() {
        // Arrange
        Object[] row = {
                1L,
                "Central District",
                "Main Street 10",
                "Apartment",
                5,
                3,
                2
        };

        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.<Object[]>of(row));
        // Act
        List<Map<String, Object>> result = dao.getHousingOccupancy();

        // Assert
        assertEquals(1, result.size());

        Map<String, Object> housing = result.get(0);

        assertEquals(1L, housing.get("housing_id"));
        assertEquals("Central District", housing.get("location_name"));
        assertEquals("Main Street 10", housing.get("street_address"));
        assertEquals("Apartment", housing.get("housing_type"));
        assertEquals(5, housing.get("capacity"));
        assertEquals(3, housing.get("current_occupants"));
        assertEquals(2, housing.get("available_spots"));

        verify(em).createNativeQuery(
                contains("v_housing_occupancy")
        );

        verify(em).close();
    }

    @Test
    void getTotalCitizenSalaryExecutesFunctionWithCorrectId() {
        // Arrange
        Long citizenId = 1L;
        BigDecimal expectedSalary = new BigDecimal("45000.00");

        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.setParameter("citizenId", citizenId)).thenReturn(query);
        when(query.getSingleResult()).thenReturn(expectedSalary);

        // Act
        BigDecimal result = dao.getTotalCitizenSalary(citizenId);

        // Assert
        assertEquals(expectedSalary, result);

        verify(em).createNativeQuery(
                contains("total_citizen_salary")
        );

        verify(query).setParameter("citizenId", citizenId);
        verify(query).getSingleResult();
        verify(em).close();
    }

    @Test
    void getCitizenDetailsMapsResultsCorrectly() {
        // Arrange
        Object[] row = {
                1L, "Alex", "Smith", null, 25, "Male",
                "Alive", null, "Smith Family", 1L,
                "Apartment", 5, "Central District"
        };

        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.<Object[]>of(row));

        // Act
        List<Map<String, Object>> result = dao.getCitizenDetails();

        // Assert
        assertEquals(1, result.size());

        Map<String, Object> citizen = result.get(0);

        assertEquals(1L, citizen.get("citizen_id"));
        assertEquals("Alex", citizen.get("first_name"));
        assertEquals("Smith", citizen.get("last_name"));
        assertEquals(25, citizen.get("age"));
        assertEquals("Male", citizen.get("gender"));
        assertEquals("Alive", citizen.get("status"));
        assertEquals("Smith Family", citizen.get("family_name"));
        assertEquals(1L, citizen.get("housing_id"));

        verify(em).close();
    }

    @Test
    void getCompanyFinancialSummaryMapsResultsCorrectly() {
        // Arrange
        Object[] row = {
                1L, "Test Company", 5, new BigDecimal("250000.00")
        };

        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.<Object[]>of(row));

        // Act
        List<Map<String, Object>> result =
                dao.getCompanyFinancialSummary();

        // Assert
        assertEquals(1, result.size());

        Map<String, Object> company = result.get(0);

        assertEquals(1L, company.get("company_id"));
        assertEquals("Test Company", company.get("company_name"));
        assertEquals(5, company.get("active_job_count"));
        assertEquals(
                new BigDecimal("250000.00"),
                company.get("total_active_salaries")
        );

        verify(em).close();
    }

    @Test
    void getActiveEmploymentMapsResultsCorrectly() {
        // Arrange
        Object[] row = {
                1L, "Alex Smith", "Developer",
                new BigDecimal("45000.00"),
                2L, "Test Company", null
        };

        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.<Object[]>of(row));

        // Act
        List<Map<String, Object>> result = dao.getActiveEmployment();

        // Assert
        assertEquals(1, result.size());

        Map<String, Object> employment = result.get(0);

        assertEquals(1L, employment.get("citizen_id"));
        assertEquals("Alex Smith", employment.get("citizen_name"));
        assertEquals("Developer", employment.get("job_title"));
        assertEquals(
                new BigDecimal("45000.00"),
                employment.get("salary")
        );
        assertEquals(2L, employment.get("company_id"));
        assertEquals("Test Company", employment.get("company_name"));

        verify(em).close();
    }

    @Test
    void getCitizenRelationshipsMapsResultsCorrectly() {
        // Arrange
        Object[] row = {
                1L, 1L, "Alex Smith", "Friend",
                2L, "John Doe", null, null
        };

        when(em.createNativeQuery(anyString())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.<Object[]>of(row));

        // Act
        List<Map<String, Object>> result =
                dao.getCitizenRelationships();

        // Assert
        assertEquals(1, result.size());

        Map<String, Object> relationship = result.get(0);

        assertEquals(1L, relationship.get("relationship_id"));
        assertEquals(1L, relationship.get("citizen_1_id"));
        assertEquals("Alex Smith", relationship.get("citizen_1_name"));
        assertEquals("Friend", relationship.get("relationship_type"));
        assertEquals(2L, relationship.get("citizen_2_id"));
        assertEquals("John Doe", relationship.get("citizen_2_name"));

        verify(em).close();
    }
}
