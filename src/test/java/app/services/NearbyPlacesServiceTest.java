package app.services;

import app.daos.GenericDAO;
import app.dtos.NearbyPlacesDTO;
import app.entities.Location;
import app.exceptions.NotFoundException;
import app.external.overpass.NearbyPlace;
import app.external.overpass.OverpassClient;
import app.external.overpass.OverpassException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers requirement(s): FR-18, FR-19, NFR-06.
 * Mocks the location DAO and unmanaged Overpass client; a mutable Clock controls cache time.
 * The service remains real so its validation, fallback, and caching behavior is tested deterministically.
 */
@ExtendWith(MockitoExtension.class)
class NearbyPlacesServiceTest
{
    @Mock
    private GenericDAO<Location> locationDAO;
    @Mock
    private OverpassClient client;

    private MutableClock clock;
    private NearbyPlacesService service;
    private Location location;
    private List<NearbyPlace> places;

    @BeforeEach
    void setUp()
    {
        clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        service = new NearbyPlacesService(locationDAO, client, clock);
        location = new Location();
        location.setId(1L);
        location.setLatitude(55.6761);
        location.setLongitude(12.5683);
        lenient().when(locationDAO.findByIdOrThrow(1L)).thenReturn(location);
        places = List.of(new NearbyPlace("Central Hospital", "hospital", 55.68, 12.57));
    }

    @Test
    @DisplayName("returns nearby places and marks the result enriched")
    void getNearby()
    {
        // Arrange
        when(client.findNearby(55.6761, 12.5683, 1000, "hospital")).thenReturn(places);

        // Act
        NearbyPlacesDTO result = service.getNearby(1L, 1000, "hospital");

        // Assert
        assertEquals(1L, result.locationId());
        assertEquals(places, result.places());
        assertTrue(result.enriched());
        assertNull(result.message());
        verify(client).findNearby(55.6761, 12.5683, 1000, "hospital");
    }

    @Test
    @DisplayName("returns an unavailable result when Overpass fails")
    void getNearbyUnavailable()
    {
        // Arrange
        when(client.findNearby(55.6761, 12.5683, 1000, "hospital"))
                .thenThrow(new OverpassException("unavailable"));

        // Act
        NearbyPlacesDTO result = service.getNearby(1L, 1000, "hospital");

        // Assert
        assertTrue(result.places().isEmpty());
        assertFalse(result.enriched());
        assertEquals("Nearby places are currently unavailable", result.message());
    }

    @Test
    @DisplayName("serves identical requests from cache within 24 hours")
    void getNearbyCached()
    {
        // Arrange
        when(client.findNearby(55.6761, 12.5683, 1000, "hospital")).thenReturn(places);

        // Act
        service.getNearby(1L, 1000, "hospital");
        NearbyPlacesDTO result = service.getNearby(1L, 1000, "hospital");

        // Assert
        assertEquals(places, result.places());
        verify(client, times(1)).findNearby(55.6761, 12.5683, 1000, "hospital");
    }

    @Test
    @DisplayName("refreshes cached results after 24 hours")
    void getNearbyExpiredCache()
    {
        // Arrange
        when(client.findNearby(55.6761, 12.5683, 1000, "hospital")).thenReturn(places);

        // Act
        service.getNearby(1L, 1000, "hospital");
        clock.advance(Duration.ofHours(24).plusSeconds(1));
        service.getNearby(1L, 1000, "hospital");

        // Assert
        verify(client, times(2)).findNearby(55.6761, 12.5683, 1000, "hospital");
    }

    @Test
    @DisplayName("does not share cached results across radius or amenity")
    void getNearbyDifferentParameters()
    {
        // Arrange
        when(client.findNearby(anyDouble(), anyDouble(), anyInt(), anyString())).thenReturn(places);

        // Act
        service.getNearby(1L, 1000, "hospital");
        service.getNearby(1L, 900, "hospital");
        service.getNearby(1L, 900, "school");

        // Assert
        verify(client, times(3)).findNearby(anyDouble(), anyDouble(), anyInt(), anyString());
    }

    @Test
    @DisplayName("does not cache failed requests")
    void getNearbyFailureNotCached()
    {
        // Arrange
        when(client.findNearby(55.6761, 12.5683, 1000, "hospital"))
                .thenThrow(new OverpassException("unavailable"))
                .thenReturn(places);

        // Act
        NearbyPlacesDTO failed = service.getNearby(1L, 1000, "hospital");
        NearbyPlacesDTO recovered = service.getNearby(1L, 1000, "hospital");

        // Assert
        assertFalse(failed.enriched());
        assertTrue(recovered.enriched());
        verify(client, times(2)).findNearby(55.6761, 12.5683, 1000, "hospital");
    }

    @Test
    @DisplayName("propagates an unknown location")
    void getNearbyUnknownLocation()
    {
        // Arrange
        when(locationDAO.findByIdOrThrow(2L)).thenThrow(new NotFoundException("Location not found"));

        // Act / Assert
        assertThrows(NotFoundException.class, () -> service.getNearby(2L, 1000, "hospital"));
        verify(client, never()).findNearby(anyDouble(), anyDouble(), anyInt(), anyString());
    }

    @ParameterizedTest
    @CsvSource({"true,false", "false,true"})
    @DisplayName("rejects a location missing either coordinate")
    void getNearbyMissingCoordinates(boolean latitudeMissing, boolean longitudeMissing)
    {
        // Arrange
        location.setLatitude(latitudeMissing ? null : 55.6761);
        location.setLongitude(longitudeMissing ? null : 12.5683);

        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> service.getNearby(1L, 1000, "hospital"));

        // Assert
        assertEquals("location has no coordinates", exception.getMessage());
        verify(client, never()).findNearby(anyDouble(), anyDouble(), anyInt(), anyString());
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> coordinates()
    {
        return Stream.of(
                arguments(90.0, 0.0, true), arguments(-90.0, 0.0, true),
                arguments(90.01, 0.0, false), arguments(-90.01, 0.0, false),
                arguments(0.0, 180.0, true), arguments(0.0, -180.0, true),
                arguments(0.0, 180.01, false), arguments(0.0, -180.01, false)
        );
    }

    @ParameterizedTest
    @MethodSource("coordinates")
    @DisplayName("accepts inclusive coordinate boundaries and rejects out-of-range values")
    void getNearbyCoordinates(double latitude, double longitude, boolean valid)
    {
        // Arrange
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        lenient().when(client.findNearby(anyDouble(), anyDouble(), anyInt(), anyString())).thenReturn(List.of());

        // Act
        if (valid)
        {
            assertDoesNotThrow(() -> service.getNearby(1L, 1000, "hospital"));
        }
        else
        {
            assertThrows(IllegalArgumentException.class, () -> service.getNearby(1L, 1000, "hospital"));
            verify(client, never()).findNearby(anyDouble(), anyDouble(), anyInt(), anyString());
        }
    }

    @ParameterizedTest
    @CsvSource({"0,false", "1,true", "5000,true", "5001,false"})
    @DisplayName("checks inclusive radius boundaries")
    void getNearbyRadius(int radius, boolean valid)
    {
        // Arrange
        lenient().when(client.findNearby(anyDouble(), anyDouble(), anyInt(), anyString())).thenReturn(List.of());

        // Act
        if (valid)
        {
            assertDoesNotThrow(() -> service.getNearby(1L, radius, "hospital"));
        }
        else
        {
            assertThrows(IllegalArgumentException.class, () -> service.getNearby(1L, radius, "hospital"));
            verify(client, never()).findNearby(anyDouble(), anyDouble(), anyInt(), anyString());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"hospital", "school", "college", "pharmacy", "restaurant", "cafe", "library", "bank"})
    @DisplayName("accepts each whitelisted amenity")
    void getNearbyAmenity(String amenity)
    {
        // Arrange
        when(client.findNearby(anyDouble(), anyDouble(), anyInt(), anyString())).thenReturn(List.of());

        // Act / Assert
        assertDoesNotThrow(() -> service.getNearby(1L, 1000, amenity));
    }

    @ParameterizedTest
    @MethodSource("invalidAmenities")
    @DisplayName("rejects a missing, unknown or injected amenity")
    void getNearbyInvalidAmenity(String amenity)
    {
        // Arrange / Act
        assertThrows(IllegalArgumentException.class, () -> service.getNearby(1L, 1000, amenity));

        // Assert
        verify(client, never()).findNearby(anyDouble(), anyDouble(), anyInt(), anyString());
    }

    static Stream<org.junit.jupiter.params.provider.Arguments> invalidAmenities()
    {
        return Stream.of(arguments((String) null), arguments(""), arguments("unknown"), arguments("hospital];out;"));
    }

    private static class MutableClock extends Clock
    {
        private Instant current;

        private MutableClock(Instant current)
        {
            this.current = current;
        }

        private void advance(Duration duration)
        {
            current = current.plus(duration);
        }

        @Override
        public ZoneId getZone()
        {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone)
        {
            return this;
        }

        @Override
        public Instant instant()
        {
            return current;
        }
    }
}
