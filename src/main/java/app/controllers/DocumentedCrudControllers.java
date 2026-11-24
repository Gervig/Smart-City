package app.controllers;

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
import app.dtos.LocationDTO;
import app.dtos.LocationRequestDTO;
import app.services.CrudService;
import io.javalin.http.Context;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiContentProperty;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiRequestBody;
import io.javalin.openapi.OpenApiResponse;
import io.javalin.openapi.OpenApiSecurity;

public final class DocumentedCrudControllers {
    private DocumentedCrudControllers() {
    }

    public static final class Locations extends CrudController<LocationDTO, LocationRequestDTO> {
        public Locations(CrudService<LocationDTO, LocationRequestDTO> service) {
            super(service, LocationRequestDTO.class);
        }

        @Override
        @OpenApi(path = "/locations", methods = HttpMethod.GET, operationId = "listLocations",
                summary = "List locations", tags = "Locations",
                description = "Requires a valid bearer token and the USER or ADMIN role. String filters use case-insensitive contains matching.",
                security = @OpenApiSecurity(name = "BearerAuth"),
                queryParams = {
                        @OpenApiParam(name = "page", type = Integer.class, description = "Zero-based page index; defaults to 0."),
                        @OpenApiParam(name = "size", type = Integer.class, description = "Page size from 1 to 100; defaults to 20."),
                        @OpenApiParam(name = "sort", description = "Sort field and direction as field,asc|desc. Fields: id, name, streetAddress; defaults to id,asc."),
                        @OpenApiParam(name = "name", description = "Optional substring filter."),
                        @OpenApiParam(name = "streetAddress", description = "Optional substring filter.")
                },
                responses = {
                        @OpenApiResponse(status = "200", description = "Paginated location results.", content = @OpenApiContent(mimeType = "application/json", type = "object", properties = {
                                @OpenApiContentProperty(name = "data", from = LocationDTO.class, isArray = true),
                                @OpenApiContentProperty(name = "page", type = "integer", format = "int32"),
                                @OpenApiContentProperty(name = "size", type = "integer", format = "int32"),
                                @OpenApiContentProperty(name = "totalElements", type = "integer", format = "int64"),
                                @OpenApiContentProperty(name = "totalPages", type = "integer", format = "int64")
                        })),
                        @OpenApiResponse(status = "400", description = "Invalid pagination, sort, or filter parameter."),
                        @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                        @OpenApiResponse(status = "500", description = "Unexpected server error.")
                })
        public void getAll(Context ctx) {
            super.getAll(ctx);
        }

        @Override
        @OpenApi(path = "/locations/{id}", methods = HttpMethod.GET, operationId = "getLocation",
                summary = "Get a location", tags = "Locations",
                description = "Requires a valid bearer token and the USER or ADMIN role.",
                security = @OpenApiSecurity(name = "BearerAuth"),
                pathParams = @OpenApiParam(name = "id", type = Long.class, required = true, description = "Location ID."),
                responses = {
                        @OpenApiResponse(status = "200", description = "Location details.", content = @OpenApiContent(from = LocationDTO.class)),
                        @OpenApiResponse(status = "400", description = "The ID is not a valid integer."),
                        @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                        @OpenApiResponse(status = "404", description = "Location not found."),
                        @OpenApiResponse(status = "500", description = "Unexpected server error.")
                })
        public void getById(Context ctx) {
            super.getById(ctx);
        }

        @Override
        @OpenApi(path = "/locations", methods = HttpMethod.POST, operationId = "createLocation",
                summary = "Create a location", tags = "Locations",
                description = "Requires a valid bearer token and the USER or ADMIN role.",
                security = @OpenApiSecurity(name = "BearerAuth"),
                requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = LocationRequestDTO.class), required = true),
                responses = {
                        @OpenApiResponse(status = "201", description = "Location created.", content = @OpenApiContent(from = LocationDTO.class)),
                        @OpenApiResponse(status = "400", description = "Invalid location data."),
                        @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                        @OpenApiResponse(status = "409", description = "A database constraint was violated."),
                        @OpenApiResponse(status = "500", description = "Unexpected server error.")
                })
        public void create(Context ctx) {
            super.create(ctx);
        }

        @Override
        @OpenApi(path = "/locations/{id}", methods = HttpMethod.PUT, operationId = "updateLocation",
                summary = "Update a location", tags = "Locations",
                description = "Requires a valid bearer token and the USER or ADMIN role.",
                security = @OpenApiSecurity(name = "BearerAuth"),
                pathParams = @OpenApiParam(name = "id", type = Long.class, required = true, description = "Location ID."),
                requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = LocationRequestDTO.class), required = true),
                responses = {
                        @OpenApiResponse(status = "200", description = "Updated location.", content = @OpenApiContent(from = LocationDTO.class)),
                        @OpenApiResponse(status = "400", description = "Invalid ID or location data."),
                        @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                        @OpenApiResponse(status = "404", description = "Location not found."),
                        @OpenApiResponse(status = "409", description = "A database constraint was violated."),
                        @OpenApiResponse(status = "500", description = "Unexpected server error.")
                })
        public void update(Context ctx) {
            super.update(ctx);
        }

        @Override
        @OpenApi(path = "/locations/{id}", methods = HttpMethod.DELETE, operationId = "deleteLocation",
                summary = "Delete a location", tags = "Locations",
                description = "Requires a valid bearer token and the USER or ADMIN role.",
                security = @OpenApiSecurity(name = "BearerAuth"),
                pathParams = @OpenApiParam(name = "id", type = Long.class, required = true, description = "Location ID."),
                responses = {
                        @OpenApiResponse(status = "204", description = "Location deleted."),
                        @OpenApiResponse(status = "400", description = "The ID is not a valid integer."),
                        @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                        @OpenApiResponse(status = "404", description = "Location not found."),
                        @OpenApiResponse(status = "409", description = "A database constraint prevents deletion."),
                        @OpenApiResponse(status = "500", description = "Unexpected server error.")
                })
        public void delete(Context ctx) {
            super.delete(ctx);
        }
    }

    public static final class Families extends CrudController<FamilyDTO, FamilyRequestDTO> {
        public Families(CrudService<FamilyDTO, FamilyRequestDTO> service) {
            super(service, FamilyRequestDTO.class);
        }

        @Override
        @OpenApi(path = "/families", methods = HttpMethod.GET, operationId = "listFamilies",
                summary = "List families", tags = "Families",
                description = "Requires a valid bearer token and the USER or ADMIN role. The familyName filter uses case-insensitive contains matching.",
                security = @OpenApiSecurity(name = "BearerAuth"),
                queryParams = {
                        @OpenApiParam(name = "page", type = Integer.class, description = "Zero-based page index; defaults to 0."),
                        @OpenApiParam(name = "size", type = Integer.class, description = "Page size from 1 to 100; defaults to 20."),
                        @OpenApiParam(name = "sort", description = "Sort field and direction as field,asc|desc. Fields: id, familyName; defaults to id,asc."),
                        @OpenApiParam(name = "familyName", description = "Optional substring filter.")
                },
                responses = {
                        @OpenApiResponse(status = "200", description = "Paginated family results.", content = @OpenApiContent(mimeType = "application/json", type = "object", properties = {
                                @OpenApiContentProperty(name = "data", from = FamilyDTO.class, isArray = true),
                                @OpenApiContentProperty(name = "page", type = "integer", format = "int32"),
                                @OpenApiContentProperty(name = "size", type = "integer", format = "int32"),
                                @OpenApiContentProperty(name = "totalElements", type = "integer", format = "int64"),
                                @OpenApiContentProperty(name = "totalPages", type = "integer", format = "int64")
                        })),
                        @OpenApiResponse(status = "400", description = "Invalid pagination, sort, or filter parameter."),
                        @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                        @OpenApiResponse(status = "500", description = "Unexpected server error.")
                })
        public void getAll(Context ctx) {
            super.getAll(ctx);
        }

        @Override
        @OpenApi(path = "/families/{id}", methods = HttpMethod.GET, operationId = "getFamily",
                summary = "Get a family", tags = "Families",
                description = "Requires a valid bearer token and the USER or ADMIN role.",
                security = @OpenApiSecurity(name = "BearerAuth"),
                pathParams = @OpenApiParam(name = "id", type = Long.class, required = true, description = "Family ID."),
                responses = {
                        @OpenApiResponse(status = "200", description = "Family details.", content = @OpenApiContent(from = FamilyDTO.class)),
                        @OpenApiResponse(status = "400", description = "The ID is not a valid integer."),
                        @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                        @OpenApiResponse(status = "404", description = "Family not found."),
                        @OpenApiResponse(status = "500", description = "Unexpected server error.")
                })
        public void getById(Context ctx) {
            super.getById(ctx);
        }

        @Override
        @OpenApi(path = "/families", methods = HttpMethod.POST, operationId = "createFamily",
                summary = "Create a family", tags = "Families",
                description = "Requires a valid bearer token and the USER or ADMIN role.",
                security = @OpenApiSecurity(name = "BearerAuth"),
                requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = FamilyRequestDTO.class), required = true),
                responses = {
                        @OpenApiResponse(status = "201", description = "Family created.", content = @OpenApiContent(from = FamilyDTO.class)),
                        @OpenApiResponse(status = "400", description = "Invalid family data."),
                        @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                        @OpenApiResponse(status = "409", description = "A database constraint was violated."),
                        @OpenApiResponse(status = "500", description = "Unexpected server error.")
                })
        public void create(Context ctx) {
            super.create(ctx);
        }

        @Override
        @OpenApi(path = "/families/{id}", methods = HttpMethod.PUT, operationId = "updateFamily",
                summary = "Update a family", tags = "Families",
                description = "Requires a valid bearer token and the USER or ADMIN role.",
                security = @OpenApiSecurity(name = "BearerAuth"),
                pathParams = @OpenApiParam(name = "id", type = Long.class, required = true, description = "Family ID."),
                requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = FamilyRequestDTO.class), required = true),
                responses = {
                        @OpenApiResponse(status = "200", description = "Updated family.", content = @OpenApiContent(from = FamilyDTO.class)),
                        @OpenApiResponse(status = "400", description = "Invalid ID or family data."),
                        @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                        @OpenApiResponse(status = "404", description = "Family not found."),
                        @OpenApiResponse(status = "409", description = "A database constraint was violated."),
                        @OpenApiResponse(status = "500", description = "Unexpected server error.")
                })
        public void update(Context ctx) {
            super.update(ctx);
        }

        @Override
        @OpenApi(path = "/families/{id}", methods = HttpMethod.DELETE, operationId = "deleteFamily",
                summary = "Delete a family", tags = "Families",
                description = "Requires a valid bearer token and the USER or ADMIN role.",
                security = @OpenApiSecurity(name = "BearerAuth"),
                pathParams = @OpenApiParam(name = "id", type = Long.class, required = true, description = "Family ID."),
                responses = {
                        @OpenApiResponse(status = "204", description = "Family deleted."),
                        @OpenApiResponse(status = "400", description = "The ID is not a valid integer."),
                        @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                        @OpenApiResponse(status = "404", description = "Family not found."),
                        @OpenApiResponse(status = "409", description = "A database constraint prevents deletion."),
                        @OpenApiResponse(status = "500", description = "Unexpected server error.")
                })
        public void delete(Context ctx) {
            super.delete(ctx);
        }
    }

    public static final class Housings extends CrudController<HousingDTO, HousingRequestDTO> {
        public Housings(CrudService<HousingDTO, HousingRequestDTO> service) {
            super(service, HousingRequestDTO.class);
        }

        @Override
        @OpenApi(path = "/housings", methods = HttpMethod.GET, operationId = "listHousings",
                summary = "List housing units", tags = "Housing",
                description = "Requires a valid bearer token and the USER or ADMIN role. String filters use case-insensitive contains matching; capacity ranges are inclusive.",
                security = @OpenApiSecurity(name = "BearerAuth"),
                queryParams = {
                        @OpenApiParam(name = "page", type = Integer.class, description = "Zero-based page index; defaults to 0."),
                        @OpenApiParam(name = "size", type = Integer.class, description = "Page size from 1 to 100; defaults to 20."),
                        @OpenApiParam(name = "sort", description = "Sort field and direction as field,asc|desc. Fields: id, locationId, housingType, capacity; defaults to id,asc."),
                        @OpenApiParam(name = "locationId", type = Long.class, description = "Filter by location ID."),
                        @OpenApiParam(name = "housingType", description = "Filter by housing type."),
                        @OpenApiParam(name = "capacityFrom", type = Integer.class, description = "Inclusive minimum capacity."),
                        @OpenApiParam(name = "capacityTo", type = Integer.class, description = "Inclusive maximum capacity.")
                },
                responses = {
                        @OpenApiResponse(status = "200", description = "Paginated housing results.", content = @OpenApiContent(mimeType = "application/json", type = "object", properties = {
                                @OpenApiContentProperty(name = "data", from = HousingDTO.class, isArray = true),
                                @OpenApiContentProperty(name = "page", type = "integer", format = "int32"),
                                @OpenApiContentProperty(name = "size", type = "integer", format = "int32"),
                                @OpenApiContentProperty(name = "totalElements", type = "integer", format = "int64"),
                                @OpenApiContentProperty(name = "totalPages", type = "integer", format = "int64")
                        })),
                        @OpenApiResponse(status = "400", description = "Invalid pagination, sort, or filter parameter."),
                        @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                        @OpenApiResponse(status = "500", description = "Unexpected server error.")
                })
        public void getAll(Context ctx) {
            super.getAll(ctx);
        }

        @Override
        @OpenApi(path = "/housings/{id}", methods = HttpMethod.GET, operationId = "getHousing",
                summary = "Get a housing unit", tags = "Housing",
                description = "Requires a valid bearer token and the USER or ADMIN role.",
                security = @OpenApiSecurity(name = "BearerAuth"),
                pathParams = @OpenApiParam(name = "id", type = Long.class, required = true, description = "Housing unit ID."),
                responses = {
                        @OpenApiResponse(status = "200", description = "Housing unit details.", content = @OpenApiContent(from = HousingDTO.class)),
                        @OpenApiResponse(status = "400", description = "The ID is not a valid integer."),
                        @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                        @OpenApiResponse(status = "404", description = "Housing unit not found."),
                        @OpenApiResponse(status = "500", description = "Unexpected server error.")
                })
        public void getById(Context ctx) {
            super.getById(ctx);
        }

        @Override
        @OpenApi(path = "/housings", methods = HttpMethod.POST, operationId = "createHousing",
                summary = "Create a housing unit", tags = "Housing",
                description = "Requires a valid bearer token and the USER or ADMIN role.",
                security = @OpenApiSecurity(name = "BearerAuth"),
                requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = HousingRequestDTO.class), required = true),
                responses = {
                        @OpenApiResponse(status = "201", description = "Housing unit created.", content = @OpenApiContent(from = HousingDTO.class)),
                        @OpenApiResponse(status = "400", description = "Invalid housing data or location reference."),
                        @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                        @OpenApiResponse(status = "409", description = "A database constraint was violated."),
                        @OpenApiResponse(status = "500", description = "Unexpected server error.")
                })
        public void create(Context ctx) {
            super.create(ctx);
        }

        @Override
        @OpenApi(path = "/housings/{id}", methods = HttpMethod.PUT, operationId = "updateHousing",
                summary = "Update a housing unit", tags = "Housing",
                description = "Requires a valid bearer token and the USER or ADMIN role.",
                security = @OpenApiSecurity(name = "BearerAuth"),
                pathParams = @OpenApiParam(name = "id", type = Long.class, required = true, description = "Housing unit ID."),
                requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = HousingRequestDTO.class), required = true),
                responses = {
                        @OpenApiResponse(status = "200", description = "Updated housing unit.", content = @OpenApiContent(from = HousingDTO.class)),
                        @OpenApiResponse(status = "400", description = "Invalid ID, housing data, or location reference."),
                        @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                        @OpenApiResponse(status = "404", description = "Housing unit not found."),
                        @OpenApiResponse(status = "409", description = "A database constraint was violated."),
                        @OpenApiResponse(status = "500", description = "Unexpected server error.")
                })
        public void update(Context ctx) {
            super.update(ctx);
        }

        @Override
        @OpenApi(path = "/housings/{id}", methods = HttpMethod.DELETE, operationId = "deleteHousing",
                summary = "Delete a housing unit", tags = "Housing",
                description = "Requires a valid bearer token and the USER or ADMIN role.",
                security = @OpenApiSecurity(name = "BearerAuth"),
                pathParams = @OpenApiParam(name = "id", type = Long.class, required = true, description = "Housing unit ID."),
                responses = {
                        @OpenApiResponse(status = "204", description = "Housing unit deleted."),
                        @OpenApiResponse(status = "400", description = "The ID is not a valid integer."),
                        @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                        @OpenApiResponse(status = "404", description = "Housing unit not found."),
                        @OpenApiResponse(status = "409", description = "A database constraint prevents deletion."),
                        @OpenApiResponse(status = "500", description = "Unexpected server error.")
                })
        public void delete(Context ctx) {
            super.delete(ctx);
        }
    }

    public static final class Citizens extends CrudController<CitizenDTO, CitizenRequestDTO> {
        public Citizens(CrudService<CitizenDTO, CitizenRequestDTO> service) {
            super(service, CitizenRequestDTO.class);
        }

        @Override
        @OpenApi(path = "/citizens", methods = HttpMethod.GET, operationId = "listCitizens",
                summary = "List citizens", tags = "Citizens",
                description = "Requires a valid bearer token and the USER or ADMIN role. lastName uses case-insensitive contains matching; date ranges are inclusive.",
                security = @OpenApiSecurity(name = "BearerAuth"),
                queryParams = {
                        @OpenApiParam(name = "page", type = Integer.class, description = "Zero-based page index; defaults to 0."),
                        @OpenApiParam(name = "size", type = Integer.class, description = "Page size from 1 to 100; defaults to 20."),
                        @OpenApiParam(name = "sort", description = "Sort field and direction as field,asc|desc. Fields: id, firstName, lastName, birthDate, deathDate, status, gender, familyId, housingId; defaults to id,asc."),
                        @OpenApiParam(name = "status", description = "Filter by status."),
                        @OpenApiParam(name = "lastName", description = "Optional substring filter."),
                        @OpenApiParam(name = "birthDateFrom", description = "Inclusive lower date bound (YYYY-MM-DD)."),
                        @OpenApiParam(name = "birthDateTo", description = "Inclusive upper date bound (YYYY-MM-DD)."),
                        @OpenApiParam(name = "familyId", type = Long.class, description = "Filter by family ID."),
                        @OpenApiParam(name = "housingId", type = Long.class, description = "Filter by housing unit ID.")
                },
                responses = {
                        @OpenApiResponse(status = "200", description = "Paginated citizen results.", content = @OpenApiContent(mimeType = "application/json", type = "object", properties = {
                                @OpenApiContentProperty(name = "data", from = CitizenDTO.class, isArray = true),
                                @OpenApiContentProperty(name = "page", type = "integer", format = "int32"),
                                @OpenApiContentProperty(name = "size", type = "integer", format = "int32"),
                                @OpenApiContentProperty(name = "totalElements", type = "integer", format = "int64"),
                                @OpenApiContentProperty(name = "totalPages", type = "integer", format = "int64")
                        })),
                        @OpenApiResponse(status = "400", description = "Invalid pagination, sort, or filter parameter."),
                        @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                        @OpenApiResponse(status = "500", description = "Unexpected server error.")
                })
        public void getAll(Context ctx) {
            super.getAll(ctx);
        }

        @Override
        @OpenApi(path = "/citizens/{id}", methods = HttpMethod.GET, operationId = "getCitizen",
                summary = "Get a citizen", tags = "Citizens",
                description = "Requires a valid bearer token and the USER or ADMIN role.",
                security = @OpenApiSecurity(name = "BearerAuth"),
                pathParams = @OpenApiParam(name = "id", type = Long.class, required = true, description = "Citizen ID."),
                responses = {
                        @OpenApiResponse(status = "200", description = "Citizen details.", content = @OpenApiContent(from = CitizenDTO.class)),
                        @OpenApiResponse(status = "400", description = "The ID is not a valid integer."),
                        @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                        @OpenApiResponse(status = "404", description = "Citizen not found."),
                        @OpenApiResponse(status = "500", description = "Unexpected server error.")
                })
        public void getById(Context ctx) {
            super.getById(ctx);
        }

        @Override
        @OpenApi(path = "/citizens", methods = HttpMethod.POST, operationId = "createCitizen",
                summary = "Create a citizen", tags = "Citizens",
                description = "Requires a valid bearer token and the USER or ADMIN role.",
                security = @OpenApiSecurity(name = "BearerAuth"),
                requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = CitizenRequestDTO.class), required = true),
                responses = {
                        @OpenApiResponse(status = "201", description = "Citizen created.", content = @OpenApiContent(from = CitizenDTO.class)),
                        @OpenApiResponse(status = "400", description = "Invalid citizen data or family/housing reference."),
                        @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                        @OpenApiResponse(status = "409", description = "A database constraint was violated."),
                        @OpenApiResponse(status = "500", description = "Unexpected server error.")
                })
        public void create(Context ctx) {
            super.create(ctx);
        }

        @Override
        @OpenApi(path = "/citizens/{id}", methods = HttpMethod.PUT, operationId = "updateCitizen",
                summary = "Update a citizen", tags = "Citizens",
                description = "Requires a valid bearer token and the USER or ADMIN role.",
                security = @OpenApiSecurity(name = "BearerAuth"),
                pathParams = @OpenApiParam(name = "id", type = Long.class, required = true, description = "Citizen ID."),
                requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = CitizenRequestDTO.class), required = true),
                responses = {
                        @OpenApiResponse(status = "200", description = "Updated citizen.", content = @OpenApiContent(from = CitizenDTO.class)),
                        @OpenApiResponse(status = "400", description = "Invalid ID, citizen data, or family/housing reference."),
                        @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                        @OpenApiResponse(status = "404", description = "Citizen not found."),
                        @OpenApiResponse(status = "409", description = "A database constraint was violated."),
                        @OpenApiResponse(status = "500", description = "Unexpected server error.")
                })
        public void update(Context ctx) {
            super.update(ctx);
        }

        @Override
        @OpenApi(path = "/citizens/{id}", methods = HttpMethod.DELETE, operationId = "deleteCitizen",
                summary = "Delete a citizen", tags = "Citizens",
                description = "Requires a valid bearer token and the USER or ADMIN role.",
                security = @OpenApiSecurity(name = "BearerAuth"),
                pathParams = @OpenApiParam(name = "id", type = Long.class, required = true, description = "Citizen ID."),
                responses = {
                        @OpenApiResponse(status = "204", description = "Citizen deleted."),
                        @OpenApiResponse(status = "400", description = "The ID is not a valid integer."),
                        @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                        @OpenApiResponse(status = "404", description = "Citizen not found."),
                        @OpenApiResponse(status = "409", description = "A database constraint prevents deletion."),
                        @OpenApiResponse(status = "500", description = "Unexpected server error.")
                })
        public void delete(Context ctx) {
            super.delete(ctx);
        }
    }

    public static final class CitizenRelationships extends CrudController<CitizenRelationshipDTO, CitizenRelationshipRequestDTO> {
        public CitizenRelationships(CrudService<CitizenRelationshipDTO, CitizenRelationshipRequestDTO> service) {
            super(service, CitizenRelationshipRequestDTO.class);
        }

        @Override
        @OpenApi(path = "/citizen-relationships", methods = HttpMethod.GET, operationId = "listCitizenRelationships",
                summary = "List citizen relationships", tags = "Citizens",
                description = "Requires a valid bearer token and the USER or ADMIN role. relationshipType uses case-insensitive contains matching; date ranges are inclusive.",
                security = @OpenApiSecurity(name = "BearerAuth"),
                queryParams = {
                        @OpenApiParam(name = "page", type = Integer.class, description = "Zero-based page index; defaults to 0."),
                        @OpenApiParam(name = "size", type = Integer.class, description = "Page size from 1 to 100; defaults to 20."),
                        @OpenApiParam(name = "sort", description = "Sort field and direction as field,asc|desc. Fields: id, citizenId1, citizenId2, relationshipType, startDate, endDate; defaults to id,asc."),
                        @OpenApiParam(name = "citizenId1", type = Long.class, description = "Filter by first citizen ID."),
                        @OpenApiParam(name = "citizenId2", type = Long.class, description = "Filter by second citizen ID."),
                        @OpenApiParam(name = "relationshipType", description = "Optional substring filter."),
                        @OpenApiParam(name = "startDateFrom", description = "Inclusive lower date bound (YYYY-MM-DD)."),
                        @OpenApiParam(name = "startDateTo", description = "Inclusive upper date bound (YYYY-MM-DD)."),
                        @OpenApiParam(name = "endDateFrom", description = "Inclusive lower date bound (YYYY-MM-DD)."),
                        @OpenApiParam(name = "endDateTo", description = "Inclusive upper date bound (YYYY-MM-DD).")
                },
                responses = {
                        @OpenApiResponse(status = "200", description = "Paginated relationship results.", content = @OpenApiContent(mimeType = "application/json", type = "object", properties = {
                                @OpenApiContentProperty(name = "data", from = CitizenRelationshipDTO.class, isArray = true),
                                @OpenApiContentProperty(name = "page", type = "integer", format = "int32"),
                                @OpenApiContentProperty(name = "size", type = "integer", format = "int32"),
                                @OpenApiContentProperty(name = "totalElements", type = "integer", format = "int64"),
                                @OpenApiContentProperty(name = "totalPages", type = "integer", format = "int64")
                        })),
                        @OpenApiResponse(status = "400", description = "Invalid pagination, sort, or filter parameter."),
                        @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                        @OpenApiResponse(status = "500", description = "Unexpected server error.")
                })
        public void getAll(Context ctx) {
            super.getAll(ctx);
        }

        @Override
        @OpenApi(path = "/citizen-relationships/{id}", methods = HttpMethod.GET, operationId = "getCitizenRelationship",
                summary = "Get a citizen relationship", tags = "Citizens",
                description = "Requires a valid bearer token and the USER or ADMIN role.",
                security = @OpenApiSecurity(name = "BearerAuth"),
                pathParams = @OpenApiParam(name = "id", type = Long.class, required = true, description = "Relationship ID."),
                responses = {
                        @OpenApiResponse(status = "200", description = "Relationship details.", content = @OpenApiContent(from = CitizenRelationshipDTO.class)),
                        @OpenApiResponse(status = "400", description = "The ID is not a valid integer."),
                        @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                        @OpenApiResponse(status = "404", description = "Relationship not found."),
                        @OpenApiResponse(status = "500", description = "Unexpected server error.")
                })
        public void getById(Context ctx) {
            super.getById(ctx);
        }

        @Override
        @OpenApi(path = "/citizen-relationships", methods = HttpMethod.POST, operationId = "createCitizenRelationship",
                summary = "Create a citizen relationship", tags = "Citizens",
                description = "Requires a valid bearer token and the USER or ADMIN role.",
                security = @OpenApiSecurity(name = "BearerAuth"),
                requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = CitizenRelationshipRequestDTO.class), required = true),
                responses = {
                        @OpenApiResponse(status = "201", description = "Relationship created.", content = @OpenApiContent(from = CitizenRelationshipDTO.class)),
                        @OpenApiResponse(status = "400", description = "Invalid relationship data or citizen references."),
                        @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                        @OpenApiResponse(status = "409", description = "A database constraint was violated."),
                        @OpenApiResponse(status = "500", description = "Unexpected server error.")
                })
        public void create(Context ctx) {
            super.create(ctx);
        }

        @Override
        @OpenApi(path = "/citizen-relationships/{id}", methods = HttpMethod.PUT, operationId = "updateCitizenRelationship",
                summary = "Update a citizen relationship", tags = "Citizens",
                description = "Requires a valid bearer token and the USER or ADMIN role.",
                security = @OpenApiSecurity(name = "BearerAuth"),
                pathParams = @OpenApiParam(name = "id", type = Long.class, required = true, description = "Relationship ID."),
                requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = CitizenRelationshipRequestDTO.class), required = true),
                responses = {
                        @OpenApiResponse(status = "200", description = "Updated relationship.", content = @OpenApiContent(from = CitizenRelationshipDTO.class)),
                        @OpenApiResponse(status = "400", description = "Invalid ID, relationship data, or citizen references."),
                        @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                        @OpenApiResponse(status = "404", description = "Relationship not found."),
                        @OpenApiResponse(status = "409", description = "A database constraint was violated."),
                        @OpenApiResponse(status = "500", description = "Unexpected server error.")
                })
        public void update(Context ctx) {
            super.update(ctx);
        }

        @Override
        @OpenApi(path = "/citizen-relationships/{id}", methods = HttpMethod.DELETE, operationId = "deleteCitizenRelationship",
                summary = "Delete a citizen relationship", tags = "Citizens",
                description = "Requires a valid bearer token and the USER or ADMIN role.",
                security = @OpenApiSecurity(name = "BearerAuth"),
                pathParams = @OpenApiParam(name = "id", type = Long.class, required = true, description = "Relationship ID."),
                responses = {
                        @OpenApiResponse(status = "204", description = "Relationship deleted."),
                        @OpenApiResponse(status = "400", description = "The ID is not a valid integer."),
                        @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                        @OpenApiResponse(status = "404", description = "Relationship not found."),
                        @OpenApiResponse(status = "409", description = "A database constraint prevents deletion."),
                        @OpenApiResponse(status = "500", description = "Unexpected server error.")
                })
        public void delete(Context ctx) {
            super.delete(ctx);
        }
    }

    public static final class Institutions extends CrudController<InstitutionDTO, InstitutionRequestDTO> {
        public Institutions(CrudService<InstitutionDTO, InstitutionRequestDTO> service) {
            super(service, InstitutionRequestDTO.class);
        }

        @Override
        @OpenApi(path = "/institutions", methods = HttpMethod.GET, operationId = "listInstitutions",
                summary = "List institutions", tags = "Institutions",
                description = "Requires a valid bearer token and the USER or ADMIN role. String filters use case-insensitive contains matching.",
                security = @OpenApiSecurity(name = "BearerAuth"),
                queryParams = {
                        @OpenApiParam(name = "page", type = Integer.class, description = "Zero-based page index; defaults to 0."),
                        @OpenApiParam(name = "size", type = Integer.class, description = "Page size from 1 to 100; defaults to 20."),
                        @OpenApiParam(name = "sort", description = "Sort field and direction as field,asc|desc. Fields: id, locationId, name, type; defaults to id,asc."),
                        @OpenApiParam(name = "locationId", type = Long.class, description = "Filter by location ID."),
                        @OpenApiParam(name = "name", description = "Optional substring filter."),
                        @OpenApiParam(name = "type", description = "Optional substring filter.")
                },
                responses = {
                        @OpenApiResponse(status = "200", description = "Paginated institution results.", content = @OpenApiContent(mimeType = "application/json", type = "object", properties = {
                                @OpenApiContentProperty(name = "data", from = InstitutionDTO.class, isArray = true),
                                @OpenApiContentProperty(name = "page", type = "integer", format = "int32"),
                                @OpenApiContentProperty(name = "size", type = "integer", format = "int32"),
                                @OpenApiContentProperty(name = "totalElements", type = "integer", format = "int64"),
                                @OpenApiContentProperty(name = "totalPages", type = "integer", format = "int64")
                        })),
                        @OpenApiResponse(status = "400", description = "Invalid pagination, sort, or filter parameter."),
                        @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                        @OpenApiResponse(status = "500", description = "Unexpected server error.")
                })
        public void getAll(Context ctx) {
            super.getAll(ctx);
        }

        @Override
        @OpenApi(path = "/institutions/{id}", methods = HttpMethod.GET, operationId = "getInstitution",
                summary = "Get an institution", tags = "Institutions",
                description = "Requires a valid bearer token and the USER or ADMIN role.",
                security = @OpenApiSecurity(name = "BearerAuth"),
                pathParams = @OpenApiParam(name = "id", type = Long.class, required = true, description = "Institution ID."),
                responses = {
                        @OpenApiResponse(status = "200", description = "Institution details.", content = @OpenApiContent(from = InstitutionDTO.class)),
                        @OpenApiResponse(status = "400", description = "The ID is not a valid integer."),
                        @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                        @OpenApiResponse(status = "404", description = "Institution not found."),
                        @OpenApiResponse(status = "500", description = "Unexpected server error.")
                })
        public void getById(Context ctx) {
            super.getById(ctx);
        }

        @Override
        @OpenApi(path = "/institutions", methods = HttpMethod.POST, operationId = "createInstitution",
                summary = "Create an institution", tags = "Institutions",
                description = "Requires a valid bearer token and the USER or ADMIN role.",
                security = @OpenApiSecurity(name = "BearerAuth"),
                requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = InstitutionRequestDTO.class), required = true),
                responses = {
                        @OpenApiResponse(status = "201", description = "Institution created.", content = @OpenApiContent(from = InstitutionDTO.class)),
                        @OpenApiResponse(status = "400", description = "Invalid institution data or location reference."),
                        @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                        @OpenApiResponse(status = "409", description = "A database constraint was violated."),
                        @OpenApiResponse(status = "500", description = "Unexpected server error.")
                })
        public void create(Context ctx) {
            super.create(ctx);
        }

        @Override
        @OpenApi(path = "/institutions/{id}", methods = HttpMethod.PUT, operationId = "updateInstitution",
                summary = "Update an institution", tags = "Institutions",
                description = "Requires a valid bearer token and the USER or ADMIN role.",
                security = @OpenApiSecurity(name = "BearerAuth"),
                pathParams = @OpenApiParam(name = "id", type = Long.class, required = true, description = "Institution ID."),
                requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = InstitutionRequestDTO.class), required = true),
                responses = {
                        @OpenApiResponse(status = "200", description = "Updated institution.", content = @OpenApiContent(from = InstitutionDTO.class)),
                        @OpenApiResponse(status = "400", description = "Invalid ID, institution data, or location reference."),
                        @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                        @OpenApiResponse(status = "404", description = "Institution not found."),
                        @OpenApiResponse(status = "409", description = "A database constraint was violated."),
                        @OpenApiResponse(status = "500", description = "Unexpected server error.")
                })
        public void update(Context ctx) {
            super.update(ctx);
        }

        @Override
        @OpenApi(path = "/institutions/{id}", methods = HttpMethod.DELETE, operationId = "deleteInstitution",
                summary = "Delete an institution", tags = "Institutions",
                description = "Requires a valid bearer token and the USER or ADMIN role.",
                security = @OpenApiSecurity(name = "BearerAuth"),
                pathParams = @OpenApiParam(name = "id", type = Long.class, required = true, description = "Institution ID."),
                responses = {
                        @OpenApiResponse(status = "204", description = "Institution deleted."),
                        @OpenApiResponse(status = "400", description = "The ID is not a valid integer."),
                        @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                        @OpenApiResponse(status = "404", description = "Institution not found."),
                        @OpenApiResponse(status = "409", description = "A database constraint prevents deletion."),
                        @OpenApiResponse(status = "500", description = "Unexpected server error.")
                })
        public void delete(Context ctx) {
            super.delete(ctx);
        }
    }

    public static final class Companies extends CrudController<CompanyDTO, CompanyRequestDTO> {
        public Companies(CrudService<CompanyDTO, CompanyRequestDTO> service) {
            super(service, CompanyRequestDTO.class);
        }

        @Override
        @OpenApi(path = "/companies", methods = HttpMethod.GET, operationId = "listCompanies",
                summary = "List companies", tags = "Companies",
                description = "Requires a valid bearer token and the USER or ADMIN role. name uses case-insensitive contains matching; founded-date ranges are inclusive.",
                security = @OpenApiSecurity(name = "BearerAuth"),
                queryParams = {
                        @OpenApiParam(name = "page", type = Integer.class, description = "Zero-based page index; defaults to 0."),
                        @OpenApiParam(name = "size", type = Integer.class, description = "Page size from 1 to 100; defaults to 20."),
                        @OpenApiParam(name = "sort", description = "Sort field and direction as field,asc|desc. Fields: id, locationId, name, foundedDate, dissolvedDate, status; defaults to id,asc."),
                        @OpenApiParam(name = "locationId", type = Long.class, description = "Filter by location ID."),
                        @OpenApiParam(name = "name", description = "Optional substring filter."),
                        @OpenApiParam(name = "status", description = "Filter by status."),
                        @OpenApiParam(name = "foundedDateFrom", description = "Inclusive lower date bound (YYYY-MM-DD)."),
                        @OpenApiParam(name = "foundedDateTo", description = "Inclusive upper date bound (YYYY-MM-DD).")
                },
                responses = {
                        @OpenApiResponse(status = "200", description = "Paginated company results.", content = @OpenApiContent(mimeType = "application/json", type = "object", properties = {
                                @OpenApiContentProperty(name = "data", from = CompanyDTO.class, isArray = true),
                                @OpenApiContentProperty(name = "page", type = "integer", format = "int32"),
                                @OpenApiContentProperty(name = "size", type = "integer", format = "int32"),
                                @OpenApiContentProperty(name = "totalElements", type = "integer", format = "int64"),
                                @OpenApiContentProperty(name = "totalPages", type = "integer", format = "int64")
                        })),
                        @OpenApiResponse(status = "400", description = "Invalid pagination, sort, or filter parameter."),
                        @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                        @OpenApiResponse(status = "500", description = "Unexpected server error.")
                })
        public void getAll(Context ctx) {
            super.getAll(ctx);
        }

        @Override
        @OpenApi(path = "/companies/{id}", methods = HttpMethod.GET, operationId = "getCompany",
                summary = "Get a company", tags = "Companies",
                description = "Requires a valid bearer token and the USER or ADMIN role.",
                security = @OpenApiSecurity(name = "BearerAuth"),
                pathParams = @OpenApiParam(name = "id", type = Long.class, required = true, description = "Company ID."),
                responses = {
                        @OpenApiResponse(status = "200", description = "Company details.", content = @OpenApiContent(from = CompanyDTO.class)),
                        @OpenApiResponse(status = "400", description = "The ID is not a valid integer."),
                        @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                        @OpenApiResponse(status = "404", description = "Company not found."),
                        @OpenApiResponse(status = "500", description = "Unexpected server error.")
                })
        public void getById(Context ctx) {
            super.getById(ctx);
        }

        @Override
        @OpenApi(path = "/companies", methods = HttpMethod.POST, operationId = "createCompany",
                summary = "Create a company", tags = "Companies",
                description = "Requires a valid bearer token and the USER or ADMIN role.",
                security = @OpenApiSecurity(name = "BearerAuth"),
                requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = CompanyRequestDTO.class), required = true),
                responses = {
                        @OpenApiResponse(status = "201", description = "Company created.", content = @OpenApiContent(from = CompanyDTO.class)),
                        @OpenApiResponse(status = "400", description = "Invalid company data or location reference."),
                        @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                        @OpenApiResponse(status = "409", description = "A database constraint was violated."),
                        @OpenApiResponse(status = "500", description = "Unexpected server error.")
                })
        public void create(Context ctx) {
            super.create(ctx);
        }

        @Override
        @OpenApi(path = "/companies/{id}", methods = HttpMethod.PUT, operationId = "updateCompany",
                summary = "Update a company", tags = "Companies",
                description = "Requires a valid bearer token and the USER or ADMIN role.",
                security = @OpenApiSecurity(name = "BearerAuth"),
                pathParams = @OpenApiParam(name = "id", type = Long.class, required = true, description = "Company ID."),
                requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = CompanyRequestDTO.class), required = true),
                responses = {
                        @OpenApiResponse(status = "200", description = "Updated company.", content = @OpenApiContent(from = CompanyDTO.class)),
                        @OpenApiResponse(status = "400", description = "Invalid ID, company data, or location reference."),
                        @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                        @OpenApiResponse(status = "404", description = "Company not found."),
                        @OpenApiResponse(status = "409", description = "A database constraint was violated."),
                        @OpenApiResponse(status = "500", description = "Unexpected server error.")
                })
        public void update(Context ctx) {
            super.update(ctx);
        }

        @Override
        @OpenApi(path = "/companies/{id}", methods = HttpMethod.DELETE, operationId = "deleteCompany",
                summary = "Delete a company", tags = "Companies",
                description = "Requires a valid bearer token and the USER or ADMIN role.",
                security = @OpenApiSecurity(name = "BearerAuth"),
                pathParams = @OpenApiParam(name = "id", type = Long.class, required = true, description = "Company ID."),
                responses = {
                        @OpenApiResponse(status = "204", description = "Company deleted."),
                        @OpenApiResponse(status = "400", description = "The ID is not a valid integer."),
                        @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                        @OpenApiResponse(status = "404", description = "Company not found."),
                        @OpenApiResponse(status = "409", description = "A database constraint prevents deletion."),
                        @OpenApiResponse(status = "500", description = "Unexpected server error.")
                })
        public void delete(Context ctx) {
            super.delete(ctx);
        }
    }

    public static final class EducationRecords extends CrudController<EducationRecordDTO, EducationRecordRequestDTO> {
        public EducationRecords(CrudService<EducationRecordDTO, EducationRecordRequestDTO> service) {
            super(service, EducationRecordRequestDTO.class);
        }

        @Override
        @OpenApi(path = "/education-records", methods = HttpMethod.GET, operationId = "listEducationRecords",
                summary = "List education records", tags = "Education",
                description = "Requires a valid bearer token and the USER or ADMIN role. Date ranges are inclusive.",
                security = @OpenApiSecurity(name = "BearerAuth"),
                queryParams = {
                        @OpenApiParam(name = "page", type = Integer.class, description = "Zero-based page index; defaults to 0."),
                        @OpenApiParam(name = "size", type = Integer.class, description = "Page size from 1 to 100; defaults to 20."),
                        @OpenApiParam(name = "sort", description = "Sort field and direction as field,asc|desc. Fields: id, citizenId, institutionId, startDate, endDate; defaults to id,asc."),
                        @OpenApiParam(name = "citizenId", type = Long.class, description = "Filter by citizen ID."),
                        @OpenApiParam(name = "institutionId", type = Long.class, description = "Filter by institution ID."),
                        @OpenApiParam(name = "startDateFrom", description = "Inclusive lower date bound (YYYY-MM-DD)."),
                        @OpenApiParam(name = "startDateTo", description = "Inclusive upper date bound (YYYY-MM-DD)."),
                        @OpenApiParam(name = "endDateFrom", description = "Inclusive lower date bound (YYYY-MM-DD)."),
                        @OpenApiParam(name = "endDateTo", description = "Inclusive upper date bound (YYYY-MM-DD).")
                },
                responses = {
                        @OpenApiResponse(status = "200", description = "Paginated education record results.", content = @OpenApiContent(mimeType = "application/json", type = "object", properties = {
                                @OpenApiContentProperty(name = "data", from = EducationRecordDTO.class, isArray = true),
                                @OpenApiContentProperty(name = "page", type = "integer", format = "int32"),
                                @OpenApiContentProperty(name = "size", type = "integer", format = "int32"),
                                @OpenApiContentProperty(name = "totalElements", type = "integer", format = "int64"),
                                @OpenApiContentProperty(name = "totalPages", type = "integer", format = "int64")
                        })),
                        @OpenApiResponse(status = "400", description = "Invalid pagination, sort, or filter parameter."),
                        @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                        @OpenApiResponse(status = "500", description = "Unexpected server error.")
                })
        public void getAll(Context ctx) {
            super.getAll(ctx);
        }

        @Override
        @OpenApi(path = "/education-records/{id}", methods = HttpMethod.GET, operationId = "getEducationRecord",
                summary = "Get an education record", tags = "Education",
                description = "Requires a valid bearer token and the USER or ADMIN role.",
                security = @OpenApiSecurity(name = "BearerAuth"),
                pathParams = @OpenApiParam(name = "id", type = Long.class, required = true, description = "Education record ID."),
                responses = {
                        @OpenApiResponse(status = "200", description = "Education record details.", content = @OpenApiContent(from = EducationRecordDTO.class)),
                        @OpenApiResponse(status = "400", description = "The ID is not a valid integer."),
                        @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                        @OpenApiResponse(status = "404", description = "Education record not found."),
                        @OpenApiResponse(status = "500", description = "Unexpected server error.")
                })
        public void getById(Context ctx) {
            super.getById(ctx);
        }

        @Override
        @OpenApi(path = "/education-records", methods = HttpMethod.POST, operationId = "createEducationRecord",
                summary = "Create an education record", tags = "Education",
                description = "Requires a valid bearer token and the USER or ADMIN role.",
                security = @OpenApiSecurity(name = "BearerAuth"),
                requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = EducationRecordRequestDTO.class), required = true),
                responses = {
                        @OpenApiResponse(status = "201", description = "Education record created.", content = @OpenApiContent(from = EducationRecordDTO.class)),
                        @OpenApiResponse(status = "400", description = "Invalid education data or citizen/institution references."),
                        @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                        @OpenApiResponse(status = "409", description = "A database constraint was violated."),
                        @OpenApiResponse(status = "500", description = "Unexpected server error.")
                })
        public void create(Context ctx) {
            super.create(ctx);
        }

        @Override
        @OpenApi(path = "/education-records/{id}", methods = HttpMethod.PUT, operationId = "updateEducationRecord",
                summary = "Update an education record", tags = "Education",
                description = "Requires a valid bearer token and the USER or ADMIN role.",
                security = @OpenApiSecurity(name = "BearerAuth"),
                pathParams = @OpenApiParam(name = "id", type = Long.class, required = true, description = "Education record ID."),
                requestBody = @OpenApiRequestBody(content = @OpenApiContent(from = EducationRecordRequestDTO.class), required = true),
                responses = {
                        @OpenApiResponse(status = "200", description = "Updated education record.", content = @OpenApiContent(from = EducationRecordDTO.class)),
                        @OpenApiResponse(status = "400", description = "Invalid ID, education data, or citizen/institution references."),
                        @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                        @OpenApiResponse(status = "404", description = "Education record not found."),
                        @OpenApiResponse(status = "409", description = "A database constraint was violated."),
                        @OpenApiResponse(status = "500", description = "Unexpected server error.")
                })
        public void update(Context ctx) {
            super.update(ctx);
        }

        @Override
        @OpenApi(path = "/education-records/{id}", methods = HttpMethod.DELETE, operationId = "deleteEducationRecord",
                summary = "Delete an education record", tags = "Education",
                description = "Requires a valid bearer token and the USER or ADMIN role.",
                security = @OpenApiSecurity(name = "BearerAuth"),
                pathParams = @OpenApiParam(name = "id", type = Long.class, required = true, description = "Education record ID."),
                responses = {
                        @OpenApiResponse(status = "204", description = "Education record deleted."),
                        @OpenApiResponse(status = "400", description = "The ID is not a valid integer."),
                        @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                        @OpenApiResponse(status = "404", description = "Education record not found."),
                        @OpenApiResponse(status = "409", description = "A database constraint prevents deletion."),
                        @OpenApiResponse(status = "500", description = "Unexpected server error.")
                })
        public void delete(Context ctx) {
            super.delete(ctx);
        }
    }
}
