package app;

import app.config.HibernateConfig;
import app.exceptions.ApiException;
import app.exceptions.NotAuthorizedException;
import app.exceptions.NotFoundException;
import app.exceptions.ValidationException;
import app.mongo.MongoStore;
import app.mongo.MongoUserStore;
import app.neo4j.Neo4jStore;
import app.neo4j.Neo4jUserStore;
import app.nosql.NoSqlSettings;
import app.populators.StartupPopulator;
import app.routes.CrudServices;
import app.routes.Routes;
import app.security.daos.UserDAO;
import app.security.rest.SecurityController;
import app.security.rest.SecurityRoutes;
import app.security.rest.TokenDenylist;
import com.mongodb.MongoSocketException;
import com.mongodb.MongoTimeoutException;
import io.javalin.Javalin;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import org.neo4j.driver.exceptions.AuthenticationException;
import org.neo4j.driver.exceptions.ServiceUnavailableException;
import org.neo4j.driver.exceptions.SessionExpiredException;

import java.util.Map;

import static io.javalin.apibuilder.ApiBuilder.path;

public class Main {

    /**
     * Another database solution next to PostgreSQL: its URL prefix ("/mongodb"), its nine CRUD services and the
     * SecurityController that logs in against its own users.
     */
    public record Solution(String prefix, CrudServices services, SecurityController security) {
    }

    public static void main(String[] args) {
        EntityManagerFactory emf = HibernateConfig.getEntityManagerFactory();
        UserDAO.getInstance(emf);
        StartupPopulator.populate(emf, StartupPopulator.adminPassword());

        // The document and graph solutions; their data comes from the migrator, their users too
        NoSqlSettings nosql = NoSqlSettings.fromConfig();
        MongoStore mongo = new MongoStore(nosql.mongoUri(), nosql.mongoDatabase());
        Neo4jStore neo4j = new Neo4jStore(nosql.neo4jUri(), nosql.neo4jUser(), nosql.neo4jPassword());

        createApp(emf, new SecurityController(),
                new Solution("/mongodb", CrudServices.mongo(mongo),
                        new SecurityController(null, TokenDenylist.getInstance(), new MongoUserStore(mongo))),
                new Solution("/neo4j", CrudServices.neo4j(neo4j),
                        new SecurityController(null, TokenDenylist.getInstance(), new Neo4jUserStore(neo4j))))
                .start(7070);
    }

    /**
     * Builds the application with all routes, filters and exception handlers, without starting it. The API tests
     * start it on a free port with their own SecurityController.
     * <p>
     * PostgreSQL keeps its paths (/citizens, /api/auth/login); every extra solution gets the same resources and
     * login under its prefix (/mongodb/citizens, /mongodb/api/auth/login). The token check and the role rules are
     * shared: authenticate and authorize run before every route.
     */
    public static Javalin createApp(EntityManagerFactory emf, SecurityController securityController,
                                    Solution... solutions) {
        UserDAO.getInstance(emf);

        Javalin app = Javalin.create(config -> {
            config.bundledPlugins.enableRouteOverview("/api/routes");
            config.router.apiBuilder(() -> {
                path("/api", SecurityRoutes.getRoutes(emf, securityController));
                for (Solution solution : solutions) {
                    path(solution.prefix() + "/api", SecurityRoutes.getRoutes(emf, solution.security()));
                }
            });
        })
        .beforeMatched(securityController.authenticate())
        .beforeMatched(securityController.authorize())
        .exception(ApiException.class, (e, ctx) -> ctx.status(e.getCode()).json(Map.of("error", e.getMessage(), "status", e.getCode())))
        .exception(NotAuthorizedException.class, (e, ctx) -> ctx.status(e.getStatusCode()).json(Map.of("error", e.getMessage(), "status", e.getStatusCode())))
        .exception(ValidationException.class, (e, ctx) -> ctx.status(400).json(Map.of("error", e.getMessage())))
        .exception(NotFoundException.class, (e, ctx) -> ctx.status(404).json(Map.of("error", e.getMessage())))
        .exception(IllegalArgumentException.class, (e, ctx) -> ctx.status(400).json(Map.of("error", e.getMessage())))
        .exception(PersistenceException.class, (e, ctx) -> ctx.status(409).json(Map.of("error", "Operation violates a database constraint")))
        // MongoDB or Neo4j cannot be reached: a clear 503 instead of a 500
        .exception(MongoTimeoutException.class, (e, ctx) -> unavailable(ctx))
        .exception(MongoSocketException.class, (e, ctx) -> unavailable(ctx))
        .exception(ServiceUnavailableException.class, (e, ctx) -> unavailable(ctx))
        .exception(SessionExpiredException.class, (e, ctx) -> unavailable(ctx))
        .exception(AuthenticationException.class, (e, ctx) -> unavailable(ctx));

        Routes.register(app, emf);
        for (Solution solution : solutions) {
            Routes.registerSolution(app, solution.prefix(), solution.services());
        }

        return app;
    }

    private static void unavailable(io.javalin.http.Context ctx) {
        ctx.status(503).json(Map.of("error", "Database unavailable", "status", 503));
    }
}
