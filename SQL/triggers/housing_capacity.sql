
CREATE OR REPLACE FUNCTION trg_fn_enforce_housing_capacity()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
DECLARE
    v_occupants INTEGER;
    v_capacity INTEGER;
BEGIN
    IF NEW.housing_id IS NULL THEN
        RETURN NEW;
    END IF;

    SELECT capacity
    INTO v_capacity
    FROM housing
    WHERE id = NEW.housing_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION
            'Housing unit % does not exist.', NEW.housing_id;
    END IF;

    SELECT COUNT(*)
    INTO v_occupants
    FROM citizen
    WHERE housing_id = NEW.housing_id
      AND id <> NEW.id;

    IF v_occupants >= v_capacity THEN
        RAISE EXCEPTION
            'Housing unit % is full. Capacity: %.',
            NEW.housing_id, v_capacity;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_enforce_housing_capacity
BEFORE INSERT OR UPDATE OF housing_id ON citizen
FOR EACH ROW
EXECUTE FUNCTION trg_fn_enforce_housing_capacity();