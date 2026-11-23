# Smart City migrator

A separate application, with its own `pom.xml`, that does a one-time migration of the Smart City data from PostgreSQL
into MongoDB and Neo4j. It reads every table, maps the rows to documents and to a graph, writes both, and then
compares the counts.

**Structure and data are separate**, like in PostgreSQL (where `SQL/schema/database_schema.sql` creates the tables):

| | PostgreSQL | MongoDB | Neo4j |
|---|---|---|---|
| Structure | `SQL/schema/database_schema.sql` | `NoSQL/mongodb/01_collections.js`: 7 collections with `$jsonSchema` validators, 5 indexes | `NoSQL/neo4j/01_constraints.cypher`: 8 uniqueness constraints, 3 indexes |
| Data | the backend | **this migrator** | **this migrator** |

`NoSQL/setup.ps1` runs both structure scripts against the Docker containers. The migrator only writes data: it
checks that the structure exists and stops with a message that names the missing script if it does not.

| Part | Technology |
|---|---|
| Source | PostgreSQL, plain JDBC (`org.postgresql:postgresql`) |
| Document database | MongoDB, official driver (`mongodb-driver-sync` 5.15) |
| Graph database | Neo4j, official driver (`neo4j-java-driver` 5.28; 6.x needs Java 21) |
| Tests | JUnit 5, Testcontainers 1.21.4 (PostgreSQL, MongoDB and Neo4j in Docker) |

## How it works

1. **Read** (`source/SqlReader`): every table in one read-only `REPEATABLE READ` transaction, so all tables are seen as
   they were at the same moment. It logs in as `smartcity_readonly` from `SQL/users/08_users.sql`, which only has
   `SELECT`, so the migration cannot change the source. The queries are fixed strings without input.
2. **Map**: `document/DocumentMapper` and `graph/GraphMapper` are plain Java without database access, so they are
   unit tested.
3. **Write**: `document/MongoWriter` and `graph/Neo4jWriter`. Each checks the structure, then removes what an earlier
   run wrote (MongoDB: the documents, keeping the collections and validators; Neo4j: only nodes with the migrator's
   labels), so running it again gives the same result instead of duplicates. MongoDB checks every document against
   the validator. Neo4j is written in one transaction; values are always Cypher parameters, never part of the query
   text.
4. **Report** (`MigrationReport`): expected counts (from the SQL rows) against actual counts (in MongoDB and Neo4j).

## Document design (MongoDB, database `smart_city`)

Rule: embed what is read together with its owner and belongs to it alone; reference what is shared.

| Collection | Document | Embedded | Referenced |
|---|---|---|---|
| `citizens` | one citizen | `family {id, familyName}`, `jobs[]` (with salary and `employer {type, id, name}`), `education[]` (with `institution {id, name, type}`), `relationships[]` (the other citizen's id and name) | `housingId`: several citizens share one housing |
| `housings` | one housing | `location` | `residentIds[]` |
| `companies` | one company | `location` | |
| `institutions` | one institution | `location` | |
| `locations` | one location, with `point` (GeoJSON `[longitude, latitude]`) and `osm {id, type}` | | |
| `families` | one family | | `memberIds[]` |
| `users` | `_id` = username, `password` (BCrypt hash), `roles[]` | roles | |

- The relational id becomes `_id`. Null columns are left out. Dates are BSON dates, and the salary is a `Decimal128` (exact, like `numeric(12,2)`).
- A relationship row links two citizens, so it is embedded in both, each pointing at the other.
- The embedded names (employer, institution, family, relationship partner) are copies. They are what makes a citizen readable in one query, at the cost of updating them in two places when a name changes.
- Indexes: `citizens` on `lastName + firstName`, `family.id`, `housingId` and `jobs.employer`; `locations` 2dsphere on `point` (for "near" queries, like the nearby-places feature).

## Graph design (Neo4j)

```
(:Citizen)-[:MEMBER_OF]->(:Family)
(:Citizen)-[:LIVES_IN]->(:Housing)-[:LOCATED_AT]->(:Location)
(:Citizen)-[:WORKS_AT {id, title, salary, startDate, endDate, status}]->(:Company|:Institution)
(:Citizen)-[:STUDIED_AT {id, startDate, endDate}]->(:Institution)
(:Citizen)-[:RELATED_TO {id, type, startDate, endDate}]->(:Citizen)
(:Company|:Institution)-[:LOCATED_AT]->(:Location)
(:User)-[:HAS_ROLE]->(:Role)
```

- Every table with its own identity becomes a node label. The relational id stays as the property `id` (`username` / `name` for `User` / `Role`), with a uniqueness constraint, which also indexes the lookups.
- Foreign keys become relationships. The join tables `job`, `education_record` and `citizen_relationship` become relationships **with properties**, because in a graph the connection itself is the data.
- `RELATED_TO` points from `citizen_id_1` to `citizen_id_2`; queries can ignore the direction (`-[:RELATED_TO]-`).
- Neo4j has no decimal type, so the salary is stored as a float. Null columns are not stored.

Example: who works where, and with whom is that person married?
```cypher
MATCH (c:Citizen)-[w:WORKS_AT]->(employer), (c)-[:RELATED_TO {type: 'marriage'}]-(partner)
RETURN c.firstName, employer.name, w.title, partner.firstName
```

## Run it

Start the databases (`docker compose -f docker/docker-compose.yml up -d` from the project root). The Smart City
database must exist, and `08_users.sql` must have been run (for `smartcity_readonly`).

1. Create the structure in MongoDB and Neo4j (once; safe to repeat), from the project root:
   ```text
   powershell -ExecutionPolicy Bypass -File NoSQL\setup.ps1
   ```
2. Migrate the data, from the `migrator` folder:
   ```text
   mvn package
   java -jar target/migrator.jar
   ```
It prints the report. The exit code is 0 when every count matches, 1 when not, and 2 when the migration failed.

Settings are environment variables; the defaults match `docker/docker-compose.yml`:

| Variable | Default |
|---|---|
| `PG_URL` | `jdbc:postgresql://localhost:5432/smart_city` |
| `PG_USER` / `PG_PASSWORD` | `smartcity_readonly` / `readonly-dev-password` |
| `MONGO_URI` / `MONGO_DB` | `mongodb://localhost:27017` / `smart_city` |
| `NEO4J_URI` | `bolt://localhost:7687` |
| `NEO4J_USER` / `NEO4J_PASSWORD` | `neo4j` / `smartcity-dev` |

Look at the result in the Neo4j Browser (<http://localhost:7474>, `MATCH (n) RETURN n LIMIT 100`) or in MongoDB
(VS Code's MongoDB extension, or `docker exec -it mongo mongosh smart_city`).

## Tests

```text
mvn test     # 15 unit tests: document mapping, graph mapping
mvn verify   # + 10 end-to-end tests in Docker
```
The end-to-end test (`MigrationIntegrationTest`) starts PostgreSQL with the project's schema
(`SQL/schema/database_schema.sql`) and a small sample city, and MongoDB and Neo4j set up with the scripts in
`NoSQL/`. It checks the structure: all 7 collections with validators and indexes, the validator rejects an invalid
citizen, Neo4j has its constraints and indexes, and the migrator refuses to run without the setup. Then it runs the
migration as a read-only user and checks: every count matches; Anna's document has her job with the exact salary; a geo query finds the
location; a graph traversal finds employer, spouse and school; a second run gives the same result; and the read-only
user cannot change PostgreSQL.
