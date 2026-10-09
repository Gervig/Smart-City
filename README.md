# Smart City
Smart City - database school project

## Project status

The current repository contains the initial domain ERD for the **World Sim**
project. The implementation must satisfy the final-project requirements:
PostgreSQL, MongoDB, and Neo4j solutions; a CRUD backend; a one-time migrator;
authentication and authorization; integration tests; Docker-based local
development; cloud deployment; and persisted AI-based data enrichment.

The scoped implementation plan is in
[docs/PROJECT_PLAN.md](docs/PROJECT_PLAN.md). It defines the core workflows,
database responsibilities, delivery phases, and definition of done.

## Smart City "World Sim" - Entity Relationship Diagram (ERD)

This document outlines the database schema for the persistent simulated city project. It provides a structured foundation for tracking citizens, companies, relationships, real estate, and education history.

### Relational Database Design

#### 1.1 Mermaid Conceptual Model

```mermaid

flowchart TD
    Citizen["Citizen"]
    Family["Family"]
    Housing["Housing"]
    Location["Location"]
    Company["Company"]
    Institution["Institution"]
    Employment["Employment"]
    Education["Education"]
    Relationship["Personal Relationship"]

    Family -->|"groups members"| Citizen
    Citizen -->|"may reside in"| Housing
    Housing -->|"located at"| Location
    Company -->|"located at"| Location
    Institution -->|"located at"| Location

    Citizen -->|"has"| Employment
    Employment -->|"at a company or institution"| Company
    Employment -->|"at a company or institution"| Institution

    Citizen -->|"has"| Education
    Education -->|"recorded at"| Institution

    Citizen -->|"participates in"| Relationship
    Relationship -->|"connects two citizens"| Citizen
```

#### 1.2 Mermaid Logical Model

```mermaid

erDiagram
    FAMILY {
        bigint id PK
        varchar family_name
    }

    CITIZEN {
        bigint id PK
        varchar first_name
        varchar last_name
        date birth_date
        date death_date "Nullable"
        varchar status
        varchar gender
        bigint family_id FK
        bigint housing_id FK "Nullable"
    }

    LOCATION {
        bigint id PK
        varchar name
        varchar street_address
    }

    HOUSING {
        bigint id PK
        bigint location_id FK
        varchar housing_type
        int capacity
    }

    COMPANY {
        bigint id PK
        bigint location_id FK
        varchar name
        date founded_date
        date dissolved_date "Nullable"
        varchar status
    }

    INSTITUTION {
        bigint id PK
        bigint location_id FK
        varchar name
        varchar type
    }

    JOB {
        bigint id PK
        bigint citizen_id FK
        bigint company_id FK "Nullable"
        bigint institution_id FK "Nullable"
        varchar title
        numeric salary
        date start_date
        date end_date "Nullable"
        varchar status
    }

    EDUCATION_RECORD {
        bigint id PK
        bigint citizen_id FK
        bigint institution_id FK
        date start_date
        date end_date "Nullable"
    }

    CITIZEN_RELATIONSHIP {
        bigint id PK
        bigint citizen_id_1 FK
        bigint citizen_id_2 FK
        varchar relationship_type
        date start_date
        date end_date "Nullable"
    }

    FAMILY ||--o{ CITIZEN : has_members
    HOUSING o|--o{ CITIZEN : houses
    LOCATION ||--o{ HOUSING : contains
    LOCATION ||--o{ COMPANY : locates
    LOCATION ||--o{ INSTITUTION : locates
    CITIZEN ||--o{ JOB : has_jobs
    COMPANY o|--o{ JOB : employs
    INSTITUTION o|--o{ JOB : employs
    CITIZEN ||--o{ EDUCATION_RECORD : has_education
    INSTITUTION ||--o{ EDUCATION_RECORD : records
    CITIZEN ||--o{ CITIZEN_RELATIONSHIP : participant_one
    CITIZEN ||--o{ CITIZEN_RELATIONSHIP : participant_two
```

#### 1.3 Mermaid ERD

```mermaid
erDiagram
    FAMILY {
        bigint id PK
        varchar family_name
    }

    CITIZEN {
        bigint id PK
        varchar first_name
        varchar last_name
        date birth_date
        date death_date
        varchar status "alive / dead"
        varchar gender
        bigint family_id FK
        bigint housing_id FK "Nullable"
    }

    CITIZEN_RELATIONSHIP {
        bigint id PK
        bigint citizen_id_1 FK
        bigint citizen_id_2 FK
        varchar relationship_type "parent_child / marriage"
        date start_date
        date end_date "Nullable"
    }

    LOCATION {
        bigint id PK
        varchar name
        varchar street_address
    }

    HOUSING {
        bigint id PK
        bigint location_id FK
        varchar housing_type "house / apartment"
        int capacity
    }

    COMPANY {
        bigint id PK
        bigint location_id FK
        varchar name
        date founded_date
        date dissolved_date "Nullable"
        varchar status "active / dissolved"
    }

    INSTITUTION {
        bigint id PK
        bigint location_id FK
        varchar name
        varchar type "high_school / college / hospital"
    }

    JOB {
        bigint id PK
        bigint citizen_id FK
        bigint company_id FK "Nullable"
        bigint institution_id FK "Nullable"
        varchar title
        numeric salary
        date start_date
        date end_date "Nullable"
        varchar status "active / inactive"
    }

    EDUCATION_RECORD {
        bigint id PK
        bigint citizen_id FK
        bigint institution_id FK
        date start_date
        date end_date "Nullable"
    }

    FAMILY ||--o{ CITIZEN : "has members"
    HOUSING o|--o{ CITIZEN : "houses"
    LOCATION ||--o{ HOUSING : "contains"
    LOCATION ||--o{ COMPANY : "locates"
    LOCATION ||--o{ INSTITUTION : "locates"

    CITIZEN ||--o{ JOB : "has employment records"
    COMPANY o|--o{ JOB : "employs"
    INSTITUTION o|--o{ JOB : "employs"

    CITIZEN ||--o{ EDUCATION_RECORD : "has education records"
    INSTITUTION ||--o{ EDUCATION_RECORD : "provides education"

    CITIZEN ||--o{ CITIZEN_RELATIONSHIP : "participant one"
    CITIZEN ||--o{ CITIZEN_RELATIONSHIP : "participant two"
```

### 2. Core Tables Explained

#### Citizens & Connections
* **CITIZEN:** The foundation of the sim. Tracks lifetime, current living arrangements, and survival state.
* **RELATIONSHIP:** A bridge / junction table handling many-to-many dynamics between citizens. It records relationships explicitly with type tracking (e.g., historical marriages, parental links, or friendships).

#### Economy & Corporate Tracking
* **COMPANY:** Represents organizations. Built with a self-referencing foreign key (`parent_company_id`) to naturally store corporate takeovers, spin-offs, and mergers.
* **FINANCIAL_RECORD:** A clean time-series design. By utilizing a `period_type` column ("Daily", "Monthly", etc.), a single table elegantly tracks historical growth or decline without splitting schemas.
* **JOB:** Bridges citizens and corporations, maintaining a historical record of employment durations and wages.

#### Geography & Education
* **LOCATION:** Base infrastructure records mapping physical footprints across the city grid.
* **HOUSING:** Real estate footprints handling multi-family settings like apartment complexes mapping back to specific structural locations.
* **INSTITUTION:** Physical entities managing community operations (Schools, Hospitals).
* **EDUCATION_RECORD:** Captures academic tiers (HighSchool, College) paired with specific study focuses (Physics, Medical) across a citizen's timeline.


### Google docs
[Smart City - Google docs link](https://github.com/Gervig/Smart-City)