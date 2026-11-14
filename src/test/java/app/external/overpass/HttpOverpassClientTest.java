package app.external.overpass;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers requirement(s): FR-18, FR-19, NFR-06.
 * Uses a real HttpOverpassClient and real HTTP against local WireMock, so transport and JSON parsing are exercised
 * without calling the unmanaged public Overpass service.
 */
class HttpOverpassClientTest
{
    private static WireMockServer server;
    private HttpOverpassClient client;

    @BeforeAll
    static void startServer()
    {
        server = new WireMockServer(com.github.tomakehurst.wiremock.core.WireMockConfiguration.options().dynamicPort());
        server.start();
    }

    @AfterAll
    static void stopServer()
    {
        server.stop();
    }

    @BeforeEach
    void setUp()
    {
        server.resetAll();
        client = new HttpOverpassClient(server.baseUrl() + "/api/interpreter", Duration.ofSeconds(2));
    }

    @Test
    @DisplayName("parses named node and way elements and skips elements without names")
    void findNearbyParsesElements()
    {
        // Arrange
        server.stubFor(post(urlEqualTo("/api/interpreter"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"version":0.6,"generator":"Overpass API","elements":[
                                  {"type":"node","id":1,"lat":55.6761,"lon":12.5683,
                                   "tags":{"amenity":"hospital","name":"Test Hospital"}},
                                  {"type":"way","id":2,"center":{"lat":55.68,"lon":12.57},
                                   "tags":{"amenity":"hospital","name":"Test Clinic"}},
                                  {"type":"node","id":3,"lat":55.69,"lon":12.58,
                                   "tags":{"amenity":"hospital"}}
                                ]}
                                """)));

        // Act
        List<NearbyPlace> result = client.findNearby(55.6761, 12.5683, 500, "hospital");

        // Assert
        assertEquals(List.of(
                new NearbyPlace("Test Hospital", "hospital", 55.6761, 12.5683),
                new NearbyPlace("Test Clinic", "hospital", 55.68, 12.57)
        ), result);
    }

    @Test
    @DisplayName("parses the saved real response with all three pharmacies")
    void findNearbyRealResponse() throws IOException
    {
        // Arrange
        String response;
        try (InputStream input = getClass().getResourceAsStream("/overpass/pharmacy-response.json"))
        {
            assertNotNull(input, "pharmacy-response.json test fixture must be available");
            response = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
        server.stubFor(post(urlEqualTo("/api/interpreter"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(response)));

        // Act
        List<NearbyPlace> result = client.findNearby(55.6761, 12.5683, 500, "pharmacy");

        // Assert
        assertEquals(List.of(
                new NearbyPlace("Steno Apotek", "pharmacy", 55.6745260, 12.5633101),
                new NearbyPlace("Nytorv Apotek", "pharmacy", 55.6776378, 12.5721348),
                new NearbyPlace("Hovedbanens Apotek", "pharmacy", 55.6726493, 12.5639470)
        ), result);
    }

    @Test
    @DisplayName("skips an element without coordinates")
    void findNearbySkipsMissingCoordinates()
    {
        // Arrange
        server.stubFor(post(urlEqualTo("/api/interpreter"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"elements":[{"type":"way","tags":{"amenity":"hospital","name":"No center"}}]}
                                """)));

        // Act
        List<NearbyPlace> result = client.findNearby(55.6761, 12.5683, 500, "hospital");

        // Assert
        assertEquals(List.of(), result);
    }

    @Test
    @DisplayName("returns an empty list when there are no elements")
    void findNearbyEmpty()
    {
        // Arrange
        server.stubFor(post(urlEqualTo("/api/interpreter"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody("{\"elements\":[]}")));

        // Act
        List<NearbyPlace> result = client.findNearby(55.6761, 12.5683, 500, "hospital");

        // Assert
        assertEquals(List.of(), result);
    }

    @ParameterizedTest
    @ValueSource(ints = {429, 500, 502, 503, 504})
    @DisplayName("rejects unsuccessful HTTP responses")
    void findNearbyHttpError(int status)
    {
        // Arrange
        server.stubFor(post(urlEqualTo("/api/interpreter")).willReturn(aResponse().withStatus(status)));

        // Act / Assert
        OverpassException exception = assertThrows(OverpassException.class,
                () -> client.findNearby(55.6761, 12.5683, 500, "hospital"));
        assertEquals(status, exception.getStatusCode());
        assertFalse(exception.isTimeout());
    }

    @Test
    @DisplayName("rejects a response that is not JSON")
    void findNearbyInvalidJson()
    {
        // Arrange
        server.stubFor(post(urlEqualTo("/api/interpreter")).willReturn(aResponse().withBody("not json")));

        // Act / Assert
        assertThrows(OverpassException.class,
                () -> client.findNearby(55.6761, 12.5683, 500, "hospital"));
    }

    @Test
    @DisplayName("rejects JSON without an elements array")
    void findNearbyMissingElements()
    {
        // Arrange
        server.stubFor(post(urlEqualTo("/api/interpreter"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody("{\"version\":0.6}")));

        // Act / Assert
        assertThrows(OverpassException.class,
                () -> client.findNearby(55.6761, 12.5683, 500, "hospital"));
    }

    @Test
    @DisplayName("converts a request timeout into OverpassException")
    void findNearbyTimeout()
    {
        // Arrange
        server.stubFor(post(urlEqualTo("/api/interpreter"))
                .willReturn(aResponse().withFixedDelay(2000).withBody("{\"elements\":[]}")));
        HttpOverpassClient shortTimeoutClient =
                new HttpOverpassClient(server.baseUrl() + "/api/interpreter", Duration.ofMillis(500));

        // Act / Assert
        OverpassException exception = assertThrows(OverpassException.class,
                () -> shortTimeoutClient.findNearby(55.6761, 12.5683, 500, "hospital"));
        assertEquals(0, exception.getStatusCode());
        assertTrue(exception.isTimeout());
    }

    @Test
    @DisplayName("posts the encoded query with the required user agent")
    void findNearbyRequest()
    {
        // Arrange
        server.stubFor(post(urlEqualTo("/api/interpreter"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody("{\"elements\":[]}")));

        // Act
        client.findNearby(55.6761, 12.5683, 500, "hospital");

        // Assert
        server.verify(postRequestedFor(urlEqualTo("/api/interpreter"))
                .withHeader("Content-Type", containing("application/x-www-form-urlencoded"))
                .withHeader("User-Agent", equalTo("SmartCity-StudentProject/1.0")));
        String encodedForm = server.getAllServeEvents().get(0).getRequest().getBodyAsString();
        String query = URLDecoder.decode(encodedForm.substring("data=".length()), StandardCharsets.UTF_8);
        org.junit.jupiter.api.Assertions.assertTrue(query.contains("around:500,55.6761,12.5683"));
        org.junit.jupiter.api.Assertions.assertTrue(query.contains("amenity=hospital"));
        assertTrue(query.contains("[timeout:2]"));
    }

    @Test
    @DisplayName("uses a zero HTTP status and no timeout flag for response parsing failures")
    void findNearbyParseErrorDetails()
    {
        // Arrange
        server.stubFor(post(urlEqualTo("/api/interpreter")).willReturn(aResponse().withBody("not json")));

        // Act
        OverpassException exception = assertThrows(OverpassException.class,
                () -> client.findNearby(55.6761, 12.5683, 500, "hospital"));

        // Assert
        assertEquals(0, exception.getStatusCode());
        assertFalse(exception.isTimeout());
    }
}
