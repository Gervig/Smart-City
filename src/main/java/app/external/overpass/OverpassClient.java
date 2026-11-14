package app.external.overpass;

import java.util.List;

public interface OverpassClient {
    List<NearbyPlace> findNearby(double latitude, double longitude, int radiusMeters, String amenity)
            throws OverpassException;
}
