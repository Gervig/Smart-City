package app.populators.generators;

import app.dtos.CitizenRequestDTO;
import app.entities.Citizen;
import app.validators.CitizenValidator;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;

public final class CitizenGenerator
{
    private static final List<String> FIRST_NAMES = List.of(
            "Anna", "Freja", "Clara", "Ida", "Emma", "Alma", "Karla", "Sofie", "Ella", "Agnes",
            "William", "Noah", "Oscar", "Carl", "Malthe", "Alfred", "August", "Emil", "Oliver", "Aksel");
    private static final List<String> LAST_NAMES = List.of(
            "Jensen", "Nielsen", "Hansen", "Pedersen", "Andersen", "Christensen", "Larsen", "Sørensen",
            "Rasmussen", "Jørgensen", "Petersen", "Madsen", "Kristensen", "Olsen", "Thomsen", "Christiansen",
            "Poulsen", "Johansen", "Møller", "Mortensen", "Knudsen", "Jakobsen", "Jacobsen", "Laursen",
            "Henriksen", "Lund", "Holm", "Bertelsen", "Friis", "Iversen");
    private static final List<String> GENDERS = List.of("female", "male", "other");

    private final Random random;

    public CitizenGenerator(Random random)
    {
        this.random = random;
    }

    public Citizen generate()
    {
        LocalDate today = LocalDate.now();
        int birthYear = 1930 + random.nextInt(91);
        LocalDate birthDate = dateInYear(birthYear);
        while (birthDate.isAfter(today))
        {
            birthDate = birthDate.minusDays(1);
        }

        boolean dead = random.nextInt(100) >= 90;
        if (dead && birthDate.isAfter(today.minusDays(1)))
        {
            birthDate = today.minusDays(1);
        }
        LocalDate deathDate = dead ? randomDateAfter(birthDate, today) : null;

        Citizen citizen = new Citizen();
        citizen.setFirstName(FIRST_NAMES.get(random.nextInt(FIRST_NAMES.size())));
        citizen.setLastName(LAST_NAMES.get(random.nextInt(LAST_NAMES.size())));
        citizen.setBirthDate(birthDate);
        citizen.setDeathDate(deathDate);
        citizen.setStatus(dead ? "deceased" : "alive");
        citizen.setGender(GENDERS.get(random.nextInt(GENDERS.size())));
        CitizenValidator.validate(new CitizenRequestDTO(citizen.getFirstName(), citizen.getLastName(), birthDate,
                deathDate, citizen.getStatus(), citizen.getGender(), null, null));
        return citizen;
    }

    private LocalDate dateInYear(int year)
    {
        LocalDate first = LocalDate.of(year, 1, 1);
        int days = first.lengthOfYear();
        return first.plusDays(random.nextInt(days));
    }

    private LocalDate randomDateAfter(LocalDate start, LocalDate end)
    {
        long days = end.toEpochDay() - start.toEpochDay();
        return start.plusDays(1 + random.nextInt((int) days));
    }
}
