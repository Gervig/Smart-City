
DECLARE
    i INTEGER;
    v_family_id BIGINT;
    v_location_id BIGINT;
    v_housing_id BIGINT;
    v_company_id BIGINT;
    v_institution_id BIGINT;
    v_citizen_id BIGINT;

    v_families BIGINT[] := ARRAY[]::BIGINT[];
    v_locations BIGINT[] := ARRAY[]::BIGINT[];
    v_housings BIGINT[] := ARRAY[]::BIGINT[];
    v_companies BIGINT[] := ARRAY[]::BIGINT[];
    v_institutions BIGINT[] := ARRAY[]::BIGINT[];
    v_citizens BIGINT[] := ARRAY[]::BIGINT[];

    v_first_names TEXT[] := ARRAY[
        'Alex', 'Sam', 'Jordan', 'Taylor', 'Morgan',
        'Jamie', 'Robin', 'Casey', 'Avery', 'Riley'
    ];

    v_last_names TEXT[] := ARRAY[
        'Andersen', 'Nielsen', 'Hansen', 'Larsen', 'Pedersen',
        'Jensen', 'Madsen', 'Kristensen', 'Olsen', 'Thomsen'
    ];
BEGIN
    -- Remove the previous generated batch, if any.
    PERFORM remove_test_data();

    -- 1. Create 20 families.
    FOR i IN 1..20 LOOP
        INSERT INTO family (family_name)
        VALUES ('TEST_DATA_FAMILY_' || i)
        RETURNING id INTO v_family_id;

        v_families := array_append(v_families, v_family_id);
    END LOOP;

    -- 2. Create 5 locations.
    FOR i IN 1..5 LOOP
        INSERT INTO location (name, street_address)
        VALUES (
            'TEST_DATA_LOCATION_' || i,
            'Test Street ' || i
        )
        RETURNING id INTO v_location_id;

        v_locations := array_append(v_locations, v_location_id);
    END LOOP;

    -- 3. Create 10 housing records.
    FOR i IN 1..10 LOOP
        INSERT INTO housing (location_id, housing_type, capacity)
        VALUES (
            v_locations[((i - 1) % 5) + 1],
            'apartment',
            50
        )
        RETURNING id INTO v_housing_id;

        v_housings := array_append(v_housings, v_housing_id);
    END LOOP;

    -- 4. Create 10 companies.
    FOR i IN 1..10 LOOP
        INSERT INTO company (
            location_id, name, founded_date, dissolved_date, status
        )
        VALUES (
            v_locations[((i - 1) % 5) + 1],
            'TEST_DATA_COMPANY_' || i,
            DATE '2000-01-01' + (i * 100),
            NULL,
            'active'
        )
        RETURNING id INTO v_company_id;

        v_companies := array_append(v_companies, v_company_id);
    END LOOP;

	-- 5. Create 5 institutions.
	FOR i IN 1..5 LOOP
		INSERT INTO institution (location_id, name, type)
		VALUES (
			v_locations[i],
			'TEST_DATA_INSTITUTION_' || i,
			'high_school'
		)
		RETURNING id INTO v_institution_id;

		v_institutions := array_append(
			v_institutions, v_institution_id
		);
	END LOOP;

    -- 6. Create 200 citizens.
    FOR i IN 1..200 LOOP
        INSERT INTO citizen (
            first_name, last_name, birth_date,
            death_date, status, gender, family_id, housing_id
        )
        VALUES (
            v_first_names[1 + floor(random() * 10)::INTEGER],
            v_last_names[1 + floor(random() * 10)::INTEGER],
            DATE '1970-01-01' + (floor(random() * 12000)::INTEGER),
            NULL,
            'alive',
            (ARRAY['female', 'male', 'other'])
                [1 + floor(random() * 3)::INTEGER],
            v_families[1 + floor(random() * 20)::INTEGER],
            v_housings[1 + floor(random() * 10)::INTEGER]
        )
        RETURNING id INTO v_citizen_id;

        v_citizens := array_append(v_citizens, v_citizen_id);
    END LOOP;

    -- 7. Create 100 job records.
    FOR i IN 1..100 LOOP
        INSERT INTO job (
            citizen_id, company_id, institution_id,
            title, salary, start_date, end_date, status
        )
        VALUES (
            v_citizens[i],
            v_companies[1 + floor(random() * 10)::INTEGER],
            NULL,
            'Test Employee',
            30000 + floor(random() * 50000)::INTEGER,
            DATE '2015-01-01' + (i * 10),
            NULL,
            'active'
        );
    END LOOP;

    -- 8. Create 100 education records.
    FOR i IN 1..100 LOOP
        INSERT INTO education_record (
            citizen_id, institution_id, start_date, end_date
        )
        VALUES (
            v_citizens[i],
            v_institutions[1 + floor(random() * 5)::INTEGER],
            DATE '2005-01-01' + (i * 5),
            NULL
        );
    END LOOP;

	-- 9. Create 100 parent-child relationships.
	FOR i IN 1..100 LOOP
		INSERT INTO citizen_relationship (
			citizen_id_1,
			citizen_id_2,
			relationship_type,
			start_date,
			end_date
		)
		VALUES (
			v_citizens[i],
			v_citizens[i + 100],
			'parent_child',
			DATE '2020-01-01',
			NULL
		);
	END LOOP;
END;
