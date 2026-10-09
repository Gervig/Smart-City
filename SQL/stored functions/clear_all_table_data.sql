
BEGIN
    TRUNCATE TABLE
        citizen_relationship,
        education_record,
        job,
        citizen,
        family,
        housing,
        company,
        institution,
        location
    RESTART IDENTITY CASCADE;

    -- Also clear the optional statistics table, if it exists.
    IF to_regclass('public.statistics_snapshot') IS NOT NULL THEN
        EXECUTE
            'TRUNCATE TABLE public.statistics_snapshot RESTART IDENTITY';
    END IF;
END;
