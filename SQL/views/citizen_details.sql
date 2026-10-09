 SELECT c.id AS citizen_id,
    c.first_name,
    c.last_name,
    c.birth_date,
        CASE
            WHEN c.birth_date IS NOT NULL THEN EXTRACT(year FROM age(COALESCE(c.death_date, CURRENT_DATE)::timestamp with time zone, c.birth_date::timestamp with time zone))::integer
            ELSE NULL::integer
        END AS age,
    c.gender,
    c.status,
    c.death_date,
    f.family_name,
    h.id AS housing_id,
    h.housing_type,
    h.capacity AS housing_capacity,
    l.name AS housing_location
   FROM citizen c
     LEFT JOIN family f ON c.family_id = f.id
     LEFT JOIN housing h ON c.housing_id = h.id
     LEFT JOIN location l ON h.location_id = l.id;