package app.dtos;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PageRequestTest
{
    @Test
    void defaultsToFirstPageAndStandardSize()
    {
        PageRequest request = PageRequest.defaults();

        assertEquals(0, request.page());
        assertEquals(20, request.size());
        assertEquals("id", request.sortField());
        assertEquals("asc", request.sortDirection());
    }

    @Test
    void rejectsInvalidPageSizeAndOverflowingOffset()
    {
        assertThrows(IllegalArgumentException.class,
                () -> new PageRequest(0, 0, "id", "asc", Map.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new PageRequest(0, 101, "id", "asc", Map.of()));
        assertThrows(IllegalArgumentException.class,
                () -> new PageRequest(Integer.MAX_VALUE, 100, "id", "asc", Map.of()));
    }

    @Test
    void rejectsUnsupportedSortDirection()
    {
        assertThrows(IllegalArgumentException.class,
                () -> new PageRequest(0, 20, "id", "sideways", Map.of()));
    }

    @Test
    void calculatesTotalPagesForNormalAndEmptyResults()
    {
        assertEquals(3, PageResponse.of(List.of(1, 2), 0, 2, 5).totalPages());
        assertEquals(0, PageResponse.of(List.of(), 0, 20, 0).totalPages());
    }
}
