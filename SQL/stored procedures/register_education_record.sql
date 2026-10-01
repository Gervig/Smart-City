CREATE OR REPLACE PROCEDURE add_education_record(
    p_citizen_id BIGINT,
    p_institution_id BIGINT,
    p_start_date DATE,
    p_end_date DATE DEFAULT NULL
)
LANGUAGE plpgsql
AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM citizen WHERE id = p_citizen_id
    ) THEN
        RAISE EXCEPTION 'Citizen with ID % does not exist', p_citizen_id;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM institution WHERE id = p_institution_id
    ) THEN
        RAISE EXCEPTION 'Institution with ID % does not exist',
            p_institution_id;
    END IF;

    IF p_end_date IS NOT NULL AND p_end_date < p_start_date THEN
        RAISE EXCEPTION 'End date cannot be before start date';
    END IF;

    INSERT INTO education_record (
        citizen_id,
        institution_id,
        start_date,
        end_date
    )
    VALUES (
        p_citizen_id,
        p_institution_id,
        p_start_date,
        p_end_date
    );
END;
$$;