CREATE OR REPLACE PROCEDURE register_citizen(
    p_first_name VARCHAR,
    p_last_name VARCHAR,
    p_birth_date DATE,
    p_gender VARCHAR,
    p_family_id BIGINT DEFAULT NULL,
    p_housing_id BIGINT DEFAULT NULL
)
LANGUAGE plpgsql
AS $$
BEGIN
    -- Check that the family exists if one was provided
    IF p_family_id IS NOT NULL
       AND NOT EXISTS (
           SELECT 1 FROM family WHERE id = p_family_id
       )
    THEN
        RAISE EXCEPTION 'Family with ID % does not exist', p_family_id;
    END IF;

    -- Check that the housing exists if one was provided
    IF p_housing_id IS NOT NULL
       AND NOT EXISTS (
           SELECT 1 FROM housing WHERE id = p_housing_id
       )
    THEN
        RAISE EXCEPTION 'Housing with ID % does not exist', p_housing_id;
    END IF;

    INSERT INTO citizen (
        first_name,
        last_name,
        birth_date,
        gender,
        family_id,
        housing_id
    )
    VALUES (
        p_first_name,
        p_last_name,
        p_birth_date,
        p_gender,
        p_family_id,
        p_housing_id
    );
END;
$$;