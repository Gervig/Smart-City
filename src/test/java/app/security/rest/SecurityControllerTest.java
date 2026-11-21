package app.security.rest;

import app.exceptions.ApiException;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import dk.bugelhartmann.TokenSecurity;
import dk.bugelhartmann.UserDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers requirement(s): FR-02 (login returns a JWT), FR-03 (logout needs a token id), FR-04 (invalid tokens give 401)
 */
class SecurityControllerTest
{
    private static final String SECRET = "test-secret-that-is-at-least-32-characters-long";
    private static final TokenSettings SETTINGS = new TokenSettings("smart-city-test", "1800000", SECRET);
    private static final UserDTO USER = new UserDTO("anna", Set.of("user", "admin"));

    private final SecurityController controller =
            new SecurityController(SETTINGS, new TokenDenylist(Clock.systemUTC()));

    @Test
    @DisplayName("a created token is accepted by TokenSecurity and carries the username and roles")
    void createTokenIsVerifiedByLibrary() throws Exception
    {
        // Arrange
        TokenSecurity library = new TokenSecurity();

        // Act
        String token = controller.createToken(USER);

        // Assert
        assertTrue(library.tokenIsValid(token, SECRET));
        assertTrue(library.tokenNotExpired(token));
        UserDTO fromToken = library.getUserWithRolesFromToken(token);
        assertEquals("anna", fromToken.getUsername());
        assertEquals(Set.of("user", "admin"), fromToken.getRoles());
    }

    @Test
    @DisplayName("a created token has an id, an issuer and expires after the configured time")
    void createTokenClaims() throws Exception
    {
        // Arrange
        Instant before = Instant.now();

        // Act
        JWTClaimsSet claims = SignedJWT.parse(controller.createToken(USER)).getJWTClaimsSet();

        // Assert
        assertNotNull(claims.getJWTID());
        assertEquals("smart-city-test", claims.getIssuer());
        assertEquals("anna", claims.getSubject());
        Instant expires = claims.getExpirationTime().toInstant();
        assertTrue(!expires.isBefore(before.plus(Duration.ofMinutes(30)).minusSeconds(1)));
        assertTrue(expires.isBefore(Instant.now().plus(Duration.ofMinutes(30)).plusSeconds(1)));
    }

    @Test
    @DisplayName("two logins give two different token ids")
    void tokenIdsAreUnique()
    {
        // Arrange

        // Act
        String first = SecurityController.tokenId(controller.createToken(USER));
        String second = SecurityController.tokenId(controller.createToken(USER));

        // Assert
        assertNotEquals(first, second);
    }

    @Test
    @DisplayName("the token id is the jti claim")
    void tokenIdIsJti() throws Exception
    {
        // Arrange
        String token = controller.createToken(USER);

        // Act
        String id = SecurityController.tokenId(token);

        // Assert
        assertEquals(SignedJWT.parse(token).getJWTClaimsSet().getJWTID(), id);
    }

    @Test
    @DisplayName("a token without jti gets the same SHA-256 id every time")
    void tokenIdWithoutJti() throws Exception
    {
        // Arrange
        String oldToken = new TokenSecurity().createToken(USER, "smart-city-test", "1800000", SECRET);

        // Act
        String id = SecurityController.tokenId(oldToken);

        // Assert
        assertEquals(64, id.length());
        assertEquals(id, SecurityController.tokenId(oldToken));
    }

    @Test
    @DisplayName("an unreadable token gives 401")
    void tokenIdOfGarbage()
    {
        // Arrange

        // Act
        ApiException exception = assertThrows(ApiException.class, () -> SecurityController.tokenId("not-a-token"));

        // Assert
        assertEquals(401, exception.getCode());
    }

    @Test
    @DisplayName("verifyToken returns the user of a valid token")
    void verifyValidToken()
    {
        // Arrange
        String token = controller.createToken(USER);

        // Act
        UserDTO user = controller.verifyToken(token);

        // Assert
        assertEquals("anna", user.getUsername());
        assertEquals(Set.of("user", "admin"), user.getRoles());
    }

    @Test
    @DisplayName("verifyToken rejects a token signed with another secret with 401")
    void verifyTokenWrongSecret()
    {
        // Arrange
        TokenSettings other = new TokenSettings("other", "1800000", "another-secret-that-is-also-32-characters");
        String token = new SecurityController(other, new TokenDenylist(Clock.systemUTC())).createToken(USER);

        // Act
        ApiException exception = assertThrows(ApiException.class, () -> controller.verifyToken(token));

        // Assert
        assertEquals(401, exception.getCode());
    }

    @Test
    @DisplayName("verifyToken rejects an expired token with 401")
    void verifyExpiredToken()
    {
        // Arrange
        TokenSettings expired = new TokenSettings("smart-city-test", "-1000", SECRET);
        String token = new SecurityController(expired, new TokenDenylist(Clock.systemUTC())).createToken(USER);

        // Act
        ApiException exception = assertThrows(ApiException.class, () -> controller.verifyToken(token));

        // Assert
        assertEquals(401, exception.getCode());
    }

    @Test
    @DisplayName("a secret shorter than 32 characters cannot sign a token (500)")
    void createTokenShortSecret()
    {
        // Arrange
        SecurityController weak = new SecurityController(new TokenSettings("i", "1800000", "too-short"),
                new TokenDenylist(Clock.systemUTC()));

        // Act
        ApiException exception = assertThrows(ApiException.class, () -> weak.createToken(USER));

        // Assert
        assertEquals(500, exception.getCode());
    }

    @Test
    @DisplayName("a token lifetime that is not a number gives 500")
    void createTokenBadExpireTime()
    {
        // Arrange
        SecurityController broken = new SecurityController(new TokenSettings("i", "thirty minutes", SECRET),
                new TokenDenylist(Clock.systemUTC()));

        // Act
        ApiException exception = assertThrows(ApiException.class, () -> broken.createToken(USER));

        // Assert
        assertEquals(500, exception.getCode());
    }
}
