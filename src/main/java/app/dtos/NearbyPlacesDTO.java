package app.dtos;

import app.external.overpass.NearbyPlace;

import java.util.List;

public record NearbyPlacesDTO(Long locationId, List<NearbyPlace> places, boolean enriched, String message) {
}
