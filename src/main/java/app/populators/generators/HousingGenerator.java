package app.populators.generators;

import app.dtos.HousingRequestDTO;
import app.entities.Housing;
import app.validators.HousingValidator;

import java.util.List;
import java.util.Random;

public final class HousingGenerator
{
    private static final List<String> TYPES = List.of("house", "apartment");
    private final Random random;

    public HousingGenerator(Random random)
    {
        this.random = random;
    }

    public Housing generate()
    {
        Housing housing = new Housing();
        housing.setHousingType(TYPES.get(random.nextInt(TYPES.size())));
        housing.setCapacity(1 + random.nextInt(6));
        HousingValidator.validate(new HousingRequestDTO(null, housing.getHousingType(), housing.getCapacity()));
        return housing;
    }
}
