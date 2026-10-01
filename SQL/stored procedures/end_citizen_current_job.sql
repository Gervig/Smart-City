CREATE OR REPLACE PROCEDURE end_job(
    p_job_id BIGINT,
    p_end_date DATE
)
LANGUAGE plpgsql
AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM job WHERE id = p_job_id
    ) THEN
        RAISE EXCEPTION 'Job with ID % does not exist', p_job_id;
    END IF;

    UPDATE job
    SET
        end_date = p_end_date,
        status = 'ended'
    WHERE id = p_job_id;
END;
$$;