--
-- PostgreSQL database dump
--

-- Dumped from database version 16.2 (Debian 16.2-1.pgdg120+2)
-- Dumped by pg_dump version 16.1

-- Started on 2026-10-09 08:04:23 UTC

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- TOC entry 262 (class 1255 OID 57506)
-- Name: add_education_record(bigint, bigint, date, date); Type: PROCEDURE; Schema: public; Owner: postgres
--

CREATE PROCEDURE public.add_education_record(IN p_citizen_id bigint, IN p_institution_id bigint, IN p_start_date date, IN p_end_date date DEFAULT NULL::date)
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


ALTER PROCEDURE public.add_education_record(IN p_citizen_id bigint, IN p_institution_id bigint, IN p_start_date date, IN p_end_date date) OWNER TO postgres;

--
-- TOC entry 260 (class 1255 OID 57504)
-- Name: add_relationship(bigint, bigint, character varying, date, date); Type: PROCEDURE; Schema: public; Owner: postgres
--

CREATE PROCEDURE public.add_relationship(IN p_citizen_id_1 bigint, IN p_citizen_id_2 bigint, IN p_relationship_type character varying, IN p_start_date date, IN p_end_date date DEFAULT NULL::date)
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


ALTER PROCEDURE public.add_relationship(IN p_citizen_id_1 bigint, IN p_citizen_id_2 bigint, IN p_relationship_type character varying, IN p_start_date date, IN p_end_date date) OWNER TO postgres;

--
-- TOC entry 244 (class 1255 OID 57497)
-- Name: check_housing_capacity(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.check_housing_capacity() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    current_occupants INTEGER;
    max_capacity INTEGER;
BEGIN
    -- If the citizen isn't being assigned to housing, nothing to check
    IF NEW.housing_id IS NULL THEN
        RETURN NEW;
    END IF;

    SELECT capacity
    INTO max_capacity
    FROM housing
    WHERE id = NEW.housing_id;

    SELECT COUNT(*)
    INTO current_occupants
    FROM citizen
    WHERE housing_id = NEW.housing_id
      AND id <> NEW.id;

    IF current_occupants >= max_capacity THEN
        RAISE EXCEPTION
            'Housing % is already at full capacity',
            NEW.housing_id;
    END IF;

    RETURN NEW;
END;
$$;


ALTER FUNCTION public.check_housing_capacity() OWNER TO postgres;

--
-- TOC entry 263 (class 1255 OID 57509)
-- Name: clear_all_table_data(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.clear_all_table_data() RETURNS void
    LANGUAGE plpgsql
    AS $$
BEGIN
    TRUNCATE TABLE
        citizen_relationship,
        education_record,
        job,
        citizen,
        family,
        housing,
        company,
        institution,
        location
    RESTART IDENTITY CASCADE;

    -- Also clear the optional statistics table, if it exists.
    IF to_regclass('public.statistics_snapshot') IS NOT NULL THEN
        EXECUTE
            'TRUNCATE TABLE public.statistics_snapshot RESTART IDENTITY';
    END IF;
END;
$$;


ALTER FUNCTION public.clear_all_table_data() OWNER TO postgres;

--
-- TOC entry 238 (class 1255 OID 57486)
-- Name: count_citizen_jobs(bigint); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.count_citizen_jobs(p_citizen_id bigint) RETURNS integer
    LANGUAGE plpgsql
    AS $$
DECLARE
    job_count INTEGER;
BEGIN
    SELECT COUNT(*)
    INTO job_count
    FROM job
    WHERE citizen_id = p_citizen_id;

    RETURN job_count;
END;
$$;


ALTER FUNCTION public.count_citizen_jobs(p_citizen_id bigint) OWNER TO postgres;

--
-- TOC entry 239 (class 1255 OID 57487)
-- Name: create_statistics_snapshot(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.create_statistics_snapshot() RETURNS void
    LANGUAGE plpgsql
    AS $$
BEGIN
    -- Create the statistics table if it does not already exist
    CREATE TABLE IF NOT EXISTS statistics_snapshot (
        snapshot_date DATE PRIMARY KEY,
        citizen_count INTEGER NOT NULL,
        employed_citizen_count INTEGER NOT NULL,
        housing_count INTEGER NOT NULL,
        company_count INTEGER NOT NULL,
        institution_count INTEGER NOT NULL
    );

    -- Take a snapshot of the current database state
    INSERT INTO statistics_snapshot (
        snapshot_date,
        citizen_count,
        employed_citizen_count,
        housing_count,
        company_count,
        institution_count
    )
    SELECT
        CURRENT_DATE,
        (SELECT COUNT(*) FROM citizen),
        (SELECT COUNT(DISTINCT citizen_id)
         FROM job
         WHERE end_date IS NULL),
        (SELECT COUNT(*) FROM housing),
        (SELECT COUNT(*) FROM company),
        (SELECT COUNT(*) FROM institution)
    ON CONFLICT (snapshot_date)
    DO UPDATE SET
        citizen_count = EXCLUDED.citizen_count,
        employed_citizen_count = EXCLUDED.employed_citizen_count,
        housing_count = EXCLUDED.housing_count,
        company_count = EXCLUDED.company_count,
        institution_count = EXCLUDED.institution_count;
END;
$$;


ALTER FUNCTION public.create_statistics_snapshot() OWNER TO postgres;

--
-- TOC entry 247 (class 1255 OID 57503)
-- Name: end_job(bigint, date); Type: PROCEDURE; Schema: public; Owner: postgres
--

CREATE PROCEDURE public.end_job(IN p_job_id bigint, IN p_end_date date)
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM job WHERE id = p_job_id
    ) THEN
        RAISE EXCEPTION 'Job with ID % does not exist', p_job_id;
    END IF;

    UPDATE job
    SET
        end_date = p_end_date,
        status = 'ended'
    WHERE id = p_job_id;
END;
$$;


ALTER PROCEDURE public.end_job(IN p_job_id bigint, IN p_end_date date) OWNER TO postgres;

--
-- TOC entry 240 (class 1255 OID 57488)
-- Name: have_relationship(bigint, bigint, character varying); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.have_relationship(p_citizen_id_1 bigint, p_citizen_id_2 bigint, p_relationship_type character varying) RETURNS boolean
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


ALTER FUNCTION public.have_relationship(p_citizen_id_1 bigint, p_citizen_id_2 bigint, p_relationship_type character varying) OWNER TO postgres;

--
-- TOC entry 241 (class 1255 OID 57489)
-- Name: housing_occupancy(bigint); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.housing_occupancy(p_housing_id bigint) RETURNS integer
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


ALTER FUNCTION public.housing_occupancy(p_housing_id bigint) OWNER TO postgres;

--
-- TOC entry 242 (class 1255 OID 57490)
-- Name: is_citizen_employed(bigint); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.is_citizen_employed(p_citizen_id bigint) RETURNS boolean
    LANGUAGE plpgsql
    AS $$
BEGIN
    RETURN EXISTS (
        SELECT 1
        FROM job
        WHERE citizen_id = p_citizen_id
          AND end_date IS NULL
    );
END;
$$;


ALTER FUNCTION public.is_citizen_employed(p_citizen_id bigint) OWNER TO postgres;

--
-- TOC entry 269 (class 1255 OID 57508)
-- Name: populate_test_data(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.populate_test_data() RETURNS void
    LANGUAGE plpgsql
    AS $$
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
$$;


ALTER FUNCTION public.populate_test_data() OWNER TO postgres;

--
-- TOC entry 261 (class 1255 OID 57505)
-- Name: register_citizen(character varying, character varying, date, character varying, bigint, bigint); Type: PROCEDURE; Schema: public; Owner: postgres
--

CREATE PROCEDURE public.register_citizen(IN p_first_name character varying, IN p_last_name character varying, IN p_birth_date date, IN p_gender character varying, IN p_family_id bigint DEFAULT NULL::bigint, IN p_housing_id bigint DEFAULT NULL::bigint)
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


ALTER PROCEDURE public.register_citizen(IN p_first_name character varying, IN p_last_name character varying, IN p_birth_date date, IN p_gender character varying, IN p_family_id bigint, IN p_housing_id bigint) OWNER TO postgres;

--
-- TOC entry 248 (class 1255 OID 57507)
-- Name: remove_test_data(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.remove_test_data() RETURNS void
    LANGUAGE plpgsql
    AS $$
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
$$;


ALTER FUNCTION public.remove_test_data() OWNER TO postgres;

--
-- TOC entry 243 (class 1255 OID 57491)
-- Name: total_citizen_salary(bigint); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.total_citizen_salary(p_citizen_id bigint) RETURNS numeric
    LANGUAGE plpgsql
    AS $$
DECLARE
    total NUMERIC;
BEGIN
    SELECT COALESCE(SUM(salary), 0)
    INTO total
    FROM job
    WHERE citizen_id = p_citizen_id;

    RETURN total;
END;
$$;


ALTER FUNCTION public.total_citizen_salary(p_citizen_id bigint) OWNER TO postgres;

--
-- TOC entry 264 (class 1255 OID 57530)
-- Name: trg_fn_citizen_death_status(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.trg_fn_citizen_death_status() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF NEW.death_date IS NOT NULL THEN
        NEW.status := 'deceased';

    ELSIF NEW.status = 'deceased' THEN
        RAISE EXCEPTION
            'A deceased citizen must have a death_date recorded.';
    END IF;

    RETURN NEW;
END;
$$;


ALTER FUNCTION public.trg_fn_citizen_death_status() OWNER TO postgres;

--
-- TOC entry 265 (class 1255 OID 57532)
-- Name: trg_fn_citizen_post_death_cleanup(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.trg_fn_citizen_post_death_cleanup() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF NEW.death_date IS NOT NULL
       AND OLD.death_date IS DISTINCT FROM NEW.death_date THEN

        -- End active jobs on the death date.
        UPDATE job
        SET end_date = NEW.death_date
        WHERE citizen_id = NEW.id
          AND (end_date IS NULL OR end_date > NEW.death_date);

        -- Remove the citizen's housing assignment.
        UPDATE citizen
        SET housing_id = NULL
        WHERE id = NEW.id
          AND housing_id IS NOT NULL;

        -- End active relationships.
        UPDATE citizen_relationship
        SET end_date = NEW.death_date
        WHERE (citizen_id_1 = NEW.id OR citizen_id_2 = NEW.id)
          AND end_date IS NULL;
    END IF;

    RETURN NEW;
END;
$$;


ALTER FUNCTION public.trg_fn_citizen_post_death_cleanup() OWNER TO postgres;

--
-- TOC entry 266 (class 1255 OID 57534)
-- Name: trg_fn_company_dissolved_status(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.trg_fn_company_dissolved_status() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF NEW.dissolved_date IS NOT NULL THEN
        NEW.status := 'dissolved';
    END IF;

    RETURN NEW;
END;
$$;


ALTER FUNCTION public.trg_fn_company_dissolved_status() OWNER TO postgres;

--
-- TOC entry 267 (class 1255 OID 57536)
-- Name: trg_fn_enforce_housing_capacity(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.trg_fn_enforce_housing_capacity() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_occupants INTEGER;
    v_capacity INTEGER;
BEGIN
    IF NEW.housing_id IS NULL THEN
        RETURN NEW;
    END IF;

    SELECT capacity
    INTO v_capacity
    FROM housing
    WHERE id = NEW.housing_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION
            'Housing unit % does not exist.', NEW.housing_id;
    END IF;

    SELECT COUNT(*)
    INTO v_occupants
    FROM citizen
    WHERE housing_id = NEW.housing_id
      AND id <> NEW.id;

    IF v_occupants >= v_capacity THEN
        RAISE EXCEPTION
            'Housing unit % is full. Capacity: %.',
            NEW.housing_id, v_capacity;
    END IF;

    RETURN NEW;
END;
$$;


ALTER FUNCTION public.trg_fn_enforce_housing_capacity() OWNER TO postgres;

--
-- TOC entry 268 (class 1255 OID 57538)
-- Name: trg_fn_validate_job_dates(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.trg_fn_validate_job_dates() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
    v_birth_date DATE;
    v_death_date DATE;
BEGIN
    SELECT birth_date, death_date
    INTO v_birth_date, v_death_date
    FROM citizen
    WHERE id = NEW.citizen_id;

    IF NOT FOUND THEN
        RAISE EXCEPTION
            'Citizen % does not exist.', NEW.citizen_id;
    END IF;

    IF v_birth_date IS NOT NULL
       AND NEW.start_date < v_birth_date THEN
        RAISE EXCEPTION
            'Job start date cannot be before the citizen birth date.';
    END IF;

    IF v_death_date IS NOT NULL
       AND NEW.start_date > v_death_date THEN
        RAISE EXCEPTION
            'A job cannot start after the citizen death date.';
    END IF;

    IF NEW.end_date IS NOT NULL
       AND NEW.end_date < NEW.start_date THEN
        RAISE EXCEPTION
            'Job end date cannot be earlier than its start date.';
    END IF;

    RETURN NEW;
END;
$$;


ALTER FUNCTION public.trg_fn_validate_job_dates() OWNER TO postgres;

--
-- TOC entry 245 (class 1255 OID 57499)
-- Name: update_citizen_status(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.update_citizen_status() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF NEW.death_date IS NOT NULL THEN
        NEW.status := 'deceased';
    ELSE
        NEW.status := 'alive';
    END IF;

    RETURN NEW;
END;
$$;


ALTER FUNCTION public.update_citizen_status() OWNER TO postgres;

--
-- TOC entry 246 (class 1255 OID 57501)
-- Name: update_job_status(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE FUNCTION public.update_job_status() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
BEGIN
    IF NEW.end_date IS NOT NULL THEN
        NEW.status := 'ended';
    ELSE
        NEW.status := 'active';
    END IF;

    RETURN NEW;
END;
$$;


ALTER FUNCTION public.update_job_status() OWNER TO postgres;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- TOC entry 226 (class 1259 OID 57387)
-- Name: citizen; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.citizen (
    id bigint NOT NULL,
    first_name character varying(100) NOT NULL,
    last_name character varying(100) NOT NULL,
    birth_date date NOT NULL,
    death_date date,
    status character varying(10) DEFAULT 'alive'::character varying NOT NULL,
    gender character varying(50),
    family_id bigint NOT NULL,
    housing_id bigint,
    CONSTRAINT chk_citizen_dates CHECK (((birth_date <= CURRENT_DATE) AND ((death_date IS NULL) OR (death_date >= birth_date)))),
    CONSTRAINT chk_citizen_status CHECK (((status)::text = ANY ((ARRAY['alive'::character varying, 'dead'::character varying])::text[]))),
    CONSTRAINT chk_citizen_status_dates CHECK (((((status)::text = 'alive'::text) AND (death_date IS NULL)) OR (((status)::text = 'dead'::text) AND (death_date IS NOT NULL))))
);


ALTER TABLE public.citizen OWNER TO postgres;

--
-- TOC entry 216 (class 1259 OID 57335)
-- Name: family; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.family (
    id bigint NOT NULL,
    family_name character varying(150) NOT NULL
);


ALTER TABLE public.family OWNER TO postgres;

--
-- TOC entry 224 (class 1259 OID 57374)
-- Name: housing; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.housing (
    id bigint NOT NULL,
    location_id bigint NOT NULL,
    housing_type character varying(30) NOT NULL,
    capacity integer NOT NULL,
    CONSTRAINT chk_housing_capacity CHECK ((capacity > 0)),
    CONSTRAINT chk_housing_type CHECK (((housing_type)::text = ANY ((ARRAY['house'::character varying, 'apartment'::character varying])::text[])))
);


ALTER TABLE public.housing OWNER TO postgres;

--
-- TOC entry 218 (class 1259 OID 57341)
-- Name: location; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.location (
    id bigint NOT NULL,
    name character varying(150) NOT NULL,
    street_address character varying(255) NOT NULL
);


ALTER TABLE public.location OWNER TO postgres;

--
-- TOC entry 233 (class 1259 OID 57492)
-- Name: citizen_details; Type: VIEW; Schema: public; Owner: postgres
--

CREATE VIEW public.citizen_details AS
 SELECT c.id AS citizen_id,
    c.first_name,
    c.last_name,
    c.birth_date,
        CASE
            WHEN (c.birth_date IS NOT NULL) THEN (EXTRACT(year FROM age((COALESCE(c.death_date, CURRENT_DATE))::timestamp with time zone, (c.birth_date)::timestamp with time zone)))::integer
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
   FROM (((public.citizen c
     LEFT JOIN public.family f ON ((c.family_id = f.id)))
     LEFT JOIN public.housing h ON ((c.housing_id = h.id)))
     LEFT JOIN public.location l ON ((h.location_id = l.id)));


ALTER VIEW public.citizen_details OWNER TO postgres;

--
-- TOC entry 225 (class 1259 OID 57386)
-- Name: citizen_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.citizen ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.citizen_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 228 (class 1259 OID 57407)
-- Name: citizen_relationship; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.citizen_relationship (
    id bigint NOT NULL,
    citizen_id_1 bigint NOT NULL,
    citizen_id_2 bigint NOT NULL,
    relationship_type character varying(20) NOT NULL,
    start_date date NOT NULL,
    end_date date,
    CONSTRAINT chk_different_citizens CHECK ((citizen_id_1 <> citizen_id_2)),
    CONSTRAINT chk_relationship_dates CHECK (((end_date IS NULL) OR (end_date >= start_date))),
    CONSTRAINT chk_relationship_type CHECK (((relationship_type)::text = ANY ((ARRAY['parent_child'::character varying, 'marriage'::character varying])::text[])))
);


ALTER TABLE public.citizen_relationship OWNER TO postgres;

--
-- TOC entry 227 (class 1259 OID 57406)
-- Name: citizen_relationship_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.citizen_relationship ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.citizen_relationship_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 220 (class 1259 OID 57347)
-- Name: company; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.company (
    id bigint NOT NULL,
    location_id bigint NOT NULL,
    name character varying(200) NOT NULL,
    founded_date date NOT NULL,
    dissolved_date date,
    status character varying(20) DEFAULT 'active'::character varying NOT NULL,
    CONSTRAINT chk_company_dates CHECK (((dissolved_date IS NULL) OR (dissolved_date >= founded_date))),
    CONSTRAINT chk_company_status CHECK (((status)::text = ANY ((ARRAY['active'::character varying, 'dissolved'::character varying])::text[]))),
    CONSTRAINT chk_company_status_dates CHECK (((((status)::text = 'active'::text) AND (dissolved_date IS NULL)) OR (((status)::text = 'dissolved'::text) AND (dissolved_date IS NOT NULL))))
);


ALTER TABLE public.company OWNER TO postgres;

--
-- TOC entry 219 (class 1259 OID 57346)
-- Name: company_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.company ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.company_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 232 (class 1259 OID 57454)
-- Name: education_record; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.education_record (
    id bigint NOT NULL,
    citizen_id bigint NOT NULL,
    institution_id bigint NOT NULL,
    start_date date NOT NULL,
    end_date date,
    CONSTRAINT chk_education_dates CHECK (((end_date IS NULL) OR (end_date >= start_date)))
);


ALTER TABLE public.education_record OWNER TO postgres;

--
-- TOC entry 231 (class 1259 OID 57453)
-- Name: education_record_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.education_record ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.education_record_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 215 (class 1259 OID 57334)
-- Name: family_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.family ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.family_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 223 (class 1259 OID 57373)
-- Name: housing_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.housing ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.housing_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 222 (class 1259 OID 57362)
-- Name: institution; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.institution (
    id bigint NOT NULL,
    location_id bigint NOT NULL,
    name character varying(200) NOT NULL,
    type character varying(30) NOT NULL,
    CONSTRAINT chk_institution_type CHECK (((type)::text = ANY ((ARRAY['high_school'::character varying, 'college'::character varying, 'hospital'::character varying])::text[])))
);


ALTER TABLE public.institution OWNER TO postgres;

--
-- TOC entry 221 (class 1259 OID 57361)
-- Name: institution_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.institution ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.institution_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 230 (class 1259 OID 57427)
-- Name: job; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE public.job (
    id bigint NOT NULL,
    citizen_id bigint NOT NULL,
    company_id bigint,
    institution_id bigint,
    title character varying(150) NOT NULL,
    salary numeric(12,2) NOT NULL,
    start_date date NOT NULL,
    end_date date,
    status character varying(20) DEFAULT 'active'::character varying NOT NULL,
    CONSTRAINT chk_job_dates CHECK (((end_date IS NULL) OR (end_date >= start_date))),
    CONSTRAINT chk_job_employer CHECK ((((company_id IS NOT NULL) AND (institution_id IS NULL)) OR ((company_id IS NULL) AND (institution_id IS NOT NULL)))),
    CONSTRAINT chk_job_salary CHECK ((salary >= (0)::numeric)),
    CONSTRAINT chk_job_status CHECK (((status)::text = ANY ((ARRAY['active'::character varying, 'inactive'::character varying])::text[]))),
    CONSTRAINT chk_job_status_dates CHECK (((((status)::text = 'active'::text) AND (end_date IS NULL)) OR (((status)::text = 'inactive'::text) AND (end_date IS NOT NULL))))
);


ALTER TABLE public.job OWNER TO postgres;

--
-- TOC entry 229 (class 1259 OID 57426)
-- Name: job_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.job ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.job_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 217 (class 1259 OID 57340)
-- Name: location_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

ALTER TABLE public.location ALTER COLUMN id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME public.location_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);


--
-- TOC entry 234 (class 1259 OID 57510)
-- Name: v_active_employment; Type: VIEW; Schema: public; Owner: postgres
--

CREATE VIEW public.v_active_employment AS
 SELECT c.id AS citizen_id,
    (((c.first_name)::text || ' '::text) || (c.last_name)::text) AS citizen_name,
    j.title AS job_title,
    j.salary,
    comp.id AS company_id,
    comp.name AS company_name,
    j.start_date
   FROM ((public.job j
     JOIN public.citizen c ON ((j.citizen_id = c.id)))
     JOIN public.company comp ON ((j.company_id = comp.id)))
  WHERE ((j.end_date IS NULL) OR (j.end_date > CURRENT_DATE));


ALTER VIEW public.v_active_employment OWNER TO postgres;

--
-- TOC entry 237 (class 1259 OID 57525)
-- Name: v_citizen_relationships; Type: VIEW; Schema: public; Owner: postgres
--

CREATE VIEW public.v_citizen_relationships AS
 SELECT r.id AS relationship_id,
    c1.id AS citizen_1_id,
    (((c1.first_name)::text || ' '::text) || (c1.last_name)::text) AS citizen_1_name,
    r.relationship_type,
    c2.id AS citizen_2_id,
    (((c2.first_name)::text || ' '::text) || (c2.last_name)::text) AS citizen_2_name,
    r.start_date,
    r.end_date
   FROM ((public.citizen_relationship r
     JOIN public.citizen c1 ON ((r.citizen_id_1 = c1.id)))
     JOIN public.citizen c2 ON ((r.citizen_id_2 = c2.id)));


ALTER VIEW public.v_citizen_relationships OWNER TO postgres;

--
-- TOC entry 236 (class 1259 OID 57520)
-- Name: v_company_financial_summary; Type: VIEW; Schema: public; Owner: postgres
--

CREATE VIEW public.v_company_financial_summary AS
 SELECT comp.id AS company_id,
    comp.name AS company_name,
    count(j.id) FILTER (WHERE ((j.end_date IS NULL) OR (j.end_date > CURRENT_DATE))) AS active_job_count,
    COALESCE(sum(j.salary) FILTER (WHERE ((j.end_date IS NULL) OR (j.end_date > CURRENT_DATE))), (0)::numeric) AS total_active_salaries
   FROM (public.company comp
     LEFT JOIN public.job j ON ((j.company_id = comp.id)))
  GROUP BY comp.id, comp.name;


ALTER VIEW public.v_company_financial_summary OWNER TO postgres;

--
-- TOC entry 235 (class 1259 OID 57515)
-- Name: v_housing_occupancy; Type: VIEW; Schema: public; Owner: postgres
--

CREATE VIEW public.v_housing_occupancy AS
 SELECT h.id AS housing_id,
    l.name AS location_name,
    l.street_address,
    h.housing_type,
    h.capacity,
    count(c.id) AS current_occupants,
    (h.capacity - count(c.id)) AS available_spots
   FROM ((public.housing h
     JOIN public.location l ON ((h.location_id = l.id)))
     LEFT JOIN public.citizen c ON ((c.housing_id = h.id)))
  GROUP BY h.id, l.name, l.street_address, h.housing_type, h.capacity;


ALTER VIEW public.v_housing_occupancy OWNER TO postgres;

--
-- TOC entry 3319 (class 2606 OID 57395)
-- Name: citizen citizen_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.citizen
    ADD CONSTRAINT citizen_pkey PRIMARY KEY (id);


--
-- TOC entry 3323 (class 2606 OID 57414)
-- Name: citizen_relationship citizen_relationship_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.citizen_relationship
    ADD CONSTRAINT citizen_relationship_pkey PRIMARY KEY (id);


--
-- TOC entry 3310 (class 2606 OID 57355)
-- Name: company company_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.company
    ADD CONSTRAINT company_pkey PRIMARY KEY (id);


--
-- TOC entry 3333 (class 2606 OID 57459)
-- Name: education_record education_record_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.education_record
    ADD CONSTRAINT education_record_pkey PRIMARY KEY (id);


--
-- TOC entry 3306 (class 2606 OID 57339)
-- Name: family family_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.family
    ADD CONSTRAINT family_pkey PRIMARY KEY (id);


--
-- TOC entry 3316 (class 2606 OID 57380)
-- Name: housing housing_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.housing
    ADD CONSTRAINT housing_pkey PRIMARY KEY (id);


--
-- TOC entry 3314 (class 2606 OID 57367)
-- Name: institution institution_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.institution
    ADD CONSTRAINT institution_pkey PRIMARY KEY (id);


--
-- TOC entry 3331 (class 2606 OID 57437)
-- Name: job job_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.job
    ADD CONSTRAINT job_pkey PRIMARY KEY (id);


--
-- TOC entry 3308 (class 2606 OID 57345)
-- Name: location location_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.location
    ADD CONSTRAINT location_pkey PRIMARY KEY (id);


--
-- TOC entry 3320 (class 1259 OID 57474)
-- Name: idx_citizen_family; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_citizen_family ON public.citizen USING btree (family_id);


--
-- TOC entry 3321 (class 1259 OID 57475)
-- Name: idx_citizen_housing; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_citizen_housing ON public.citizen USING btree (housing_id);


--
-- TOC entry 3311 (class 1259 OID 57483)
-- Name: idx_company_location; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_company_location ON public.company USING btree (location_id);


--
-- TOC entry 3334 (class 1259 OID 57481)
-- Name: idx_education_citizen; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_education_citizen ON public.education_record USING btree (citizen_id);


--
-- TOC entry 3335 (class 1259 OID 57482)
-- Name: idx_education_institution; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_education_institution ON public.education_record USING btree (institution_id);


--
-- TOC entry 3317 (class 1259 OID 57485)
-- Name: idx_housing_location; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_housing_location ON public.housing USING btree (location_id);


--
-- TOC entry 3312 (class 1259 OID 57484)
-- Name: idx_institution_location; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_institution_location ON public.institution USING btree (location_id);


--
-- TOC entry 3327 (class 1259 OID 57478)
-- Name: idx_job_citizen; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_job_citizen ON public.job USING btree (citizen_id);


--
-- TOC entry 3328 (class 1259 OID 57479)
-- Name: idx_job_company; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_job_company ON public.job USING btree (company_id);


--
-- TOC entry 3329 (class 1259 OID 57480)
-- Name: idx_job_institution; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_job_institution ON public.job USING btree (institution_id);


--
-- TOC entry 3324 (class 1259 OID 57476)
-- Name: idx_relationship_citizen_1; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_relationship_citizen_1 ON public.citizen_relationship USING btree (citizen_id_1);


--
-- TOC entry 3325 (class 1259 OID 57477)
-- Name: idx_relationship_citizen_2; Type: INDEX; Schema: public; Owner: postgres
--

CREATE INDEX idx_relationship_citizen_2 ON public.citizen_relationship USING btree (citizen_id_2);


--
-- TOC entry 3326 (class 1259 OID 57425)
-- Name: uq_citizen_relationship; Type: INDEX; Schema: public; Owner: postgres
--

CREATE UNIQUE INDEX uq_citizen_relationship ON public.citizen_relationship USING btree (LEAST(citizen_id_1, citizen_id_2), GREATEST(citizen_id_1, citizen_id_2), relationship_type);


--
-- TOC entry 3349 (class 2620 OID 57498)
-- Name: citizen trg_check_housing_capacity; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_check_housing_capacity BEFORE INSERT OR UPDATE OF housing_id ON public.citizen FOR EACH ROW EXECUTE FUNCTION public.check_housing_capacity();


--
-- TOC entry 3350 (class 2620 OID 57531)
-- Name: citizen trg_citizen_death_status; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_citizen_death_status BEFORE INSERT OR UPDATE ON public.citizen FOR EACH ROW EXECUTE FUNCTION public.trg_fn_citizen_death_status();


--
-- TOC entry 3351 (class 2620 OID 57533)
-- Name: citizen trg_citizen_post_death_cleanup; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_citizen_post_death_cleanup AFTER UPDATE OF death_date ON public.citizen FOR EACH ROW EXECUTE FUNCTION public.trg_fn_citizen_post_death_cleanup();


--
-- TOC entry 3348 (class 2620 OID 57535)
-- Name: company trg_company_dissolved_status; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_company_dissolved_status BEFORE INSERT OR UPDATE ON public.company FOR EACH ROW EXECUTE FUNCTION public.trg_fn_company_dissolved_status();


--
-- TOC entry 3352 (class 2620 OID 57537)
-- Name: citizen trg_enforce_housing_capacity; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_enforce_housing_capacity BEFORE INSERT OR UPDATE OF housing_id ON public.citizen FOR EACH ROW EXECUTE FUNCTION public.trg_fn_enforce_housing_capacity();


--
-- TOC entry 3353 (class 2620 OID 57500)
-- Name: citizen trg_update_citizen_status; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_update_citizen_status BEFORE INSERT OR UPDATE OF death_date ON public.citizen FOR EACH ROW EXECUTE FUNCTION public.update_citizen_status();


--
-- TOC entry 3354 (class 2620 OID 57502)
-- Name: job trg_update_job_status; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_update_job_status BEFORE INSERT OR UPDATE OF end_date ON public.job FOR EACH ROW EXECUTE FUNCTION public.update_job_status();


--
-- TOC entry 3355 (class 2620 OID 57539)
-- Name: job trg_validate_job_dates; Type: TRIGGER; Schema: public; Owner: postgres
--

CREATE TRIGGER trg_validate_job_dates BEFORE INSERT OR UPDATE ON public.job FOR EACH ROW EXECUTE FUNCTION public.trg_fn_validate_job_dates();


--
-- TOC entry 3339 (class 2606 OID 57396)
-- Name: citizen fk_citizen_family; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.citizen
    ADD CONSTRAINT fk_citizen_family FOREIGN KEY (family_id) REFERENCES public.family(id) ON DELETE RESTRICT;


--
-- TOC entry 3340 (class 2606 OID 57401)
-- Name: citizen fk_citizen_housing; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.citizen
    ADD CONSTRAINT fk_citizen_housing FOREIGN KEY (housing_id) REFERENCES public.housing(id) ON DELETE SET NULL;


--
-- TOC entry 3336 (class 2606 OID 57356)
-- Name: company fk_company_location; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.company
    ADD CONSTRAINT fk_company_location FOREIGN KEY (location_id) REFERENCES public.location(id) ON DELETE RESTRICT;


--
-- TOC entry 3346 (class 2606 OID 57460)
-- Name: education_record fk_education_citizen; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.education_record
    ADD CONSTRAINT fk_education_citizen FOREIGN KEY (citizen_id) REFERENCES public.citizen(id) ON DELETE RESTRICT;


--
-- TOC entry 3347 (class 2606 OID 57465)
-- Name: education_record fk_education_institution; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.education_record
    ADD CONSTRAINT fk_education_institution FOREIGN KEY (institution_id) REFERENCES public.institution(id) ON DELETE RESTRICT;


--
-- TOC entry 3338 (class 2606 OID 57381)
-- Name: housing fk_housing_location; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.housing
    ADD CONSTRAINT fk_housing_location FOREIGN KEY (location_id) REFERENCES public.location(id) ON DELETE RESTRICT;


--
-- TOC entry 3337 (class 2606 OID 57368)
-- Name: institution fk_institution_location; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.institution
    ADD CONSTRAINT fk_institution_location FOREIGN KEY (location_id) REFERENCES public.location(id) ON DELETE RESTRICT;


--
-- TOC entry 3343 (class 2606 OID 57438)
-- Name: job fk_job_citizen; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.job
    ADD CONSTRAINT fk_job_citizen FOREIGN KEY (citizen_id) REFERENCES public.citizen(id) ON DELETE RESTRICT;


--
-- TOC entry 3344 (class 2606 OID 57443)
-- Name: job fk_job_company; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.job
    ADD CONSTRAINT fk_job_company FOREIGN KEY (company_id) REFERENCES public.company(id) ON DELETE RESTRICT;


--
-- TOC entry 3345 (class 2606 OID 57448)
-- Name: job fk_job_institution; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.job
    ADD CONSTRAINT fk_job_institution FOREIGN KEY (institution_id) REFERENCES public.institution(id) ON DELETE RESTRICT;


--
-- TOC entry 3341 (class 2606 OID 57415)
-- Name: citizen_relationship fk_relationship_citizen_1; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.citizen_relationship
    ADD CONSTRAINT fk_relationship_citizen_1 FOREIGN KEY (citizen_id_1) REFERENCES public.citizen(id) ON DELETE RESTRICT;


--
-- TOC entry 3342 (class 2606 OID 57420)
-- Name: citizen_relationship fk_relationship_citizen_2; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.citizen_relationship
    ADD CONSTRAINT fk_relationship_citizen_2 FOREIGN KEY (citizen_id_2) REFERENCES public.citizen(id) ON DELETE RESTRICT;


-- Completed on 2026-10-09 08:04:23 UTC

--
-- PostgreSQL database dump complete
--

