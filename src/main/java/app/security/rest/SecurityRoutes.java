package app.security.rest;

import app.security.enums.Role;
import io.javalin.apibuilder.EndpointGroup;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.javalin.apibuilder.ApiBuilder.*;


public class SecurityRoutes
{
    private static Logger logger = LoggerFactory.getLogger(SecurityRoutes.class);

    public static EndpointGroup getRoutes(EntityManagerFactory emf)
    {
        return getRoutes(emf, new SecurityController());
    }

    public static EndpointGroup getRoutes(EntityManagerFactory emf, SecurityController securityController)
    {
        app.security.daos.UserDAO.getInstance(emf);
        return () ->
        {
            path("auth", () ->
            {
                post("register", securityController.register(), Role.ANYONE);
                post("login", securityController.login(), Role.ANYONE);
                post("logout", securityController.logout(), Role.USER, Role.ADMIN);
            });
            path("secured", () ->
            {
                get("demo", securityController::securedDemo, Role.USER);
            });
        };
    }
}