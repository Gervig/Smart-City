package app.populators.generators;

import app.dtos.InstitutionRequestDTO;
import app.entities.Institution;
import app.validators.InstitutionValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Business rule, not yet in the SRS
 * <p>
 * Uses real deterministic generators and validators; no database or network is needed for institution attributes.
 */
class InstitutionGeneratorTest
{
    @Test
    @DisplayName("generates deterministic validated institutions")
    void generateDeterministic()
    {
        // Arrange
        var first = generateSequence(1);
        var same = generateSequence(1);
        var different = generateSequence(2);

        // Act
        var samples = IntStream.range(0, 50).mapToObj(seed -> new InstitutionGenerator(new Random(seed)).generate()).toList();

        // Assert
        assertEquals(first, same);
        assertNotEquals(first, different);
        for (Institution institution : samples)
        {
            InstitutionValidator.validate(new InstitutionRequestDTO(null, institution.getName(), institution.getType()));
        }
    }

    private java.util.List<String> generateSequence(long seed)
    {
        return IntStream.range(0, 50).mapToObj(i ->
        {
            Institution institution = new InstitutionGenerator(new Random(seed + i)).generate();
            return institution.getName() + institution.getType();
        }).toList();
    }
}
