package app.routes;

import app.controllers.CrudController;
import app.daos.GenericDAO;
import app.dtos.*;
import app.entities.*;
import app.services.*;
import io.javalin.Javalin;
import jakarta.persistence.EntityManagerFactory;

public class Routes {
    public static void register(Javalin app, EntityManagerFactory emf) {
        GenericDAO<Location> locationDAO = new GenericDAO<>(emf, Location.class);
        GenericDAO<Family> familyDAO = new GenericDAO<>(emf, Family.class);
        GenericDAO<Housing> housingDAO = new GenericDAO<>(emf, Housing.class);
        GenericDAO<Citizen> citizenDAO = new GenericDAO<>(emf, Citizen.class);
        GenericDAO<CitizenRelationship> citizenRelationshipDAO = new GenericDAO<>(emf, CitizenRelationship.class);
        GenericDAO<Institution> institutionDAO = new GenericDAO<>(emf, Institution.class);
        GenericDAO<Company> companyDAO = new GenericDAO<>(emf, Company.class);
        GenericDAO<EducationRecord> educationRecordDAO = new GenericDAO<>(emf, EducationRecord.class);
        GenericDAO<Job> jobDAO = new GenericDAO<>(emf, Job.class);

        LocationService locationService = new LocationService(locationDAO);
        FamilyService familyService = new FamilyService(familyDAO);
        HousingService housingService = new HousingService(housingDAO, locationDAO);
        CitizenService citizenService = new CitizenService(citizenDAO, familyDAO, housingDAO);
        CitizenRelationshipService citizenRelationshipService = new CitizenRelationshipService(citizenRelationshipDAO, citizenDAO);
        InstitutionService institutionService = new InstitutionService(institutionDAO, locationDAO);
        CompanyService companyService = new CompanyService(companyDAO, locationDAO);
        EducationRecordService educationRecordService = new EducationRecordService(educationRecordDAO, citizenDAO, institutionDAO);
        JobService jobService = new JobService(jobDAO, citizenDAO, companyDAO, institutionDAO);

        crud(app, "/locations", new CrudController<>(locationService, LocationRequestDTO.class));
        crud(app, "/families", new CrudController<>(familyService, FamilyRequestDTO.class));
        crud(app, "/housings", new CrudController<>(housingService, HousingRequestDTO.class));
        crud(app, "/citizens", new CrudController<>(citizenService, CitizenRequestDTO.class));
        crud(app, "/citizen-relationships", new CrudController<>(citizenRelationshipService, CitizenRelationshipRequestDTO.class));
        crud(app, "/institutions", new CrudController<>(institutionService, InstitutionRequestDTO.class));
        crud(app, "/companies", new CrudController<>(companyService, CompanyRequestDTO.class));
        crud(app, "/education-records", new CrudController<>(educationRecordService, EducationRecordRequestDTO.class));
        crud(app, "/jobs", new CrudController<>(jobService, JobRequestDTO.class));
    }

    private static void crud(Javalin app, String path, CrudController<?, ?> c) {
        app.get(path, c::getAll);
        app.get(path + "/{id}", c::getById);
        app.post(path, c::create);
        app.put(path + "/{id}", c::update);
        app.delete(path + "/{id}", c::delete);
    }
}
