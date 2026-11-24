package app.neo4j;

import app.nosql.QueryRules.Condition;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.TransactionContext;
import org.neo4j.driver.Value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * The connection to the Neo4j solution and the helpers its services share. Constraints and indexes come from
 * NoSQL/neo4j/01_constraints.cypher and the data from the migrator. Every write runs in one transaction
 * (executeWrite), so a create or update that touches several nodes and relationships is all or nothing.
 */
public class Neo4jStore implements AutoCloseable
{
    private final Driver driver;

    public Neo4jStore(String uri, String user, String password)
    {
        this.driver = GraphDatabase.driver(uri, AuthTokens.basic(user, password));
    }

    public <T> T read(Function<TransactionContext, T> work)
    {
        try (Session session = driver.session())
        {
            return session.executeRead(work::apply);
        }
    }

    public <T> T write(Function<TransactionContext, T> work)
    {
        try (Session session = driver.session())
        {
            return session.executeWrite(work::apply);
        }
    }

    /**
     * Neo4j has no sequences, so ids come from one :Sequence node per entity, increased in the same transaction as
     * the create (the node is locked until it commits). The migrator sets each one to the highest migrated id.
     */
    public static long nextId(TransactionContext tx, String sequence)
    {
        return tx.run("MERGE (s:Sequence {name: $name}) ON CREATE SET s.value = 0 SET s.value = s.value + 1 "
                + "RETURN s.value AS id", Map.of("name", sequence)).single().get("id").asLong();
    }

    /** true when the query returns at least one row */
    public static boolean exists(TransactionContext tx, String cypher, Map<String, Object> parameters)
    {
        return tx.run(cypher, parameters).hasNext();
    }

    /**
     * The shared filter rules as a WHERE clause. The expressions come from the services' allow-lists; the values
     * go in as parameters ($f0, $f1, ...), never into the query text.
     */
    public static String where(List<Condition> conditions, Map<String, Object> parameters)
    {
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < conditions.size(); i++)
        {
            Condition c = conditions.get(i);
            String name = "f" + i;
            parameters.put(name, value(c.value()));
            parts.add(switch (c.operator())
            {
                case CONTAINS -> "toLower(" + c.path() + ") CONTAINS $" + name;
                case EQUALS -> c.path() + " = $" + name;
                case AT_LEAST -> c.path() + " >= $" + name;
                case AT_MOST -> c.path() + " <= $" + name;
            });
        }
        return parts.isEmpty() ? "" : " WHERE " + String.join(" AND ", parts);
    }

    /** A parameter value Neo4j can store: it has no decimal type, so a BigDecimal becomes a float */
    public static Object value(Object value)
    {
        return value instanceof BigDecimal decimal ? decimal.doubleValue() : value;
    }

    /** Key/value pairs to a parameter map, keeping null values (SET n.x = null removes the property) */
    public static Map<String, Object> parameters(Object... keysAndValues)
    {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2)
        {
            map.put((String) keysAndValues[i], value(keysAndValues[i + 1]));
        }
        return map;
    }

    /** Properties for CREATE / SET r = $props: null values are left out (Neo4j does not store nulls) */
    public static Map<String, Object> properties(Object... keysAndValues)
    {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keysAndValues.length; i += 2)
        {
            if (keysAndValues[i + 1] != null)
            {
                map.put((String) keysAndValues[i], value(keysAndValues[i + 1]));
            }
        }
        return map;
    }

    public static Long id(Value value)
    {
        return value.isNull() ? null : value.asLong();
    }

    public static LocalDate date(Value value)
    {
        return value.isNull() ? null : value.asLocalDate();
    }

    public static String text(Value value)
    {
        return value.isNull() ? null : value.asString();
    }

    /** Back from a float to the salary's numeric(12,2) */
    public static BigDecimal decimal(Value value)
    {
        return value.isNull() ? null : BigDecimal.valueOf(value.asDouble()).setScale(2, RoundingMode.HALF_UP);
    }

    @Override
    public void close()
    {
        driver.close();
    }
}
