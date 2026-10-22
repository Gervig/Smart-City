package app;

import app.config.HibernateConfig;
import app.exceptions.NotFoundException;
import app.routes.Routes;
import io.javalin.Javalin;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;

import java.util.Map;

public class Main {
    public static void main(String[] args) {
        EntityManagerFactory emf = HibernateConfig.getEntityManagerFactory();

        Javalin app = Javalin.create()
                .exception(NotFoundException.class, (e, ctx) -> ctx.status(404).json(Map.of("error", e.getMessage())))
                .exception(IllegalArgumentException.class, (e, ctx) -> ctx.status(400).json(Map.of("error", e.getMessage())))
                .exception(PersistenceException.class, (e, ctx) -> ctx.status(409).json(Map.of("error", "Operation violates a database constraint")));

        Routes.register(app, emf);

        app.start(7070);
    }
}
