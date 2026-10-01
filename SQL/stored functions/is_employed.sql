CREATE OR REPLACE FUNCTION is_citizen_employed(
    p_citizen_id BIGINT
)
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1
        FROM job
        WHERE citizen_id = p_citizen_id
          AND end_date IS NULL
    );
END;
$$;