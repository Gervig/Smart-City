-- Auditing for Smart City (PostgreSQL): every change of a citizen's status is written to citizen_status_audit by a
-- trigger. Example: a PUT /citizens/{id} that makes a citizen "dead" leaves a row with the old and new status, the
-- death date, who did it and when.
--
-- Run as postgres AFTER the schema (SQL/schema/database_schema.sql) and the users (SQL/users/08_users.sql):
--   psql -U postgres -d smart_city -f /sql/audit/09_citizen_status_audit.sql
-- The script can be run again.
--
-- Only an UPDATE that really changes citizen.status is logged (WHEN OLD.status IS DISTINCT FROM NEW.status). That also
-- catches a status set by a BEFORE trigger (trg_citizen_death_status), and skips updates of other columns.
-- The API user comes from the backend: GenericDAO runs  SELECT set_config('smartcity.app_user', <username>, true)
-- at the start of each transaction, and the trigger reads it with current_setting('smartcity.app_user', true).
-- Changes made outside the API (psql, pgAdmin) have app_user NULL.
--
-- Security: the log is append-only. Only the trigger function writes to it (SECURITY DEFINER, it runs as its owner),
-- the application user cannot read, change or delete it, and a second trigger refuses UPDATE, DELETE and TRUNCATE
-- for everybody except a superuser who disables that trigger on purpose.

-- 1. The audit table -----------------------------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS public.citizen_status_audit (
    id             bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    citizen_id     bigint      NOT NULL,   -- no foreign key: the history must outlive a deleted citizen
    old_status     varchar(20) NOT NULL,
    new_status     varchar(20) NOT NULL,
    old_death_date date,
    new_death_date date,
    db_user        text        NOT NULL DEFAULT session_user,
    app_user       text,                    -- the API user, set by the backend
    changed_at     timestamptz NOT NULL DEFAULT now()
);

COMMENT ON TABLE public.citizen_status_audit IS
    'Append-only history of citizen status changes (alive/dead), written by trg_audit_citizen_status';

-- "What happened to citizen 42?"
CREATE INDEX IF NOT EXISTS idx_citizen_status_audit_citizen ON public.citizen_status_audit (citizen_id, changed_at);

-- 2. The trigger function ------------------------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION public.audit_citizen_status() RETURNS trigger
    LANGUAGE plpgsql
    SECURITY DEFINER                      -- writes the audit table with the owner's rights, not the caller's
    SET search_path = public, pg_temp     -- a SECURITY DEFINER function must not trust the caller's search_path
AS $$
BEGIN
    INSERT INTO citizen_status_audit (citizen_id, old_status, new_status, old_death_date, new_death_date, app_user)
    VALUES (NEW.id, OLD.status, NEW.status, OLD.death_date, NEW.death_date,
            NULLIF(current_setting('smartcity.app_user', true), ''));
    RETURN NULL;                          -- AFTER trigger: the return value is ignored
END;
$$;

DROP TRIGGER IF EXISTS trg_audit_citizen_status ON public.citizen;
CREATE TRIGGER trg_audit_citizen_status
    AFTER UPDATE ON public.citizen
    FOR EACH ROW
    WHEN (OLD.status IS DISTINCT FROM NEW.status)
    EXECUTE FUNCTION public.audit_citizen_status();

-- 3. Append-only: nobody may rewrite or remove the history ----------------------------------------------------------
CREATE OR REPLACE FUNCTION public.audit_is_append_only() RETURNS trigger
    LANGUAGE plpgsql
AS $$
BEGIN
    RAISE EXCEPTION '% is append-only: % is not allowed', TG_TABLE_NAME, TG_OP
        USING ERRCODE = 'insufficient_privilege';
END;
$$;

DROP TRIGGER IF EXISTS trg_citizen_status_audit_append_only ON public.citizen_status_audit;
CREATE TRIGGER trg_citizen_status_audit_append_only BEFORE UPDATE OR DELETE ON public.citizen_status_audit
    FOR EACH ROW EXECUTE FUNCTION public.audit_is_append_only();
DROP TRIGGER IF EXISTS trg_citizen_status_audit_no_truncate ON public.citizen_status_audit;
CREATE TRIGGER trg_citizen_status_audit_no_truncate BEFORE TRUNCATE ON public.citizen_status_audit
    FOR EACH STATEMENT EXECUTE FUNCTION public.audit_is_append_only();

-- 4. Privileges (the users come from 08_users.sql; skipped for a user that does not exist) ---------------------------
-- Nobody writes the table directly: the trigger function does, as its owner. 08_users.sql gives smartcity_app rights
-- on every new table (ALTER DEFAULT PRIVILEGES), so they are taken away again here.
REVOKE ALL ON public.citizen_status_audit FROM PUBLIC;
REVOKE ALL ON FUNCTION public.audit_citizen_status() FROM PUBLIC;
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_roles WHERE rolname = 'smartcity_app') THEN
        REVOKE ALL ON public.citizen_status_audit FROM smartcity_app;
    END IF;
    IF EXISTS (SELECT FROM pg_roles WHERE rolname = 'smartcity_restricted') THEN
        REVOKE ALL ON public.citizen_status_audit FROM smartcity_restricted;
    END IF;
    IF EXISTS (SELECT FROM pg_roles WHERE rolname = 'smartcity_readonly') THEN
        REVOKE ALL ON public.citizen_status_audit FROM smartcity_readonly;
        GRANT SELECT ON public.citizen_status_audit TO smartcity_readonly;
    END IF;
    IF EXISTS (SELECT FROM pg_roles WHERE rolname = 'smartcity_admin') THEN
        REVOKE ALL ON public.citizen_status_audit FROM smartcity_admin;
        GRANT SELECT ON public.citizen_status_audit TO smartcity_admin;   -- read it; the trigger refuses changes
    END IF;
END
$$;
