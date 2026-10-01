
-- ============================================================
-- DATABASE SCHEMA
-- PostgreSQL
-- ============================================================

-- ============================================================
-- 1. FAMILY
-- ============================================================

CREATE TABLE family (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    family_name VARCHAR(150) NOT NULL
);


-- ============================================================
-- 2. LOCATION
-- ============================================================

CREATE TABLE location (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    street_address VARCHAR(255) NOT NULL
);


-- ============================================================
-- 3. COMPANY
-- ============================================================

CREATE TABLE company (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    location_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    founded_date DATE NOT NULL,
    dissolved_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'active',

    CONSTRAINT fk_company_location
        FOREIGN KEY (location_id)
        REFERENCES location(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_company_dates
        CHECK (
            dissolved_date IS NULL
            OR dissolved_date >= founded_date
        ),

    CONSTRAINT chk_company_status
        CHECK (
            status IN ('active', 'dissolved')
        ),

    CONSTRAINT chk_company_status_dates
        CHECK (
            (status = 'active' AND dissolved_date IS NULL)
            OR
            (status = 'dissolved' AND dissolved_date IS NOT NULL)
        )
);


-- ============================================================
-- 4. INSTITUTION
-- ============================================================

CREATE TABLE institution (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    location_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    type VARCHAR(30) NOT NULL,

    CONSTRAINT fk_institution_location
        FOREIGN KEY (location_id)
        REFERENCES location(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_institution_type
        CHECK (
            type IN (
                'high_school',
                'college',
                'hospital'
            )
        )
);


-- ============================================================
-- 5. HOUSING
-- ============================================================

CREATE TABLE housing (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    location_id BIGINT NOT NULL,
    housing_type VARCHAR(30) NOT NULL,
    capacity INTEGER NOT NULL,

    CONSTRAINT fk_housing_location
        FOREIGN KEY (location_id)
        REFERENCES location(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_housing_type
        CHECK (
            housing_type IN ('house', 'apartment')
        ),

    CONSTRAINT chk_housing_capacity
        CHECK (capacity > 0)
);


-- ============================================================
-- 6. CITIZEN
-- ============================================================

CREATE TABLE citizen (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    birth_date DATE NOT NULL,
    death_date DATE,
    status VARCHAR(10) NOT NULL DEFAULT 'alive',
    gender VARCHAR(50),
    family_id BIGINT NOT NULL,
    housing_id BIGINT,

    CONSTRAINT fk_citizen_family
        FOREIGN KEY (family_id)
        REFERENCES family(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_citizen_housing
        FOREIGN KEY (housing_id)
        REFERENCES housing(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_citizen_dates
        CHECK (
            birth_date <= CURRENT_DATE
            AND (
                death_date IS NULL
                OR death_date >= birth_date
            )
        ),

    CONSTRAINT chk_citizen_status
        CHECK (
            status IN ('alive', 'dead')
        ),

    CONSTRAINT chk_citizen_status_dates
        CHECK (
            (status = 'alive' AND death_date IS NULL)
            OR
            (status = 'dead' AND death_date IS NOT NULL)
        )
);


-- ============================================================
-- 7. CITIZEN RELATIONSHIP
-- ============================================================

CREATE TABLE citizen_relationship (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    citizen_id_1 BIGINT NOT NULL,
    citizen_id_2 BIGINT NOT NULL,

    relationship_type VARCHAR(20) NOT NULL,

    start_date DATE NOT NULL,
    end_date DATE,

    CONSTRAINT fk_relationship_citizen_1
        FOREIGN KEY (citizen_id_1)
        REFERENCES citizen(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_relationship_citizen_2
        FOREIGN KEY (citizen_id_2)
        REFERENCES citizen(id)
        ON DELETE RESTRICT,

    -- Citizens cannot have a relationship with themselves.
    CONSTRAINT chk_different_citizens
        CHECK (citizen_id_1 <> citizen_id_2),

    CONSTRAINT chk_relationship_type
        CHECK (
            relationship_type IN (
                'parent_child',
                'marriage'
            )
        ),

    CONSTRAINT chk_relationship_dates
        CHECK (
            end_date IS NULL
            OR end_date >= start_date
        )
);


-- Prevent duplicate relationships regardless of ID order.
-- Example: (10,20) and (20,10) are treated as identical.
CREATE UNIQUE INDEX uq_citizen_relationship
ON citizen_relationship (
    LEAST(citizen_id_1, citizen_id_2),
    GREATEST(citizen_id_1, citizen_id_2),
    relationship_type
);


-- ============================================================
-- 8. JOB
-- ============================================================

CREATE TABLE job (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    citizen_id BIGINT NOT NULL,
    company_id BIGINT,
    institution_id BIGINT,

    title VARCHAR(150) NOT NULL,
    salary NUMERIC(12,2) NOT NULL,

    start_date DATE NOT NULL,
    end_date DATE,

    status VARCHAR(20) NOT NULL DEFAULT 'active',

    CONSTRAINT fk_job_citizen
        FOREIGN KEY (citizen_id)
        REFERENCES citizen(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_job_company
        FOREIGN KEY (company_id)
        REFERENCES company(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_job_institution
        FOREIGN KEY (institution_id)
        REFERENCES institution(id)
        ON DELETE RESTRICT,

    -- A job must belong to exactly one employer type.
    CONSTRAINT chk_job_employer
        CHECK (
            (company_id IS NOT NULL AND institution_id IS NULL)
            OR
            (company_id IS NULL AND institution_id IS NOT NULL)
        ),

    CONSTRAINT chk_job_salary
        CHECK (salary >= 0),

    CONSTRAINT chk_job_dates
        CHECK (
            end_date IS NULL
            OR end_date >= start_date
        ),

    CONSTRAINT chk_job_status
        CHECK (
            status IN ('active', 'inactive')
        ),

    CONSTRAINT chk_job_status_dates
        CHECK (
            (status = 'active' AND end_date IS NULL)
            OR
            (status = 'inactive' AND end_date IS NOT NULL)
        )
);


-- ============================================================
-- 9. EDUCATION RECORD
-- ============================================================

CREATE TABLE education_record (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,

    citizen_id BIGINT NOT NULL,
    institution_id BIGINT NOT NULL,

    start_date DATE NOT NULL,
    end_date DATE,

    CONSTRAINT fk_education_citizen
        FOREIGN KEY (citizen_id)
        REFERENCES citizen(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_education_institution
        FOREIGN KEY (institution_id)
        REFERENCES institution(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_education_dates
        CHECK (
            end_date IS NULL
            OR end_date >= start_date
        )
);


-- ============================================================
-- 10. CITIZEN DETAILS VIEW
-- ============================================================

-- Age is calculated dynamically.
-- Deceased citizens have their age calculated at death.

CREATE VIEW citizen_details AS
SELECT
    id,
    first_name,
    last_name,
    birth_date,
    death_date,
    status,
    gender,
    family_id,
    housing_id,

    EXTRACT(
        YEAR FROM AGE(
            COALESCE(death_date, CURRENT_DATE),
            birth_date
        )
    )::INTEGER AS age

FROM citizen;


-- ============================================================
-- 11. INDEXES
-- ============================================================

-- Citizen
CREATE INDEX idx_citizen_family
    ON citizen(family_id);

CREATE INDEX idx_citizen_housing
    ON citizen(housing_id);


-- Citizen Relationship
CREATE INDEX idx_relationship_citizen_1
    ON citizen_relationship(citizen_id_1);

CREATE INDEX idx_relationship_citizen_2
    ON citizen_relationship(citizen_id_2);


-- Job
CREATE INDEX idx_job_citizen
    ON job(citizen_id);

CREATE INDEX idx_job_company
    ON job(company_id);

CREATE INDEX idx_job_institution
    ON job(institution_id);


-- Education
CREATE INDEX idx_education_citizen
    ON education_record(citizen_id);

CREATE INDEX idx_education_institution
    ON education_record(institution_id);


-- Location relationships
CREATE INDEX idx_company_location
    ON company(location_id);

CREATE INDEX idx_institution_location
    ON institution(location_id);

CREATE INDEX idx_housing_location
    ON housing(location_id);


-- ============================================================
-- END OF DATABASE SCHEMA
-- ============================================================