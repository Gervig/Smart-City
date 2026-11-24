package app.security.rest;

import app.exceptions.ApiException;
import app.exceptions.NotAuthorizedException;
import app.exceptions.ValidationException;
import app.security.daos.PostgresUserStore;
import app.security.daos.UserDAO;
import app.security.daos.UserStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import app.validators.CredentialsValidator;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import dk.bugelhartmann.ITokenSecurity;
import dk.bugelhartmann.TokenSecurity;
import dk.bugelhartmann.UserDTO;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.Handler;
import io.javalin.http.HttpStatus;
import io.javalin.http.UnauthorizedResponse;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityNotFoundException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.util.Date;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class SecurityController implements ISecurityController
{
    // Request attribute that holds the verified token, set by authenticate() and read by authorize() and logout()
    static final String TOKEN_ATTRIBUTE = "token";

    private ITokenSecurity tokenSecurity = new TokenSecurity();
    private ObjectMapper objectMapper = new ObjectMapper();
    private static EntityManagerFactory emf;
    private final UserStore users; // which database the login users live in
    private final TokenSettings settings; // null: read from config.properties / environment on each use
    private final TokenDenylist denylist;

    public SecurityController()
    {
        this(null, TokenDenylist.getInstance());
    }

    public SecurityController(TokenSettings settings, TokenDenylist denylist)
    {
        this(settings, denylist, new PostgresUserStore(UserDAO.getInstance(emf)));
    }

    /**
     * Login and register against the users of one database solution. The tokens, the denylist and the role rules
     * are the same for all three solutions.
     */
    public SecurityController(TokenSettings settings, TokenDenylist denylist, UserStore users)
    {
        this.settings = settings;
        this.denylist = denylist;
        this.users = users;
    }

    private TokenSettings settings()
    {
        return settings != null ? settings : TokenSettings.fromConfig();
    }

    @Override
    public Handler register()
    {
        return (ctx) ->
        {
            UserDTO newUser = readCredentials(ctx);
            CredentialsValidator.validateRegistration(newUser.getUsername(), newUser.getPassword());
            Set<String> createdRoles = users.register(newUser.getUsername(), newUser.getPassword()); // 409 if taken

            // Only the username and roles go back: the password (hash) never leaves the server
            ObjectNode body = objectMapper.createObjectNode().put("username", newUser.getUsername());
            ArrayNode roles = body.putArray("roles");
            createdRoles.forEach(roles::add);
            ctx.status(201).json(body);
        };
    }

    @Override
    public UserDTO verifyToken(String token)
    {
        try
        {
            if (tokenSecurity.tokenIsValid(token, settings().secret()) && tokenSecurity.tokenNotExpired(token))
            {
                return tokenSecurity.getUserWithRolesFromToken(token);
            } else
            {
                throw new NotAuthorizedException(403, "Token is not valid");
            }
        } catch (Exception e)
        {
            // Any problem with the token (malformed, bad signature, expired, unparsable) means 401, never 500
            System.out.println("Token rejected: " + e.getMessage());
            throw new ApiException(HttpStatus.UNAUTHORIZED.getCode(), "Unauthorized. Could not verify token");
        }
    }

    @Override
    public Handler login()
    {
        return (ctx) ->
        {
            ObjectNode returnObject = objectMapper.createObjectNode(); // for sending json messages back to the client
            UserDTO user = readCredentials(ctx);
            CredentialsValidator.validateLogin(user.getUsername(), user.getPassword()); // 400 if one is missing
            try
            {
                UserDTO verifiedUser = users.getVerifiedUser(user.getUsername(), user.getPassword());
                String token = createToken(verifiedUser);

                ctx.status(200).json(returnObject
                        .put("token", token)
                        .put("username", verifiedUser.getUsername()));

            } catch (EntityNotFoundException | ValidationException e)
            {
                ctx.status(401);
                System.out.println(e.getMessage());
                ctx.json(returnObject.put("msg", e.getMessage()));
            }
        };
    }

    // Purpose: To end the session of the token in the request. The route is protected, so authenticate() has
    // already verified the token. Its id goes on the denylist until the token expires.
    @Override
    public Handler logout()
    {
        return (ctx) ->
        {
            String token = verifiedToken(ctx);
            JWTClaimsSet claims = SignedJWT.parse(token).getJWTClaimsSet();
            denylist.revoke(tokenId(token, claims), claims.getExpirationTime().toInstant());
            ctx.status(200).json(objectMapper.createObjectNode().put("msg", "Logged out"));
        };
    }

    @Override
    public Handler authenticate()
    {
        return (ctx) ->
        {
            // This is a preflight request => no need for authentication
            if (ctx.method().toString().equals("OPTIONS"))
            {
                ctx.status(200);
                return;
            }
            //TODO det her er ikke smart,
            // hvis ens token er forkert kommer man stadig igennem hvis det er et åbent endpoint

            // If the endpoint is not protected with roles or is open to ANYONE role, then skip
            Set<String> allowedRoles = ctx.routeRoles().stream().map(role -> role.toString().toUpperCase()).collect(Collectors.toSet());
            if (isOpenEndpoint(allowedRoles))
            {
                return;
            }

            // If there is no token we do not allow entry
            String header = ctx.header("Authorization");
            if (header == null)
            {
                throw new UnauthorizedResponse("Authorization header is missing"); // UnauthorizedResponse is javalin 6 specific but response is not json!
            }

            // If the Authorization Header was malformed, then no entry
            String[] parts = header.split(" ");
            if (parts.length != 2)
            {
                throw new UnauthorizedResponse("Authorization header is malformed"); // UnauthorizedResponse is javalin 6 specific but response is not json!
            }
            String token = parts[1];

            UserDTO verifiedTokenUser = verifyToken(token);
            if (verifiedTokenUser == null)
            {
                throw new UnauthorizedResponse("Invalid user or token"); // UnauthorizedResponse is javalin 6 specific but response is not json!
            }
            ctx.attribute("user", verifiedTokenUser); // -> ctx.attribute("user") in ApplicationConfig beforeMatched filter
            ctx.attribute(TOKEN_ATTRIBUTE, token); // -> the denylist check in authorize() and logout()
        };
    }

     // Purpose: To check if the Authenticated user has the rights to access a protected endpoint
    @Override
    public Handler authorize()
    {
        return (ctx) ->
        {
            Set<String> allowedRoles = ctx.routeRoles()
                    .stream()
                    .map(role -> role.toString().toUpperCase())
                    .collect(Collectors.toSet());

            // 1. Check if the endpoint is open to all (either by not having any roles or having the ANYONE role set
            if (isOpenEndpoint(allowedRoles))
            {
                return;
            }
            // 2. A token that was logged out is not accepted any more, even though it has not expired yet
            if (denylist.isRevoked(tokenId(verifiedToken(ctx))))
            {
                throw new ApiException(HttpStatus.UNAUTHORIZED.getCode(), "Token has been logged out. Log in again");
            }
            // 3. Get user and ensure it is not null
            UserDTO user = ctx.attribute("user");
            if (user == null)
            {
                throw new ForbiddenResponse("No user was added from the token");
            }
            // 4. See if any role matches
            if (!userHasAllowedRole(user, allowedRoles))
            {
                throw new ForbiddenResponse("User was not authorized with roles: " + user.getRoles() + ". Needed roles are: " + allowedRoles);
            }
        };
    }

    // An empty body or one that is not JSON gives 400 instead of a 500 from the JSON parser
    private static UserDTO readCredentials(Context ctx)
    {
        try
        {
            UserDTO credentials = ctx.bodyAsClass(UserDTO.class);
            if (credentials != null)
            {
                return credentials;
            }
        } catch (Exception e)
        {
            // Javalin (Kotlin) lets Jackson's checked IOException through undeclared, so catch Exception, not
            // only RuntimeException. Falls through to the 400 below.
        }
        throw new IllegalArgumentException("The body must be JSON with a username and a password");
    }

    private static String verifiedToken(Context ctx)
    {
        String token = ctx.attribute(TOKEN_ATTRIBUTE);
        if (token == null)
        {
            throw new ApiException(HttpStatus.UNAUTHORIZED.getCode(), "Unauthorized. Could not verify token");
        }
        return token;
    }

    /**
     * The id of a token: its jti claim. Tokens made before the jti was added have none; for those the SHA-256 hash
     * of the token is used, which is just as unique.
     */
    static String tokenId(String token)
    {
        try
        {
            return tokenId(token, SignedJWT.parse(token).getJWTClaimsSet());
        } catch (ParseException e)
        {
            throw new ApiException(HttpStatus.UNAUTHORIZED.getCode(), "Unauthorized. Could not verify token");
        }
    }

    private static String tokenId(String token, JWTClaimsSet claims)
    {
        if (claims.getJWTID() != null)
        {
            return claims.getJWTID();
        }
        try
        {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e)
        {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    private static boolean userHasAllowedRole(UserDTO user, Set<String> allowedRoles)
    {
        return user.getRoles().stream()
                .anyMatch(role -> allowedRoles.contains(role.toUpperCase()));
    }


    private boolean isOpenEndpoint(Set<String> allowedRoles)
    {
        // If the endpoint is not protected with any roles:
        if (allowedRoles.isEmpty())
        {
            return true;
        }

        // 1. Get permitted roles and Check if the endpoint is open to all with the ANYONE role
        if (allowedRoles.contains("ANYONE"))
        {
            return true;
        }
        return false;
    }

    /**
     * Creates the same token as {@link TokenSecurity#createToken} (HS256 with the claims sub, iss, username, roles
     * and exp, so TokenSecurity still verifies it) plus a unique id (jti) and the issue time (iat). The id is what
     * logout puts on the denylist.
     */
    @Override
    public String createToken(UserDTO user)
    {
        TokenSettings tokenSettings = settings();
        try
        {
            Date now = new Date();
            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .subject(user.getUsername())
                    .issuer(tokenSettings.issuer())
                    .claim("username", user.getUsername())
                    .claim("roles", String.join(",", user.getRoles()))
                    .jwtID(UUID.randomUUID().toString())
                    .issueTime(now)
                    .expirationTime(new Date(now.getTime() + Long.parseLong(tokenSettings.expireTime())))
                    .build();
            JWSObject jws = new JWSObject(new JWSHeader(JWSAlgorithm.HS256), new Payload(claims.toJSONObject()));
            jws.sign(new MACSigner(tokenSettings.secret()));
            return jws.serialize();
        } catch (JOSEException | NumberFormatException e)
        {
            e.printStackTrace();
            throw new ApiException(500, "Could not create token");
        }
    }
}
