package app.external.overpass;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    @DisplayName("rejects a null amenity before making a request")
    void findInAreaNullAmenity()
    {
        // Arrange / Act
        assertThrows(IllegalArgumentException.class,
                () -> client.findInArea(55.66, 12.55, 55.70, 12.62, null, 20));

        // Assert
        server.verify(0, postRequestedFor(urlEqualTo("/api/interpreter")));
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 201})
    @DisplayName("rejects a limit outside the supported range")
    void findInAreaInvalidLimit(int limit)
    {
        // Arrange / Act
        assertThrows(IllegalArgumentException.class,
                () -> client.findInArea(55.66, 12.55, 55.70, 12.62, "hospital", limit));

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

    @ParameterizedTest
    @ValueSource(ints = {502, 503, 504})
    @DisplayName("retries each retryable gateway response")
    void findInAreaRetryableGatewayStatus(int status)
    {
        // Arrange
        String scenario = "retry-" + status;
        server.stubFor(post(urlEqualTo("/api/interpreter"))
                .inScenario(scenario).whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withStatus(status)).willSetStateTo("success"));
        server.stubFor(post(urlEqualTo("/api/interpreter"))
                .inScenario(scenario).whenScenarioStateIs("success")
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

    @ParameterizedTest
    @ValueSource(ints = {400, 500})
    @DisplayName("does not retry a non-retryable response")
    void findInAreaBadRequest(int status)
    {
        // Arrange
        server.stubFor(post(urlEqualTo("/api/interpreter")).willReturn(aResponse().withStatus(status)));

        // Act
        OverpassException exception = assertThrows(OverpassException.class,
                () -> client.findInArea(55.66, 12.55, 55.70, 12.62, "hospital", 20));

        // Assert
        assertEquals(status, exception.getStatusCode());
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

    @ParameterizedTest
    @ValueSource(strings = {"null", "{\"elements\":{}}"})
    @DisplayName("rejects a null or non-array elements response")
    void findInAreaInvalidElements(String responseBody)
    {
        // Arrange
        server.stubFor(post(urlEqualTo("/api/interpreter"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody(responseBody)));

        // Act / Assert
        assertThrows(OverpassException.class,
                () -> client.findInArea(55.66, 12.55, 55.70, 12.62, "hospital", 20));
    }

    @Test
    @DisplayName("converts a failed connection into OverpassException")
    void findInAreaConnectionFailure()
    {
        // Arrange
        server.stubFor(post(urlEqualTo("/api/interpreter"))
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        // Act / Assert
        assertThrows(OverpassException.class,
                () -> client.findInArea(55.66, 12.55, 55.70, 12.62, "hospital", 20));
    }

    @Test
    @DisplayName("restores the interrupt flag after an interrupted import request")
    void findInAreaInterrupted()
    {
        // Arrange
        Thread.currentThread().interrupt();

        // Act / Assert
        try
        {
            assertThrows(OverpassException.class,
                    () -> client.findInArea(55.66, 12.55, 55.70, 12.62, "hospital", 20));
            assertTrue(Thread.currentThread().isInterrupted());
        }
        finally
        {
            boolean interrupted = Thread.interrupted();
            assertTrue(interrupted);
        }
    }

    @Test
    @DisplayName("restores the interrupt flag when a retry delay is interrupted")
    void findInAreaRetryInterrupted()
    {
        // Arrange
        server.stubFor(post(urlEqualTo("/api/interpreter")).willReturn(aResponse().withStatus(429)));
        client = new HttpOverpassClient(server.baseUrl() + "/api/interpreter", Duration.ofSeconds(2), millis ->
        {
            throw new InterruptedException("interrupted retry delay");
        });

        // Act / Assert
        try
        {
            assertThrows(OverpassException.class,
                    () -> client.findInArea(55.66, 12.55, 55.70, 12.62, "hospital", 20));
            assertTrue(Thread.currentThread().isInterrupted());
        }
        finally
        {
            boolean interrupted = Thread.interrupted();
            assertTrue(interrupted);
        }
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
    @DisplayName("returns no elements for a null or empty id list")
    void findByIdsNullOrEmpty()
    {
        // Arrange / Act
        List<OsmElement> nullIds = client.findByIds("node", null);
        List<OsmElement> emptyIds = client.findByIds("node", List.of());

        // Assert
        assertTrue(nullIds.isEmpty());
        assertTrue(emptyIds.isEmpty());
        server.verify(0, postRequestedFor(urlEqualTo("/api/interpreter")));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = "unknown")
    @DisplayName("rejects a null or unknown OSM type")
    void findByIdsInvalidOsmType(String osmType)
    {
        // Arrange
        List<Long> ids = List.of(1L);

        // Act / Assert
        assertThrows(IllegalArgumentException.class, () -> client.findByIds(osmType, ids));
    }

    @Test
    @DisplayName("rejects a zero id")
    void findByIdsZeroId()
    {
        // Arrange
        List<Long> ids = List.of(0L);

        // Act / Assert
        assertThrows(IllegalArgumentException.class, () -> client.findByIds("node", ids));
    }

    @Test
    @DisplayName("rejects a null id")
    void findByIdsNullId()
    {
        // Arrange / Act / Assert
        assertThrows(IllegalArgumentException.class,
                () -> client.findByIds("node", java.util.Collections.singletonList(null)));
    }

    @Test
    @DisplayName("rejects more than fifty ids")
    void findByIdsTooMany()
    {
        // Arrange
        List<Long> ids = java.util.stream.LongStream.rangeClosed(1, 51).boxed().toList();

        // Act / Assert
        assertThrows(IllegalArgumentException.class, () -> client.findByIds("node", ids));
    }

    @Test
    @DisplayName("parses missing tags and preserves unknown element types")
    void findByIdsParsingBranches()
    {
        // Arrange
        server.stubFor(post(urlEqualTo("/api/interpreter"))
                .willReturn(aResponse().withHeader("Content-Type", "application/json").withBody("""
                        {"elements":[
                          {"id":1,"type":"mystery","lat":55.6,"lon":12.5,
                           "tags":{"name":"Unknown type","nonTextual":3}},
                          {"id":2,"type":"node","lat":55.7,"lon":12.6},
                          {"id":3,"lat":55.8,"lon":12.7,"tags":{"name":"Missing type"}},
                          {"id":4,"type":"way","lon":12.8,"tags":{"name":"Missing latitude"}},
                          {"id":5,"type":"way","lat":55.9,"tags":{"name":"Missing longitude"}}
                        ]}
                        """)));

        // Act
        List<OsmElement> elements = client.findByIds("node", List.of(1L));

        // Assert
        assertEquals(2, elements.size());
        assertEquals("mystery", elements.get(0).osmType());
        assertEquals("Unknown type", elements.get(0).name());
        assertEquals(Map.of("name", "Unknown type"), elements.get(0).tags());
        assertEquals("node", elements.get(1).osmType());
        assertNull(elements.get(1).name());
        assertEquals(Map.of(), elements.get(1).tags());
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
