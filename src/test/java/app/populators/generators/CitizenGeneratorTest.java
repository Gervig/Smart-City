package app.populators.generators;

import app.dtos.CitizenRequestDTO;
import app.entities.Citizen;
import app.validators.CitizenValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Business rule, not yet in the SRS
 * <p>
 * Uses real deterministic generators and validators; no database or network is needed for citizen attributes.
 */
class CitizenGeneratorTest
{
    @Test
    @DisplayName("generates deterministic citizens valid across repeated seeds")
    void generateDeterministic()
    {
        // Arrange
        List<String> first = generateSequence(42);
        List<String> same = generateSequence(42);
        List<String> different = generateSequence(43);

        // Act
        List<Citizen> citizens = IntStream.range(0, 50)
                .mapToObj(seed -> new CitizenGenerator(new Random(seed)).generate()).toList();

        // Assert
        assertEquals(first, same);
        assertNotEquals(first, different);
        for (Citizen citizen : citizens)
        {
            CitizenValidator.validate(request(citizen));
            assertFalse(citizen.getBirthDate().isBefore(LocalDate.of(1930, 1, 1)));
            assertFalse(citizen.getBirthDate().isAfter(LocalDate.of(2020, 12, 31)));
            if ("dead".equals(citizen.getStatus()))
            {
                assertNotEquals(null, citizen.getDeathDate());
                assertFalse(citizen.getDeathDate().isBefore(citizen.getBirthDate()));
                assertNull(citizen.getHousing());
            }
        }
    }

    private List<String> generateSequence(long seed)
    {
        return IntStream.range(0, 50).mapToObj(i ->
        {
            Citizen c = new CitizenGenerator(new Random(seed + i)).generate();
            return c.getFirstName() + c.getLastName() + c.getBirthDate() + c.getStatus() + c.getDeathDate();
        }).toList();
    }

    private CitizenRequestDTO request(Citizen c)
    {
        return new CitizenRequestDTO(c.getFirstName(), c.getLastName(), c.getBirthDate(), c.getDeathDate(),
                c.getStatus(), c.getGender(), null, null);
    }
}
