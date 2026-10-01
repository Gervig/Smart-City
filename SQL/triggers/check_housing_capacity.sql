CREATE OR REPLACE FUNCTION check_housing_capacity()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    current_occupants INTEGER;
    max_capacity INTEGER;
BEGIN
    -- If the citizen isn't being assigned to housing, nothing to check
    IF NEW.housing_id IS NULL THEN
        RETURN NEW;
    END IF;

    SELECT capacity
    INTO max_capacity
    FROM housing
    WHERE id = NEW.housing_id;

    SELECT COUNT(*)
    INTO current_occupants
    FROM citizen
    WHERE housing_id = NEW.housing_id
      AND id <> NEW.id;

    IF current_occupants >= max_capacity THEN
        RAISE EXCEPTION
            'Housing % is already at full capacity',
            NEW.housing_id;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_check_housing_capacity
BEFORE INSERT OR UPDATE OF housing_id
ON citizen
FOR EACH ROW
EXECUTE FUNCTION check_housing_capacity();