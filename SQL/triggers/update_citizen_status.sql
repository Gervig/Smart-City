CREATE OR REPLACE FUNCTION update_citizen_status()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.death_date IS NOT NULL THEN
        NEW.status := 'deceased';
    ELSE
        NEW.status := 'alive';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_update_citizen_status
BEFORE INSERT OR UPDATE OF death_date
ON citizen
FOR EACH ROW
EXECUTE FUNCTION update_citizen_status();