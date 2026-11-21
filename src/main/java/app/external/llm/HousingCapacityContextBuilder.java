package app.external.llm;

import app.entities.Housing;
import app.entities.Location;

import java.util.Locale;

/**
 * Turns one Housing (and its Location) into the plain-text "context" the LLM API expects.
 * Field names match docs/llm/HOUSING_DATA_CONTEXT.md. Missing values are written as "unknown"
 * on purpose, so the model sees that the data is missing instead of guessing it was zero.
 */
public final class HousingCapacityContextBuilder {
    private static final String UNKNOWN = "unknown";

    private HousingCapacityContextBuilder() {
    }

    public static String build(Housing housing) {
        if (housing == null) {
            throw new IllegalArgumentException("housing is required");
        }
        Location location = housing.getLocation();
        StringBuilder context = new StringBuilder();
        context.append("Housing record from Smart City / World Sim\n");
        line(context, "housingType", housing.getHousingType());
        line(context, "numberOfLevels", housing.getNumberOfLevels());
        line(context, "numberOfFlats", housing.getNumberOfFlats());
        line(context, "heightMeters", housing.getHeight());
        if (location == null) {
            line(context, "location", null);
        } else {
            line(context, "location.name", location.getName());
            line(context, "location.streetAddress", location.getStreetAddress());
            line(context, "location.latitude", location.getLatitude());
            line(context, "location.longitude", location.getLongitude());
            line(context, "location.osmType", location.getOsmType());
            line(context, "location.osmId", location.getOsmId());
        }
        return context.toString();
    }

    private static void line(StringBuilder context, String key, Object value) {
        context.append(key).append(": ").append(format(value)).append('\n');
    }

    private static String format(Object value) {
        if (value == null) {
            return UNKNOWN;
        }
        if (value instanceof String text) {
            return text.isBlank() ? UNKNOWN : text.trim();
        }
        if (value instanceof Double number) {
            return String.format(Locale.ROOT, "%s", number);
        }
        return String.valueOf(value);
    }
}
