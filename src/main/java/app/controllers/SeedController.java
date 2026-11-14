package app.controllers;

import app.services.SeedReport;
import app.services.SeedService;
import io.javalin.http.Context;

public class SeedController
{
    private final SeedService seedService;

    public SeedController(SeedService seedService)
    {
        this.seedService = seedService;
    }

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

    public void seedFakeData(Context ctx)
    {
        int citizens = integer(ctx, "citizens", 100);
        long seed = longInteger(ctx, "seed", 42L);
        report(ctx, seedService.seedFakeData(citizens, seed));
    }

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
