
package app.controllers;

import app.dtos.PageRequest;
import io.javalin.http.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class PageRequestParser {

    private static final Set<String> RESERVED_PARAMETERS =
            Set.of("page", "size", "sort");

    public static PageRequest parse(Context ctx) {

        // Read pagination parameters.
        int page = parseInteger(
                singleParameter(ctx, "page"),
                PageRequest.DEFAULT_PAGE,
                "page"
        );

        int size = parseInteger(
                singleParameter(ctx, "size"),
                PageRequest.DEFAULT_SIZE,
                "size"
        );

        // Read sorting parameters.
        String sort = singleParameter(ctx, "sort");

        String sortField = PageRequest.DEFAULT_SORT_FIELD;
        String sortDirection = PageRequest.DEFAULT_SORT_DIRECTION;

        if (sort != null && !sort.isBlank()) {
            String[] parts = sort.split(",", -1);

            if (parts.length != 2
                    || parts[0].isBlank()
                    || parts[1].isBlank()) {
                throw new IllegalArgumentException(
                        "Sort must have the format field,direction"
                );
            }

            sortField = parts[0].trim();
            sortDirection = parts[1].trim();
        }

        // Treat all non-reserved parameters as filters.
        Map<String, String> filters = new HashMap<>();

        ctx.queryParamMap().forEach((key, values) -> {
            if (!RESERVED_PARAMETERS.contains(key)) {
                if (values.size() > 1) {
                    throw new IllegalArgumentException(
                            "Filter '" + key + "' must be specified once");
                }
                if (!values.isEmpty()) {
                    filters.put(key, values.get(0));
                }
            }
        });

        return new PageRequest(
                page,
                size,
                sortField,
                sortDirection,
                filters
        );
    }

    private static String singleParameter(Context ctx, String name) {
        var values = ctx.queryParamMap().get(name);
        if (values == null || values.isEmpty()) {
            return null;
        }
        if (values.size() > 1) {
            throw new IllegalArgumentException(
                    "Parameter '" + name + "' must be specified once");
        }
        return values.get(0);
    }

    private static int parseInteger(
            String value,
            int defaultValue,
            String parameterName
    ) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Parameter '" + parameterName
                            + "' must be an integer"
            );
        }
    }
}
