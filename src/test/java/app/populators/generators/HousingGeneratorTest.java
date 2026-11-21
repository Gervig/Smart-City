package app.populators.generators;

import app.dtos.HousingRequestDTO;
import app.entities.Housing;
import app.validators.HousingValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Business rule, not yet in the SRS
 * <p>
 * Uses real deterministic generators and validators; no database or network is needed for housing attributes.
 */
class HousingGeneratorTest
{
    @Test
    @DisplayName("generates deterministic validated housing types and capacities")
    void generateDeterministic()
    {
        // Arrange
        var first = generateSequence(1);
        var same = generateSequence(1);
        var different = generateSequence(2);

        // Act
        var samples = IntStream.range(0, 50).mapToObj(seed -> new HousingGenerator(new Random(seed)).generate()).toList();

        // Assert
        assertEquals(first, same);
        assertNotEquals(first, different);
        for (Housing housing : samples)
        {
            HousingValidator.validate(new HousingRequestDTO(null, housing.getHousingType(), housing.getCapacity()));
        }
    }

    private java.util.List<String> generateSequence(long seed)
    {
        return IntStream.range(0, 50).mapToObj(i ->
        {
            Housing housing = new HousingGenerator(new Random(seed + i)).generate();
            return housing.getHousingType() + housing.getCapacity();
        }).toList();
    }
}
