package app.daos;

import app.config.HibernateConfig;
import app.dtos.NearbyPlacesDTO;
import app.entities.Location;
import app.exceptions.NotFoundException;
import app.external.overpass.NearbyPlace;
import app.external.overpass.OverpassClient;
import app.external.overpass.OverpassException;
import app.services.NearbyPlacesService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers requirement(s): FR-18, FR-19, NFR-06.
 * Uses a real PostgreSQL database and GenericDAO, but a hand-written Overpass fake because the public API is unmanaged.
 */
class NearbyPlacesServiceWithDatabaseTest
{
    private EntityManagerFactory emf;
    private GenericDAO<Location> locationDAO;

    @BeforeEach
    void setUp()
    {
        emf = HibernateConfig.getEntityManagerFactoryForTest();
        try (EntityManager em = emf.createEntityManager())
        {
            em.getTransaction().begin();
            em.createNativeQuery("TRUNCATE TABLE citizen_relationship, job, education_record, citizen, company, institution, housing, family, location RESTART IDENTITY CASCADE")
                    .executeUpdate();
            em.getTransaction().commit();
        }
        locationDAO = new GenericDAO<>(emf, Location.class);
    }

    private Location location(String name, Double latitude, Double longitude)
    {
        Location location = new Location();
        location.setName(name);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        return locationDAO.persist(location);
    }

    private NearbyPlacesService service(OverpassClient client)
    {
        return new NearbyPlacesService(locationDAO, client, Clock.systemUTC());
    }

    @Test
    @DisplayName("enriches a saved location with nearby places")
    void getNearby()
    {
        // Arrange
        Location saved = location("Central", 55.6761, 12.5683);
        NearbyPlace place = new NearbyPlace("Hospital", "hospital", 55.68, 12.57);
        NearbyPlacesService nearbyService = service((latitude, longitude, radius, amenity) -> List.of(place));

        // Act
        NearbyPlacesDTO result = nearbyService.getNearby(saved.getId(), 500, "hospital");

        // Assert
        assertTrue(result.enriched());
        assertEquals(List.of(place), result.places());
    }

    @Test
    @DisplayName("rejects a saved location without coordinates")
    void getNearbyWithoutCoordinates()
    {
        // Arrange
        Location saved = location("No coordinates", null, null);
        NearbyPlacesService nearbyService = service((latitude, longitude, radius, amenity) -> List.of());

        // Act / Assert
        assertThrows(IllegalArgumentException.class,
                () -> nearbyService.getNearby(saved.getId(), 500, "hospital"));
    }

    @Test
    @DisplayName("propagates an unknown saved location")
    void getNearbyUnknownLocation()
    {
        // Arrange
        NearbyPlacesService nearbyService = service((latitude, longitude, radius, amenity) -> List.of());

        // Act / Assert
        assertThrows(NotFoundException.class, () -> nearbyService.getNearby(999L, 500, "hospital"));
    }

    @Test
    @DisplayName("returns fallback on Overpass failure without changing the saved location")
    void getNearbyUnavailable()
    {
        // Arrange
        Location saved = location("Central", 55.6761, 12.5683);
        NearbyPlacesService nearbyService = service((latitude, longitude, radius, amenity) ->
        {
            throw new OverpassException("unavailable");
        });

        // Act
        NearbyPlacesDTO result = nearbyService.getNearby(saved.getId(), 500, "hospital");

        // Assert
        assertFalse(result.enriched());
        assertTrue(result.places().isEmpty());
        try (EntityManager em = emf.createEntityManager())
        {
            Location row = em.find(Location.class, saved.getId());
            assertEquals(55.6761, row.getLatitude());
            assertEquals(12.5683, row.getLongitude());
        }
    }
}
