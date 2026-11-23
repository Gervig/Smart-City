package app.services;

import app.daos.GenericDAO;
import app.dtos.NearbyPlacesDTO;
import app.entities.Location;
import app.external.overpass.NearbyPlace;
import app.external.overpass.OverpassClient;
import app.external.overpass.OverpassException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class NearbyPlacesService {
    private static final Duration CACHE_DURATION = Duration.ofHours(24);
    private static final Set<String> ALLOWED_AMENITIES =
            Set.of("hospital", "school", "college", "pharmacy", "restaurant", "cafe", "library", "bank");
    private static final String UNAVAILABLE_MESSAGE = "Nearby places are currently unavailable";

    private final GenericDAO<Location> locationDAO;
    private final OverpassClient client;
    private final Clock clock;
    private final ConcurrentHashMap<CacheKey, CacheEntry> cache = new ConcurrentHashMap<>();

    public NearbyPlacesService(GenericDAO<Location> locationDAO, OverpassClient client, Clock clock) {
        this.locationDAO = locationDAO;
        this.client = client;
        this.clock = clock;
    }

    public NearbyPlacesDTO getNearby(Long locationId, int radiusMeters, String amenity) {
        Location location = locationDAO.findByIdOrThrow(locationId);
        Double latitude = location.getLatitude();
        Double longitude = location.getLongitude();
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("location has no coordinates");
        }
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90) {
            throw new IllegalArgumentException("latitude must be between -90 and 90");
        }
        if (!Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("longitude must be between -180 and 180");
        }
        if (radiusMeters < 1 || radiusMeters > 5000) {
            throw new IllegalArgumentException("radius must be between 1 and 5000");
        }
        if (amenity == null || !ALLOWED_AMENITIES.contains(amenity)) {
            throw new IllegalArgumentException("amenity is not supported");
        }

        CacheKey key = new CacheKey(latitude, longitude, radiusMeters, amenity);
        Instant now = clock.instant();
        CacheEntry cached = cache.get(key);
        if (cached != null) {
            if (now.isBefore(cached.expiresAt())) {
                return new NearbyPlacesDTO(locationId, cached.places(), true, null);
            }
            cache.remove(key, cached);
        }

        try {
            List<NearbyPlace> places = List.copyOf(client.findNearby(latitude, longitude, radiusMeters, amenity));
            cache.put(key, new CacheEntry(places, now.plus(CACHE_DURATION)));
            return new NearbyPlacesDTO(locationId, places, true, null);
        } catch (OverpassException exception) {
            return new NearbyPlacesDTO(locationId, List.of(), false, UNAVAILABLE_MESSAGE);
        }
    }

    private record CacheKey(double latitude, double longitude, int radiusMeters, String amenity) {
    }

    private record CacheEntry(List<NearbyPlace> places, Instant expiresAt) {
    }
}
