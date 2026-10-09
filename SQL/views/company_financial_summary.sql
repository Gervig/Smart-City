
CREATE OR REPLACE VIEW v_company_financial_summary AS
SELECT
    comp.id AS company_id,
    comp.name AS company_name,
    COUNT(j.id) FILTER (
        WHERE j.end_date IS NULL OR j.end_date > CURRENT_DATE
    ) AS active_job_count,
    COALESCE(
        SUM(j.salary) FILTER (
            WHERE j.end_date IS NULL OR j.end_date > CURRENT_DATE
        ),
        0
    ) AS total_active_salaries
FROM company comp
LEFT JOIN job j ON j.company_id = comp.id
GROUP BY comp.id, comp.name;