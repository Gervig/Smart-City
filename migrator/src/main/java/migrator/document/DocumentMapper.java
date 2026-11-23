package migrator.document;

import migrator.source.Snapshot;
import migrator.source.Snapshot.CitizenRow;
import migrator.source.Snapshot.CompanyRow;
import migrator.source.Snapshot.EducationRow;
import migrator.source.Snapshot.FamilyRow;
import migrator.source.Snapshot.InstitutionRow;
import migrator.source.Snapshot.JobRow;
import migrator.source.Snapshot.LocationRow;
import migrator.source.Snapshot.RelationshipRow;
import org.bson.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Turns the relational rows into MongoDB documents. Rule of thumb: embed what is read together with its owner and
 * belongs to it alone; reference what is shared or grows without limit.
 * <ul>
 *     <li>citizens: family (small, read with the citizen), jobs, education and relationships are embedded. The
 *     housing is shared by several citizens, so it is a reference (housingId).</li>
 *     <li>housings, companies, institutions: their location is embedded (a copy), because they are always shown
 *     with their address.</li>
 *     <li>locations: their own collection too, with a GeoJSON point for geo queries.</li>
 *     <li>families: name and the ids of their members.</li>
 *     <li>users: roles embedded as a list of names.</li>
 * </ul>
 * The relational id becomes {@code _id}. Columns that are null are left out of the document.
 */
public final class DocumentMapper
{
    private DocumentMapper()
    {
    }

    /** Collection name to its documents, in the order they are written */
    public static Map<String, List<Document>> map(Snapshot s)
    {
        // Every list is grouped once here, so building a document is a map lookup instead of a scan of a whole
        // list (linear instead of quadratic in the number of rows). groupingBy keeps the row order in each group.
        Map<Long, LocationRow> locations = byId(s.locations(), LocationRow::id);
        Map<Long, List<Long>> membersByFamily = s.citizens().stream().collect(Collectors.groupingBy(
                CitizenRow::familyId, Collectors.mapping(CitizenRow::id, Collectors.toList())));
        Map<Long, List<Long>> residentsByHousing = s.citizens().stream().filter(c -> c.housingId() != null)
                .collect(Collectors.groupingBy(CitizenRow::housingId,
                        Collectors.mapping(CitizenRow::id, Collectors.toList())));
        Lookups lookups = new Lookups(byId(s.families(), FamilyRow::id), byId(s.companies(), CompanyRow::id),
                byId(s.institutions(), InstitutionRow::id), byId(s.citizens(), CitizenRow::id),
                s.jobs().stream().collect(Collectors.groupingBy(JobRow::citizenId)),
                s.educationRecords().stream().collect(Collectors.groupingBy(EducationRow::citizenId)),
                relationshipsByCitizen(s.relationships()));

        Map<String, List<Document>> collections = new LinkedHashMap<>();
        collections.put("locations", s.locations().stream()
                .map(l -> location(l).append("_id", l.id())).toList());
        collections.put("families", s.families().stream().map(f -> doc()
                .append("_id", f.id())
                .append("familyName", f.familyName())
                .append("memberIds", membersByFamily.getOrDefault(f.id(), List.of()))).toList());
        collections.put("housings", s.housings().stream().map(h -> withoutNulls(doc()
                .append("_id", h.id())
                .append("housingType", h.housingType())
                .append("capacity", h.capacity())
                .append("numberOfLevels", h.numberOfLevels())
                .append("numberOfFlats", h.numberOfFlats())
                .append("height", h.height())
                .append("location", embeddedLocation(locations.get(h.locationId())))
                .append("residentIds", residentsByHousing.getOrDefault(h.id(), List.of())))).toList());
        collections.put("companies", s.companies().stream().map(c -> withoutNulls(doc()
                .append("_id", c.id())
                .append("name", c.name())
                .append("foundedDate", c.foundedDate())
                .append("dissolvedDate", c.dissolvedDate())
                .append("status", c.status())
                .append("cvrNumber", c.cvrNumber())
                .append("productionUnitNumber", c.productionUnitNumber())
                .append("website", c.website())
                .append("phone", c.phone())
                .append("email", c.email())
                .append("category", c.category())
                .append("openingHours", c.openingHours())
                .append("location", embeddedLocation(locations.get(c.locationId()))))).toList());
        collections.put("institutions", s.institutions().stream().map(i -> withoutNulls(doc()
                .append("_id", i.id())
                .append("name", i.name())
                .append("type", i.type())
                .append("website", i.website())
                .append("phone", i.phone())
                .append("email", i.email())
                .append("location", embeddedLocation(locations.get(i.locationId()))))).toList());
        collections.put("citizens", s.citizens().stream().map(c -> citizen(c, lookups)).toList());
        collections.put("users", s.users().stream().map(u -> doc()
                .append("_id", u.username())
                .append("password", u.password())
                .append("roles", u.roles())).toList());
        return collections;
    }

    /** The rows by id, and the rows that belong to a citizen by citizen id, built once per migration */
    private record Lookups(Map<Long, FamilyRow> families, Map<Long, CompanyRow> companies,
                           Map<Long, InstitutionRow> institutions, Map<Long, CitizenRow> citizens,
                           Map<Long, List<JobRow>> jobs, Map<Long, List<EducationRow>> education,
                           Map<Long, List<RelationshipRow>> relationships) {}

    // A relationship row links two citizens, so it is listed under both of them
    private static Map<Long, List<RelationshipRow>> relationshipsByCitizen(List<RelationshipRow> rows)
    {
        Map<Long, List<RelationshipRow>> byCitizen = new HashMap<>();
        for (RelationshipRow r : rows)
        {
            byCitizen.computeIfAbsent(r.citizenId1(), id -> new ArrayList<>()).add(r);
            byCitizen.computeIfAbsent(r.citizenId2(), id -> new ArrayList<>()).add(r);
        }
        return byCitizen;
    }

    private static Document citizen(CitizenRow c, Lookups lookups)
    {
        Map<Long, CompanyRow> companies = lookups.companies();
        Map<Long, InstitutionRow> institutions = lookups.institutions();
        List<Document> jobs = lookups.jobs().getOrDefault(c.id(), List.of()).stream().map(j -> withoutNulls(doc()
                .append("id", j.id())
                .append("title", j.title())
                .append("salary", j.salary())
                .append("startDate", j.startDate())
                .append("endDate", j.endDate())
                .append("status", j.status())
                .append("employer", j.companyId() != null
                        ? doc().append("type", "company").append("id", j.companyId())
                                .append("name", companies.get(j.companyId()).name())
                        : doc().append("type", "institution").append("id", j.institutionId())
                                .append("name", institutions.get(j.institutionId()).name())))).toList();

        List<Document> education = lookups.education().getOrDefault(c.id(), List.of()).stream()
                .map(e -> withoutNulls(doc()
                        .append("id", e.id())
                        .append("institution", doc().append("id", e.institutionId())
                                .append("name", institutions.get(e.institutionId()).name())
                                .append("type", institutions.get(e.institutionId()).type()))
                        .append("startDate", e.startDate())
                        .append("endDate", e.endDate()))).toList();

        // Each of the two citizens gets the relationship, pointing at the other one
        List<Document> relationships = lookups.relationships().getOrDefault(c.id(), List.of()).stream().map(r ->
        {
            long other = r.citizenId1() == c.id() ? r.citizenId2() : r.citizenId1();
            CitizenRow o = lookups.citizens().get(other);
            return withoutNulls(doc()
                    .append("id", r.id())
                    .append("type", r.type())
                    .append("citizenId", other)
                    .append("name", o.firstName() + " " + o.lastName())
                    .append("startDate", r.startDate())
                    .append("endDate", r.endDate()));
        }).toList();

        FamilyRow family = lookups.families().get(c.familyId());
        return withoutNulls(doc()
                .append("_id", c.id())
                .append("firstName", c.firstName())
                .append("lastName", c.lastName())
                .append("birthDate", c.birthDate())
                .append("deathDate", c.deathDate())
                .append("status", c.status())
                .append("gender", c.gender())
                .append("family", doc().append("id", family.id()).append("familyName", family.familyName()))
                .append("housingId", c.housingId())
                .append("jobs", jobs)
                .append("education", education)
                .append("relationships", relationships));
    }

    private static Document location(LocationRow l)
    {
        Document d = doc().append("name", l.name()).append("streetAddress", l.streetAddress());
        if (l.latitude() != null && l.longitude() != null)
        {
            // GeoJSON order is [longitude, latitude]
            d.append("point", doc().append("type", "Point").append("coordinates", List.of(l.longitude(), l.latitude())));
        }
        if (l.osmId() != null)
        {
            d.append("osm", doc().append("id", l.osmId()).append("type", l.osmType()));
        }
        return withoutNulls(d);
    }

    private static Document embeddedLocation(LocationRow l)
    {
        Document d = doc().append("id", l.id());
        d.putAll(location(l));
        return d;
    }

    private static Document doc()
    {
        return new Document();
    }

    private static Document withoutNulls(Document d)
    {
        d.values().removeIf(v -> v == null);
        return d;
    }

    private static <T> Map<Long, T> byId(List<T> rows, Function<T, Long> id)
    {
        return rows.stream().collect(Collectors.toMap(id, Function.identity()));
    }
}
