package app.security.rest;

import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Ids of tokens that were logged out before they expired. A JWT stays valid until its expiry time, so logout puts
 * the token's id here and the authorize filter rejects it. An entry is kept only until the token expires: after
 * that the token is rejected anyway, so the entry is removed to keep the list small.
 * <p>
 * The list lives in memory and is shared by the whole application (see {@link #getInstance()}). A restart empties
 * it, so a logged-out token that has not expired yet would work again after a restart.
 */
public class TokenDenylist
{
    private static final TokenDenylist INSTANCE = new TokenDenylist(Clock.systemUTC());

    private final Map<String, Instant> revoked = new ConcurrentHashMap<>();
    private final Clock clock;

    public TokenDenylist(Clock clock)
    {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public static TokenDenylist getInstance()
    {
        return INSTANCE;
    }

    /**
     * Rejects the token with this id until it expires. A token that has already expired is not stored.
     */
    public void revoke(String tokenId, Instant expiresAt)
    {
        Objects.requireNonNull(tokenId, "tokenId");
        Objects.requireNonNull(expiresAt, "expiresAt");
        removeExpired();
        if (expiresAt.isAfter(clock.instant()))
        {
            revoked.put(tokenId, expiresAt);
        }
    }

    public boolean isRevoked(String tokenId)
    {
        if (tokenId == null)
        {
            return false;
        }
        Instant expiresAt = revoked.get(tokenId);
        if (expiresAt == null)
        {
            return false;
        }
        if (!expiresAt.isAfter(clock.instant()))
        {
            revoked.remove(tokenId, expiresAt);
            return false;
        }
        return true;
    }

    /**
     * Number of tokens that are logged out and not expired yet.
     */
    public int size()
    {
        removeExpired();
        return revoked.size();
    }

    private void removeExpired()
    {
        Instant now = clock.instant();
        revoked.values().removeIf(expiresAt -> !expiresAt.isAfter(now));
    }
}
