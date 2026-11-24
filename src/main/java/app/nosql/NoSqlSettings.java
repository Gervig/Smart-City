package app.nosql;

import app.exceptions.ApiException;
import app.utils.Utils;

/**
 * Where the MongoDB and Neo4j solutions connect. Each value comes from an environment variable, else from
 * config.properties, else a default that matches docker/docker-compose.yml, so local development needs no extra
 * configuration. In the cloud, set MONGO_URI (e.g. a MongoDB Atlas connection string) and NEO4J_URI (e.g. a Neo4j
 * AuraDB neo4j+s:// address) with their credentials.
 */
public record NoSqlSettings(String mongoUri, String mongoDatabase, String neo4jUri, String neo4jUser,
                            String neo4jPassword)
{
    public static NoSqlSettings fromConfig()
    {
        return new NoSqlSettings(
                value("MONGO_URI", "mongodb://localhost:27017"),
                value("MONGO_DB", "smart_city"),
                value("NEO4J_URI", "bolt://localhost:7687"),
                value("NEO4J_USER", "neo4j"),
                value("NEO4J_PASSWORD", "smartcity-dev"));
    }

    private static String value(String name, String defaultValue)
    {
        String env = System.getenv(name);
        if (env != null && !env.isBlank())
        {
            return env;
        }
        try
        {
            return Utils.getPropertyValue(name, "config.properties");
        }
        catch (ApiException missing)
        {
            return defaultValue;
        }
    }
}
