
CREATE OR REPLACE VIEW v_active_employment AS
SELECT
    c.id AS citizen_id,
    c.first_name || ' ' || c.last_name AS citizen_name,
    j.title AS job_title,
    j.salary,
    comp.id AS company_id,
    comp.name AS company_name,
    j.start_date
FROM job j
JOIN citizen c ON j.citizen_id = c.id
JOIN company comp ON j.company_id = comp.id
WHERE j.end_date IS NULL
   OR j.end_date > CURRENT_DATE;