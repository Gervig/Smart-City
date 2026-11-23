package app.populators.generators;

import app.dtos.CompanyRequestDTO;
import app.entities.Company;
import app.validators.CompanyValidator;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;

public final class CompanyGenerator
{
    private static final List<String> NAMES = List.of(
            "Nordlys", "Fyrtårn", "København", "Øresund", "Birk", "Havblik", "Grønne Gader", "Lille Danmark",
            "Blå Himmel", "Morgen", "Fællesskab", "Byliv", "Broen", "Skov", "Solskin", "Nordic",
            "Håndværk", "Fremtid", "Kyst", "Vækst");
    private static final List<String> SUFFIXES = List.of("ApS", "A/S", "Group", "Service");

    private final Random random;

    public CompanyGenerator(Random random)
    {
        this.random = random;
    }

    public Company generate()
    {
        int foundedYear = 1950 + random.nextInt(71);
        LocalDate foundedDate = dateInYear(foundedYear);
        boolean dissolved = random.nextInt(100) >= 85;
        LocalDate dissolvedDate = dissolved ? dateAfter(foundedDate, LocalDate.now()) : null;

        Company company = new Company();
        company.setName(NAMES.get(random.nextInt(NAMES.size())) + " " + SUFFIXES.get(random.nextInt(SUFFIXES.size())));
        company.setFoundedDate(foundedDate);
        company.setDissolvedDate(dissolvedDate);
        company.setStatus(dissolved ? "dissolved" : "active");
        CompanyValidator.validate(new CompanyRequestDTO(null, company.getName(), foundedDate, dissolvedDate,
                company.getStatus()));
        return company;
    }

    private LocalDate dateInYear(int year)
    {
        LocalDate first = LocalDate.of(year, 1, 1);
        return first.plusDays(random.nextInt(first.lengthOfYear()));
    }

    private LocalDate dateAfter(LocalDate start, LocalDate end)
    {
        long days = end.toEpochDay() - start.toEpochDay();
        return start.plusDays(days == 0 ? 0 : 1 + random.nextInt((int) days));
    }
}
