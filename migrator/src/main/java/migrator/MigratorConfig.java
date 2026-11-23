package migrator;

/**
 * Where to read from and write to. Every value can be overridden with an environment variable; the defaults match
 * docker/docker-compose.yml. PostgreSQL is read as smartcity_readonly (SQL/users/08_users.sql): the migrator only
 * needs SELECT, so it gets nothing more.
 */
public record MigratorConfig(String postgresUrl, String postgresUser, String postgresPassword,
                             String mongoUri, String mongoDatabase,
                             String neo4jUri, String neo4jUser, String neo4jPassword)
{
    public static MigratorConfig fromEnvironment()
    {
        return new MigratorConfig(
                env("PG_URL", "jdbc:postgresql://localhost:5432/smart_city"),
                env("PG_USER", "smartcity_readonly"),
                env("PG_PASSWORD", "readonly-dev-password"),
                env("MONGO_URI", "mongodb://localhost:27017"),
                env("MONGO_DB", "smart_city"),
                env("NEO4J_URI", "bolt://localhost:7687"),
                env("NEO4J_USER", "neo4j"),
                env("NEO4J_PASSWORD", "smartcity-dev"));
    }

    private static String env(String name, String defaultValue)
    {
        String value = System.getenv(name);
        return value == null || value.isBlank() ? defaultValue : value;
    }
}
