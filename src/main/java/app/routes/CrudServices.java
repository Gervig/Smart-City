package app.routes;

import app.dtos.CitizenDTO;
import app.dtos.CitizenRelationshipDTO;
import app.dtos.CitizenRelationshipRequestDTO;
import app.dtos.CitizenRequestDTO;
import app.dtos.CompanyDTO;
import app.dtos.CompanyRequestDTO;
import app.dtos.EducationRecordDTO;
import app.dtos.EducationRecordRequestDTO;
import app.dtos.FamilyDTO;
import app.dtos.FamilyRequestDTO;
import app.dtos.HousingDTO;
import app.dtos.HousingRequestDTO;
import app.dtos.InstitutionDTO;
import app.dtos.InstitutionRequestDTO;
import app.dtos.JobDTO;
import app.dtos.JobRequestDTO;
import app.dtos.LocationDTO;
import app.dtos.LocationRequestDTO;
import app.mongo.MongoCitizenRelationshipService;
import app.mongo.MongoCitizenService;
import app.mongo.MongoCompanyService;
import app.mongo.MongoEducationRecordService;
import app.mongo.MongoFamilyService;
import app.mongo.MongoHousingService;
import app.mongo.MongoInstitutionService;
import app.mongo.MongoJobService;
import app.mongo.MongoLocationService;
import app.mongo.MongoStore;
import app.neo4j.Neo4jCitizenRelationshipService;
import app.neo4j.Neo4jCitizenService;
import app.neo4j.Neo4jCompanyService;
import app.neo4j.Neo4jEducationRecordService;
import app.neo4j.Neo4jFamilyService;
import app.neo4j.Neo4jHousingService;
import app.neo4j.Neo4jInstitutionService;
import app.neo4j.Neo4jJobService;
import app.neo4j.Neo4jLocationService;
import app.neo4j.Neo4jStore;
import app.services.CrudService;

/**
 * The nine CRUD services of one database solution. The controllers only know the CrudService interface, so the same
 * controllers, DTOs, validators and role rules serve PostgreSQL, MongoDB and Neo4j; only the data layer differs.
 */
public record CrudServices(
        CrudService<LocationDTO, LocationRequestDTO> locations,
        CrudService<FamilyDTO, FamilyRequestDTO> families,
        CrudService<HousingDTO, HousingRequestDTO> housings,
        CrudService<CitizenDTO, CitizenRequestDTO> citizens,
        CrudService<CitizenRelationshipDTO, CitizenRelationshipRequestDTO> relationships,
        CrudService<InstitutionDTO, InstitutionRequestDTO> institutions,
        CrudService<CompanyDTO, CompanyRequestDTO> companies,
        CrudService<EducationRecordDTO, EducationRecordRequestDTO> educationRecords,
        CrudService<JobDTO, JobRequestDTO> jobs)
{
    public static CrudServices mongo(MongoStore store)
    {
        return new CrudServices(new MongoLocationService(store), new MongoFamilyService(store),
                new MongoHousingService(store), new MongoCitizenService(store),
                new MongoCitizenRelationshipService(store), new MongoInstitutionService(store),
                new MongoCompanyService(store), new MongoEducationRecordService(store), new MongoJobService(store));
    }

    public static CrudServices neo4j(Neo4jStore store)
    {
        return new CrudServices(new Neo4jLocationService(store), new Neo4jFamilyService(store),
                new Neo4jHousingService(store), new Neo4jCitizenService(store),
                new Neo4jCitizenRelationshipService(store), new Neo4jInstitutionService(store),
                new Neo4jCompanyService(store), new Neo4jEducationRecordService(store), new Neo4jJobService(store));
    }
}
