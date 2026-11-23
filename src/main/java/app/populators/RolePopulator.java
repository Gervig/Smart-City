package app.populators;

import app.security.entities.Role;

import java.util.ArrayList;
import java.util.List;

public class RolePopulator
{
    public static List<Role> populate()
    {
        List<Role> roles = new ArrayList<>();

        roles.add(new Role("user"));
        roles.add(new Role("admin"));

        return roles;
    }
}
