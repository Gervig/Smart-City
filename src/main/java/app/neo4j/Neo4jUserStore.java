package app.neo4j;

import app.exceptions.ApiException;
import app.exceptions.ValidationException;
import app.security.daos.UserStore;
import dk.bugelhartmann.UserDTO;
import jakarta.persistence.EntityNotFoundException;
import org.mindrot.jbcrypt.BCrypt;
import org.neo4j.driver.Record;
import org.neo4j.driver.exceptions.ClientException;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Login users of the Neo4j solution: (:User {username, password: BCrypt hash})-[:HAS_ROLE]->(:Role {name}). The
 * uniqueness constraint on User.username (01_constraints.cypher) rejects a second user with the same name.
 */
public class Neo4jUserStore implements UserStore
{
    private final Neo4jStore store;

    public Neo4jUserStore(Neo4jStore store)
    {
        this.store = store;
    }

    @Override
    public UserDTO getVerifiedUser(String username, String password) throws ValidationException
    {
        Record user = store.read(tx ->
        {
            var result = tx.run("MATCH (u:User {username: $username}) OPTIONAL MATCH (u)-[:HAS_ROLE]->(r:Role) "
                    + "RETURN u.password AS password, collect(r.name) AS roles", Map.of("username", username));
            return result.hasNext() ? result.next() : null;
        });
        if (user == null)
        {
            throw new EntityNotFoundException("No user found with username: " + username);
        }
        if (!BCrypt.checkpw(password, user.get("password").asString()))
        {
            throw new ValidationException("Wrong password");
        }
        return new UserDTO(username, new HashSet<>(user.get("roles").asList(value -> value.asString())));
    }

    @Override
    public Set<String> register(String username, String password)
    {
        try
        {
            store.write(tx ->
            {
                if (tx.run("MATCH (u:User {username: $username}) RETURN u", Map.of("username", username)).hasNext())
                {
                    throw new ApiException(409, "Username " + username + " is already taken");
                }
                tx.run("MERGE (r:Role {name: 'user'}) CREATE (u:User {username: $username, password: $password}) "
                                + "CREATE (u)-[:HAS_ROLE]->(r)",
                        Map.of("username", username, "password", BCrypt.hashpw(password, BCrypt.gensalt())));
                return null;
            });
            return Set.of("user");
        }
        catch (ClientException e)
        {
            if ("Neo.ClientError.Schema.ConstraintValidationFailed".equals(e.code()))
            {
                throw new ApiException(409, "Username " + username + " is already taken");
            }
            throw e;
        }
    }
}
