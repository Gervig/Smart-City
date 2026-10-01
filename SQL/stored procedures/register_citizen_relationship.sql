CREATE OR REPLACE PROCEDURE add_relationship(
    p_citizen_id_1 BIGINT,
    p_citizen_id_2 BIGINT,
    p_relationship_type VARCHAR,
    p_start_date DATE,
    p_end_date DATE DEFAULT NULL
)
LANGUAGE plpgsql
AS $$
BEGIN
    IF p_citizen_id_1 = p_citizen_id_2 THEN
        RAISE EXCEPTION 'A citizen cannot have a relationship with themselves';
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM citizen WHERE id = p_citizen_id_1
    ) THEN
        RAISE EXCEPTION 'Citizen with ID % does not exist', p_citizen_id_1;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM citizen WHERE id = p_citizen_id_2
    ) THEN
        RAISE EXCEPTION 'Citizen with ID % does not exist', p_citizen_id_2;
    END IF;

    INSERT INTO citizen_relationship (
        citizen_id_1,
        citizen_id_2,
        relationship_type,
        start_date,
        end_date
    )
    VALUES (
        p_citizen_id_1,
        p_citizen_id_2,
        p_relationship_type,
        p_start_date,
        p_end_date
    );
END;
$$;