package app.populators.generators;

import app.dtos.InstitutionRequestDTO;
import app.entities.Institution;
import app.validators.InstitutionValidator;

import java.util.List;
import java.util.Random;

public final class InstitutionGenerator
{
    private static final List<String> PLACES = List.of(
            "Nørrebro", "Vesterbro", "Østerbro", "Amager", "Frederiksberg", "Valby", "Vanløse", "Brønshøj",
            "Bispebjerg", "Christianshavn", "Indre By", "Nordhavn", "Sydhavn", "Kastrup", "Hellerup");
    private static final List<String> TYPES = List.of("high_school", "college", "hospital");
    private static final List<String> TYPE_WORDS = List.of("Gymnasium", "College", "Hospital");

    private final Random random;

    public InstitutionGenerator(Random random)
    {
        this.random = random;
    }

    public Institution generate()
    {
        int typeIndex = random.nextInt(TYPES.size());
        Institution institution = new Institution();
        institution.setName(PLACES.get(random.nextInt(PLACES.size())) + " " + TYPE_WORDS.get(typeIndex));
        institution.setType(TYPES.get(typeIndex));
        InstitutionValidator.validate(new InstitutionRequestDTO(null, institution.getName(), institution.getType()));
        return institution;
    }
}
