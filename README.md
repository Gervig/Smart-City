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

### 1. Mermaid ERD Source Code

Copy the code below into any Mermaid-compatible editor (like [Mermaid Live Editor](https://mermaid.live/)) to visualize and edit the diagram with your team.

```mermaid
erDiagram
    CITIZEN {
        int citizen_id PK
        string first_name
        string last_name
        date birth_date
        date death_date
        string status "Alive / Deceased"
        int current_housing_id FK
    }

    RELATIONSHIP {
        int relationship_id PK
        int citizen_id_1 FK
        int citizen_id_2 FK
        string type "Parent/Child, Marriage, Partner, Friend"
        date established_date
        date dissolved_date
    }

    COMPANY {
        int company_id PK
        string name
        date founded_date
        date dissolved_date
        int parent_company_id FK "Self-referencing for M&A"
        string status "Active / Dissolved / Acquired"
    }

    FINANCIAL_RECORD {
        int record_id PK
        int company_id FK
        string period_type "Daily, Monthly, Quarterly, Annual"
        date record_date
        decimal revenue
        decimal expenses
    }

    LOCATION {
        int location_id PK
        string name
        string street_address
        string type "Commercial, Residential, Institutional"
    }

    HOUSING {
        int housing_id PK
        int location_id FK
        string unit_number "Apartment / House number"
        string type "Apartment, Condo, House"
        int capacity
    }

    JOB {
        int job_id PK
        int citizen_id FK
        int company_id FK
        string title
        decimal salary
        date start_date
        date end_date
    }

    INSTITUTION {
        int institution_id PK
        int location_id FK
        string name
        string type "HighSchool, College, Hospital"
    }

    EDUCATION_RECORD {
        int education_id PK
        int citizen_id FK
        int institution_id FK
        string level "HighSchool, Bachelor, Master, PhD"
        string major_type "Medical, Physics, Social Studies, None"
        date start_date
        date graduation_date
    }

    CITIZEN }o--o| HOUSING : "resides_in"
    LOCATION ||--o{ HOUSING : "contains"
    LOCATION ||--o| INSTITUTION : "hosts"

    CITIZEN ||--o{ RELATIONSHIP : "participates_as_1"
    CITIZEN ||--o{ RELATIONSHIP : "participates_as_2"

    COMPANY ||--o{ JOB : "employs"
    CITIZEN ||--o{ JOB : "works_at"
    COMPANY ||--o{ FINANCIAL_RECORD : "tracks"
    COMPANY o|--o{ COMPANY : "parent_of"

    INSTITUTION ||--o{ EDUCATION_RECORD : "offers"
    CITIZEN ||--o{ EDUCATION_RECORD : "attends"
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