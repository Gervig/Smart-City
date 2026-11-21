package app.external.overpass;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects; // [CHANGED] Added for null checks in constructor
import java.util.Set;

public class HttpOverpassClient implements OverpassClient, OverpassImportClient {
    public static final String DEFAULT_ENDPOINT_URL = "https://overpass-api.de/api/interpreter";
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);
    private static final int MAX_RADIUS_METERS = 5000; // [CHANGED] Upper bound for radiusMeters in findNearby

    private final String endpointUrl;
    private final Duration timeout;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Sleeper sleeper;
    private long accumulatedSleepNanos;
    private long lastRequestNanos;

    // [CHANGED] Renamed from IMPORT_AMENITIES so both findNearby and findInArea use the same allow-list
    private static final Set<String> ALLOWED_AMENITIES = Set.of(
            "hospital", "school", "college", "pharmacy", "restaurant", "cafe", "library", "bank");

    public HttpOverpassClient(String endpointUrl, Duration timeout) {
        this(endpointUrl, timeout, Thread::sleep);
    }

    public HttpOverpassClient(String endpointUrl, Duration timeout, Sleeper sleeper) {
        // [CHANGED] Validate endpointUrl format/scheme/host and require non-null timeout and sleeper
        this.endpointUrl = validateEndpointUrl(endpointUrl);
        this.timeout = Objects.requireNonNull(timeout, "timeout is required");
        this.sleeper = Objects.requireNonNull(sleeper, "sleeper is required");
        this.httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    @Override
    public List<NearbyPlace> findNearby(double latitude, double longitude, int radiusMeters, String amenity) {
        // [CHANGED] Validate coordinates, radius range, and amenity before building Overpass QL
        validateLatitude(latitude, "latitude");
        validateLongitude(longitude, "longitude");
        if (radiusMeters < 1 || radiusMeters > MAX_RADIUS_METERS) {
            throw new IllegalArgumentException("radiusMeters must be between 1 and " + MAX_RADIUS_METERS);
        }
        validateAmenity(amenity);

        long timeoutSeconds = Math.max(1, timeout.getSeconds() + (timeout.getNano() == 0 ? 0 : 1));
        String query = "[out:json][timeout:" + timeoutSeconds + "];nwr(around:" + radiusMeters + "," + latitude + "," + longitude
                + ")[amenity=" + amenity + "];out center tags 20;";
        String body = "data=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpointUrl))
                .timeout(timeout)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", "SmartCity-StudentProject/1.0")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        try {
            HttpResponse<String> response;
            synchronized (this) {
                waitForRateLimit();
                try {
                    response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                } finally {
                    lastRequestNanos = logicalNow();
                }
            }
            if (response.statusCode() != 200) {
                throw new OverpassException("Overpass API returned HTTP " + response.statusCode(),
                        response.statusCode());
            }
            return parsePlaces(response.body());
        } catch (HttpTimeoutException exception) {
            throw new OverpassException("Overpass request timed out", exception, 0, true);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new OverpassException("Overpass request was interrupted", exception);
        } catch (IOException exception) {
            throw new OverpassException("Overpass request failed", exception);
        }
    }

    @Override
    public List<OsmElement> findInArea(double south, double west, double north, double east, String amenity, int limit) {
        // [CHANGED] Validate bounding-box coordinates, south <= north ordering, and amenity allow-list
        validateLatitude(south, "south");
        validateLatitude(north, "north");
        validateLongitude(west, "west");
        validateLongitude(east, "east");
        if (south > north) {
            throw new IllegalArgumentException("south must not be greater than north");
        }
        validateAmenity(amenity);
        if (limit < 1 || limit > 200) {
            throw new IllegalArgumentException("limit must be between 1 and 200");
        }
        String query = "[out:json][timeout:60];nwr[amenity=" + amenity + "](" + south + "," + west + ","
                + north + "," + east + ");out center tags " + limit + ";";
        return parseOsmElements(sendImportQuery(query)).stream()
                .filter(element -> element.name() != null)
                .toList();
    }

    @Override
    public List<OsmElement> findByIds(String osmType, List<Long> ids) {
        if (osmType == null || !Set.of("node", "way", "relation").contains(osmType)) {
            throw new IllegalArgumentException("osmType must be node, way or relation");
        }
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        if (ids.size() > 50 || ids.stream().anyMatch(id -> id == null || id < 1)) {
            throw new IllegalArgumentException("ids must contain at most 50 positive values");
        }
        String joinedIds = String.join(",", ids.stream().map(String::valueOf).toList());
        String query = "[out:json][timeout:60];" + osmType + "(id:" + joinedIds + ");out center tags;";
        return parseOsmElements(sendImportQuery(query));
    }

    // [CHANGED] New helper to enforce ALLOWED_AMENITIES consistently
    private void validateAmenity(String amenity) {
        if (amenity == null || !ALLOWED_AMENITIES.contains(amenity)) {
            throw new IllegalArgumentException("amenity is not supported");
        }
    }

    // [CHANGED] New helper to ensure latitude is finite and within [-90, 90]
    private void validateLatitude(double value, String name) {
        if (!Double.isFinite(value) || value < -90.0 || value > 90.0) {
            throw new IllegalArgumentException(name + " must be a finite number between -90 and 90");
        }
    }

    // [CHANGED] New helper to ensure longitude is finite and within [-180, 180]
    private void validateLongitude(double value, String name) {
        if (!Double.isFinite(value) || value < -180.0 || value > 180.0) {
            throw new IllegalArgumentException(name + " must be a finite number between -180 and 180");
        }
    }

    // [CHANGED] New helper to require a non-blank HTTP/HTTPS URI with a host
    private static String validateEndpointUrl(String endpointUrl) {
        if (endpointUrl == null || endpointUrl.isBlank()) {
            throw new IllegalArgumentException("endpointUrl is required");
        }
        URI uri = URI.create(endpointUrl.trim());
        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))
                || uri.getHost() == null || uri.getHost().isBlank()) {
            throw new IllegalArgumentException("endpointUrl must be a valid http or https URL");
        }
        return uri.toString();
    }

    private String sendImportQuery(String query) {
        String body = "data=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpointUrl))
                .timeout(timeout)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("User-Agent", "SmartCity-StudentProject/1.0")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        synchronized (this) {
            for (int attempt = 0; ; attempt++) {
                waitForRateLimit();
                try {
                    HttpResponse<String> response;
                    try {
                        response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                    } finally {
                        lastRequestNanos = logicalNow();
                    }
                    int status = response.statusCode();
                    if (status == 200) {
                        return response.body();
                    }
                    if (!isRetryable(status) || attempt == 3) {
                        throw new OverpassException("Overpass API returned HTTP " + status, status);
                    }
                    sleepAndAccount(5000L << attempt);
                } catch (HttpTimeoutException exception) {
                    throw new OverpassException("Overpass request timed out", exception, 0, true);
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new OverpassException("Overpass request was interrupted", exception);
                } catch (IOException exception) {
                    throw new OverpassException("Overpass request failed", exception);
                }
            }
        }
    }

    private void waitForRateLimit() {
        if (lastRequestNanos == 0) {
            return;
        }
        long remainingNanos = Duration.ofSeconds(2).toNanos() - (logicalNow() - lastRequestNanos);
        if (remainingNanos > 0) {
            sleepAndAccount(2000L);
        }
    }

    private void sleepAndAccount(long millis) {
        try {
            sleeper.sleep(millis);
            accumulatedSleepNanos += Duration.ofMillis(millis).toNanos();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new OverpassException("Overpass request was interrupted", exception);
        }
    }

    private long logicalNow() {
        return System.nanoTime() + accumulatedSleepNanos;
    }

    private boolean isRetryable(int status) {
        return status == 429 || status == 502 || status == 503 || status == 504;
    }

    private List<OsmElement> parseOsmElements(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode elements = root == null ? null : root.path("elements");
            if (elements == null || !elements.isArray()) {
                throw new OverpassException("Overpass response has no elements array");
            }
            List<OsmElement> result = new ArrayList<>();
            for (JsonNode element : elements) {
                JsonNode tagsNode = element.path("tags");
                JsonNode lat = element.has("lat") ? element.path("lat") : element.path("center").path("lat");
                JsonNode lon = element.has("lon") ? element.path("lon") : element.path("center").path("lon");
                JsonNode id = element.path("id");
                String type = text(element.path("type"));
                if (!id.isIntegralNumber() || type == null || !lat.isNumber() || !lon.isNumber()) {
                    continue;
                }
                Map<String, String> tags = new LinkedHashMap<>();
                tagsNode.fields().forEachRemaining(entry -> {
                    if (entry.getValue().isTextual()) {
                        tags.put(entry.getKey(), entry.getValue().asText());
                    }
                });
                result.add(new OsmElement(id.longValue(), type, text(tagsNode.path("name")), lat.doubleValue(),
                        lon.doubleValue(), tags));
            }
            return List.copyOf(result);
        } catch (IOException exception) {
            throw new OverpassException("Overpass response was not valid JSON", exception);
        }
    }

    private List<NearbyPlace> parsePlaces(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode elements = root == null ? null : root.path("elements");
            if (elements == null || !elements.isArray()) {
                throw new OverpassException("Overpass response has no elements array");
            }

            List<NearbyPlace> places = new ArrayList<>();
            for (JsonNode element : elements) {
                JsonNode tags = element.path("tags");
                String name = text(tags.path("name"));
                JsonNode latitude = element.has("lat") ? element.path("lat") : element.path("center").path("lat");
                JsonNode longitude = element.has("lon") ? element.path("lon") : element.path("center").path("lon");
                if (name == null || !latitude.isNumber() || !longitude.isNumber()) {
                    continue;
                }
                places.add(new NearbyPlace(name, text(tags.path("amenity")),
                        latitude.doubleValue(), longitude.doubleValue()));
            }
            return List.copyOf(places);
        } catch (IOException exception) {
            throw new OverpassException("Overpass response was not valid JSON", exception);
        }
    }

    private String text(JsonNode node) {
        if (!node.isTextual() || node.asText().isBlank()) {
            return null;
        }
        return node.asText();
    }
}