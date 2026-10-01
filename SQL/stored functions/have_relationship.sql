CREATE OR REPLACE FUNCTION have_relationship(
    p_citizen_id_1 BIGINT,
    p_citizen_id_2 BIGINT,
    p_relationship_type VARCHAR
)
RETURNS BOOLEAN
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1
        FROM citizen_relationship
        WHERE LEAST(citizen_id_1, citizen_id_2)
              = LEAST(p_citizen_id_1, p_citizen_id_2)
          AND GREATEST(citizen_id_1, citizen_id_2)
              = GREATEST(p_citizen_id_1, p_citizen_id_2)
          AND relationship_type = p_relationship_type
    );
END;
$$;