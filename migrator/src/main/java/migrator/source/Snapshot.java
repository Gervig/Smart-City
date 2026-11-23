package migrator.source;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Every row of the relational database, read in one consistent transaction. One record per table, with the same
 * columns as the table (SQL/schema/database_schema.sql).
 */
public record Snapshot(List<LocationRow> locations, List<FamilyRow> families, List<HousingRow> housings,
                       List<CompanyRow> companies, List<InstitutionRow> institutions, List<CitizenRow> citizens,
                       List<JobRow> jobs, List<EducationRow> educationRecords,
                       List<RelationshipRow> relationships, List<UserRow> users)
{
    public record LocationRow(long id, String name, String streetAddress, Double latitude, Double longitude,
                              Long osmId, String osmType) {}

    public record FamilyRow(long id, String familyName) {}

    public record HousingRow(long id, long locationId, String housingType, int capacity, Integer numberOfLevels,
                             Integer numberOfFlats, Double height) {}

    public record CompanyRow(long id, long locationId, String name, LocalDate foundedDate, LocalDate dissolvedDate,
                             String status, String cvrNumber, String productionUnitNumber, String website,
                             String phone, String email, String category, String openingHours) {}

    public record InstitutionRow(long id, long locationId, String name, String type, String website, String phone,
                                 String email) {}

    public record CitizenRow(long id, String firstName, String lastName, LocalDate birthDate, LocalDate deathDate,
                             String status, String gender, long familyId, Long housingId) {}

    public record JobRow(long id, long citizenId, Long companyId, Long institutionId, String title,
                         BigDecimal salary, LocalDate startDate, LocalDate endDate, String status) {}

    public record EducationRow(long id, long citizenId, long institutionId, LocalDate startDate, LocalDate endDate) {}

    public record RelationshipRow(long id, long citizenId1, long citizenId2, String type, LocalDate startDate,
                                  LocalDate endDate) {}

    /** password is the BCrypt hash, never the plain password */
    public record UserRow(String username, String password, List<String> roles) {}

    public int rowCount()
    {
        return locations.size() + families.size() + housings.size() + companies.size() + institutions.size()
                + citizens.size() + jobs.size() + educationRecords.size() + relationships.size() + users.size();
    }
}
