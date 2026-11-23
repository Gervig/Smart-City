package app.populators.generators;

import app.validators.FamilyValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Business rule, not yet in the SRS
 * <p>
 * Uses a real deterministic generator and no database or network because family names are generated locally.
 */
class FamilyGeneratorTest
{
    @Test
    @DisplayName("generates deterministic valid family names")
    void generateDeterministic()
    {
        // Arrange
        FamilyGenerator first = new FamilyGenerator(new Random(42));
        FamilyGenerator sameSeed = new FamilyGenerator(new Random(42));
        FamilyGenerator differentSeed = new FamilyGenerator(new Random(43));

        // Act
        var firstNames = IntStream.range(0, 50).mapToObj(i -> first.generate().getFamilyName()).toList();
        var sameNames = IntStream.range(0, 50).mapToObj(i -> sameSeed.generate().getFamilyName()).toList();
        var otherNames = IntStream.range(0, 50).mapToObj(i -> differentSeed.generate().getFamilyName()).toList();

        // Assert
        assertEquals(firstNames, sameNames);
        assertNotEquals(firstNames, otherNames);
        for (String name : firstNames)
        {
            FamilyValidator.validate(new app.dtos.FamilyRequestDTO(name));
        }
    }
}
