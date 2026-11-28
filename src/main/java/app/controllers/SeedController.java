package app.controllers;

import app.services.SeedReport;
import app.services.SeedService;
import io.javalin.http.Context;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiResponse;
import io.javalin.openapi.OpenApiSecurity;

public class SeedController
{
    private final SeedService seedService;

    public SeedController(SeedService seedService)
    {
        this.seedService = seedService;
    }

    @OpenApi(path = "/api/admin/seed/locations", methods = HttpMethod.POST, operationId = "seedLocations",
            summary = "Import locations from OpenStreetMap", tags = "Administration",
            description = "Requires a valid bearer token and the ADMIN role. The default bounding box is the Copenhagen area, amenity defaults to hospital, and limit defaults to 20.",
            security = @OpenApiSecurity(name = "BearerAuth"),
            queryParams = {
                    @OpenApiParam(name = "south", type = Double.class, description = "Southern latitude; defaults to 55.66."),
                    @OpenApiParam(name = "west", type = Double.class, description = "Western longitude; defaults to 12.55."),
                    @OpenApiParam(name = "north", type = Double.class, description = "Northern latitude; defaults to 55.70."),
                    @OpenApiParam(name = "east", type = Double.class, description = "Eastern longitude; defaults to 12.62."),
                    @OpenApiParam(name = "amenity", description = "OpenStreetMap amenity to import; defaults to hospital."),
                    @OpenApiParam(name = "limit", type = Integer.class, description = "Maximum imported matches from 1 to 200; defaults to 20.")
            },
            responses = {
                    @OpenApiResponse(status = "200", description = "Location import report.", content = @OpenApiContent(from = SeedReport.class)),
                    @OpenApiResponse(status = "400", description = "Invalid query parameter or limit."),
                    @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                    @OpenApiResponse(status = "403", description = "The authenticated user does not have the ADMIN role."),
                    @OpenApiResponse(status = "409", description = "A database constraint was violated."),
                    @OpenApiResponse(status = "500", description = "The external data service or server failed.")
            })
    public void seedLocations(Context ctx)
    {
        double south = decimal(ctx, "south", 55.66);
        double west = decimal(ctx, "west", 12.55);
        double north = decimal(ctx, "north", 55.70);
        double east = decimal(ctx, "east", 12.62);
        String amenity = ctx.queryParam("amenity");
        int limit = integer(ctx, "limit", 20);
        if (limit < 1 || limit > 200)
        {
            throw new IllegalArgumentException("limit must be between 1 and 200");
        }
        report(ctx, seedService.seedLocationsFromOsm(south, west, north, east,
                amenity == null ? "hospital" : amenity, limit));
    }

    @OpenApi(path = "/api/admin/seed/fake", methods = HttpMethod.POST, operationId = "seedFakeData",
            summary = "Generate fake city data", tags = "Administration",
            description = "Requires a valid bearer token and the ADMIN role. citizens defaults to 100 and must be from 1 to 1000. seed defaults to 42 and controls deterministic generation.",
            security = @OpenApiSecurity(name = "BearerAuth"),
            queryParams = {
                    @OpenApiParam(name = "citizens", type = Integer.class, description = "Number of generated citizens from 1 to 1000; defaults to 100."),
                    @OpenApiParam(name = "seed", type = Long.class, description = "Random generator seed; defaults to 42.")
            },
            responses = {
                    @OpenApiResponse(status = "200", description = "Fake data generation report.", content = @OpenApiContent(from = SeedReport.class)),
                    @OpenApiResponse(status = "400", description = "Invalid citizen count or no locations are available."),
                    @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                    @OpenApiResponse(status = "403", description = "The authenticated user does not have the ADMIN role."),
                    @OpenApiResponse(status = "409", description = "A database constraint was violated."),
                    @OpenApiResponse(status = "500", description = "Unexpected server error.")
            })
    public void seedFakeData(Context ctx)
    {
        int citizens = integer(ctx, "citizens", 100);
        long seed = longInteger(ctx, "seed", 42L);
        report(ctx, seedService.seedFakeData(citizens, seed));
    }

    @OpenApi(path = "/api/admin/seed/enrich", methods = HttpMethod.POST, operationId = "enrichFromOpenStreetMap",
            summary = "Enrich city data from OpenStreetMap", tags = "Administration",
            description = "Requires a valid bearer token and the ADMIN role. Enrichment may stop early when OpenStreetMap is unavailable; the response report indicates whether it completed.",
            security = @OpenApiSecurity(name = "BearerAuth"),
            responses = {
                    @OpenApiResponse(status = "200", description = "Enrichment report, including completion status.", content = @OpenApiContent(from = SeedReport.class)),
                    @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                    @OpenApiResponse(status = "403", description = "The authenticated user does not have the ADMIN role."),
                    @OpenApiResponse(status = "409", description = "A database constraint was violated."),
                    @OpenApiResponse(status = "500", description = "Unexpected server error.")
            })
    public void enrich(Context ctx)
    {
        report(ctx, seedService.enrichFromOsm());
    }

    private void report(Context ctx, SeedReport report)
    {
        ctx.json(report);
    }

    private double decimal(Context ctx, String name, double defaultValue)
    {
        String value = ctx.queryParam(name);
        if (value == null)
        {
            return defaultValue;
        }
        try
        {
            return Double.parseDouble(value);
        }
        catch (NumberFormatException exception)
        {
            throw new IllegalArgumentException(name + " must be a number", exception);
        }
    }

    private int integer(Context ctx, String name, int defaultValue)
    {
        String value = ctx.queryParam(name);
        if (value == null)
        {
            return defaultValue;
        }
        try
        {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException exception)
        {
            throw new IllegalArgumentException(name + " must be an integer", exception);
        }
    }

    private long longInteger(Context ctx, String name, long defaultValue)
    {
        String value = ctx.queryParam(name);
        if (value == null)
        {
            return defaultValue;
        }
        try
        {
            return Long.parseLong(value);
        }
        catch (NumberFormatException exception)
        {
            throw new IllegalArgumentException(name + " must be an integer", exception);
        }
    }
}
