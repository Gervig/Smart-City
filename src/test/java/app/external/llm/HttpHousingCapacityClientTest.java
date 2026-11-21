package app.external.llm;

import app.entities.Housing;
import app.entities.Location;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Real HTTP against a local WireMock standing in for the local LLM API,
 * so no model has to run during the test.
 */
class HttpHousingCapacityClientTest {
    private static final String PATH = "/choose-your-fate/ai/ask";
    private static WireMockServer server;
    private HttpHousingCapacityClient client;

    @BeforeAll
    static void startServer() {
        server = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        server.start();
    }

    @AfterAll
    static void stopServer() {
        server.stop();
    }

    @BeforeEach
    void setUp() {
        server.resetAll();
        client = new HttpHousingCapacityClient(server.baseUrl() + PATH, Duration.ofSeconds(2));
    }

    private static Housing housing() {
        Location location = new Location();
        location.setName("Test Street 1");
        location.setLatitude(55.68);
        location.setLongitude(12.57);
        location.setOsmType("way");
        location.setOsmId(42L);
        Housing housing = new Housing();
        housing.setLocation(location);
        housing.setHousingType("apartment");
        housing.setNumberOfLevels(4);
        return housing;
    }

    private static void stubAnswer(String modelText) {
        String escaped = modelText.replace("\\", "\\\\").replace("\"", "\\\"");
        server.stubFor(post(urlEqualTo(PATH)).willReturn(aResponse().withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"response\":\"" + escaped + "\"}")));
    }

    @Test
    @DisplayName("sends HOUSING_CAPACITY with the housing data as context and parses the JSON answer")
    void sendsRequestAndParsesAnswer() {
        stubAnswer("{\"capacity\": 8, \"confidence\": \"low\", \"reasoning\": \"4 levels, flats unknown\"}");

        HousingCapacityClient.Estimate estimate = client.estimate(housing());

        assertEquals(8, estimate.capacity());
        assertEquals("low", estimate.confidence());
        server.verify(postRequestedFor(urlEqualTo(PATH))
                .withRequestBody(equalToJson("{\"requestType\":\"HOUSING_CAPACITY\"}", true, true))
                .withRequestBody(containing("numberOfLevels: 4"))
                .withRequestBody(containing("numberOfFlats: unknown")));
    }

    @Test
    @DisplayName("finds the JSON object even when the model adds text around it")
    void toleratesTextAroundJson() {
        stubAnswer("Here is my estimate: {\"capacity\": 3, \"confidence\": \"medium\", \"reasoning\": \"house\"} Done.");

        assertEquals(3, client.estimate(housing()).capacity());
    }

    @Test
    @DisplayName("rejects an answer whose capacity is missing or below 1")
    void rejectsInvalidCapacity() {
        stubAnswer("{\"capacity\": 0, \"confidence\": \"low\", \"reasoning\": \"?\"}");

        assertThrows(LlmException.class, () -> client.estimate(housing()));
    }

    @Test
    @DisplayName("rejects an answer with no JSON object")
    void rejectsPlainText() {
        stubAnswer("I think about four people.");

        assertThrows(LlmException.class, () -> client.estimate(housing()));
    }

    @Test
    @DisplayName("turns HTTP 503 (AI unavailable) into an LlmException with the status code")
    void handlesUnavailable() {
        server.stubFor(post(urlEqualTo(PATH)).willReturn(aResponse().withStatus(503)));

        LlmException exception = assertThrows(LlmException.class, () -> client.estimate(housing()));

        assertEquals(503, exception.getStatusCode());
    }

    @Test
    @DisplayName("context builder writes unknown for missing OSM values")
    void contextShowsMissingValues() {
        String context = HousingCapacityContextBuilder.build(housing());

        assertTrue(context.contains("numberOfFlats: unknown"));
        assertTrue(context.contains("heightMeters: unknown"));
        assertTrue(context.contains("location.osmId: 42"));
    }
}
