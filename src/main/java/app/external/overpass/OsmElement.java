package app.external.overpass;

import java.util.Map;

public record OsmElement(long osmId, String osmType, String name, double latitude, double longitude,
                         Map<String, String> tags)
{
    public OsmElement
    {
        tags = Map.copyOf(tags);
    }
}
