package app.services;

import app.daos.GenericDAO;
import app.entities.Citizen;
import app.entities.CitizenRelationship;
import app.entities.Company;
import app.entities.EducationRecord;
import app.entities.Family;
import app.entities.Housing;
import app.entities.Institution;
import app.entities.Job;
import app.entities.Location;
import app.external.overpass.OsmElement;
import app.external.overpass.OverpassException;
import app.external.overpass.OverpassImportClient;
import app.populators.generators.CitizenGenerator;
import app.populators.generators.CompanyGenerator;
import app.populators.generators.EducationRecordGenerator;
import app.populators.generators.FamilyGenerator;
import app.populators.generators.HousingGenerator;
import app.populators.generators.InstitutionGenerator;
import app.populators.generators.JobGenerator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;

public class SeedService
{
    private final GenericDAO<Location> locationDAO;
    private final GenericDAO<Family> familyDAO;
    private final GenericDAO<Housing> housingDAO;
    private final GenericDAO<Citizen> citizenDAO;
    private final GenericDAO<Institution> institutionDAO;
    private final GenericDAO<Company> companyDAO;
    private final GenericDAO<EducationRecord> educationRecordDAO;
    private final GenericDAO<Job> jobDAO;
    private final OverpassImportClient overpassClient;
    private final Random random;

    public SeedService(GenericDAO<Location> locationDAO, GenericDAO<Family> familyDAO,
                       GenericDAO<Housing> housingDAO, GenericDAO<Citizen> citizenDAO,
                       GenericDAO<Institution> institutionDAO, GenericDAO<Company> companyDAO,
                       GenericDAO<EducationRecord> educationRecordDAO, GenericDAO<Job> jobDAO,
                       GenericDAO<CitizenRelationship> citizenRelationshipDAO,
                       OverpassImportClient overpassClient, Random random)
    {
        this.locationDAO = locationDAO;
        this.familyDAO = familyDAO;
        this.housingDAO = housingDAO;
        this.citizenDAO = citizenDAO;
        this.institutionDAO = institutionDAO;
        this.companyDAO = companyDAO;
        this.educationRecordDAO = educationRecordDAO;
        this.jobDAO = jobDAO;
        this.overpassClient = overpassClient;
        this.random = random;
    }

    public SeedReport seedLocationsFromOsm(double south, double west, double north, double east,
                                           String amenity, int limit)
    {
        List<OsmElement> elements = overpassClient.findInArea(south, west, north, east, amenity, limit);
        Set<String> existing = new HashSet<>();
        for (Location location : locationDAO.findAll())
        {
            if (location.getOsmId() != null && location.getOsmType() != null)
            {
                existing.add(osmKey(location.getOsmId(), location.getOsmType()));
            }
        }

        List<Location> locations = new ArrayList<>();
        int skipped = 0;
        for (OsmElement element : elements)
        {
            if (element.name() == null || element.name().isBlank())
            {
                skipped++;
                continue;
            }
            if (!existing.add(osmKey(element.osmId(), element.osmType())))
            {
                skipped++;
                continue;
            }
            Location location = new Location();
            location.setName(element.name());
            location.setLatitude(element.latitude());
            location.setLongitude(element.longitude());
            location.setOsmId(element.osmId());
            location.setOsmType(element.osmType());
            location.setStreetAddress(streetAddress(element.tags()));
            locations.add(location);
        }
        locationDAO.persistAll(locations);
        return new SeedReport(Map.of("imported", locations.size(), "skipped", skipped), true,
                "OSM location import completed");
    }

    public SeedReport seedFakeData(int citizens, long seed)
    {
        if (citizens < 1 || citizens > 1000)
        {
            throw new IllegalArgumentException("citizens must be between 1 and 1000");
        }
        List<Location> locations = locationDAO.findAll();
        if (locations.isEmpty())
        {
            throw new IllegalArgumentException("no locations: run the OSM import first");
        }
        random.setSeed(seed);

        int familyCount = Math.max(1, (citizens + 3) / 4);
        List<Family> families = new ArrayList<>(familyCount);
        FamilyGenerator familyGenerator = new FamilyGenerator(random);
        for (int i = 0; i < familyCount; i++)
        {
            families.add(familyGenerator.generate());
        }
        familyDAO.persistAll(families);

        int housingCount = Math.max(1, (citizens + 2) / 3);
        List<Housing> housings = new ArrayList<>(housingCount);
        HousingGenerator housingGenerator = new HousingGenerator(random);
        for (int i = 0; i < housingCount; i++)
        {
            Housing housing = housingGenerator.generate();
            housing.setLocation(pick(locations));
            housings.add(housing);
        }
        housingDAO.persistAll(housings);

        List<Citizen> generatedCitizens = new ArrayList<>(citizens);
        CitizenGenerator citizenGenerator = new CitizenGenerator(random);
        Map<Housing, Integer> occupancy = new HashMap<>();
        for (int i = 0; i < citizens; i++)
        {
            Citizen citizen = citizenGenerator.generate();
            citizen.setFamily(families.get(random.nextInt(families.size())));
            if ("alive".equals(citizen.getStatus()))
            {
                Housing housing = availableHousing(housings, occupancy);
                if (housing != null)
                {
                    citizen.setHousing(housing);
                    occupancy.merge(housing, 1, Integer::sum);
                }
            }
            generatedCitizens.add(citizen);
        }
        citizenDAO.persistAll(generatedCitizens);

        int organizationCount = Math.max(1, (citizens + 9) / 10);
        List<Institution> institutions = new ArrayList<>(organizationCount);
        InstitutionGenerator institutionGenerator = new InstitutionGenerator(random);
        for (int i = 0; i < organizationCount; i++)
        {
            Institution institution = institutionGenerator.generate();
            institution.setLocation(pick(locations));
            institutions.add(institution);
        }
        institutionDAO.persistAll(institutions);

        List<Company> companies = new ArrayList<>(organizationCount);
        CompanyGenerator companyGenerator = new CompanyGenerator(random);
        for (int i = 0; i < organizationCount; i++)
        {
            Company company = companyGenerator.generate();
            company.setLocation(pick(locations));
            companies.add(company);
        }
        companyDAO.persistAll(companies);

        List<EducationRecord> educationRecords = new ArrayList<>(citizens);
        EducationRecordGenerator educationGenerator = new EducationRecordGenerator(random);
        for (Citizen citizen : generatedCitizens)
        {
            educationRecords.add(educationGenerator.generate(citizen, pick(institutions)));
        }
        educationRecordDAO.persistAll(educationRecords);

        List<Job> jobs = new ArrayList<>(citizens);
        JobGenerator jobGenerator = new JobGenerator(random);
        for (Citizen citizen : generatedCitizens)
        {
            Company company = random.nextBoolean() ? pick(companies) : null;
            Institution institution = company == null ? pick(institutions) : null;
            jobs.add(jobGenerator.generate(citizen, company, institution));
        }
        jobDAO.persistAll(jobs);

        return new SeedReport(Map.of("families", families.size(), "housings", housings.size(),
                "citizens", generatedCitizens.size(), "institutions", institutions.size(), "companies", companies.size(),
                "educationRecords", educationRecords.size(), "jobs", jobs.size()), true, "Fake data seeded");
    }

    public SeedReport enrichFromOsm()
    {
        int updated = 0;
        List<Institution> institutions = institutionDAO.findAllWithLocation();
        List<Company> companies = companyDAO.findAllWithLocation();
        List<Housing> housings = housingDAO.findAllWithLocation();
        Map<String, List<EnrichmentTarget>> byType = new LinkedHashMap<>();
        institutions.forEach(value -> addTarget(byType, value.getLocation(), value));
        companies.forEach(value -> addTarget(byType, value.getLocation(), value));
        housings.forEach(value -> addTarget(byType, value.getLocation(), value));

        for (Map.Entry<String, List<EnrichmentTarget>> entry : byType.entrySet())
        {
            List<EnrichmentTarget> targets = entry.getValue();
            Map<Long, List<EnrichmentTarget>> targetsById = new LinkedHashMap<>();
            targets.forEach(target -> targetsById.computeIfAbsent(target.location().getOsmId(), key -> new ArrayList<>()).add(target));
            List<Long> ids = new ArrayList<>(targetsById.keySet());
            for (int start = 0; start < ids.size(); start += 50)
            {
                List<Long> batch = ids.subList(start, Math.min(start + 50, ids.size()));
                try
                {
                    List<OsmElement> elements = overpassClient.findByIds(entry.getKey(), batch);
                    Map<Long, OsmElement> elementById = new HashMap<>();
                    elements.forEach(element -> elementById.put(element.osmId(), element));
                    List<Object> changedEntities = new ArrayList<>();
                    for (Long id : batch)
                    {
                        OsmElement element = elementById.get(id);
                        if (element == null)
                        {
                            continue;
                        }
                        for (EnrichmentTarget target : targetsById.get(id))
                        {
                            boolean changed = target.enrich(element);
                            if (changed)
                            {
                                changedEntities.add(target.entity());
                            }
                        }
                    }
                    updated += locationDAO.mergeAllEntities(changedEntities);
                }
                catch (OverpassException exception)
                {
                    return new SeedReport(Map.of("updated", updated), false,
                            "OSM enrichment stopped after " + updated + " updates: " + exception.getMessage());
                }
            }
        }
        return new SeedReport(Map.of("updated", updated), true, "OSM enrichment completed");
    }

    private void addTarget(Map<String, List<EnrichmentTarget>> byType, Location location, Object entity)
    {
        if (location == null || location.getOsmId() == null || location.getOsmType() == null)
        {
            return;
        }
        EntityType type;
        if (entity instanceof Institution)
        {
            type = EntityType.INSTITUTION;
        }
        else if (entity instanceof Company)
        {
            type = EntityType.COMPANY;
        }
        else
        {
            type = EntityType.HOUSING;
        }
        byType.computeIfAbsent(location.getOsmType(), key -> new ArrayList<>())
                .add(new EnrichmentTarget(location, entity, type));
    }

    private String streetAddress(Map<String, String> tags)
    {
        String street = tags.get("addr:street");
        String number = tags.get("addr:housenumber");
        if (street == null || street.isBlank())
        {
            return number == null || number.isBlank() ? null : number;
        }
        return number == null || number.isBlank() ? street : street + " " + number;
    }

    private String osmKey(long id, String type)
    {
        return type + ":" + id;
    }

    private <T> T pick(List<T> values)
    {
        return values.get(random.nextInt(values.size()));
    }

    private Housing availableHousing(List<Housing> housings, Map<Housing, Integer> occupancy)
    {
        List<Housing> available = housings.stream()
                .filter(housing -> occupancy.getOrDefault(housing, 0) < housing.getCapacity())
                .toList();
        return available.isEmpty() ? null : pick(available);
    }

    private enum EntityType
    {
        INSTITUTION, COMPANY, HOUSING
    }

    private record EnrichmentTarget(Location location, Object entity, EntityType entityType)
    {
        private boolean enrich(OsmElement element)
        {
            return switch (entityType)
            {
                case INSTITUTION -> enrichInstitution((Institution) entity, element.tags());
                case COMPANY -> enrichCompany((Company) entity, element.tags());
                case HOUSING -> enrichHousing((Housing) entity, element.tags());
            };
        }
    }

    private static boolean enrichInstitution(Institution institution, Map<String, String> tags)
    {
        boolean changed = false;
        changed |= setIfNull(institution.getWebsite(), institution::setWebsite, contact(tags, "website"));
        changed |= setIfNull(institution.getPhone(), institution::setPhone, contact(tags, "phone"));
        changed |= setIfNull(institution.getEmail(), institution::setEmail, contact(tags, "email"));
        return changed;
    }

    private static boolean enrichCompany(Company company, Map<String, String> tags)
    {
        boolean changed = false;
        changed |= setIfNull(company.getWebsite(), company::setWebsite, contact(tags, "website"));
        changed |= setIfNull(company.getPhone(), company::setPhone, contact(tags, "phone"));
        changed |= setIfNull(company.getEmail(), company::setEmail, contact(tags, "email"));
        changed |= setIfNull(company.getCvrNumber(), company::setCvrNumber, tags.get("ref:DK:cvr"));
        changed |= setIfNull(company.getProductionUnitNumber(), company::setProductionUnitNumber, tags.get("ref:DK:pnr"));
        changed |= setIfNull(company.getCategory(), company::setCategory, first(tags.get("amenity"), tags.get("shop")));
        changed |= setIfNull(company.getOpeningHours(), company::setOpeningHours, tags.get("opening_hours"));
        return changed;
    }

    private static boolean enrichHousing(Housing housing, Map<String, String> tags)
    {
        boolean changed = false;
        Integer levels = integer(tags.get("building:levels"));
        Integer flats = integer(tags.get("building:flats"));
        Double height = decimal(tags.get("height"));
        if (housing.getNumberOfLevels() == null && levels != null)
        {
            housing.setNumberOfLevels(levels);
            changed = true;
        }
        if (housing.getNumberOfFlats() == null && flats != null)
        {
            housing.setNumberOfFlats(flats);
            changed = true;
        }
        if (housing.getHeight() == null && height != null)
        {
            housing.setHeight(height);
            changed = true;
        }
        return changed;
    }

    private static boolean setIfNull(String current, Consumer<String> setter, String value)
    {
        if (current == null && value != null && !value.isBlank())
        {
            setter.accept(value);
            return true;
        }
        return false;
    }

    private static String contact(Map<String, String> tags, String field)
    {
        return first(tags.get(field), tags.get("contact:" + field));
    }

    private static String first(String primary, String secondary)
    {
        return primary == null || primary.isBlank() ? secondary : primary;
    }

    private static Integer integer(String value)
    {
        try
        {
            Integer parsed = value == null ? null : Integer.valueOf(value.trim());
            return parsed == null || parsed < 0 ? null : parsed;
        }
        catch (NumberFormatException exception)
        {
            return null;
        }
    }

    private static Double decimal(String value)
    {
        try
        {
            Double parsed = value == null ? null : Double.valueOf(value.trim());
            return parsed == null || !Double.isFinite(parsed) || parsed < 0 ? null : parsed;
        }
        catch (NumberFormatException exception)
        {
            return null;
        }
    }
}
