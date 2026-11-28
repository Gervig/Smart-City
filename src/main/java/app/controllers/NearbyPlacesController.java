package app.controllers;

import app.services.NearbyPlacesService;
import io.javalin.http.Context;
import io.javalin.openapi.HttpMethod;
import io.javalin.openapi.OpenApi;
import io.javalin.openapi.OpenApiContent;
import io.javalin.openapi.OpenApiParam;
import io.javalin.openapi.OpenApiResponse;
import io.javalin.openapi.OpenApiSecurity;

public class NearbyPlacesController {
    private final NearbyPlacesService service;

    public NearbyPlacesController(NearbyPlacesService service) {
        this.service = service;
    }

    @OpenApi(path = "/locations/{id}/nearby", methods = HttpMethod.GET, operationId = "getNearbyPlaces",
            summary = "Find nearby places", tags = "Nearby Places",
            description = "Requires a valid bearer token and the USER or ADMIN role. The default amenity is hospital; supported values are hospital, school, college, pharmacy, restaurant, cafe, library, and bank. Results may be returned from a 24-hour cache. An unavailable external service returns an empty result with enriched=false instead of an HTTP error.",
            security = @OpenApiSecurity(name = "BearerAuth"),
            pathParams = @OpenApiParam(name = "id", type = Long.class, required = true, description = "Location ID."),
            queryParams = {
                    @OpenApiParam(name = "radius", type = Integer.class, description = "Search radius in meters from 1 to 5000; defaults to 1000."),
                    @OpenApiParam(name = "amenity", description = "Place category; defaults to hospital.")
            },
            responses = {
                    @OpenApiResponse(status = "200", description = "Nearby place results and enrichment status.", content = @OpenApiContent(from = app.dtos.NearbyPlacesDTO.class)),
                    @OpenApiResponse(status = "400", description = "Invalid ID, radius, or unsupported amenity."),
                    @OpenApiResponse(status = "401", description = "Missing or invalid bearer token."),
                    @OpenApiResponse(status = "404", description = "Location not found."),
                    @OpenApiResponse(status = "500", description = "Unexpected server error.")
            })
    public void getNearby(Context ctx) {
        Long locationId = ctx.pathParamAsClass("id", Long.class).get();
        int radius = ctx.queryParamAsClass("radius", Integer.class).getOrDefault(1000);
        String amenity = ctx.queryParamAsClass("amenity", String.class).getOrDefault("hospital");
        ctx.json(service.getNearby(locationId, radius, amenity));
    }
}
