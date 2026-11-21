package app.services;

import java.util.Map;

public record SeedReport(Map<String, Integer> counts, boolean completed, String message)
{
    public SeedReport
    {
        counts = Map.copyOf(counts);
    }
}
