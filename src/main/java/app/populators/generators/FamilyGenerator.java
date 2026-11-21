package app.populators.generators;

import app.dtos.FamilyRequestDTO;
import app.entities.Family;
import app.validators.FamilyValidator;

import java.util.List;
import java.util.Random;

public final class FamilyGenerator
{
    private static final List<String> SURNAMES = List.of(
            "Jensen", "Nielsen", "Hansen", "Pedersen", "Andersen", "Christensen", "Larsen", "Sørensen",
            "Rasmussen", "Jørgensen", "Petersen", "Madsen", "Kristensen", "Olsen", "Thomsen", "Christiansen",
            "Poulsen", "Johansen", "Møller", "Mortensen", "Knudsen", "Jakobsen", "Jacobsen", "Laursen",
            "Madsen", "Henriksen", "Lund", "Holm", "Bertelsen", "Friis", "Iversen");

    private final Random random;

    public FamilyGenerator(Random random)
    {
        this.random = random;
    }

    public Family generate()
    {
        Family family = new Family();
        family.setFamilyName(SURNAMES.get(random.nextInt(SURNAMES.size())));
        FamilyValidator.validate(new FamilyRequestDTO(family.getFamilyName()));
        return family;
    }
}
