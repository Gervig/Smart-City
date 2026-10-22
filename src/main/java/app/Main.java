package app;

import app.config.HibernateConfig;
import app.exceptions.ApiException;
import app.exceptions.NotAuthorizedException;
import app.exceptions.NotFoundException;
import app.exceptions.ValidationException;
import app.routes.Routes;
import app.security.daos.UserDAO;
import app.security.rest.SecurityController;
import app.security.rest.SecurityRoutes;
import io.javalin.Javalin;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;

import java.util.Map;

import static io.javalin.apibuilder.ApiBuilder.path;

public class Main {
    public static void main(String[] args) {
        EntityManagerFactory emf = HibernateConfig.getEntityManagerFactory();
        UserDAO.getInstance(emf);

        SecurityController securityController = new SecurityController();

        Javalin app = Javalin.create(config -> {
            config.router.apiBuilder(() -> {
                path("/api", SecurityRoutes.getRoutes(emf));
            });
        })
        .beforeMatched(securityController.authenticate())
        .beforeMatched(securityController.authorize())
        .exception(ApiException.class, (e, ctx) -> ctx.status(e.getCode()).json(Map.of("error", e.getMessage(), "status", e.getCode())))
        .exception(NotAuthorizedException.class, (e, ctx) -> ctx.status(e.getStatusCode()).json(Map.of("error", e.getMessage(), "status", e.getStatusCode())))
        .exception(ValidationException.class, (e, ctx) -> ctx.status(400).json(Map.of("error", e.getMessage())))
        .exception(NotFoundException.class, (e, ctx) -> ctx.status(404).json(Map.of("error", e.getMessage())))
        .exception(IllegalArgumentException.class, (e, ctx) -> ctx.status(400).json(Map.of("error", e.getMessage())))
        .exception(PersistenceException.class, (e, ctx) -> ctx.status(409).json(Map.of("error", "Operation violates a database constraint")));

        Routes.register(app, emf);

        app.start(7070);
    }
}
