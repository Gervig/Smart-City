
CREATE OR REPLACE FUNCTION trg_fn_validate_job_dates()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_birth_date DATE;
    v_death_date DATE;
BEGIN
    SELECT birth_date, death_date
    INTO v_birth_date, v_death_date
    FROM citizen
    WHERE id = NEW.citizen_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION
            'Citizen % does not exist.', NEW.citizen_id;
    END IF;

    IF v_birth_date IS NOT NULL
       AND NEW.start_date < v_birth_date THEN
        RAISE EXCEPTION
            'Job start date cannot be before the citizen birth date.';
    END IF;

    IF v_death_date IS NOT NULL
       AND NEW.start_date > v_death_date THEN
        RAISE EXCEPTION
            'A job cannot start after the citizen death date.';
    END IF;

    IF NEW.end_date IS NOT NULL
       AND NEW.end_date < NEW.start_date THEN
        RAISE EXCEPTION
            'Job end date cannot be earlier than its start date.';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_validate_job_dates
BEFORE INSERT OR UPDATE ON job
FOR EACH ROW
EXECUTE FUNCTION trg_fn_validate_job_dates();