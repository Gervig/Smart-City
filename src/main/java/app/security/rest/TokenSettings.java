package app.security.rest;

import app.utils.Utils;

/**
 * The values used to create and verify JWTs. In production they come from {@link #fromConfig()}; tests pass their
 * own so that they do not depend on the git-ignored config.properties.
 *
 * @param issuer     the iss claim
 * @param expireTime lifetime of a token in milliseconds
 * @param secret     HMAC secret, at least 32 characters (HS256 needs 256 bits)
 */
public record TokenSettings(String issuer, String expireTime, String secret)
{
    /**
     * Environment variables when DEPLOYED is set, otherwise config.properties.
     */
    public static TokenSettings fromConfig()
    {
        if (System.getenv("DEPLOYED") != null)
        {
            return new TokenSettings(System.getenv("ISSUER"), System.getenv("TOKEN_EXPIRE_TIME"),
                    System.getenv("SECRET_KEY"));
        }
        return new TokenSettings(Utils.getPropertyValue("ISSUER", "config.properties"),
                Utils.getPropertyValue("TOKEN_EXPIRE_TIME", "config.properties"),
                Utils.getPropertyValue("SECRET_KEY", "config.properties"));
    }
}
