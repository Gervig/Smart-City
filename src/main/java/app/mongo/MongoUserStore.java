package app.mongo;

import app.exceptions.ApiException;
import app.exceptions.ValidationException;
import app.security.daos.UserStore;
import com.mongodb.ErrorCategory;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import dk.bugelhartmann.UserDTO;
import jakarta.persistence.EntityNotFoundException;
import org.bson.Document;
import org.mindrot.jbcrypt.BCrypt;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Login users of the MongoDB solution: collection "users", {_id: username, password: BCrypt hash, roles: [names]}.
 * The username is the _id, so MongoDB itself rejects a second user with the same name (duplicate key -> 409).
 */
public class MongoUserStore implements UserStore
{
    private final MongoCollection<Document> users;

    public MongoUserStore(MongoStore store)
    {
        this.users = store.collection("users");
    }

    @Override
    public UserDTO getVerifiedUser(String username, String password) throws ValidationException
    {
        Document user = users.find(Filters.eq("_id", username)).first();
        if (user == null)
        {
            throw new EntityNotFoundException("No user found with username: " + username);
        }
        if (!BCrypt.checkpw(password, user.getString("password")))
        {
            throw new ValidationException("Wrong password");
        }
        return new UserDTO(username, new HashSet<>(user.getList("roles", String.class)));
    }

    @Override
    public Set<String> register(String username, String password)
    {
        try
        {
            users.insertOne(new Document("_id", username)
                    .append("password", BCrypt.hashpw(password, BCrypt.gensalt()))
                    .append("roles", List.of("user")));
            return Set.of("user");
        }
        catch (MongoWriteException e)
        {
            if (e.getError().getCategory() == ErrorCategory.DUPLICATE_KEY)
            {
                throw new ApiException(409, "Username " + username + " is already taken");
            }
            throw e;
        }
    }
}
