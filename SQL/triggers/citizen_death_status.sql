
CREATE OR REPLACE FUNCTION trg_fn_citizen_death_status()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.death_date IS NOT NULL THEN
        NEW.status := 'deceased';

    ELSIF NEW.status = 'deceased' THEN
        RAISE EXCEPTION
            'A deceased citizen must have a death_date recorded.';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_citizen_death_status
BEFORE INSERT OR UPDATE ON citizen
FOR EACH ROW
EXECUTE FUNCTION trg_fn_citizen_death_status();


CREATE OR REPLACE FUNCTION trg_fn_citizen_post_death_cleanup()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.death_date IS NOT NULL
       AND OLD.death_date IS DISTINCT FROM NEW.death_date THEN

        -- End active jobs on the death date.
        UPDATE job
        SET end_date = NEW.death_date
        WHERE citizen_id = NEW.id
          AND (end_date IS NULL OR end_date > NEW.death_date);

        -- Remove the citizen's housing assignment.
        UPDATE citizen
        SET housing_id = NULL
        WHERE id = NEW.id
          AND housing_id IS NOT NULL;

        -- End active relationships.
        UPDATE citizen_relationship
        SET end_date = NEW.death_date
        WHERE (citizen_id_1 = NEW.id OR citizen_id_2 = NEW.id)
          AND end_date IS NULL;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_citizen_post_death_cleanup
AFTER UPDATE OF death_date ON citizen
FOR EACH ROW
EXECUTE FUNCTION trg_fn_citizen_post_death_cleanup();