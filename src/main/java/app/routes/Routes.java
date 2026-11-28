
package app.routes;

import app.controllers.CrudController;
import app.controllers.DocumentedCrudControllers;
import app.controllers.JobController;
import app.controllers.NearbyPlacesController;
import app.controllers.SeedController;
import app.controllers.DatabaseViewsController;
import app.daos.GenericDAO;
import app.daos.DatabaseViewsDAO;
import app.dtos.*;
import app.entities.*;
import app.external.overpass.HttpOverpassClient;
import app.security.enums.Role;
import app.services.*;
import io.javalin.Javalin;
import io.javalin.http.HandlerType;
import io.javalin.security.RouteRole;
import jakarta.persistence.EntityManagerFactory;

import java.time.Clock;
import java.time.Duration;
import java.util.Random;

public class Routes {

    public static void register(Javalin app, EntityManagerFactory emf) {

        GenericDAO<Location> locationDAO = new GenericDAO<>(emf, Location.class);
        GenericDAO<Family> familyDAO = new GenericDAO<>(emf, Family.class);
        GenericDAO<Housing> housingDAO = new GenericDAO<>(emf, Housing.class);
        GenericDAO<Citizen> citizenDAO = new GenericDAO<>(emf, Citizen.class);
        GenericDAO<CitizenRelationship> citizenRelationshipDAO =
                new GenericDAO<>(emf, CitizenRelationship.class);
        GenericDAO<Institution> institutionDAO = new GenericDAO<>(emf, Institution.class);
        GenericDAO<Company> companyDAO = new GenericDAO<>(emf, Company.class);
        GenericDAO<EducationRecord> educationRecordDAO =
                new GenericDAO<>(emf, EducationRecord.class);
        GenericDAO<Job> jobDAO = new GenericDAO<>(emf, Job.class);

        LocationService locationService = new LocationService(locationDAO);
        FamilyService familyService = new FamilyService(familyDAO);
        HousingService housingService = new HousingService(housingDAO, locationDAO);
        CitizenService citizenService =
                new CitizenService(citizenDAO, familyDAO, housingDAO);
        CitizenRelationshipService citizenRelationshipService =
                new CitizenRelationshipService(citizenRelationshipDAO, citizenDAO);
        InstitutionService institutionService =
                new InstitutionService(institutionDAO, locationDAO);
        CompanyService companyService = new CompanyService(companyDAO, locationDAO);
        EducationRecordService educationRecordService =
                new EducationRecordService(educationRecordDAO, citizenDAO, institutionDAO);
        JobService jobService =
                new JobService(jobDAO, citizenDAO, companyDAO, institutionDAO);

        String overpassUrl = System.getenv("OVERPASS_URL");

        if (overpassUrl == null || overpassUrl.isBlank()) {
            overpassUrl = HttpOverpassClient.DEFAULT_ENDPOINT_URL;
        }

        HttpOverpassClient overpassClient =
                new HttpOverpassClient(overpassUrl, Duration.ofSeconds(10));

        NearbyPlacesService nearbyPlacesService =
                new NearbyPlacesService(locationDAO, overpassClient, Clock.systemUTC());

        SeedService seedService = new SeedService(
                locationDAO,
                familyDAO,
                housingDAO,
                citizenDAO,
                institutionDAO,
                companyDAO,
                educationRecordDAO,
                jobDAO,
                citizenRelationshipDAO,
                overpassClient,
                new Random()
        );

        // Database views and stored function
        DatabaseViewsDAO databaseViewsDAO = new DatabaseViewsDAO(emf);

        DatabaseViewsService databaseViewsService =
                new DatabaseViewsService(databaseViewsDAO);

        DatabaseViewsController databaseViewsController =
                new DatabaseViewsController(databaseViewsService);

        // Existing CRUD routes
        crud(app, "/locations",
                new DocumentedCrudControllers.Locations(locationService));

        crud(app, "/families",
                new DocumentedCrudControllers.Families(familyService));

        crud(app, "/housings",
                new DocumentedCrudControllers.Housings(housingService));

        crud(app, "/citizens",
                new DocumentedCrudControllers.Citizens(citizenService));

        crud(app, "/citizen-relationships",
                new DocumentedCrudControllers.CitizenRelationships(citizenRelationshipService));

        crud(app, "/institutions",
                new DocumentedCrudControllers.Institutions(institutionService));

        crud(app, "/companies",
                new DocumentedCrudControllers.Companies(companyService));

        crud(app, "/education-records",
                new DocumentedCrudControllers.EducationRecords(educationRecordService));

        crud(app, "/jobs", new JobController(jobService), ADMINS);

        app.get("/locations/{id}/nearby",
                new NearbyPlacesController(nearbyPlacesService)::getNearby, USERS);

        // Read-only database view endpoints
        app.get("/citizen-details",
                databaseViewsController::getCitizenDetails, USERS);

        app.get("/housing-occupancy",
                databaseViewsController::getHousingOccupancy, USERS);

        app.get("/company-financial-summary",
                databaseViewsController::getCompanyFinancialSummary, ADMINS);

        app.get("/active-employment",
                databaseViewsController::getActiveEmployment, USERS);

        app.get("/citizen-relationships-view",
                databaseViewsController::getCitizenRelationships, USERS);

        // Stored database function endpoint
        app.get("/citizens/{id}/total-salary",
                databaseViewsController::getTotalCitizenSalary, ADMINS);

        // Admin seed endpoints
        SeedController seedController = new SeedController(seedService);

        app.addHttpHandler(
                HandlerType.POST,
                "/api/admin/seed/locations",
                seedController::seedLocations,
                Role.ADMIN
        );

        app.addHttpHandler(
                HandlerType.POST,
                "/api/admin/seed/fake",
                seedController::seedFakeData,
                Role.ADMIN
        );

        app.addHttpHandler(
                HandlerType.POST,
                "/api/admin/seed/enrich",
                seedController::enrich,
                Role.ADMIN
        );
    }

    /**
     * The same nine resources for another database solution, under a prefix: /mongodb/citizens, /neo4j/jobs, ...
     * The controllers, DTOs, validators, paging and role rules (including the admin-only salary in JobController) are
     * the ones the PostgreSQL routes use; only the services behind them differ.
     */
    public static void registerSolution(Javalin app, String prefix, CrudServices s) {
        crud(app, prefix + "/locations", new CrudController<>(s.locations(), LocationRequestDTO.class));
        crud(app, prefix + "/families", new CrudController<>(s.families(), FamilyRequestDTO.class));
        crud(app, prefix + "/housings", new CrudController<>(s.housings(), HousingRequestDTO.class));
        crud(app, prefix + "/citizens", new CrudController<>(s.citizens(), CitizenRequestDTO.class));
        crud(app, prefix + "/citizen-relationships",
                new CrudController<>(s.relationships(), CitizenRelationshipRequestDTO.class));
        crud(app, prefix + "/institutions", new CrudController<>(s.institutions(), InstitutionRequestDTO.class));
        crud(app, prefix + "/companies", new CrudController<>(s.companies(), CompanyRequestDTO.class));
        crud(app, prefix + "/education-records",
                new CrudController<>(s.educationRecords(), EducationRecordRequestDTO.class));
        crud(app, prefix + "/jobs", new JobController(s.jobs()), ADMINS);
    }

    // Role rules: users and administrators have full CRUD on all city data.
    // Job salary information is restricted to administrators.
    // Without a valid token every protected route answers 401;
    // without the required role, the response is 403.

    private static final RouteRole[] USERS = {Role.USER, Role.ADMIN};
    private static final RouteRole[] ADMINS = {Role.ADMIN};

    private static void crud(
            Javalin app,
            String path,
            CrudController<?, ?> c) {

        crud(app, path, c, USERS);
    }

    private static void crud(
            Javalin app,
            String path,
            CrudController<?, ?> c,
            RouteRole[] createRoles) {

        app.get(path, c::getAll, USERS);
        app.get(path + "/{id}", c::getById, USERS);
        app.post(path, c::create, createRoles);
        app.put(path + "/{id}", c::update, USERS);
        app.delete(path + "/{id}", c::delete, USERS);
    }
}
