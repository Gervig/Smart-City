package app.nosql;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The filter and sort rules of the list endpoints, shared by the MongoDB and Neo4j solutions so that they accept
 * exactly the same query parameters as the PostgreSQL solution (GenericDAO):
 * <ul>
 *     <li>a text field matches case-insensitively when it contains the value (SQL: LIKE %value%),</li>
 *     <li>a filter ending in {@code From} / {@code To} is an inclusive range on a number or a date,</li>
 *     <li>any other field must be equal,</li>
 *     <li>an unknown filter or sort field is rejected with 400.</li>
 * </ul>
 * Only field names from the allow-lists reach a query, and values are always passed as parameters, so a query
 * parameter can never change the query itself (no injection).
 */
public final class QueryRules
{
    /** A filterable field: where it is stored and which Java type its value has */
    public record Field(String path, Class<?> type)
    {
    }

    public enum Operator
    {
        CONTAINS, EQUALS, AT_LEAST, AT_MOST
    }

    /** One filter, ready to translate: path, operator and the converted value (text in lower case for CONTAINS) */
    public record Condition(String path, Operator operator, Object value)
    {
    }

    private QueryRules()
    {
    }

    public static List<Condition> conditions(Map<String, String> filters, Map<String, Field> allowed)
    {
        List<Condition> conditions = new ArrayList<>();
        filters.forEach((name, value) ->
        {
            Field field = allowed.get(name);
            if (field == null)
            {
                throw new IllegalArgumentException("Unsupported filter: " + name);
            }
            if (value == null || value.isBlank())
            {
                return;
            }
            Object converted = convert(value, field.type());
            if (name.endsWith("From") || name.endsWith("To"))
            {
                if (!Number.class.isAssignableFrom(field.type()) && field.type() != LocalDate.class)
                {
                    throw new IllegalArgumentException("Range filtering is not supported for: " + name);
                }
                conditions.add(new Condition(field.path(),
                        name.endsWith("From") ? Operator.AT_LEAST : Operator.AT_MOST, converted));
            }
            else if (field.type() == String.class)
            {
                conditions.add(new Condition(field.path(), Operator.CONTAINS, value.toLowerCase(Locale.ROOT)));
            }
            else
            {
                conditions.add(new Condition(field.path(), Operator.EQUALS, converted));
            }
        });
        return conditions;
    }

    public static String sortPath(String sortField, Map<String, String> allowed)
    {
        String path = allowed.get(sortField);
        if (path == null)
        {
            throw new IllegalArgumentException("Unsupported sort field: " + sortField);
        }
        return path;
    }

    static Object convert(String value, Class<?> type)
    {
        try
        {
            if (type == String.class)
            {
                return value;
            }
            if (type == Long.class)
            {
                return Long.valueOf(value);
            }
            if (type == Integer.class)
            {
                return Integer.valueOf(value);
            }
            if (type == LocalDate.class)
            {
                return LocalDate.parse(value);
            }
            if (type == BigDecimal.class)
            {
                return new BigDecimal(value);
            }
        }
        catch (NumberFormatException | DateTimeParseException e)
        {
            throw new IllegalArgumentException("Invalid " + type.getSimpleName() + " value: " + value, e);
        }
        throw new IllegalArgumentException("Unsupported filter type: " + type.getSimpleName());
    }
}
