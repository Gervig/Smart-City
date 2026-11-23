package app.services;

import app.daos.DatabaseViewsDAO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class DatabaseViewsService {

    private final DatabaseViewsDAO dao;

    public DatabaseViewsService(DatabaseViewsDAO dao) {
        this.dao = dao;
    }

    public List<Map<String, Object>> getCitizenDetails() {
        return dao.getCitizenDetails();
    }

    public List<Map<String, Object>> getHousingOccupancy() {
        return dao.getHousingOccupancy();
    }

    public List<Map<String, Object>> getCompanyFinancialSummary() {
        return dao.getCompanyFinancialSummary();
    }

    public List<Map<String, Object>> getActiveEmployment() {
        return dao.getActiveEmployment();
    }

    public List<Map<String, Object>> getCitizenRelationships() {
        return dao.getCitizenRelationships();
    }

    public BigDecimal getTotalCitizenSalary(Long citizenId) {

        if (citizenId == null || citizenId <= 0) {
            throw new IllegalArgumentException("Invalid citizen ID");
        }

        return dao.getTotalCitizenSalary(citizenId);
    }
}
