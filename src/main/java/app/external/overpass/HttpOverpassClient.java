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
import java.util.List;

public class HttpOverpassClient implements OverpassClient {
    public static final String DEFAULT_ENDPOINT_URL = "https://overpass-api.de/api/interpreter";
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    private final String endpointUrl;
    private final Duration timeout;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HttpOverpassClient(String endpointUrl, Duration timeout) {
        this.endpointUrl = endpointUrl;
        this.timeout = timeout;
        this.httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    @Override
    public List<NearbyPlace> findNearby(double latitude, double longitude, int radiusMeters, String amenity) {
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
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
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
