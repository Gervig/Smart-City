
CREATE OR REPLACE VIEW v_housing_occupancy AS
SELECT
    h.id AS housing_id,
    l.name AS location_name,
    l.street_address,
    h.housing_type,
    h.capacity,
    COUNT(c.id) AS current_occupants,
    h.capacity - COUNT(c.id) AS available_spots
FROM housing h
JOIN location l ON h.location_id = l.id
LEFT JOIN citizen c
    ON c.housing_id = h.id
GROUP BY
    h.id,
    l.name,
    l.street_address,
    h.housing_type,
    h.capacity;