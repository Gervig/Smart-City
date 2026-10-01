# Smart City project plan

## 1. Project goal

Build a backend for **World Sim**, a persistent fictional city whose citizens,
organizations, places, and relationships have a history. The same core
business workflows must work against:

1. PostgreSQL (source of truth)
2. MongoDB (document model)
3. Neo4j (graph model)

The frontend is intentionally out of scope. Swagger UI and Postman are the
clients used to demonstrate the API.

## 2. Recommended scope

The project should focus on a small number of complete workflows instead of
trying to simulate every part of a real city.

### Main domain entities

These entities give the relational model at least ten meaningful main
entities, while keeping the model related to the original idea:

1. `citizen`
2. `family`
3. `company`
4. `financial_record`
5. `location`
6. `housing`
7. `job`
8. `institution`
9. `education_record`
10. `vehicle`

Supporting entities include `relationship`, `user`, `role`, and audit/event
tables. A hospital and a school are represented as institution types rather
than duplicated tables. Vehicles remain low priority and can be implemented
after the required workflows are stable.

### Core workflows

Every database adapter should support the following:

- list and inspect citizens, with pagination, filtering, and sorting;
- create, update, and retire a citizen while preserving birth/death history;
- assign housing and show who lives at a location;
- create and end employment, including salary history;
- record education and graduation;
- create and query relationships such as parent, partner, marriage, and friend;
- list companies and financial records for a selected period;
- produce a city summary containing population, employment, housing, and
  education statistics;
- authenticate users and authorize administrator versus normal-user actions.

The city summary is also the basis for the AI feature: generate a meaningful
monthly city report from aggregated data and persist it as a domain record.

## 3. Architecture decision

Use one monolithic backend with a layered structure:

```text
HTTP controllers -> services/use cases -> repositories -> database adapters
                         |                         |
                      DTOs/validation       PostgreSQL/MongoDB/Neo4j
```

The API should expose one consistent contract. The active database can be
selected through configuration, or separate `/postgresql`, `/mongodb`, and
`/neo4j` route groups can be used if that makes demonstrations clearer.
Database-specific mapping belongs in repositories/adapters, not controllers.

The selected backend stack is **Java + Javalin**. Keep this stack for all
three adapters. Use a relational persistence library such as Hibernate/JPA
with the PostgreSQL JDBC driver, the official MongoDB Java driver for MongoDB,
and the official
Neo4j Java driver for Neo4j. Javalin should handle HTTP routing and Swagger
integration, while DTO validation, services, and repository interfaces keep
the API independent of any one database.

The assignment recommends MySQL but permits another relational database when
the requirements are fulfilled. PostgreSQL is therefore the selected
relational database. The report should explicitly justify this choice and
map the required stored-object concepts to PostgreSQL: functions/procedures,
views, triggers, and scheduled jobs (for example, an external scheduler or
the `pg_cron` extension) in place of MySQL events.

## 4. Delivery phases

### Phase 0 - Team decisions and repository setup

- Confirm the backend language/framework, ORM, and test framework.
- Create the source layout, environment-variable template, and Docker Compose
  services for local PostgreSQL, MongoDB, Neo4j, and the backend.
- Define branch ownership and a shared definition of done.

### Phase 1 - PostgreSQL relational database (Mandatory assignment 1)

- Refine the conceptual, logical, and physical ERD.
- Normalize the relational schema and document the important decisions.
- Create SQL scripts in a repeatable order:
  `01_database.sql`, `02_tables.sql`, `03_indexes.sql`,
  `04_views.sql`, `05_routines.sql`, `06_triggers.sql`,
  `07_events.sql`, `08_users.sql`, and `09_seed.sql`.
- Add primary/foreign keys, `NOT NULL`, `UNIQUE`, `CHECK`, and referential
  actions.
- Add useful indexes based on planned API queries.
- Add at least one meaningful procedure, function, view, trigger, and event.
- Add an audit table and triggers for important history changes.
- Seed PostgreSQL with realistic data; aim for about 100 records per main entity
  where that is meaningful.
- Write the first report sections while decisions are still fresh.

### Phase 2 - PostgreSQL backend (Mandatory assignment 2)

- Implement DTOs, validation, repositories, services, and controllers.
- Add CRUD for the core workflows.
- Add bounded pagination, filtering, and sorting to collection endpoints.
- Use parameterized queries/ORM parameters; never concatenate user input into
  SQL.
- Implement login/logout, password hashing, token/session handling, and role
  checks.
- Configure the least-privileged application database user.
- Add integration tests that run against the actual PostgreSQL service.
- Add Swagger/OpenAPI documentation and a Postman collection.

### Phase 3 - MongoDB and Neo4j adapters

- Model the same business data for each database:
  - MongoDB: embed data that is read together and reference shared entities;
    add indexes and aggregation pipelines.
  - Neo4j: use `Citizen`, `Company`, `Institution`, `Housing`, and `Location`
    nodes with typed relationships and traversal queries.
- Reuse the service/API contract where possible.
- Implement the same core workflows and role restrictions.
- Add database-specific integration tests, including representative joins,
  aggregations, and graph traversals.

### Phase 4 - One-time migration

- Read the complete PostgreSQL source dataset.
- Validate counts and required relationships before writing anything.
- Transform and load MongoDB and Neo4j in an idempotent migration run.
- Produce a migration summary with source counts, target counts, and rejected
  records.
- Keep migration as a separate command/application; it is not continuous
  synchronization.

### Phase 5 - AI enrichment, deployment, and final verification

- Query aggregated city data from PostgreSQL.
- Ask a local model in development to produce a monthly city report with
  explicit structured fields such as highlights, risks, and recommended
  actions.
- Validate and persist the result in an `ai_city_report` table/collection.
- Make the production AI integration configurable and safely disable it when
  no model credentials are available.
- Deploy the backend and use a managed PostgreSQL service plus managed
  MongoDB and Neo4j services. Containers are for local development, not
  production databases.
- Run the full integration suite against all three databases.
- Export database dumps, migration scripts, seed scripts, and the final
  OpenAPI/Postman documentation.

## 5. Suggested team split

Each person owns a vertical slice and contributes to the report:

- relational schema, SQL objects, seed data, and ERD;
- backend API, authentication, validation, and Swagger;
- MongoDB model and adapter;
- Neo4j model and adapter;
- migration, integration tests, AI enrichment, and deployment.

Everyone should review another person’s work and help test all three database
implementations. The report should be written continuously, not at the end.

## 6. Definition of done

A feature is complete only when it has:

- a documented business rule and database representation;
- implementation in PostgreSQL, MongoDB, and Neo4j where it is part of the core
  contract;
- validation, authorization, and bounded query behavior;
- an integration test against the real database;
- seed/migration support where applicable;
- Swagger documentation;
- a short explanation in the final report.

## 7. Immediate next steps

1. Agree on the backend stack and assign ownership.
2. Review the existing ERD against the entity and workflow list above.
3. Decide which relationship types are directional and enforce that rule.
4. Create the SQL schema and constraints before writing application code.
5. Implement one complete vertical slice first: citizen listing plus
   housing assignment, including its test and Swagger endpoint.
