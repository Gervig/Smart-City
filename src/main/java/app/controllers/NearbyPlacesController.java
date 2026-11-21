package app.controllers;

import app.services.NearbyPlacesService;
import io.javalin.http.Context;

public class NearbyPlacesController {
    private final NearbyPlacesService service;

    public NearbyPlacesController(NearbyPlacesService service) {
        this.service = service;
    }

    public void getNearby(Context ctx) {
        Long locationId = ctx.pathParamAsClass("id", Long.class).get();
        int radius = ctx.queryParamAsClass("radius", Integer.class).getOrDefault(1000);
        String amenity = ctx.queryParamAsClass("amenity", String.class).getOrDefault("hospital");
        ctx.json(service.getNearby(locationId, radius, amenity));
    }
}
