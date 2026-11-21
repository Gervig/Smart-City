package app.populators.generators;

import app.dtos.CompanyRequestDTO;
import app.entities.Company;
import app.validators.CompanyValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Random;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Business rule, not yet in the SRS
 * <p>
 * Uses real deterministic generators and validators; no database or network is needed for company attributes.
 */
class CompanyGeneratorTest
{
    @Test
    @DisplayName("generates deterministic valid company histories")
    void generateDeterministic()
    {
        // Arrange
        var first = generateSequence(1);
        var same = generateSequence(1);
        var different = generateSequence(2);

        // Act
        var samples = IntStream.range(0, 50).mapToObj(seed -> new CompanyGenerator(new Random(seed)).generate()).toList();

        // Assert
        assertEquals(first, same);
        assertNotEquals(first, different);
        for (Company company : samples)
        {
            CompanyValidator.validate(new CompanyRequestDTO(null, company.getName(), company.getFoundedDate(),
                    company.getDissolvedDate(), company.getStatus()));
            if ("dissolved".equals(company.getStatus()))
            {
                assertFalse(company.getDissolvedDate().isBefore(company.getFoundedDate()));
                assertFalse(company.getDissolvedDate().isAfter(LocalDate.now()));
            }
        }
    }

    private java.util.List<String> generateSequence(long seed)
    {
        return IntStream.range(0, 50).mapToObj(i ->
        {
            Company company = new CompanyGenerator(new Random(seed + i)).generate();
            return company.getName() + company.getFoundedDate() + company.getDissolvedDate() + company.getStatus();
        }).toList();
    }
}
