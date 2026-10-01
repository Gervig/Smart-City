CREATE OR REPLACE FUNCTION update_job_status()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.end_date IS NOT NULL THEN
        NEW.status := 'ended';
    ELSE
        NEW.status := 'active';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_update_job_status
BEFORE INSERT OR UPDATE OF end_date
ON job
FOR EACH ROW
EXECUTE FUNCTION update_job_status();