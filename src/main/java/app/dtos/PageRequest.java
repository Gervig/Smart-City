
package app.dtos;

import java.util.Map;

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

        if (sortField == null || sortField.isBlank())
        {
            sortField = "id";
        }

        if (sortDirection == null || sortDirection.isBlank())
        {
            sortDirection = "asc";
        }

        sortDirection = sortDirection.toLowerCase();

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
                "id",
                "asc",
                Map.of()
        );
    }
}
