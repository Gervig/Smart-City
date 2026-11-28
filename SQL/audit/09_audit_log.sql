-- Auditing for Smart City (PostgreSQL): every change to the main tables is written to audit_log by triggers.
--
-- Run as postgres AFTER the schema (SQL/schema/database_schema.sql) and the users (SQL/users/08_users.sql):
--   psql -U postgres -d smart_city -f /sql/audit/09_audit_log.sql
-- The script can be run again.
--
-- What is recorded, per changed row: table, operation, the row's id, the whole row before and after (jsonb), the
-- columns that changed, the database user (session_user), the logged-in API user, the time and the transaction.
-- The API user comes from the backend: GenericDAO runs  SELECT set_config('smartcity.app_user', <username>, true)
-- at the start of each transaction, and the trigger reads it with current_setting('smartcity.app_user', true).
-- Changes made outside the API (psql, pgAdmin, pg_cron, populate_test_data()) have app_user NULL.
--
-- Security: the log is append-only. Only the trigger function writes to it (SECURITY DEFINER, it runs as its owner),
-- the application user cannot read, change or delete it, and a second trigger refuses UPDATE, DELETE and TRUNCATE
-- on audit_log for everybody except a superuser who disables that trigger on purpose.

-- 1. The audit table -----------------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.audit_log (
    id             bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    table_name     text        NOT NULL,
    operation      text        NOT NULL,
    row_id         bigint,                                   -- NULL for TRUNCATE (a whole table)
    old_data       jsonb,                                    -- the row before: UPDATE, DELETE
    new_data       jsonb,                                    -- the row after:  INSERT, UPDATE
    changed_fields text[],                                   -- UPDATE: the columns whose value changed
    db_user        text        NOT NULL DEFAULT session_user,
    app_user       text,                                     -- the API user, set by the backend
    changed_at     timestamptz NOT NULL DEFAULT now(),
    transaction_id bigint      NOT NULL DEFAULT txid_current(),
    CONSTRAINT chk_audit_operation CHECK (operation IN ('INSERT', 'UPDATE', 'DELETE', 'TRUNCATE')),
    CONSTRAINT chk_audit_row CHECK ((operation = 'TRUNCATE') = (row_id IS NULL))
);

COMMENT ON TABLE public.audit_log IS 'Append-only history of every change to the main tables, written by trg_audit_*';

-- "What happened to job 42?" and "what changed yesterday?"
CREATE INDEX IF NOT EXISTS idx_audit_log_row ON public.audit_log (table_name, row_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_changed_at ON public.audit_log (changed_at);

-- 2. The trigger function, shared by all audited tables ------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.audit_row_change() RETURNS trigger
    LANGUAGE plpgsql
    SECURITY DEFINER                      -- writes audit_log with the owner's rights, not the caller's
    SET search_path = public, pg_temp     -- a SECURITY DEFINER function must not trust the caller's search_path
AS $$
DECLARE
    old_row jsonb;
    new_row jsonb;
    changed text[];
BEGIN
    IF TG_OP = 'TRUNCATE' THEN
        INSERT INTO audit_log (table_name, operation, app_user)
        VALUES (TG_TABLE_NAME, TG_OP, NULLIF(current_setting('smartcity.app_user', true), ''));
        RETURN NULL;
    END IF;

    IF TG_OP <> 'INSERT' THEN
        old_row := to_jsonb(OLD);
    END IF;
    IF TG_OP <> 'DELETE' THEN
        new_row := to_jsonb(NEW);
    END IF;

    IF TG_OP = 'UPDATE' THEN
        SELECT array_agg(n.key ORDER BY n.key)
        INTO changed
        FROM jsonb_each(new_row) AS n
        WHERE n.value IS DISTINCT FROM old_row -> n.key;
        IF changed IS NULL THEN
            RETURN NULL;                  -- an update that changed nothing is not history
        END IF;
    END IF;

    INSERT INTO audit_log (table_name, operation, row_id, old_data, new_data, changed_fields, app_user)
    VALUES (TG_TABLE_NAME, TG_OP, (coalesce(new_row, old_row) ->> 'id')::bigint, old_row, new_row, changed,
            NULLIF(current_setting('smartcity.app_user', true), ''));
    RETURN NULL;                          -- AFTER trigger: the return value is ignored
END;
$$;

-- 3. The triggers on the nine main tables ---------------------------------------------------------------------------
DO $$
DECLARE
    audited text;
BEGIN
    FOREACH audited IN ARRAY ARRAY['location', 'family', 'housing', 'citizen', 'citizen_relationship',
                                   'institution', 'company', 'education_record', 'job']
    LOOP
        EXECUTE format('DROP TRIGGER IF EXISTS trg_audit_%1$s ON public.%1$I', audited);
        EXECUTE format('CREATE TRIGGER trg_audit_%1$s AFTER INSERT OR UPDATE OR DELETE ON public.%1$I '
                           || 'FOR EACH ROW EXECUTE FUNCTION public.audit_row_change()', audited);
        -- TRUNCATE does not fire row triggers, so it gets a statement trigger of its own
        EXECUTE format('DROP TRIGGER IF EXISTS trg_audit_%1$s_truncate ON public.%1$I', audited);
        EXECUTE format('CREATE TRIGGER trg_audit_%1$s_truncate AFTER TRUNCATE ON public.%1$I '
                           || 'FOR EACH STATEMENT EXECUTE FUNCTION public.audit_row_change()', audited);
    END LOOP;
END
$$;

-- 4. Append-only: nobody may rewrite or remove the history ----------------------------------------------------------
CREATE OR REPLACE FUNCTION public.audit_log_is_append_only() RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION 'audit_log is append-only: % is not allowed', TG_OP
        USING ERRCODE = 'insufficient_privilege';
END;
$$;

DROP TRIGGER IF EXISTS trg_audit_log_append_only ON public.audit_log;
CREATE TRIGGER trg_audit_log_append_only BEFORE UPDATE OR DELETE ON public.audit_log
    FOR EACH ROW EXECUTE FUNCTION public.audit_log_is_append_only();
DROP TRIGGER IF EXISTS trg_audit_log_no_truncate ON public.audit_log;
CREATE TRIGGER trg_audit_log_no_truncate BEFORE TRUNCATE ON public.audit_log
    FOR EACH STATEMENT EXECUTE FUNCTION public.audit_log_is_append_only();

-- 5. Privileges (the users come from 08_users.sql; skipped for a user that does not exist) ---------------------------
-- Nobody writes audit_log directly: the trigger function does, as its owner. 08_users.sql gives smartcity_app rights
-- on every new table (ALTER DEFAULT PRIVILEGES), so they are taken away again here.
REVOKE ALL ON public.audit_log FROM PUBLIC;
REVOKE ALL ON FUNCTION public.audit_row_change() FROM PUBLIC;
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_roles WHERE rolname = 'smartcity_app') THEN
        REVOKE ALL ON public.audit_log FROM smartcity_app;
    END IF;
    IF EXISTS (SELECT FROM pg_roles WHERE rolname = 'smartcity_restricted') THEN
        -- the history holds salaries (job.salary in old_data / new_data)
        REVOKE ALL ON public.audit_log FROM smartcity_restricted;
    END IF;
    IF EXISTS (SELECT FROM pg_roles WHERE rolname = 'smartcity_readonly') THEN
        REVOKE ALL ON public.audit_log FROM smartcity_readonly;
        GRANT SELECT ON public.audit_log TO smartcity_readonly;
    END IF;
    IF EXISTS (SELECT FROM pg_roles WHERE rolname = 'smartcity_admin') THEN
        REVOKE ALL ON public.audit_log FROM smartcity_admin;
        GRANT SELECT ON public.audit_log TO smartcity_admin;    -- read the history; the trigger refuses changes
    END IF;
END
$$;
