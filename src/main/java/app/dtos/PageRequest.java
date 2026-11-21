
package app.dtos;

import java.util.Map;
import java.util.Locale;

public record PageRequest(
        int page,
        int size,
        String sortField,
        String sortDirection,
        Map<String, String> filters
)
{
    public static final int DEFAULT_PAGE = 0;
    public static final int DEFAULT_SIZE = 20;
    public static final int MAX_SIZE = 100;
    public static final String DEFAULT_SORT_FIELD = "id";
    public static final String DEFAULT_SORT_DIRECTION = "asc";

    public PageRequest
    {
        if (page < 0)
        {
            throw new IllegalArgumentException(
                    "Page must be greater than or equal to 0");
        }

        if (size < 1 || size > MAX_SIZE)
        {
            throw new IllegalArgumentException(
                    "Page size must be between 1 and " + MAX_SIZE);
        }

        if (page > Integer.MAX_VALUE / size)
        {
            throw new IllegalArgumentException("Page offset is too large");
        }

        if (sortField == null || sortField.isBlank())
        {
            sortField = DEFAULT_SORT_FIELD;
        }

        if (sortDirection == null || sortDirection.isBlank())
        {
            sortDirection = DEFAULT_SORT_DIRECTION;
        }

        sortDirection = sortDirection.toLowerCase(Locale.ROOT);

        if (!sortDirection.equals("asc")
                && !sortDirection.equals("desc"))
        {
            throw new IllegalArgumentException(
                    "Sort direction must be asc or desc");
        }

        filters = filters == null ? Map.of() : Map.copyOf(filters);
    }

    public static PageRequest defaults()
    {
        return new PageRequest(
                DEFAULT_PAGE,
                DEFAULT_SIZE,
                DEFAULT_SORT_FIELD,
                DEFAULT_SORT_DIRECTION,
                Map.of()
        );
    }
}
