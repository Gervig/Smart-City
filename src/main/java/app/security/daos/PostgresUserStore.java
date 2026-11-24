package app.security.daos;

import app.exceptions.ValidationException;
import app.security.entities.Role;
import app.security.entities.User;
import dk.bugelhartmann.UserDTO;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * The PostgreSQL users (tables users, roles, user_role), through the existing UserDAO.
 */
public class PostgresUserStore implements UserStore
{
    private final UserDAO userDAO;

    public PostgresUserStore(UserDAO userDAO)
    {
        this.userDAO = userDAO;
    }

    @Override
    public UserDTO getVerifiedUser(String username, String password) throws ValidationException
    {
        return userDAO.getVerifiedUser(username, password);
    }

    @Override
    public Set<String> register(String username, String password)
    {
        User created = userDAO.create(new User(username, password));
        return created.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
    }
}
