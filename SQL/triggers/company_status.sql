
CREATE OR REPLACE FUNCTION trg_fn_company_dissolved_status()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.dissolved_date IS NOT NULL THEN
        NEW.status := 'dissolved';
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_company_dissolved_status
BEFORE INSERT OR UPDATE ON company
FOR EACH ROW
EXECUTE FUNCTION trg_fn_company_dissolved_status();