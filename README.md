# Smart City

Smart City is a Java REST API for managing a simulated city: citizens, families, housing, locations, companies, institutions, jobs, education records, and citizen relationships. It uses a PostgreSQL database (run in Docker) and includes JWT authentication, CRUD endpoints, nearby-place lookup through OpenStreetMap's Overpass API, and admin-only tools for importing and enriching seed data.

| Part | Technology |
|---|---|
| Backend (REST API) | Java 17, Javalin 6, Maven |
| Persistence | Hibernate 6, JPA |
| Database | PostgreSQL 16 (Docker, with pg_cron) |
| Local services | Docker Compose: PostgreSQL, pgAdmin, MongoDB, Neo4j |
| Authentication | JWT (TokenSecurity), BCrypt password hashing |
| Mapping and JSON | MapStruct, Jackson |
| Unit tests | JUnit 5, Mockito, Hamcrest |
| HTTP and database integration tests | WireMock, REST Assured, Testcontainers |
| Static analysis | SonarQube Community Build (Docker) |
| API examples | Postman |
| Frontend (demo UI) | Vite, React 18, JavaScript |
| End-to-end UI tests | Playwright |

## Contents

1. [Quick start](#quick-start)
2. [Repository structure](#repository-structure)
3. [Prerequisites](#prerequisites)
4. [Step 1: Get the code](#step-1-get-the-code)
5. [Step 2: Databases (Docker)](#step-2-databases-docker)
6. [Step 3: Backend](#step-3-backend)
7. [Running the tests](#running-the-tests)
8. [Static analysis with SonarQube](#static-analysis-with-sonarqube)
9. [API tests with Postman](#api-tests-with-postman)
10. [Frontend demo](#frontend-demo)
11. [End-to-end tests with Playwright](#end-to-end-tests-with-playwright)
12. [API reference](#api-reference)
13. [Database design](#database-design)
14. [Migration to MongoDB and Neo4j](#migration-to-mongodb-and-neo4j)
15. [Continuous integration and delivery](#continuous-integration-and-delivery)
16. [More documentation](#more-documentation)
17. [Troubleshooting](#troubleshooting)

## Quick start

The databases run in Docker. Start them, load the schema and create the database users first (details in [Step 2](#step-2-databases-docker)), then run the backend:

```text
git clone https://github.com/Gervig/Smart-City.git
cd Smart-City
cd docker
docker compose up -d --build
docker compose exec db psql -U postgres -d smart_city -f /sql/schema/database_schema.sql
docker compose exec db psql -U postgres -d smart_city -f /sql/users/08_users.sql
docker compose exec db psql -U postgres -d smart_city -f /sql/audit/09_citizen_status_audit.sql
cd ..
```

Create `src/main/resources/config.properties` (see [Configure the database and JWT](#configure-the-database-and-jwt)), then start the application from IntelliJ by running `app.Main` or use Maven:

```text
mvn clean package
java -jar target/app.jar
```

The API listens on <http://localhost:7070>. The Javalin route overview is at <http://localhost:7070/api/routes>.

## Repository structure

```text
Smart-City/
|-- src/main/java/app/
|   |-- Main.java                  Application entry point and exception handlers
|   |-- config/                    Hibernate and EntityManagerFactory setup
|   |-- controllers/               HTTP request handlers
|   |-- daos/                      Generic JPA data access
|   |-- dtos/                      API request and response models
|   |-- entities/                  JPA domain entities
|   |-- external/overpass/         Overpass HTTP clients and response models
|   |-- mappers/                   MapStruct entity/DTO mapping
|   |-- populators/                Role and user setup, deterministic data generators
|   |-- routes/                    CRUD, nearby-place, and seed route registration
|   |-- security/                  JWT filters, roles, users, and user DAO
|   |-- services/                  Business logic
|   `-- validators/                Request and domain validation
|-- src/test/java/app/
|   |-- daos/                      PostgreSQL/Testcontainers integration tests
|   |-- external/                  WireMock tests and opt-in Overpass smoke test
|   |-- services/                  Service unit tests
|   |-- validators/                 Validator tests
|   `-- ...                        Mapper, security, and generator tests
|-- SQL/                           PostgreSQL schema, users and privileges, routines, triggers, views, and event
|-- docker/                        Docker Compose setup: PostgreSQL (pg_cron), pgAdmin, MongoDB, Neo4j
|-- scripts/                       PowerShell backup, restore and compare scripts for PostgreSQL
|-- NoSQL/                         MongoDB and Neo4j structure scripts (used by the migrator)
|-- migrator/                      Separate app: one-time migration from PostgreSQL to MongoDB and Neo4j
|-- docs/                          SRS, assignment information, plans, and ERD diagrams
|-- postman/                       API collection and local environment
|-- frontend/                      Vite + React demo UI and Playwright E2E tests
|   |-- src/                       React app (pages, API helper, resource definitions)
|   |-- e2e/                       Playwright tests (auth.spec.js, crud.spec.js)
|   |-- vite.config.js             Dev server and proxy to the backend
|   `-- playwright.config.js       Test configuration
`-- pom.xml                        Dependencies, build, and test configuration
```

## Prerequisites

| Tool | Version | Used for |
|---|---|---|
| Git | Recent version | Cloning the repository |
| Java JDK | 17 | Building and running the backend |
| Maven | 3.8 or newer recommended | Build and test commands |
| Docker Desktop | Recent version | The local databases (PostgreSQL, pgAdmin, MongoDB, Neo4j) and the Testcontainers integration tests |
| PowerShell | Windows PowerShell 5 or newer | The backup scripts in `scripts/` and `NoSQL/setup.ps1` |
| Postman | Optional | Running the checked-in API collection |
| Node.js and npm | Node 20 or newer | Frontend demo and Playwright tests |

Check your installations:

```text
git --version
java -version
mvn --version
docker --version
node --version
npm --version
```

## Step 1: Get the code

```text
git clone https://github.com/Gervig/Smart-City.git
cd Smart-City
```

## Step 2: Databases (Docker)

The databases run in Docker, using the project's own setup in [`docker/`](docker/) (see [`docker/README.md`](docker/README.md) for everything below in more detail). It starts four containers:

| Service | Address | Login |
|---|---|---|
| PostgreSQL 16.2 (with `pg_cron`) | `localhost:5432`, database `smart_city` | `postgres` / `postgres` (admin only) |
| pgAdmin | <http://localhost:8080> | `admin@cphbusiness.dk` / `1234` |
| MongoDB 7 | `localhost:27017` | no login (development) |
| Neo4j 5 | <http://localhost:7474>, bolt on `7687` | `neo4j` / `smartcity-dev` |

These are development passwords, not secrets. Docker Desktop must be running.

**1. Start the containers** (the first start builds the PostgreSQL image and creates the `smart_city` database):

```text
cd docker
docker compose up -d --build
```

**2. Load the schema and create the database users.** The repository's `SQL/` folder is mounted in the database container as `/sql`:

```text
docker compose exec db psql -U postgres -d smart_city -f /sql/schema/database_schema.sql
docker compose exec db psql -U postgres -d smart_city -f /sql/users/08_users.sql
docker compose exec db psql -U postgres -d smart_city -f /sql/audit/09_citizen_status_audit.sql
```

The schema comes from `SQL/schema/database_schema.sql`, not from Hibernate: the application only checks it on startup (`hibernate.hbm2ddl.auto` is `validate`), so it reports `missing table` if the schema was not loaded. `08_users.sql` creates four database users with different privileges (`smartcity_app` for the backend, `smartcity_admin`, `smartcity_readonly` and `smartcity_restricted`); the table of users and how to show their privileges are in [`docker/README.md`](docker/README.md#users).

`09_citizen_status_audit.sql` adds auditing of the most important change in the city: a citizen's status. A trigger on `citizen` (`AFTER UPDATE … WHEN (OLD.status IS DISTINCT FROM NEW.status)`) writes every status change, for example `alive` → `deceased` through `PUT /citizens/{id}`, to the table `citizen_status_audit`: citizen, old and new status, old and new death date, the database user, the logged-in API user and the time. It also catches a status that the schema's own trigger `update_citizen_status` sets from the death date, and it skips updates that leave the status as it is. The API user comes from the backend: `GenericDAO` passes it to each transaction with `set_config('smartcity.app_user', …, true)` (`AuditContext`). The log is append-only (a trigger refuses `UPDATE`, `DELETE` and `TRUNCATE`, also for `smartcity_admin`), `smartcity_app` cannot read or change it (only the `SECURITY DEFINER` trigger function writes it), and `smartcity_restricted` cannot read it. Example: "who declared citizen 42 deceased?"

```sql
SELECT changed_at, old_status, new_status, new_death_date, app_user, db_user
FROM citizen_status_audit WHERE citizen_id = 42 ORDER BY id;
```

**3. Stop or start over:**

```text
docker compose stop     # stop, keeping the data
docker compose down     # remove the containers, keeping the data
```

To start with an empty database, run `docker compose down`, delete `docker/data/` and repeat steps 1 and 2. The data folders are not committed.

Other things in the Docker setup are documented in [`docker/README.md`](docker/README.md): the daily statistics event (`pg_cron`), and database backup, restore and compare with the scripts in `scripts/`.

<!-- TODO(migrator): MongoDB and Neo4j are started by the same Compose file but are empty. When the migrator
documentation is added, mention here how to create their structure (NoSQL/setup.ps1) and link to the
"Migration to MongoDB and Neo4j" section. -->

### Configure the database and JWT

Create the ignored file `src/main/resources/config.properties` (do not commit it):

```properties
DB_NAME=smart_city
DB_USERNAME=smartcity_app
DB_PASSWORD=app-dev-password
ISSUER=smart-city-local
TOKEN_EXPIRE_TIME=1800000
SECRET_KEY=replace-with-a-long-random-secret
ADMIN_PASSWORD=choose-a-local-admin-password
```

`smartcity_app` is the application user created by `08_users.sql` (it can read and write rows but not change the schema). The application connects to `localhost:5432`. On every start the application creates what is missing: the roles `user` and `admin`, the user `admin` (only when `ADMIN_PASSWORD` is set) and the test users `User1` / `Password123` and `User2` / `Password456`. Existing rows are left alone, so a restart adds nothing.

`TOKEN_EXPIRE_TIME` is in milliseconds. Use a unique random `SECRET_KEY` outside local development; never put real credentials or signing keys in source control. The repository's `.gitignore` excludes `src/main/resources/config.properties`.

For deployment, set `DEPLOYED` and provide `DB_NAME`, `CONNECTION_STR`, `DB_USERNAME`, `DB_PASSWORD`, `ISSUER`, `TOKEN_EXPIRE_TIME`, `SECRET_KEY` and `ADMIN_PASSWORD` as environment variables. `OVERPASS_URL` is optional; when unset, the application uses the public Overpass interpreter endpoint.

## Step 3: Backend

Build and run from the project root:

```text
mvn clean package
java -jar target/app.jar
```

Alternatively, open the project in IntelliJ IDEA, allow Maven to import dependencies, and run `app.Main`. The server starts on port `7070`.

### Administrator account

The application creates the `admin` user on startup when `ADMIN_PASSWORD` is set in `config.properties` (or as an environment variable). If it is not set, the application logs `ADMIN_PASSWORD is not set: no admin user was created` and no administrator exists. The password is the one you chose; do not hard-code or commit it. Existing accounts are left alone, so changing `ADMIN_PASSWORD` later does not change the password of an existing `admin` user. Normal registrations receive the `user` role by default.

### Seed locations and sample data

The seed endpoints require an admin JWT. First import named locations from OSM, then generate fake domain data that references those locations, and optionally enrich entities from OSM tags. The operations are manual; the application does not seed data on startup.

```text
POST /api/admin/seed/locations
POST /api/admin/seed/fake?citizens=100&seed=42
POST /api/admin/seed/enrich
```

The locations endpoint defaults to a central Copenhagen bounding box, `amenity=hospital`, and a result limit of 20 (maximum 200). Fake data accepts 1–1000 citizens and requires at least one imported location. Overpass is a shared public service; keep requests modest and use the endpoint only when needed.

## Running the tests

Run the unit and non-database tests with:

```text
mvn test
```

Tests under `src/test/java/app/daos/` use real PostgreSQL through Testcontainers and are run by the Failsafe integration-test phase. Docker must be running. Run the full suite with:

```text
mvn clean verify
```

The Overpass import client tests use local WireMock responses and do not call the live service. The real Overpass smoke test is disabled by default; opt into it only when you want to send a request to the public API:

```text
mvn test -Dtest=OverpassLiveTest -Doverpass.live=true
```

#### Screenshot of JaCoCo report
![docs/jacoco1.png](docs/jacoco1.png)

`app.security.rest` contains controllers and routes and is therefore not covered by unit or integration tests, just like the main routes and controllers. Those are covered by the [API tests in Postman](#api-tests-with-postman).

## Static analysis with SonarQube

SonarQube (Community Build) analyses the source code without running it and reports bugs, vulnerabilities, security hotspots, code smells and duplication. It also shows the JaCoCo coverage from the test run. The server runs locally in Docker; nothing is sent to the cloud.

### Start the server

Docker Desktop must be running.

```text
docker run -d --name sonarqube -p 9000:9000 sonarqube:community
```

Startup takes 1 to 2 minutes. Open <http://localhost:9000>. Later, start and stop the same container with `docker start sonarqube` and `docker stop sonarqube`; removing it (`docker rm`) deletes the project and its history.

### First-time setup

1. Log in with `admin` / `admin` and choose a new password. It is stored only in your own container.
2. Create a local project with project key `smart-city` and choose **Use the global setting** for the new code definition.
3. Choose **Locally**, generate a token and copy it. The token is shown once; never commit it or paste it into shared files.

### Run an analysis

First run the tests so JaCoCo writes `target/site/jacoco/jacoco.xml`, then run the Sonar scanner. Use the full plugin name, because the short name `sonar:sonar` is not known to Maven by default:

```text
mvn clean verify
mvn org.sonarsource.scanner.maven:sonar-maven-plugin:5.9.0.7382:sonar "-Dsonar.projectKey=smart-city" "-Dsonar.host.url=http://localhost:9000" "-Dsonar.token=YOUR_TOKEN" "-Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml" "-Dsonar.coverage.exclusions=**/*MapperImpl.java"
```

- The quotes are needed in PowerShell. In IntelliJ's **Execute Maven Goal** dialog, leave out `mvn` and the quotes.
- The scanner needs Java 21 and downloads its own runtime into `~/.sonar/cache` the first time, so the project itself stays on Java 17. If unpacking the runtime fails, delete that cache folder and run the command again.
- The coverage exclusion leaves out the generated MapStruct classes, as in the JaCoCo configuration. The controllers, routes, `app.security.rest` and `Main` are tested through Postman, not unit tests; to leave them out of Sonar's coverage figure as well, add `**/controllers/**`, `**/routes/**`, `**/security/rest/**` and `**/Main.java` to `sonar.coverage.exclusions`, and state that choice in the report.
- Commit your changes before an analysis. Sonar reads the blame data from git, and uncommitted files cause the warning "Missing blame information".
- The Community Build analyses one branch (shown as `main` in the dashboard), whichever branch you have checked out.

### Read the results

Open <http://localhost:9000/dashboard?id=smart-city> and use the **Overall Code** tab.

| Part | What it shows |
|---|---|
| Quality gate | Pass or fail against the default "Sonar way" conditions: no new issues, all new security hotspots reviewed, coverage of at least 80% and at most 3% duplication on new code |
| Issues | Bugs, vulnerabilities and code smells. Open one to read why it matters and how to fix it |
| Security Hotspots | Security-sensitive code that a person must review and mark as Safe or Fixed |
| Measures | Coverage (from JaCoCo), duplication, complexity and ratings from A to E |

A finding can be fixed, or set to **Accepted** or **False positive** with a comment explaining the decision. Run the analysis again after each round of fixes and compare the dashboards.

### SonarQube screenshots
#### First run
![docs/sonarqube1.png](docs/sonarqube1.png)
#### Example of an issue (GenericDAO mergeAllEntities)
![docs/sonarqube - mergeallentities1.png](docs/sonarqube%20-%20mergeallentities1.png)
### Latest run
![docs/sonarqube2.png](docs/sonarqube2.png)

Code coverage doesn't match the real picture, code for controllers and routes have been excluded for SonarQube coverage to pass quality gate. For real picture of code coverage, see the [Screenshot of JaCoCo report](#screenshot-of-jacoco-report).

## API tests with Postman

Import `postman/SmartCity.postman_collection.json` and the local environment from `postman/SmartCity.local.postman_environment.json`. Set the environment's base URL to `http://localhost:7070`, start the application, and use the collection's auth requests to register or log in before calling protected routes. The collection includes CRUD, authentication, nearby-place, and cleanup requests.

The `09 Jobs` folder uses an admin token, because only an admin can create jobs and see or set salaries. `00 Auth` gets it with "Login as admin", which uses the environment variable `adminPassword` (default `Admin123!`). It must match `ADMIN_PASSWORD` in your `config.properties`; change one of them if yours differs.

### Screenshot of Postman collection run
![docs/postman1.png](docs/postman1.png)

## Frontend demo

`frontend/` is a small Vite + React (JavaScript) app that demonstrates the API in a browser and is the system under test for the Playwright tests. It has pages to register, log in and log out, one CRUD page each for families, locations, housings and citizens, and a nearby-places page that calls `GET /locations/{id}/nearby`.

Start the backend first (port 7070, database running), then in a second terminal:

```text
cd frontend
npm install
npm run dev
```

The app is served on <http://localhost:5173>.

### How the frontend reaches the API

The backend has no CORS configuration, so the browser never calls it directly. Every request goes to `/backend/...` on the Vite dev server, which forwards it to `http://localhost:7070` and removes the `/backend` prefix (see `frontend/vite.config.js`). The backend therefore needs no changes. To point the frontend at another API, set `VITE_API_TARGET` before `npm run dev`.

### How it is built

| File | Purpose |
|---|---|
| `src/api.js` | One `request` helper; turns the API's error bodies (`{"error": ...}` and `{"msg": ...}`) into readable messages |
| `src/auth.jsx` | Login, register and logout; keeps the JWT in `localStorage` under `smartcity.auth` so a reload stays logged in |
| `src/resources.js` | Describes each CRUD resource (path, fields, types). Adding an entry creates a new page |
| `src/CrudPage.jsx` | Generic table and create/edit/delete form driven by `resources.js` |
| `src/NearbyPage.jsx` | Form and result list for nearby places |

Notes:

- A `401` from the API logs the user out and returns to the login page.
- Dates from the API may arrive as `[year, month, day]` arrays; the frontend shows them as `yyyy-mm-dd`.
- Access control is done by the backend (see [Role rules](#role-rules)): a normal user cannot see salaries or create jobs. The login redirect is only a frontend convenience.
- Elements carry `data-testid` attributes that the tests use as selectors.

## End-to-end tests with Playwright

The Playwright tests drive the real UI in Chromium against the real backend and database. They are written in code (`frontend/e2e/`) and are not recorded.

### Run the tests

1. Start the Docker databases (Step 2) and the backend on port 7070.
2. In `frontend/`:

```text
npm install
npx playwright install chromium
npm run e2e
```

Playwright starts the Vite dev server itself, or reuses one that is already running on port 5173. Other useful commands:

| Command | Effect |
|---|---|
| `npm run e2e` | Run all tests headless; prints a list and writes an HTML report |
| `npm run e2e:headed` | Run with a visible browser |
| `npm run e2e:ui` | Open Playwright's UI mode to step through tests |
| `npm run e2e:report` | Open the last HTML report (screenshots and traces of failures) |
| `npx playwright test e2e/auth.spec.js` | Run one file |
| `npx playwright test -g "wrong password"` | Run tests whose name matches |

Tests run one at a time (`workers: 1`) because they share one database. Each test creates its own user with a unique name, so the suite can be repeated without cleaning the database. The tests create and delete families, locations, housings and citizens; use a development database.

### What is tested

| File | Test | Type |
|---|---|---|
| `auth.spec.js` | Unauthenticated visitor is redirected to login | Negative |
| | Register a new user and land on the app | Positive |
| | Log in as an existing user | Positive |
| | Wrong password shows an error | Negative |
| | Unknown user cannot log in | Negative |
| | Empty login form is rejected | Negative |
| | Logout returns to login and blocks protected pages | Positive |
| | A `401` from the API logs the user out | Negative |
| `crud.spec.js` | Create, edit and delete a family | Positive |
| | Family without a name is rejected | Negative |
| | Create a location and a housing that points to it | Positive |
| | Housing with capacity 0 is rejected | Negative (boundary) |
| | Create a citizen linked to a family | Positive |
| | Citizen born in the future is rejected | Negative |
| | Alive citizen with a death date is rejected | Negative |
| | Citizen without a first name is rejected | Negative |
| | Nearby places show results (Overpass response stubbed with `page.route`) | Positive |
| | Unknown location shows an error | Negative |

The negative tests assert the validation messages from `src/main/java/app/validators/`. If a message is reworded in the backend, update the matching test.

### Design choices

- Tests sign in through the API and put the token in `localStorage` (`signIn` in `e2e/helpers.js`), so they start on the page under test instead of repeating the login form. Only `auth.spec.js` uses the login form.
- The nearby-places success test stubs the response, so the suite does not depend on the public Overpass service. The backend's own Overpass behaviour is covered by the WireMock tests and the opt-in `OverpassLiveTest`.
- Static analysis and JaCoCo coverage cover the backend only. The frontend is covered by these end-to-end tests.

### Screenshot of Playwright report
![docs/playwright1.png](docs/playwright1.png)

## API reference

The backend listens on `http://localhost:7070`. This section covers the interactive documentation, authentication and roles, the CRUD resources (with paging, sorting and filtering), and the remaining endpoints.

### API documentation (Swagger UI)

With the backend running, open <http://localhost:7070/swagger>. The page lists every endpoint with its parameters, request and response bodies, status codes and required role, and lets you try the requests.

1. Run `POST /api/auth/login` under **Authentication**, for example with `User1` / `Password123`, or `admin` with the `ADMIN_PASSWORD` of your environment.
2. Copy the `token` from the response, press **Authorize** at the top and paste it.
3. Every request now sends the token. Press **Try it out** on an endpoint, fill in the fields and press **Execute**.

The description is [`src/main/resources/public/docs/openapi.yaml`](src/main/resources/public/docs/openapi.yaml) (OpenAPI 3.0.3), served as a public static file together with `index.html`, which loads Swagger UI 5.17.14 from a CDN (internet access is needed). The file is written by hand, so **update it when you change a route in `Routes.java` or a DTO.** The route overview at `/api/routes` lists what the application registers, which is a quick way to compare.

### Authentication

| Method | Path | Access | Description |
|---|---|---|---|
| POST | `/api/auth/register` | Public | Register a user (role `user`). 201 with username and roles; 400 for a missing username or password or a password under 8 characters; 409 for a taken username |
| POST | `/api/auth/login` | Public | Verify credentials and return a JWT. 400 for a missing username or password; 401 for a wrong password or unknown user |
| POST | `/api/auth/logout` | `USER`, `ADMIN` | Reject the token in the request from now on |
| GET | `/api/secured/demo` | `USER` | Verify a user token |

Registration and login accept JSON credentials:

```json
{
  "username": "city-user",
  "password": "a-local-password"
}
```

Send the token returned by login on protected routes as `Authorization: Bearer <token>`.

**Logout.** Every token has a unique id (the `jti` claim). Logout puts that id on a denylist until the token's expiry time, and the authorize filter rejects any token on the list with 401. Other tokens of the same user keep working. The denylist is kept in memory, so a restart clears it: a logged-out token is accepted again until it expires (at most `TOKEN_EXPIRE_TIME`).

### Roles and access

Users and administrators have full CRUD on all city data. The exception is a job's salary, which only administrators can see, set, filter or sort by.

| Action | No token | `USER` | `ADMIN` |
|---|---|---|---|
| `POST /api/auth/register`, `POST /api/auth/login` | Yes | Yes | Yes |
| `GET`, `POST`, `PUT`, `DELETE` on every CRUD resource except jobs | 401 | Yes | Yes |
| `GET /locations/{id}/nearby`, the database views | 401 | Yes | Yes |
| `GET /jobs`, `GET /jobs/{id}` | 401 | Yes, `salary` is `null` | Yes, with salary |
| `GET /jobs` with a `salary…` filter or a `sort=salary` | 401 | 403 | Yes |
| `POST /jobs` (a job needs a salary) | 401 | 403 | Yes |
| `PUT /jobs/{id}` | 401 | Yes without `salary` (the stored salary is kept); sending a salary gives 403 | Yes |
| `DELETE /jobs/{id}` | 401 | Yes | Yes |
| `GET /active-employment` | 401 | Yes, `salary` is `null` | Yes, with salary |
| `GET /citizens/{id}/total-salary` | 401 | 403 | Yes |
| `POST /api/admin/seed/*` | 401 | 403 | Yes |
| `POST /api/auth/logout` | 401 | Yes | Yes |

A missing, malformed, expired, tampered or logged-out token gives 401; a valid token without the needed role gives 403. Route roles are set in `Routes.java` (`USERS`, `ADMINS`) and the salary rules in `JobController.java` and `DatabaseViewsController.java`. They are the API counterpart of the database user `smartcity_restricted`, which can read every `job` column except `salary` (`SQL/users/08_users.sql`).

### CRUD resources

Each resource is registered at the application root and supports `GET /<resource>`, `GET /<resource>/{id}`, `POST /<resource>`, `PUT /<resource>/{id}` and `DELETE /<resource>/{id}`.

| Resource | Path |
|---|---|
| Locations | `/locations` |
| Families | `/families` |
| Housings | `/housings` |
| Citizens | `/citizens` |
| Citizen relationships | `/citizen-relationships` |
| Institutions | `/institutions` |
| Companies | `/companies` |
| Education records | `/education-records` |
| Jobs | `/jobs` |

#### Paging, sorting and filtering

Collection endpoints take these query parameters:

| Parameter | Meaning | Default |
|---|---|---|
| `page` | Zero-based page number | `0` |
| `size` | Items per page, 1 to 100 | `20` |
| `sort` | `field,asc` or `field,desc` | `id,asc` |
| any filter | See the table below | none |

They return an envelope with `data`, `page`, `size`, `totalElements` and `totalPages`; the counts include all matching records. A page beyond the last returns an empty `data` array. Example:

```text
GET /citizens?page=0&size=20&sort=familyId,asc&lastName=smith&birthDateFrom=1980-01-01&birthDateTo=2000-12-31
```

Sort and filter names are allow-listed per resource. Relationship fields such as `familyId`, `housingId` and `locationId` address the related entity ids. String filters match case-insensitively on part of the value; filters ending in `From` and `To` are inclusive ranges. Unsupported fields, invalid filter values, invalid page or size values and invalid sort directions return 400.

| Resource | Filters |
|---|---|
| Citizens | `status`, `lastName`, `birthDateFrom`, `birthDateTo`, `familyId`, `housingId` |
| Families | `familyName` |
| Housings | `locationId`, `housingType`, `capacityFrom`, `capacityTo` |
| Locations | `name`, `streetAddress` |
| Companies | `locationId`, `name`, `status`, `foundedDateFrom`, `foundedDateTo` |
| Institutions | `locationId`, `name`, `type` |
| Jobs | `citizenId`, `companyId`, `institutionId`, `title`, `status`, `salaryFrom`, `salaryTo` (admin only), `startDateFrom`, `startDateTo`, `endDateFrom`, `endDateTo` |
| Education records | `citizenId`, `institutionId`, `startDateFrom`, `startDateTo`, `endDateFrom`, `endDateTo` |
| Citizen relationships | `citizenId1`, `citizenId2`, `relationshipType`, `startDateFrom`, `startDateTo`, `endDateFrom`, `endDateTo` |

### Database views and functions

These endpoints read PostgreSQL views and a stored function. The views return all rows and are not paged.

| Method | Path | Access | Description |
|---|---|---|---|
| GET | `/citizen-details` | `USER`, `ADMIN` | Each citizen with age, family name and housing |
| GET | `/housing-occupancy` | `USER`, `ADMIN` | Capacity, current occupants and free spots per housing |
| GET | `/company-financial-summary` | `USER`, `ADMIN` | Active jobs and total active salaries per company |
| GET | `/active-employment` | `USER`, `ADMIN` | Active jobs with employee and company; `salary` is `null` for users |
| GET | `/citizen-relationships-view` | `USER`, `ADMIN` | Relationships with both citizens' names |
| GET | `/citizens/{id}/total-salary` | `ADMIN` | Total salary of a citizen (stored function `total_citizen_salary`) |

### Other endpoints

| Method | Path | Access | Description |
|---|---|---|---|
| GET | `/locations/{id}/nearby?radius=1000&amenity=hospital` | `USER`, `ADMIN` | Find nearby OSM places for a location |
| POST | `/api/admin/seed/locations?south=...&west=...&north=...&east=...&amenity=hospital&limit=20` | `ADMIN` | Import named OSM locations; coordinates and query options are optional |
| POST | `/api/admin/seed/fake?citizens=100&seed=42` | `ADMIN` | Generate deterministic related fake records |
| POST | `/api/admin/seed/enrich` | `ADMIN` | Fill missing fields from OSM tags |

The route overview is available at `/api/routes` while the application runs. Exact request fields and error messages follow the DTOs and validators in `src/main/java/app/dtos/` and `src/main/java/app/validators/`.

## Database Design

The database design follows three stages of relational database modelling:

#### 1. Conceptual Model

```mermaid

flowchart TD
    Family["Family"]
    Citizen["Citizen"]
    Housing["Housing"]
    Location["Location"]
    Company["Company"]
    Institution["Institution"]
    Job["Employment"]
    Education["Education"]
    Relationship["Citizen Relationship"]

    Family -->|"groups"| Citizen
    Citizen -->|"may live in"| Housing
    Housing -->|"located at"| Location

    Company -->|"located at"| Location
    Institution -->|"located at"| Location

    Citizen -->|"has"| Job
    Job -->|"at"| Company
    Job -->|"at"| Institution

    Citizen -->|"has"| Education
    Education -->|"at"| Institution

    Citizen -->|"participates in"| Relationship
    Relationship -->|"connects two citizens"| Citizen

```

The conceptual model provides a high-level overview of the Smart City domain. It identifies the main entities and illustrates how they interact, without including implementation details such as attributes, primary keys or foreign keys.

#### 2. Logical Model

```mermaid 

erDiagram
    FAMILY {
        identifier id PK
        text familyName
    }

    CITIZEN {
        identifier id PK
        text firstName
        text lastName
        date birthDate
        date deathDate
        text status
        text gender
        identifier family_id FK
        identifier housing_id FK "Optional"
    }

    HOUSING {
        identifier id PK
        identifier location_id FK
        text housingType
        number capacity
        number numberOfLevels
        number numberOfFlats
        number height
    }

    LOCATION {
        identifier id PK
        text streetAddress
        text name
        number latitude
        number longitude
        identifier osmId
        text osmType
    }

    COMPANY {
        identifier id PK
        identifier location_id FK
        text name
        text cvrNumber
        text productionUnitNumber
        text website
        text phone
        text email
        text category
        text openingHours
        date foundedDate
        date dissolvedDate
        text status
    }

    INSTITUTION {
        identifier id PK
        identifier location_id FK
        text name
        text type
        text website
        text phone
        text email
    }

    JOB {
        identifier id PK
        identifier citizen_id FK
        identifier company_id FK "Optional"
        identifier institution_id FK "Optional"
        text title
        decimal salary
        date startDate
        date endDate
        text status
    }

    EDUCATION_RECORD {
        identifier id PK
        identifier citizen_id FK
        identifier institution_id FK
        date startDate
        date endDate
    }

    CITIZEN_RELATIONSHIP {
        identifier id PK
        identifier citizen_id_1 FK
        identifier citizen_id_2 FK
        text relationshipType
        date startDate
        date endDate
    }

    FAMILY ||--o{ CITIZEN : contains
    HOUSING o|--o{ CITIZEN : accommodates
    LOCATION ||--o{ HOUSING : contains
    LOCATION ||--o{ COMPANY : locates
    LOCATION ||--o{ INSTITUTION : locates

    CITIZEN ||--o{ JOB : has
    COMPANY o|--o{ JOB : employs
    INSTITUTION o|--o{ JOB : employs

    CITIZEN ||--o{ EDUCATION_RECORD : has
    INSTITUTION ||--o{ EDUCATION_RECORD : provides

    CITIZEN ||--o{ CITIZEN_RELATIONSHIP : participant_one
    CITIZEN ||--o{ CITIZEN_RELATIONSHIP : participant_two

```

The logical model expands on the conceptual design by defining the attributes of each entity, identifying primary and foreign keys, and specifying the relationships and cardinalities between entities.

It is independent of the database management system and focuses on the logical organization of the data.

#### 3. Physical Model (Entity Relationship Diagram)
````mermaid

erDiagram
    family {
        BIGINT id PK
        VARCHAR family_name
    }

    citizen {
        BIGINT id PK
        VARCHAR first_name
        VARCHAR last_name
        DATE birth_date
        DATE death_date
        VARCHAR status
        VARCHAR gender
        BIGINT family_id FK
        BIGINT housing_id FK
    }

    housing {
        BIGINT id PK
        BIGINT location_id FK
        VARCHAR housing_type
        INTEGER capacity
        INTEGER number_of_levels
        INTEGER number_of_flats
        DOUBLE_PRECISION height
    }

    location {
        BIGINT id PK
        VARCHAR street_address
        VARCHAR name
        DOUBLE_PRECISION latitude
        DOUBLE_PRECISION longitude
        BIGINT osm_id
        VARCHAR osm_type
    }

    company {
        BIGINT id PK
        BIGINT location_id FK
        VARCHAR name
        VARCHAR cvr_number
        VARCHAR production_unit_number
        VARCHAR website
        VARCHAR phone
        VARCHAR email
        VARCHAR category
        VARCHAR opening_hours
        DATE founded_date
        DATE dissolved_date
        VARCHAR status
    }

    institution {
        BIGINT id PK
        BIGINT location_id FK
        VARCHAR name
        VARCHAR type
        VARCHAR website
        VARCHAR phone
        VARCHAR email
    }

    job {
        BIGINT id PK
        BIGINT citizen_id FK
        BIGINT company_id FK
        BIGINT institution_id FK
        VARCHAR title
        NUMERIC salary
        DATE start_date
        DATE end_date
        VARCHAR status
    }

    education_record {
        BIGINT id PK
        BIGINT citizen_id FK
        BIGINT institution_id FK
        DATE start_date
        DATE end_date
    }

    citizen_relationship {
        BIGINT id PK
        BIGINT citizen_id_1 FK
        BIGINT citizen_id_2 FK
        VARCHAR relationship_type
        DATE start_date
        DATE end_date
    }

    family ||--o{ citizen : has
    housing o|--o{ citizen : houses
    location ||--o{ housing : contains
    location ||--o{ company : locates
    location ||--o{ institution : locates

    citizen ||--o{ job : has
    company o|--o{ job : employs
    institution o|--o{ job : employs

    citizen ||--o{ education_record : has
    institution ||--o{ education_record : provides

    citizen ||--o{ citizen_relationship : participant_one
    citizen ||--o{ citizen_relationship : participant_two

````
The physical model translates the logical design into a database structure. It specifies the database tables, columns, SQL data types, primary keys and foreign keys.

The database schema is created by `SQL/schema/database_schema.sql` (loaded in [Step 2](#step-2-databases-docker)). The application uses PostgreSQL with Hibernate and JPA, but Hibernate only validates that the Java entity classes match the schema (`hibernate.hbm2ddl.auto` is `validate`). The other scripts in `SQL/` (users and privileges, functions, procedures, triggers, views and the daily event) are applied the same way.

The three models represent different levels of detail of the same Smart City domain.

## Migration to MongoDB and Neo4j

PostgreSQL is the source system. MongoDB and Neo4j get the same data once, through the migrator, and then serve the same API under their own prefix. Details of the designs are in [migrator/README.md](migrator/README.md).

### Overview

| | PostgreSQL | MongoDB | Neo4j |
|---|---|---|---|
| Structure | `SQL/schema/database_schema.sql` | `NoSQL/mongodb/01_collections.js`: 7 collections with `$jsonSchema` validators, 5 indexes | `NoSQL/neo4j/01_constraints.cypher`: 9 uniqueness constraints, 6 indexes |
| Data | seeded (`populate_test_data()`) | the migrator | the migrator |
| API | `/citizens`, `/api/auth/login` | `/mongodb/citizens`, `/mongodb/api/auth/login` | `/neo4j/citizens`, `/neo4j/api/auth/login` |
| Dump / load | | `NoSQL/mongodb/dump/smart_city.archive.gz` | `NoSQL/neo4j/dump/neo4j.dump` |
| Users and privileges | `SQL/users/08_users.sql` | `NoSQL/mongodb/02_users.js`: 2 custom roles + view `citizens_restricted` | `NoSQL/neo4j/02_users.cypher` (users); `03_privileges_enterprise.cypher` (roles, Enterprise/AuraDB only) |

All three have the same four database users: `smartcity_app` (the backend: read and write data, no structure changes), `smartcity_admin` (everything), `smartcity_readonly` (read everything) and `smartcity_restricted` (read without salaries and login users). In MongoDB the restricted user reads citizens through the view `citizens_restricted`, which leaves out `jobs.salary`, like the column-level grant in PostgreSQL. Neo4j Community has users but no roles (`GRANT` gives "Unsupported administration command"), so there every user has full rights and the application's role rules do the restricting; on Enterprise or AuraDB `03_privileges_enterprise.cypher` adds the roles, including `DENY READ {salary}` on `WORKS_AT`. The local MongoDB container runs without authentication, so its users only take effect with authentication on (always on MongoDB Atlas); `NoSqlDatabaseUsersTest` checks every user's rights on a MongoDB with authentication.

The migrator (`migrator/`, its own `pom.xml`) reads every table in one read-only transaction as `smartcity_readonly`, maps the rows to documents and to a graph, writes both, and compares 25 counts. It also sets the id counters the CRUD application uses (MongoDB collection `counters`, Neo4j `:Sequence` nodes), so new ids continue after the migrated ones.

### Document design (MongoDB)

Citizens are the aggregate: family (id + name), jobs (with salary and employer name), education and relationships are embedded; the housing is referenced (`housingId`). Housings, companies and institutions embed their location. A relationship is embedded in both citizens, with `direction` (`outgoing` for citizen 1, `incoming` for citizen 2).

### Graph design (Neo4j)

`Citizen`, `Family`, `Housing`, `Location`, `Company`, `Institution`, `User` and `Role` are nodes. Foreign keys become `MEMBER_OF`, `LIVES_IN`, `LOCATED_AT` and `HAS_ROLE`; the join tables become relationships with properties: `WORKS_AT {title, salary, ...}`, `STUDIED_AT`, `RELATED_TO {type, ...}`.

### Run the migration

With the Docker databases running and `SQL/users/08_users.sql` loaded:

```text
powershell -ExecutionPolicy Bypass -File NoSQL\setup.ps1
cd migrator
mvn package
java -jar target/migrator.jar
```

Exit code 0 when every count matches. Instead of migrating, the committed dumps can be loaded with `powershell -ExecutionPolicy Bypass -File NoSQL\load.ps1`; `NoSQL\dump.ps1` writes new ones (it stops the Neo4j container for a few seconds, because Neo4j Community can only dump a stopped database).

### The MongoDB and Neo4j API

All nine resources exist under `/mongodb` and `/neo4j` with the same paths, DTOs, validation, paging (`page`, `size`), filters, sorting (`sort=field,direction`) and role rules as PostgreSQL, including the admin-only salary. Each solution has its own login users: `POST /mongodb/api/auth/login` checks the MongoDB `users` collection, `POST /neo4j/api/auth/login` the `User`/`Role` nodes; the migrated users keep their passwords (`admin` / your `ADMIN_PASSWORD`, `User1` / `Password123`).

The controllers only know the `CrudService` interface, so only the data layer differs (`app/mongo`, `app/neo4j`). PostgreSQL's foreign keys and triggers are done in the services: 404 for an unknown reference, 409 when a delete would break one (`ON DELETE RESTRICT`), a deleted housing leaves its residents without one (`SET NULL`), housing capacity, job dates, and the death cleanup of `trg_citizen_post_death_cleanup`. MongoDB also updates its copies when a name changes (a renamed company is renamed in every job). Neo4j writes in one transaction; a standalone MongoDB has no multi-document transactions, so a write that moves an embedded row is two atomic single-document writes.

Connection settings: `MONGO_URI`, `MONGO_DB`, `NEO4J_URI`, `NEO4J_USER`, `NEO4J_PASSWORD` (environment variable, else `config.properties`, else the Docker defaults). When a database cannot be reached its routes answer 503.

### Migrator tests

```text
cd migrator
mvn verify   # 15 unit tests + 11 end-to-end tests (PostgreSQL, MongoDB and Neo4j in Docker)
```

The main project's `NoSqlSolutionsApiTest` runs 11 scenarios (login, roles, CRUD, paging, filters, business rules, deletes, death cleanup) against both `/mongodb` and `/neo4j` with Testcontainers.

## Continuous integration and delivery

![CI](https://github.com/Gervig/Smart-City/actions/workflows/ci.yml/badge.svg)

The project has a continuous integration pipeline on GitHub Actions, defined in
[`.github/workflows/ci.yml`](.github/workflows/ci.yml). It checks for integration errors by running the unit tests, the
integration tests in code, the API tests and the end-to-end UI tests automatically.

### When it runs

- on every push to `dev` or `main`,
- on every pull request into `dev` or `main`, so a branch is tested before it is merged,
- by hand, from the **Actions** tab (**CI**, then **Run workflow**).

A newer push to the same branch cancels the run that is still going.

### The jobs

| Job | What it does | Tests |
|---|---|---|
| **Unit and integration tests** | `mvn -B clean verify` on Java 17 | Surefire runs the unit tests. Failsafe runs the integration tests in `src/test/java/app/daos/`, which start PostgreSQL with Testcontainers (Docker is available on the GitHub runner). JaCoCo creates the coverage report |
| **API tests** | Runs only if the first job passed. Starts PostgreSQL 16.2, loads the schema and the database users, starts the backend, and runs the Postman collection with Newman | The collection in `postman/` against the running API: status codes, response times, JSON content, positive and negative cases |
| **End-to-end tests** | Runs only if the first job passed, in parallel with the API tests. Starts its own PostgreSQL and backend the same way, then runs the Playwright tests with a headless Chromium against the React frontend | The tests in `frontend/e2e/`: register, log in and out, and create, edit and delete records in the real UI |

The API job follows the same steps as development:

1. A PostgreSQL 16.2 service container with the database `smart_city`.
2. `SQL/schema/database_schema.sql` creates the tables (Hibernate only validates them).
3. `SQL/users/08_users.sql` creates the database users. The backend connects as `smartcity_app`, the least-privilege
   application user, so the API tests also prove that this user has the rights the application needs.
4. `mvn -B -DskipTests package` builds `target/app.jar`, which is started in the background. It reads its settings
   from environment variables because `DEPLOYED` is set, so no `config.properties` is needed on the runner.
5. `newman run postman/SmartCity.postman_collection.json -e postman/SmartCity.local.postman_environment.json` runs the
   collection against `http://localhost:7070`. It creates the `admin` user from `ADMIN_PASSWORD`, which must be the same
   value as `adminPassword` in the Postman environment (`Admin123!`).

The database is new on every run, as the collection expects (see the note at the top of the collection).

The **end-to-end job** repeats steps 1 to 4 with its own database and backend (the jobs run on separate machines), and then:

6. `npm ci` installs the frontend dependencies and `npx playwright install --with-deps chromium` installs the browser.
7. `npm run e2e` runs the Playwright tests. Playwright starts the Vite dev server itself (`frontend/playwright.config.js`); the
   frontend sends its API requests to `/backend`, which Vite forwards to the backend on port 7070. The tests drive the real
   UI against the real backend and database, and create their own test users, so they do not depend on the API job.

### No secrets are needed

All values in the workflow (database passwords, the JWT secret, the admin password) are throw-away values that exist only
for the duration of a run, in a database that is discarded afterwards. Never put real credentials in `ci.yml`; if the
pipeline ever needs one, add it under **Settings → Secrets and variables → Actions** and use `${{ secrets.NAME }}`.

### Read the result

1. Open the **Actions** tab on GitHub and select a run. A green check means all three jobs passed.
2. Open a job to see the output of every step. The `mvn` step lists every test class, and the Newman step prints the
   same request-by-request report as the Postman runner, with a summary table of requests and assertions at the end.
3. Under **Artifacts** at the bottom of the run page you can download:
   - `maven-test-reports`: the Surefire and Failsafe reports and the JaCoCo coverage report (`target/site/jacoco/index.html`),
   - `api-test-results`: `newman-results.xml` (JUnit format) and `app.log`, the backend's log during the API tests,
   - `e2e-test-results`: the Playwright HTML report (`playwright-report/index.html`), and on a failure the screenshots and
     traces in `test-results/`, plus `app.log`. Open a trace with `npx playwright show-trace <trace.zip>`.

A pull request shows the result as a check at the bottom of the page. To make a green run required before a pull request
can be merged into `dev`, enable it under **Settings → Branches → Branch protection rules** (require status checks,
and select all three jobs).

### Run the same steps on your own computer

Unit and integration tests (Docker Desktop must be running):

```text
mvn clean verify
```

API tests, from the project root, with the Docker databases from [Step 2](#step-2-databases-docker) running and the
schema and users loaded:

```text
mvn -DskipTests package
$env:DEPLOYED = "true"
$env:DB_NAME = "smart_city"
$env:CONNECTION_STR = "jdbc:postgresql://localhost:5432/"
$env:DB_USERNAME = "smartcity_app"
$env:DB_PASSWORD = "app-dev-password"
$env:ISSUER = "smart-city-local"
$env:TOKEN_EXPIRE_TIME = "1800000"
$env:SECRET_KEY = "a-local-secret-key-of-at-least-32-characters"
$env:ADMIN_PASSWORD = "Admin123!"
java -jar target/app.jar
```

In a second terminal:

```text
npx newman run postman/SmartCity.postman_collection.json -e postman/SmartCity.local.postman_environment.json
```

The collection needs an empty database, so reset it first (see [`docker/README.md`](docker/README.md)) if you ran it before.

End-to-end tests, with the backend running as above (see [End-to-end tests with Playwright](#end-to-end-tests-with-playwright)):

```text
cd frontend
npm ci
npx playwright install chromium
npm run e2e
```

### What is not in the pipeline

| Not included | Why |
|---|---|
| Stress, load and spike tests | They take a long time, give unreliable numbers on a shared runner, and need a known environment. They are run by hand and documented separately |
| SonarQube | It runs on a local server that the runner cannot reach. The analysis is run locally (see [Static analysis with SonarQube](#static-analysis-with-sonarqube)) |
| Migrator tests | The migrator is a separate Maven project, so the root `mvn clean verify` does not run them. Run `mvn verify` in `migrator/` |

### Troubleshooting the pipeline

| Problem | Check |
|---|---|
| The first job fails in a Testcontainers test | Look for the first exception in the step output. A single failing test names the class and method; fix it locally with `mvn clean verify` |
| The API job fails at "Create the database schema" | The `psql` step stops at the first SQL error and prints it. Run the same script locally against an empty database |
| "Backend did not start in 60 seconds" | The step prints `app.log`. Common causes: a missing table (the schema step failed), or a wrong value in the `env:` block |
| Newman: `Login as admin` returns 401 | `ADMIN_PASSWORD` in `ci.yml` and `adminPassword` in `postman/SmartCity.local.postman_environment.json` must be the same |
| A Postman request fails only in CI | Download `api-test-results` and read `app.log` for the backend's side of the request |
| The end-to-end job fails | Download `e2e-test-results` and open `playwright-report/index.html`: it shows the failing test with a screenshot and a trace. Check `app.log` for the backend's side. The tests need an empty database, which every CI run has |
| `npx playwright install` fails | The step needs the runner's package manager for the browser's system libraries (`--with-deps`). Rerun the job; it is usually a temporary download problem |
| The first run is slow | Dependencies (including TokenSecurity from JitPack) are downloaded. Maven packages are cached for later runs |

## More documentation

- [Software Requirements Specification](<docs/Smart City SRS (v2).md>)

- [Exam assignment description](docs/Exam_Assignment_Description.md)
- [Exam information](docs/Exam_Information.md)
- [Postman collection](postman/SmartCity.postman_collection.json)
- [Playwright tests](frontend/e2e/)
- [Overpass API documentation](https://wiki.openstreetmap.org/wiki/Overpass_API)

## Troubleshooting

| Problem | Check |
|---|---|
| Database connection fails | The Docker containers are running (`docker compose ps` in `docker/`), and the `DB_NAME`, `DB_USERNAME` and `DB_PASSWORD` values in `config.properties` match a database user (see Step 2). |
| Startup fails with `Schema-validation: missing table` | The schema was not loaded into the database. Run `database_schema.sql` as described in Step 2. |
| `port is already allocated` when starting Docker | Another PostgreSQL is using port 5432. Stop it, or run `docker compose down` in `docker/` to remove old containers. |
| Application reports missing properties | Confirm `src/main/resources/config.properties` exists and contains all database and JWT keys shown above. |
| Existing data disappeared | The database was recreated: `docker/data/` was deleted or the schema script was loaded again. Take a backup first with `scripts/backup.ps1` (see [`docker/README.md`](docker/README.md)). |
| `mvn verify` cannot start database tests | Start Docker Desktop and make sure Testcontainers can access its Docker engine. |
| A protected request returns 401 | Log in and send the returned JWT in the `Authorization: Bearer ...` header. |
| An admin seed request returns 403 | The `admin` user does not exist or you are not logged in as admin. Set `ADMIN_PASSWORD` in `config.properties`, restart the backend, then log in as `admin`. |
| `mvn` is not recognized in PowerShell | Maven is not on your PATH. Run the goals from IntelliJ's Maven tool window (Execute Maven Goal), or install Maven. |
| Sonar scanner says `No plugin found for prefix 'sonar'` | Use the full plugin name `org.sonarsource.scanner.maven:sonar-maven-plugin:5.9.0.7382:sonar`. |
| Sonar scanner fails to extract the JRE or says Java 17 is not supported | The scanner needs Java 21. Delete `~/.sonar/cache` and run it again so it downloads its own runtime. |
| Sonar warns about missing blame information | Commit your changes and run the analysis again. |
| `npm run e2e` fails with "Executable doesn't exist" | Run `npx playwright install chromium` in `frontend/`. |
| Frontend pages show "Cannot reach the server" or a 500 error | The backend is not running on port 7070, or `VITE_API_TARGET` points elsewhere. |
| Port 5173 is already in use | Stop the other Vite process; the dev server is configured with `strictPort`. |
| Playwright tests time out waiting for the page | Check that the backend is up and that the Docker database is running with the schema loaded. |
| Overpass request fails or times out | The public service may be busy or rate-limiting requests. Retry later, or configure another compatible endpoint with `OVERPASS_URL`. |

