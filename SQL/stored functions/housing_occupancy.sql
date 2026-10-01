CREATE OR REPLACE FUNCTION housing_occupancy(
    p_housing_id BIGINT
)
RETURNS INTEGER
LANGUAGE plpgsql
AS $$
DECLARE
    occupants INTEGER;
BEGIN
    SELECT COUNT(*)
    INTO occupants
    FROM citizen
    WHERE housing_id = p_housing_id;

    RETURN occupants;
END;
$$;