package app.dtos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Covers requirement(s): FR-06
 */
class PageRequestTest
{
    @Test
    @DisplayName("defaults to the first page, 20 rows, sorted by id ascending")
    void defaults()
    {
        // Act
        PageRequest request = PageRequest.defaults();

        // Assert
        assertEquals(0, request.page());
        assertEquals(20, request.size());
        assertEquals("id", request.sortField());
        assertEquals("asc", request.sortDirection());
        assertEquals(Map.of(), request.filters());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 99, PageRequest.MAX_SIZE})
    @DisplayName("[PS-V1, PS-V2, PS-V3, PS-V4] accepts a page size from 1 to 100")
    void validSize(int size)
    {
        // Act
        PageRequest request = new PageRequest(0, size, "id", "asc", Map.of());

        // Assert
        assertEquals(size, request.size());
    }

    @ParameterizedTest
    @ValueSource(ints = {Integer.MIN_VALUE, -1, 0, 101, Integer.MAX_VALUE})
    @DisplayName("[PS-I1, PS-I2, PS-I3, PS-I4, PS-I5] rejects a page size below 1 or above 100")
    void invalidSize(int size)
    {
        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new PageRequest(0, size, "id", "asc", Map.of()));

        // Assert
        assertEquals("Page size must be between 1 and 100", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 1000})
    @DisplayName("[PP-V1, PP-V2, PP-V3] accepts page 0 and higher")
    void validPage(int page)
    {
        // Act
        PageRequest request = new PageRequest(page, 20, "id", "asc", Map.of());

        // Assert
        assertEquals(page, request.page());
    }

    @ParameterizedTest
    @ValueSource(ints = {Integer.MIN_VALUE, -1})
    @DisplayName("[PP-I1, PP-I2] rejects a negative page")
    void negativePage(int page)
    {
        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new PageRequest(page, 20, "id", "asc", Map.of()));

        // Assert
        assertEquals("Page must be greater than or equal to 0", exception.getMessage());
    }

    @ParameterizedTest
    @CsvSource({"21474836, 100, false", "21474837, 100, true", "2147483647, 1, false"})
    @DisplayName("[PP-V4, PP-V5, PP-I3] rejects a page whose offset (page * size) does not fit in an int")
    void offsetOverflow(int page, int size, boolean tooLarge)
    {
        // Act / Assert
        if (tooLarge)
        {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> new PageRequest(page, size, "id", "asc", Map.of()));
            assertEquals("Page offset is too large", exception.getMessage());
        }
        else
        {
            assertEquals(page, new PageRequest(page, size, "id", "asc", Map.of()).page());
        }
    }

    @ParameterizedTest
    @CsvSource({"asc, asc", "desc, desc", "ASC, asc", "Desc, desc"})
    @DisplayName("[PO-V1, PO-V2, PO-V3] accepts asc and desc in any case and stores them in lower case")
    void validSortDirection(String direction, String expected)
    {
        // Act
        PageRequest request = new PageRequest(0, 20, "id", direction, Map.of());

        // Assert
        assertEquals(expected, request.sortDirection());
    }

    @ParameterizedTest
    @ValueSource(strings = {"sideways", "ascending", "up"})
    @DisplayName("[PO-I1] rejects an unsupported sort direction")
    void invalidSortDirection(String direction)
    {
        // Act
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> new PageRequest(0, 20, "id", direction, Map.of()));

        // Assert
        assertEquals("Sort direction must be asc or desc", exception.getMessage());
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " "})
    @DisplayName("[PO-V4] falls back to sorting by id ascending when sort field and direction are missing")
    void missingSortFallsBack(String value)
    {
        // Act
        PageRequest request = new PageRequest(0, 20, value, value, Map.of());

        // Assert
        assertEquals("id", request.sortField());
        assertEquals("asc", request.sortDirection());
    }

    @Test
    @DisplayName("stores a copy of the filters, and an empty map for null")
    void filtersAreCopied()
    {
        // Arrange
        Map<String, String> filters = new HashMap<>(Map.of("status", "alive"));

        // Act
        PageRequest request = new PageRequest(0, 20, "id", "asc", filters);
        filters.put("status", "deceased");
        PageRequest withoutFilters = new PageRequest(0, 20, "id", "asc", null);

        // Assert
        assertEquals(Map.of("status", "alive"), request.filters());
        assertEquals(Map.of(), withoutFilters.filters());
    }

    @ParameterizedTest
    @CsvSource({"0, 20, 0", "1, 20, 1", "20, 20, 1", "21, 20, 2", "5, 2, 3"})
    @DisplayName("calculates total pages, rounding up, and 0 for no results")
    void totalPages(long totalElements, int size, int expectedPages)
    {
        // Act
        PageResponse<Integer> response = PageResponse.of(List.of(), 0, size, totalElements);

        // Assert
        assertEquals(expectedPages, response.totalPages());
    }
}
