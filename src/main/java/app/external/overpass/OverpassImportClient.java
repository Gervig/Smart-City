package app.external.overpass;

import java.util.List;

public interface OverpassImportClient
{
    List<OsmElement> findInArea(double south, double west, double north, double east, String amenity, int limit)
            throws OverpassException;

    List<OsmElement> findByIds(String osmType, List<Long> ids) throws OverpassException;
}
