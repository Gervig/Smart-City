
CREATE OR REPLACE VIEW v_citizen_relationships AS
SELECT
    r.id AS relationship_id,
    c1.id AS citizen_1_id,
    c1.first_name || ' ' || c1.last_name AS citizen_1_name,
    r.relationship_type,
    c2.id AS citizen_2_id,
    c2.first_name || ' ' || c2.last_name AS citizen_2_name,
    r.start_date,
    r.end_date
FROM citizen_relationship r
JOIN citizen c1 ON r.citizen_id_1 = c1.id
JOIN citizen c2 ON r.citizen_id_2 = c2.id;