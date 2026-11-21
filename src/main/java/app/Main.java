package app;

import app.config.HibernateConfig;
import app.exceptions.ApiException;
import app.exceptions.NotAuthorizedException;
import app.exceptions.NotFoundException;
import app.exceptions.ValidationException;
import app.populators.StartupPopulator;
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
        StartupPopulator.populate(emf, StartupPopulator.adminPassword());

        createApp(emf, new SecurityController()).start(7070);
    }

    /**
     * Builds the application with all routes, filters and exception handlers, without starting it. The API tests
     * start it on a free port with their own SecurityController.
     */
    public static Javalin createApp(EntityManagerFactory emf, SecurityController securityController) {
        UserDAO.getInstance(emf);

        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableRouteOverview("/api/routes");
            config.router.apiBuilder(() -> {
                path("/api", SecurityRoutes.getRoutes(emf, securityController));
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

        return app;
    }
}
