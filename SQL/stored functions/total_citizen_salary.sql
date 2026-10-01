CREATE OR REPLACE FUNCTION total_citizen_salary(
    p_citizen_id BIGINT
)
RETURNS NUMERIC
LANGUAGE plpgsql
AS $$
DECLARE
    total NUMERIC;
BEGIN
    SELECT COALESCE(SUM(salary), 0)
    INTO total
    FROM job
    WHERE citizen_id = p_citizen_id;

    RETURN total;
END;
$$;