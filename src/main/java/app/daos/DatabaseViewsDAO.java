package app.daos;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DatabaseViewsDAO {

    private final EntityManagerFactory emf;

    public DatabaseViewsDAO(EntityManagerFactory emf) {
        this.emf = emf;
    }

    // 1. Citizen details view
    public List<Map<String, Object>> getCitizenDetails() {
        String sql = """
                SELECT *
                FROM citizen_details
                ORDER BY citizen_id
                """;

        return executeViewQuery(sql, List.of(
                "citizen_id",
                "first_name",
                "last_name",
                "birth_date",
                "age",
                "gender",
                "status",
                "death_date",
                "family_name",
                "housing_id",
                "housing_type",
                "housing_capacity",
                "housing_location"
        ));
    }

    // 2. Housing occupancy view
    public List<Map<String, Object>> getHousingOccupancy() {
        String sql = """
                SELECT *
                FROM v_housing_occupancy
                ORDER BY housing_id
                """;

        return executeViewQuery(sql, List.of(
                "housing_id",
                "location_name",
                "street_address",
                "housing_type",
                "capacity",
                "current_occupants",
                "available_spots"
        ));
    }

    // 3. Company financial summary view
    public List<Map<String, Object>> getCompanyFinancialSummary() {
        String sql = """
                SELECT *
                FROM v_company_financial_summary
                ORDER BY company_id
                """;

        return executeViewQuery(sql, List.of(
                "company_id",
                "company_name",
                "active_job_count",
                "total_active_salaries"
        ));
    }

    // 4. Active employment view
    public List<Map<String, Object>> getActiveEmployment() {
        String sql = """
                SELECT *
                FROM v_active_employment
                ORDER BY citizen_id
                """;

        return executeViewQuery(sql, List.of(
                "citizen_id",
                "citizen_name",
                "job_title",
                "salary",
                "company_id",
                "company_name",
                "start_date"
        ));
    }

    // 5. Citizen relationships view
    public List<Map<String, Object>> getCitizenRelationships() {
        String sql = """
                SELECT *
                FROM v_citizen_relationships
                ORDER BY relationship_id
                """;

        return executeViewQuery(sql, List.of(
                "relationship_id",
                "citizen_1_id",
                "citizen_1_name",
                "relationship_type",
                "citizen_2_id",
                "citizen_2_name",
                "start_date",
                "end_date"
        ));
    }

    // Shared method for executing and mapping database view queries
    private List<Map<String, Object>> executeViewQuery(
            String sql, List<String> columns) {

        try (EntityManager em = emf.createEntityManager()) {

            List<?> results = em.createNativeQuery(sql).getResultList();

            List<Map<String, Object>> mappedResults = new ArrayList<>();

            for (Object result : results) {

                Object[] row = (Object[]) result;

                Map<String, Object> mappedRow = new LinkedHashMap<>();

                for (int i = 0; i < columns.size(); i++) {
                    mappedRow.put(columns.get(i), row[i]);
                }

                mappedResults.add(mappedRow);
            }

            return mappedResults;
        }
    }

    // 6. Stored database function
    public BigDecimal getTotalCitizenSalary(Long citizenId) {

        String sql = """
                SELECT total_citizen_salary(:citizenId)
                """;

        try (EntityManager em = emf.createEntityManager()) {

            return (BigDecimal) em.createNativeQuery(sql)
                    .setParameter("citizenId", citizenId)
                    .getSingleResult();
        }
    }
}
