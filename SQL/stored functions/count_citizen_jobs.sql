CREATE OR REPLACE FUNCTION count_citizen_jobs(
    p_citizen_id BIGINT
)
RETURNS INTEGER
LANGUAGE plpgsql
AS $$
DECLARE
    job_count INTEGER;
BEGIN
    SELECT COUNT(*)
    INTO job_count
    FROM job
    WHERE citizen_id = p_citizen_id;

    RETURN job_count;
END;
$$;