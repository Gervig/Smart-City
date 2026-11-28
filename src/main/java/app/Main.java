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
import app.security.AuditContext;
import app.security.daos.UserDAO;
import app.security.rest.SecurityController;
import app.security.rest.SecurityRoutes;
import app.security.rest.TokenDenylist;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mongodb.MongoSocketException;
import com.mongodb.MongoTimeoutException;
import io.javalin.Javalin;
import io.javalin.http.Handler;
import io.javalin.openapi.plugin.OpenApiPlugin;
import io.javalin.openapi.plugin.swagger.SwaggerPlugin;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceException;
import org.neo4j.driver.exceptions.AuthenticationException;
import org.neo4j.driver.exceptions.ServiceUnavailableException;
import org.neo4j.driver.exceptions.SessionExpiredException;

import java.util.*;

import static io.javalin.apibuilder.ApiBuilder.path;

public class Main
{
    private static final Set<String> CRUD_RESOURCES = Set.of(
            "locations", "families", "housings", "citizens", "citizen-relationships",
            "institutions", "companies", "education-records", "jobs");
    private static final Set<String> HTTP_METHODS = Set.of(
            "get", "put", "post", "delete", "options", "head", "patch", "trace");

    public static void main(String[] args)
    {
        EntityManagerFactory emf = HibernateConfig.getEntityManagerFactory();
        UserDAO.getInstance(emf);
        StartupPopulator.populate(emf, StartupPopulator.adminPassword());

        NoSqlSettings settings = NoSqlSettings.fromConfig();
        MongoStore mongoStore = new MongoStore(settings.mongoUri(), settings.mongoDatabase());
        Neo4jStore neo4jStore = new Neo4jStore(settings.neo4jUri(), settings.neo4jUser(), settings.neo4jPassword());
        TokenDenylist denylist = TokenDenylist.getInstance();

        int port = 7070;

        Javalin app = createApp(emf, new SecurityController(),
                new Solution("/mongodb", CrudServices.mongo(mongoStore),
                        new SecurityController(null, denylist, new MongoUserStore(mongoStore))),
                new Solution("/neo4j", CrudServices.neo4j(neo4jStore),
                        new SecurityController(null, denylist, new Neo4jUserStore(neo4jStore))));
        app.events(events -> events.serverStopping(() ->
        {
            mongoStore.close();
            neo4jStore.close();
        }));
        app.start(port);

        System.out.println();
        System.out.println("========================================");
        System.out.println(" Smart City API is running!");
        System.out.println(" API:     http://localhost:" + port + "/");
        System.out.println(" Swagger: http://localhost:" + port + "/swagger");
        System.out.println(" OpenAPI: http://localhost:" + port + "/openapi");
        System.out.println(" Routes:  http://localhost:" + port + "/api/routes");
        System.out.println("========================================");
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
                                    Solution... solutions)
    {
        UserDAO.getInstance(emf);

        Javalin app = Javalin.create(config ->
                {
                    config.registerPlugin(new OpenApiPlugin(openApi ->
                            openApi.withDefinitionConfiguration((version, definition) ->
                            {
                                definition.withInfo(info -> info.title("Smart City API")
                                        .version("1.0.0")
                                        .description("REST API for city data. Protected operations use HS256 JWT bearer tokens."));
                                definition.withSecurity(security -> security.withBearerAuth("BearerAuth"));
                                definition.withDefinitionProcessor(
                                        openApiDefinition -> addSolutionPaths(openApiDefinition, solutions));
                            })));
                    config.registerPlugin(new SwaggerPlugin());
                    config.bundledPlugins.enableRouteOverview("/api/routes");
                    config.router.apiBuilder(() ->
                    {
                        path("/api", SecurityRoutes.getRoutes(emf, securityController));
                        for (Solution solution : solutions)
                        {
                            path(solution.prefix() + "/api", SecurityRoutes.getRoutes(emf, solution.security()));
                        }
                    });
                })
                .beforeMatched(excludingDocumentation(securityController.authenticate()))
                .beforeMatched(excludingDocumentation(securityController.authorize()))
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
        // The audit user belongs to one request only (see AuditContext)
        app.after(ctx -> AuditContext.clear());

        Routes.register(app, emf);
        for (Solution solution : solutions)
        {
            Routes.registerSolution(app, solution.prefix(), solution.services());
        }

        return app;
    }

    private static String addSolutionPaths(ObjectNode definition, Solution... solutions)
    {
        JsonNode pathsNode = definition.get("paths");
        if (!(pathsNode instanceof ObjectNode paths))
        {
            throw new IllegalStateException("OpenAPI definition is missing its paths object");
        }

        List<String> postgresPaths = new ArrayList<>();
        paths.fieldNames().forEachRemaining(path ->
        {
            if (isSolutionPath(path))
            {
                postgresPaths.add(path);
            }
        });

        for (Solution solution : solutions)
        {
            addSolutionPaths(paths, postgresPaths, solution.prefix(), solutionName(solution.prefix()));
        }
        return definition.toString();
    }

    private static String solutionName(String prefix)
    {
        return switch (prefix)
        {
            case "/mongodb" -> "MongoDB";
            case "/neo4j" -> "Neo4j";
            default -> prefix;
        };
    }

    private static boolean isSolutionPath(String path)
    {
        if (path.startsWith("/api/auth/"))
        {
            return true;
        }

        for (String resource : CRUD_RESOURCES)
        {
            if (path.equals("/" + resource) || path.equals("/" + resource + "/{id}"))
            {
                return true;
            }
        }
        return false;
    }

    private static void addSolutionPaths(ObjectNode paths, List<String> sourcePaths, String prefix, String solution)
    {
        String solutionId = solution.toLowerCase(Locale.ROOT);
        for (String sourcePath : sourcePaths)
        {
            String targetPath = prefix + sourcePath;
            if (paths.has(targetPath))
            {
                continue;
            }

            ObjectNode copiedPath = paths.get(sourcePath).deepCopy();
            Iterator<Map.Entry<String, JsonNode>> operations = copiedPath.fields();
            while (operations.hasNext())
            {
                Map.Entry<String, JsonNode> entry = operations.next();
                String method = entry.getKey();
                if (!HTTP_METHODS.contains(method) || !(entry.getValue() instanceof ObjectNode operation))
                {
                    continue;
                }

                String operationId = operation.path("operationId").asText(method);
                String pathId = sourcePath.replaceAll("[^A-Za-z0-9]", "_");
                operation.put("operationId", operationId + "_" + solutionId + "_" + method + "_" + pathId);

                String suffix = " (" + solution + ")";
                JsonNode originalTags = operation.get("tags");
                ArrayNode tags = operation.arrayNode();
                if (originalTags instanceof ArrayNode tagArray)
                {
                    for (JsonNode tag : tagArray)
                    {
                        if (tag.isTextual())
                        {
                            tags.add(tag.asText() + suffix);
                        }
                        else
                        {
                            tags.add(tag);
                        }
                    }
                }
                if (tags.isEmpty())
                {
                    tags.add(solution);
                }
                operation.set("tags", tags);

                String summary = operation.path("summary").asText("Operation");
                operation.put("summary", summary + suffix);
            }
            paths.set(targetPath, copiedPath);
        }
    }

    private static void unavailable(io.javalin.http.Context ctx)
    {
        ctx.status(503).json(Map.of("error", "Database unavailable", "status", 503));
    }

    private static Handler excludingDocumentation(Handler securityHandler)
    {
        return ctx ->
        {
            String path = ctx.path();
            boolean documentationRequest = path.equals("/openapi")
                    || path.equals("/swagger")
                    || path.startsWith("/swagger/")
                    || path.equals("/webjars/swagger-ui")
                    || path.startsWith("/webjars/swagger-ui/");
            if (!documentationRequest)
            {
                securityHandler.handle(ctx);
            }
        };
    }

    public record Solution(String prefix, CrudServices services, SecurityController security)
    {
    }
}
