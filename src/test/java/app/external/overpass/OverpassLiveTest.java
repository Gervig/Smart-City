package app.external.overpass;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers requirement(s): FR-18, FR-19, NFR-06.
 * Mocks can drift from the public response format; this opt-in smoke test checks the real Overpass API.
 * Run by hand with: mvn test -Dtest=OverpassLiveTest -Doverpass.live=true
 * Or in maven side panel, Execute Maven Goal: test -Dtest=OverpassLiveTest -Doverpass.live=true
 */
@EnabledIfSystemProperty(named = "overpass.live", matches = "true")
class OverpassLiveTest
{
    @Test
    @DisplayName("queries pharmacies near Copenhagen City Hall")
    void findNearbyLive()
    {
        // Arrange
        String endpointUrl = System.getProperty("overpass.url", HttpOverpassClient.DEFAULT_ENDPOINT_URL);
        OverpassClient client = new HttpOverpassClient(
                endpointUrl, java.time.Duration.ofSeconds(45));

        // Act
        List<NearbyPlace> places;
        try
        {
            places = client.findNearby(55.6761, 12.5683, 500, "pharmacy");
        }
        catch (OverpassException exception)
        {
            if (exception.isTimeout() || Set.of(429, 502, 503, 504).contains(exception.getStatusCode()))
            {
                Assumptions.assumeTrue(false, "Overpass busy: " + exception.getMessage());
            }
            throw exception;
        }

        // Assert
        assertTrue(places.stream().allMatch(place ->
                place.name() != null && !place.name().isBlank()
                        && place.latitude() >= -90 && place.latitude() <= 90
                        && place.longitude() >= -180 && place.longitude() <= 180));
    }
}
