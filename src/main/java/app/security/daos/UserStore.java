package app.security.daos;

import app.exceptions.ValidationException;
import dk.bugelhartmann.UserDTO;

import java.util.Set;

/**
 * Where the login users of one database solution are stored. The PostgreSQL, MongoDB and Neo4j solutions each keep
 * their own users (table users, collection users, User/Role nodes), and login and register work the same on all
 * three: passwords are BCrypt hashes and a new user gets the role "user".
 */
public interface UserStore
{
    /**
     * @return the user with its roles
     * @throws jakarta.persistence.EntityNotFoundException when the username is unknown (401)
     * @throws ValidationException                         when the password is wrong (401)
     */
    UserDTO getVerifiedUser(String username, String password) throws ValidationException;

    /**
     * Saves a new user with the role "user".
     *
     * @return the roles of the new user
     * @throws app.exceptions.ApiException 409 when the username is already taken
     */
    Set<String> register(String username, String password);
}
