package app.external.llm;

import app.entities.Housing;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.Map;

/**
 * Calls the local LLM API (POST /choose-your-fate/ai/ask) with requestType HOUSING_CAPACITY.
 * The model is asked to answer with a JSON object: {"capacity": 4, "confidence": "low", "reasoning": "..."}.
 */
public class HttpHousingCapacityClient implements HousingCapacityClient {
    public static final String REQUEST_TYPE = "HOUSING_CAPACITY";
    // README of the LLM project says 8090 (Docker); application.properties defaults to 8080 when run with mvnw.
    public static final String DEFAULT_ENDPOINT_URL = "http://localhost:8090/choose-your-fate/ai/ask";
    // A small local model on a school laptop can be slow.
    public static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(120);

    private final String endpointUrl;
    private final Duration timeout;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HttpHousingCapacityClient() {
        this(DEFAULT_ENDPOINT_URL, DEFAULT_TIMEOUT);
    }

    public HttpHousingCapacityClient(String endpointUrl, Duration timeout) {
        this.endpointUrl = endpointUrl;
        this.timeout = timeout;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    @Override
    public Estimate estimate(Housing housing) {
        String body = requestBody(HousingCapacityContextBuilder.build(housing));
        HttpRequest request = HttpRequest.newBuilder(URI.create(endpointUrl))
                .timeout(timeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new LlmException("LLM API returned HTTP " + response.statusCode(), response.statusCode());
            }
            return parseEstimate(response.body());
        } catch (HttpTimeoutException exception) {
            throw new LlmException("LLM request timed out", exception, 0, true);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new LlmException("LLM request was interrupted", exception);
        } catch (IOException exception) {
            throw new LlmException("LLM request failed", exception);
        }
    }

    private String requestBody(String context) {
        try {
            return objectMapper.writeValueAsString(Map.of("requestType", REQUEST_TYPE, "context", context));
        } catch (IOException exception) {
            throw new LlmException("Could not build LLM request", exception);
        }
    }

    // The API wraps the model text as {"response": "..."}; the model text itself should contain our JSON object.
    private Estimate parseEstimate(String apiBody) {
        try {
            String text = objectMapper.readTree(apiBody).path("response").asText("");
            int start = text.indexOf('{');
            int end = text.lastIndexOf('}');
            if (start < 0 || end <= start) {
                throw new LlmException("LLM answer contains no JSON object");
            }
            JsonNode answer = objectMapper.readTree(text.substring(start, end + 1));
            JsonNode capacity = answer.path("capacity");
            if (!capacity.isIntegralNumber() || capacity.intValue() < 1) {
                throw new LlmException("LLM answer has no valid capacity (must be an integer of at least 1)");
            }
            return new Estimate(capacity.intValue(),
                    answer.path("confidence").asText("unknown"),
                    answer.path("reasoning").asText(""));
        } catch (IOException exception) {
            throw new LlmException("LLM answer was not valid JSON", exception);
        }
    }
}
