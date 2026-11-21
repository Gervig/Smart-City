package app.populators.generators;

import app.dtos.EducationRecordRequestDTO;
import app.entities.Citizen;
import app.entities.EducationRecord;
import app.validators.EducationRecordValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Business rule, not yet in the SRS
 * <p>
 * Uses real deterministic entities and validators; no database or network is needed for education histories.
 */
class EducationRecordGeneratorTest
{
    @Test
    @DisplayName("generates deterministic education periods within each citizen's lifetime")
    void generateDeterministic()
    {
        // Arrange
        var first = generateSequence(1);
        var same = generateSequence(1);
        var different = generateSequence(2);

        // Act
        var samples = IntStream.range(0, 50).mapToObj(this::generated).toList();

        // Assert
        assertEquals(first, same);
        assertNotEquals(first, different);
        for (var sample : samples)
        {
            EducationRecord record = sample.record();
            EducationRecordValidator.validate(new EducationRecordRequestDTO(null, null, record.getStartDate(),
                    record.getEndDate()));
            assertFalse(record.getStartDate().isBefore(sample.citizen().getBirthDate()));
            if (sample.citizen().getDeathDate() != null && record.getEndDate() != null)
            {
                assertFalse(record.getEndDate().isAfter(sample.citizen().getDeathDate()));
            }
        }
    }

    private java.util.List<String> generateSequence(long seed)
    {
        return IntStream.range(0, 50).mapToObj(i ->
        {
            var sample = generated(seed + i);
            return sample.record().getStartDate().toString() + sample.record().getEndDate();
        }).toList();
    }

    private Sample generated(long seed)
    {
        Random random = new Random(seed);
        Citizen citizen = new CitizenGenerator(random).generate();
        EducationRecord record = new EducationRecordGenerator(random).generate(citizen, new app.entities.Institution());
        return new Sample(citizen, record);
    }

    private record Sample(Citizen citizen, EducationRecord record)
    {
    }
}
