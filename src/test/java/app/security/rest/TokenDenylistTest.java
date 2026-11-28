package app.security.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers requirement(s): FR-03 (logout invalidates the token on the server, OI-04)
 */
class TokenDenylistTest
{
    private static final Instant START = Instant.parse("2026-11-21T12:00:00Z");

    private MovableClock clock;
    private TokenDenylist denylist;

    @BeforeEach
    void setUp()
    {
        clock = new MovableClock(START);
        denylist = new TokenDenylist(clock);
    }

    @Test
    @DisplayName("[EP-58] a logged-out token is revoked until it expires")
    void revokedUntilExpiry()
    {
        // Arrange

        // Act
        denylist.revoke("token-1", START.plus(Duration.ofMinutes(30)));

        // Assert
        assertTrue(denylist.isRevoked("token-1"));
        assertEquals(1, denylist.size());
    }

    @Test
    @DisplayName("[EP-60] a token that was never logged out is not revoked")
    void unknownToken()
    {
        // Arrange
        denylist.revoke("token-1", START.plus(Duration.ofMinutes(30)));

        // Act
        boolean revoked = denylist.isRevoked("token-2");

        // Assert
        assertFalse(revoked);
    }

    @Test
    @DisplayName("a null id is never revoked")
    void nullId()
    {
        // Arrange

        // Act
        boolean revoked = denylist.isRevoked(null);

        // Assert
        assertFalse(revoked);
    }

    @Test
    @DisplayName("[EP-59, BVA-47] the entry is removed once the token has expired")
    void removedAfterExpiry()
    {
        // Arrange
        denylist.revoke("token-1", START.plus(Duration.ofMinutes(30)));

        // Act
        clock.advance(Duration.ofMinutes(30));

        // Assert
        assertFalse(denylist.isRevoked("token-1"));
        assertEquals(0, denylist.size());
    }

    @Test
    @DisplayName("[EP-58, BVA-46] one second before expiry the token is still revoked (boundary)")
    void revokedJustBeforeExpiry()
    {
        // Arrange
        denylist.revoke("token-1", START.plus(Duration.ofMinutes(30)));

        // Act
        clock.advance(Duration.ofMinutes(30).minusSeconds(1));

        // Assert
        assertTrue(denylist.isRevoked("token-1"));
    }

    @Test
    @DisplayName("[EP-59] a token that has already expired is not stored")
    void alreadyExpired()
    {
        // Arrange

        // Act
        denylist.revoke("token-1", START);

        // Assert
        assertFalse(denylist.isRevoked("token-1"));
        assertEquals(0, denylist.size());
    }

    @Test
    @DisplayName("expired entries of other tokens are cleaned up when a new token is logged out")
    void cleansUpOnRevoke()
    {
        // Arrange
        denylist.revoke("token-1", START.plus(Duration.ofMinutes(5)));
        clock.advance(Duration.ofMinutes(10));

        // Act
        denylist.revoke("token-2", clock.instant().plus(Duration.ofMinutes(30)));

        // Assert
        assertEquals(1, denylist.size());
        assertTrue(denylist.isRevoked("token-2"));
    }

    @Test
    @DisplayName("a missing id or expiry time is rejected")
    void rejectsNulls()
    {
        // Arrange
        Instant expiresAt = START.plus(Duration.ofMinutes(30));

        // Act and Assert
        assertThrows(NullPointerException.class, () -> denylist.revoke(null, expiresAt));
        assertThrows(NullPointerException.class, () -> denylist.revoke("token-1", null));
    }

    @Test
    @DisplayName("the shared instance is the same object every time")
    void sharedInstance()
    {
        // Arrange

        // Act
        TokenDenylist first = TokenDenylist.getInstance();

        // Assert
        assertTrue(first == TokenDenylist.getInstance());
    }

    private static final class MovableClock extends Clock
    {
        private Instant now;

        private MovableClock(Instant now)
        {
            this.now = now;
        }

        void advance(Duration duration)
        {
            now = now.plus(duration);
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
            return now;
        }
    }
}
