package app.populators.generators;

import app.dtos.EducationRecordRequestDTO;
import app.entities.Citizen;
import app.entities.EducationRecord;
import app.entities.Institution;
import app.validators.EducationRecordValidator;

import java.time.LocalDate;
import java.util.Random;

public final class EducationRecordGenerator
{
    private final Random random;

    public EducationRecordGenerator(Random random)
    {
        this.random = random;
    }

    public EducationRecord generate(Citizen citizen, Institution institution)
    {
        LocalDate latest = citizen.getDeathDate() == null ? LocalDate.now() : citizen.getDeathDate();
        LocalDate start = randomDate(citizen.getBirthDate(), latest);
        LocalDate end = citizen.getDeathDate() == null && random.nextBoolean() ? null : randomDate(start, latest);

        EducationRecord record = new EducationRecord();
        record.setCitizen(citizen);
        record.setInstitution(institution);
        record.setStartDate(start);
        record.setEndDate(end);
        EducationRecordValidator.validate(new EducationRecordRequestDTO(null, null, start, end));
        return record;
    }

    private LocalDate randomDate(LocalDate start, LocalDate end)
    {
        long days = end.toEpochDay() - start.toEpochDay();
        return start.plusDays(days == 0 ? 0 : random.nextInt((int) days + 1));
    }
}
