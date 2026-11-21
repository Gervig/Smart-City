
package app.dtos;

import java.util.List;

public record PageResponse<T>(
        List<T> data,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <T> PageResponse<T> of(
            List<T> data,
            int page,
            int size,
            long totalElements
    ) {
        int totalPages = (int) (
                totalElements / size
                        + (totalElements % size == 0 ? 0 : 1)
        );

        return new PageResponse<>(
                data,
                page,
                size,
                totalElements,
                totalPages
        );
    }
}
