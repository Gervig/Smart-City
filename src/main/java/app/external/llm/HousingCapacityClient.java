package app.external.llm;

import app.entities.Housing;

/**
 * Asks the local LLM API to estimate how many residents a housing record can hold,
 * based on its (already OSM-enriched) data. The LLM never writes to the database:
 * the caller decides what to do with the estimate.
 */
public interface HousingCapacityClient {

    Estimate estimate(Housing housing) throws LlmException;

    /**
     * @param capacity   estimated maximum number of residents, always at least 1
     * @param confidence "low", "medium" or "high" (or "unknown" if the model left it out)
     * @param reasoning  short explanation from the model, useful for the exam demo
     */
    record Estimate(int capacity, String confidence, String reasoning) {
    }
}
