-- Demonstrates the privileges (run as postgres; SET ROLE switches to the user for the next statements).
-- Expected result is written after each statement. Useful as the screenshot for the report.
--   psql -U postgres -d smart_city -f /sql/users/test_privileges.sql

\echo '--- smartcity_app: can change rows, cannot change the schema'
SET ROLE smartcity_app;
SELECT count(*) FROM citizen;            -- works
DROP TABLE job;                          -- ERROR: must be owner of table job
RESET ROLE;

\echo '--- smartcity_readonly: can read, cannot write'
SET ROLE smartcity_readonly;
SELECT count(*) FROM citizen;            -- works
DELETE FROM citizen;                     -- ERROR: permission denied for table citizen
RESET ROLE;

\echo '--- smartcity_restricted: no salary, no users'
SET ROLE smartcity_restricted;
SELECT id, title FROM job LIMIT 3;       -- works
SELECT salary FROM job LIMIT 3;          -- ERROR: permission denied for table job
SELECT * FROM v_active_employment;       -- ERROR: permission denied for view v_active_employment
SELECT * FROM users;                     -- ERROR: permission denied for table users
RESET ROLE;
