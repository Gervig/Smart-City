package app.daos;

import app.config.HibernateConfig;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Guards against drift between the JPA entities and SQL/schema/database_schema.sql.
 * The SQL script is loaded into a real PostgreSQL container and Hibernate is started with
 * hbm2ddl.auto=validate. If an entity has a table, column or type that the script does not,
 * Hibernate throws and the test fails.
 */
class SchemaValidationTest
{
    private static final String SCHEMA_SCRIPT = "SQL/schema/database_schema.sql";

    @Test
    void entitiesMatchTheSqlSchema()
    {
        // Arrange
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16.2")
                .withDatabaseName("smart_city")
                .withUsername("postgres")   // the script sets "OWNER TO postgres"
                .withPassword("postgres")
                .withCopyFileToContainer(MountableFile.forHostPath(SCHEMA_SCRIPT),
                        "/docker-entrypoint-initdb.d/01_schema.sql"))
        {
            postgres.start();
            Properties props = new Properties();
            props.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
            props.put("hibernate.connection.driver_class", "org.postgresql.Driver");
            props.put("hibernate.connection.url", postgres.getJdbcUrl());
            props.put("hibernate.connection.username", postgres.getUsername());
            props.put("hibernate.connection.password", postgres.getPassword());
            props.put("hibernate.hbm2ddl.auto", "validate");
            props.put("hibernate.physical_naming_strategy",
                    "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");
            Configuration configuration = new Configuration();
            configuration.setProperties(props);
            HibernateConfig.getAnnotationConfiguration(configuration);
            ServiceRegistry registry = new StandardServiceRegistryBuilder()
                    .applySettings(configuration.getProperties())
                    .build();

            // Act and assert: building the session factory runs the validation
            assertDoesNotThrow(() -> closeAfterBuild(configuration, registry));
        }
    }

    private void closeAfterBuild(Configuration configuration, ServiceRegistry registry)
    {
        try (SessionFactory sessionFactory = configuration.buildSessionFactory(registry))
        {
            sessionFactory.getMetamodel();
        }
    }
}
