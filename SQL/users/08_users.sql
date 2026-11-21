-- Database users and privileges for Smart City (PostgreSQL).
-- Run as the superuser AFTER the schema is loaded and the application has created its own tables
-- (users, roles and the join table are created by Hibernate), e.g.:
--   psql -U postgres -d smart_city -f /sql/users/08_users.sql
-- The script can be run again: roles are only created when missing and GRANT is repeatable.
-- The passwords are development passwords. Change them for any shared or deployed database.

-- 1. Roles ---------------------------------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'smartcity_app') THEN
        CREATE ROLE smartcity_app LOGIN PASSWORD 'app-dev-password';
    END IF;
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'smartcity_admin') THEN
        CREATE ROLE smartcity_admin LOGIN PASSWORD 'admin-dev-password';
    END IF;
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'smartcity_readonly') THEN
        CREATE ROLE smartcity_readonly LOGIN PASSWORD 'readonly-dev-password';
    END IF;
    IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'smartcity_restricted') THEN
        CREATE ROLE smartcity_restricted LOGIN PASSWORD 'restricted-dev-password';
    END IF;
END
$$;

-- Start from nothing so that a re-run also removes rights that were taken out of this script.
REVOKE ALL ON ALL TABLES IN SCHEMA public FROM smartcity_app, smartcity_admin, smartcity_readonly, smartcity_restricted;
REVOKE ALL ON ALL SEQUENCES IN SCHEMA public FROM smartcity_app, smartcity_admin, smartcity_readonly, smartcity_restricted;
REVOKE ALL ON DATABASE smart_city FROM PUBLIC;

GRANT CONNECT ON DATABASE smart_city TO smartcity_app, smartcity_admin, smartcity_readonly, smartcity_restricted;
GRANT USAGE ON SCHEMA public TO smartcity_app, smartcity_admin, smartcity_readonly, smartcity_restricted;

-- 2. Application user: least privilege ------------------------------------------------------------
-- Read and write rows and use the id sequences. No CREATE, ALTER, DROP or TRUNCATE.
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO smartcity_app;
GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA public TO smartcity_app;
-- Only while hibernate.hbm2ddl.auto is "update" or "create" (Hibernate then creates tables).
-- Remove this line when the schema comes from the SQL scripts and Hibernate is set to "validate":
-- GRANT CREATE ON SCHEMA public TO smartcity_app;

-- 3. Admin user: full rights on the database ------------------------------------------------------
GRANT ALL PRIVILEGES ON DATABASE smart_city TO smartcity_admin;
GRANT ALL PRIVILEGES ON SCHEMA public TO smartcity_admin;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO smartcity_admin;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO smartcity_admin;
GRANT ALL PRIVILEGES ON ALL ROUTINES IN SCHEMA public TO smartcity_admin;

-- 4. Read-only user: SELECT on everything ---------------------------------------------------------
GRANT SELECT ON ALL TABLES IN SCHEMA public TO smartcity_readonly;

-- 5. Restricted read user: no access to sensitive data --------------------------------------------
-- Sensitive: job.salary (and the views built on it) and the users, roles and password hashes.
-- Only the tables and views listed here are granted, so anything new is hidden by default.
GRANT SELECT ON
    citizen, family, housing, location, citizen_relationship,
    company, institution, education_record,
    citizen_details, v_citizen_relationships, v_housing_occupancy
TO smartcity_restricted;
-- Column-level grant: every job column except salary.
GRANT SELECT (id, citizen_id, company_id, institution_id, title, start_date, end_date, status)
    ON job TO smartcity_restricted;
-- Not granted: users, roles, v_active_employment and v_company_financial_summary (both show salaries).

-- 6. Objects created later by the postgres user get the same rights -------------------------------
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO smartcity_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE, SELECT, UPDATE ON SEQUENCES TO smartcity_app;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO smartcity_admin;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO smartcity_admin;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO smartcity_readonly;
