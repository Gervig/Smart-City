package app.external.overpass;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Business rule, not yet in the SRS
 * <p>
 * Uses the real HTTP client and local WireMock responses; the unmanaged public Overpass API and real delays are faked.
 */
class HttpOverpassImportClientTest
{
    private static WireMockServer server;
    private HttpOverpassClient client;
    private List<Long> pauses;

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
        pauses = new ArrayList<>();
        client = new HttpOverpassClient(server.baseUrl() + "/api/interpreter", Duration.ofSeconds(2), pauses::add);
    }

    @Test
    @DisplayName("parses named node and way elements and skips unnamed elements")
    void findInAreaParsesElements()
    {
        // Arrange
        server.stubFor(post(urlEqualTo("/api/interpreter"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody("""
                        {"elements":[
                          {"type":"node","id":1,"lat":55.67,"lon":12.56,"tags":{"name":"Node","amenity":"hospital"}},
                          {"type":"way","id":2,"center":{"lat":55.68,"lon":12.57},"tags":{"name":"Way"}},
                          {"type":"node","id":3,"lat":55.69,"lon":12.58,"tags":{"amenity":"hospital"}}
                        ]}
                        """)));

        // Act
        List<OsmElement> elements = client.findInArea(55.66, 12.55, 55.70, 12.62, "hospital", 20);

        // Assert
        assertEquals(2, elements.size());
        assertEquals("node", elements.get(0).osmType());
        assertEquals("Node", elements.get(0).name());
        assertEquals("way", elements.get(1).osmType());
        assertEquals(55.68, elements.get(1).latitude());
    }

    @Test
    @DisplayName("posts bbox, amenity, limit and required user agent")
    void findInAreaRequest()
    {
        // Arrange
        server.stubFor(post(urlEqualTo("/api/interpreter"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody("{\"elements\":[]}")));

        // Act
        client.findInArea(55.66, 12.55, 55.70, 12.62, "hospital", 20);

        // Assert
        server.verify(postRequestedFor(urlEqualTo("/api/interpreter"))
                .withHeader("User-Agent", equalTo("SmartCity-StudentProject/1.0")));
        String encodedForm = server.getAllServeEvents().get(0).getRequest().getBodyAsString();
        String query = URLDecoder.decode(encodedForm.substring("data=".length()), StandardCharsets.UTF_8);
        assertTrue(query.contains("nwr[amenity=hospital](55.66,12.55,55.7,12.62)"));
        assertTrue(query.contains("out center tags 20"));
    }

    @Test
    @DisplayName("rejects an unknown amenity before making a request")
    void findInAreaUnknownAmenity()
    {
        // Arrange / Act
        assertThrows(IllegalArgumentException.class,
                () -> client.findInArea(55.66, 12.55, 55.70, 12.62, "unknown", 20));

        // Assert
        server.verify(0, postRequestedFor(urlEqualTo("/api/interpreter")));
    }

    @Test
    @DisplayName("retries once after 429 and succeeds")
    void findInAreaRetrySuccess()
    {
        // Arrange
        server.stubFor(post(urlEqualTo("/api/interpreter"))
                .inScenario("retry").whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withStatus(429)).willSetStateTo("success"));
        server.stubFor(post(urlEqualTo("/api/interpreter"))
                .inScenario("retry").whenScenarioStateIs("success")
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody("{\"elements\":[]}")));

        // Act
        client.findInArea(55.66, 12.55, 55.70, 12.62, "hospital", 20);

        // Assert
        server.verify(2, postRequestedFor(urlEqualTo("/api/interpreter")));
        assertEquals(List.of(5000L), pauses);
    }

    @Test
    @DisplayName("stops after four retryable responses")
    void findInAreaRetriesExhausted()
    {
        // Arrange
        server.stubFor(post(urlEqualTo("/api/interpreter"))
                .inScenario("exhaust").whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withStatus(429)).willSetStateTo("two"));
        server.stubFor(post(urlEqualTo("/api/interpreter"))
                .inScenario("exhaust").whenScenarioStateIs("two")
                .willReturn(aResponse().withStatus(429)).willSetStateTo("three"));
        server.stubFor(post(urlEqualTo("/api/interpreter"))
                .inScenario("exhaust").whenScenarioStateIs("three")
                .willReturn(aResponse().withStatus(429)).willSetStateTo("four"));
        server.stubFor(post(urlEqualTo("/api/interpreter"))
                .inScenario("exhaust").whenScenarioStateIs("four")
                .willReturn(aResponse().withStatus(429)));

        // Act
        assertThrows(OverpassException.class,
                () -> client.findInArea(55.66, 12.55, 55.70, 12.62, "hospital", 20));

        // Assert
        server.verify(4, postRequestedFor(urlEqualTo("/api/interpreter")));
        assertEquals(List.of(5000L, 10000L, 20000L), pauses);
    }

    @Test
    @DisplayName("does not retry a non-retryable response")
    void findInAreaBadRequest()
    {
        // Arrange
        server.stubFor(post(urlEqualTo("/api/interpreter")).willReturn(aResponse().withStatus(400)));

        // Act
        OverpassException exception = assertThrows(OverpassException.class,
                () -> client.findInArea(55.66, 12.55, 55.70, 12.62, "hospital", 20));

        // Assert
        assertEquals(400, exception.getStatusCode());
        server.verify(1, postRequestedFor(urlEqualTo("/api/interpreter")));
        assertTrue(pauses.isEmpty());
    }

    @Test
    @DisplayName("rejects invalid JSON")
    void findInAreaInvalidJson()
    {
        // Arrange
        server.stubFor(post(urlEqualTo("/api/interpreter")).willReturn(aResponse().withBody("not json")));

        // Act / Assert
        assertThrows(OverpassException.class,
                () -> client.findInArea(55.66, 12.55, 55.70, 12.62, "hospital", 20));
    }

    @Test
    @DisplayName("sends all ids in one query")
    void findByIds()
    {
        // Arrange
        server.stubFor(post(urlEqualTo("/api/interpreter"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody("{\"elements\":[]}")));

        // Act
        client.findByIds("way", List.of(10L, 20L, 30L));

        // Assert
        server.verify(1, postRequestedFor(urlEqualTo("/api/interpreter")));
        String body = server.getAllServeEvents().get(0).getRequest().getBodyAsString();
        String query = URLDecoder.decode(body.substring("data=".length()), StandardCharsets.UTF_8);
        assertTrue(query.contains("way(id:10,20,30);out center tags"));
    }

    @Test
    @DisplayName("waits at least two seconds between successive requests")
    void findInAreaRequestSpacing()
    {
        // Arrange
        server.stubFor(post(urlEqualTo("/api/interpreter"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody("{\"elements\":[]}")));

        // Act
        client.findInArea(55.66, 12.55, 55.70, 12.62, "hospital", 20);
        client.findInArea(55.66, 12.55, 55.70, 12.62, "hospital", 20);

        // Assert
        assertEquals(List.of(2000L), pauses);
    }
}
