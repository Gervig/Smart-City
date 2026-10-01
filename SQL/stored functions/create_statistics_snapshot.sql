CREATE OR REPLACE FUNCTION create_statistics_snapshot()
RETURNS VOID
LANGUAGE plpgsql
AS $$
BEGIN
    -- Create the statistics table if it does not already exist
    CREATE TABLE IF NOT EXISTS statistics_snapshot (
        snapshot_date DATE PRIMARY KEY,
        citizen_count INTEGER NOT NULL,
        employed_citizen_count INTEGER NOT NULL,
        housing_count INTEGER NOT NULL,
        company_count INTEGER NOT NULL,
        institution_count INTEGER NOT NULL
    );

    -- Take a snapshot of the current database state
    INSERT INTO statistics_snapshot (
        snapshot_date,
        citizen_count,
        employed_citizen_count,
        housing_count,
        company_count,
        institution_count
    )
    SELECT
        CURRENT_DATE,
        (SELECT COUNT(*) FROM citizen),
        (SELECT COUNT(DISTINCT citizen_id)
         FROM job
         WHERE end_date IS NULL),
        (SELECT COUNT(*) FROM housing),
        (SELECT COUNT(*) FROM company),
        (SELECT COUNT(*) FROM institution)
    ON CONFLICT (snapshot_date)
    DO UPDATE SET
        citizen_count = EXCLUDED.citizen_count,
        employed_citizen_count = EXCLUDED.employed_citizen_count,
        housing_count = EXCLUDED.housing_count,
        company_count = EXCLUDED.company_count,
        institution_count = EXCLUDED.institution_count;
END;
$$;