
BEGIN
    -- Delete relationships involving generated citizens.
    DELETE FROM citizen_relationship
    WHERE citizen_id_1 IN (
        SELECT c.id
        FROM citizen c
        JOIN family f ON f.id = c.family_id
        WHERE f.family_name LIKE 'TEST_DATA_FAMILY_%'
    )
    OR citizen_id_2 IN (
        SELECT c.id
        FROM citizen c
        JOIN family f ON f.id = c.family_id
        WHERE f.family_name LIKE 'TEST_DATA_FAMILY_%'
    );

    -- Delete generated jobs.
    DELETE FROM job
    WHERE citizen_id IN (
        SELECT c.id
        FROM citizen c
        JOIN family f ON f.id = c.family_id
        WHERE f.family_name LIKE 'TEST_DATA_FAMILY_%'
    )
    OR company_id IN (
        SELECT id FROM company
        WHERE name LIKE 'TEST_DATA_COMPANY_%'
    )
    OR institution_id IN (
        SELECT id FROM institution
        WHERE name LIKE 'TEST_DATA_INSTITUTION_%'
    );

    -- Delete generated education records.
    DELETE FROM education_record
    WHERE citizen_id IN (
        SELECT c.id
        FROM citizen c
        JOIN family f ON f.id = c.family_id
        WHERE f.family_name LIKE 'TEST_DATA_FAMILY_%'
    )
    OR institution_id IN (
        SELECT id FROM institution
        WHERE name LIKE 'TEST_DATA_INSTITUTION_%'
    );

    -- Delete generated citizens and families.
    DELETE FROM citizen
    WHERE family_id IN (
        SELECT id FROM family
        WHERE family_name LIKE 'TEST_DATA_FAMILY_%'
    );

    DELETE FROM family
    WHERE family_name LIKE 'TEST_DATA_FAMILY_%';

    -- Delete generated companies, institutions and housing.
    DELETE FROM company
    WHERE name LIKE 'TEST_DATA_COMPANY_%';

    DELETE FROM institution
    WHERE name LIKE 'TEST_DATA_INSTITUTION_%';

    DELETE FROM housing
    WHERE location_id IN (
        SELECT id FROM location
        WHERE name LIKE 'TEST_DATA_LOCATION_%'
    );

    -- Delete generated locations last.
    DELETE FROM location
    WHERE name LIKE 'TEST_DATA_LOCATION_%';
END;
