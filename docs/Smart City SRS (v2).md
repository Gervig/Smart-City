# **Smart City SRS (v2)**

Nov 12, 2026 · @Casper

## **1\. Introduction**

This document defines what the Smart City application shall do, so the team can review it, assess its risks, design tests from it and write the acceptance test.

### **1.1 Purpose**

The SRS defines the functional and non-functional requirements of Smart City. It is the reference for the development team, the reviewers and the examiners, and the basis for requirement reviews, risk assessment, test design and acceptance testing.

### **1.2 Scope**

Smart City is an application that simulates a persistent fictional city based on real data, with citizens, housing, employment, institutions, companies and relationships between citizens.

**In scope for this SRS and the Software Quality exam:**

* Citizen management: listing, pagination, filtering and sorting.  
* Citizen information: age, date of birth, date of death and relationships.  
* Housing management and assignment of citizens to housing units.  
* Employment management, including salary history.  
* Institutions, companies and other locations where citizens can work.  
* User registration, login and logout.  
* Role-based access control for administrators and normal users.  
* Integration with the external Overpass API to retrieve geographical data.  
* A frontend that communicates with the backend through a REST API.

**Out of scope:**

* Advanced frontend styling and visual design.  
* MongoDB and Neo4j adapters, and the one-time migrator.

### **1.3 Definitions**

| Term | Meaning |
| :---- | :---- |
| SRS | Software Requirements Specification, this document |
| FR | Functional requirement |
| NFR | Non-functional requirement |
| Citizen | A simulated person in the city |
| Housing unit | A home that citizens can live in |
| Administrator | A user with the role admin, allowed to change city data |
| Normal user | A registered user with the role user, allowed to view city data |
| JWT | JSON Web Token, issued at login and sent with each request to prove who the user is |
| Overpass API | A public API that retrieves geographical data from OpenStreetMap |
| CI/CD | Continuous Integration / Continuous Delivery |
| E2E | End-to-end tests that drive the user interface, written in code with Playwright |

### **1.4 References**

* README.md in the Smart City repository  
* [Overpass API documentation](https://wiki.openstreetmap.org/wiki/Overpass_API)  
* Software Quality exam assignment description

### **1.5 Conventions**

* "Shall" marks a mandatory requirement. Each requirement has one ID that never changes after the review.  
* Priority uses Critical, High, Medium or Low, the same scale as the risk analysis and the acceptance tests.  
* The Source column uses three values: **Assignment** (required by the exam assignment), **Team input** (decided by the team) and **Proposed** (suggested while restructuring this document; the team must confirm or reject it).

## **2\. Overall description**

Smart City is a Java backend with a React frontend that shows a simulated city and enriches its locations with data from the public Overpass API.

### **2.1 Product perspective**

The backend uses Java, Javalin and Hibernate with a PostgreSQL database and serves a REST API. It calls the external Overpass API to enrich location data. The data is presented in a frontend built with JavaScript and React.

### **2.2 Product functions**

* Register, log in and log out (section 4.1)  
* Manage and view citizens and their relationships (4.2)  
* Assign citizens to housing and view who lives where (4.3)  
* Manage employment and salary history (4.4)  
* Enrich locations with nearby places from Overpass (4.5)

### **2.3 User classes**

| User class | Description | Permissions |
| :---- | :---- | :---- |
| Visitor | A person who is not logged in | Register and log in only |
| Normal user | Uses the application to view information about the fictional city | Read-only access to city data |
| Administrator | Manages citizens, housing, employment and other city-related data | Administrative operations: create, update and manage application data |

### **2.4 Operating environment**

* The backend and database run in Docker containers; credentials are passed as environment variables.  
* The frontend runs in a modern web browser and is tested with Playwright.  
* The source code is hosted on GitHub and tested with GitHub Actions.

### **2.5 Design and implementation constraints**

* Backend stack: Java, Javalin, Hibernate and PostgreSQL.  
* Frontend stack: JavaScript, React, Vite, HTML and CSS.  
* The exam assignment requires a frontend, a backend with a REST API, a database, a public external API, sign up, login and logout, and business logic that allows for testing.  
* Tests must be written in code. Unit tests are designed with black-box techniques (equivalence partitioning and boundary values) and use parameterised tests. Database tests use a test database in a Docker container. E2E tests use Playwright.  
* Hand-in deadline: see open issue OI-01 (7 or 13 December 2026).

### **2.6 Assumptions and dependencies**

| Type | Description |
| :---- | :---- |
| Dependency | The Overpass API is used to retrieve external data. |
| Assumption | Overpass returns data in the documented format and is available within its fair-use limits. |
| Dependency | The source code is hosted on GitHub and the CI/CD pipeline runs on GitHub Actions. |
| Assumption | GitHub and GitHub Actions are available when the pipeline runs. |

## **3\. External interface requirements**

Smart City has one user interface and three software interfaces: the REST API, the PostgreSQL database and the Overpass API. Test and development tools (Postman, Playwright, the stress test tool, pgAdmin) are not interfaces of the system and are described in section 2.5.

### **3.1 User interface**

The React frontend gives users these pages: login, register, citizen list, citizen detail and housing detail. Errors from the backend are shown to the user as readable messages. Styling is out of scope.

### **3.2 Software interfaces**

| Interface | Direction | What is exchanged | Failure behaviour |
| :---- | :---- | :---- | :---- |
| REST API | Frontend and API clients call the backend | Endpoints, JSON, JWT in the Authorization header (Bearer) | Error codes and readable messages: 400 invalid input, 401 not authenticated, 403 not allowed, 404 not found |
| PostgreSQL | Backend to database | Users, roles, citizens, housing, companies and institutions, employment, relationships (entity list to be confirmed, see OI-08) | If the database is unreachable the request fails with a clear error (HTTP 503\) and no partial data is saved |
| Overpass API | Backend calls it, read-only | A query by location coordinates; a list of nearby places (name, type, coordinates) in return | On timeout, rate limit (HTTP 429), server error (5xx) or malformed data, the backend returns the location without enrichment and a message that enrichment is unavailable (FR-19) |

Access to the database, including pgAdmin, is restricted to users with credentials. Credentials are passed as secret environment variables and are not stored in the source code (NFR-03).

## **4\. Functional requirements**

The system has 19 functional requirements in five groups. Each states one testable behaviour with an acceptance criterion. Rows with source **Proposed** were added while restructuring this document and need the team's confirmation.

### **4.1 Authentication and roles**

| ID | Requirement | Priority | Acceptance criterion | Source |
| :---- | :---- | :---- | :---- | :---- |
| FR-01 | The system shall let a visitor register with a unique username and a password. A new account gets the role user. | Critical | A valid registration returns 201 and the user can log in. A username that already exists is rejected with 409\. A password shorter than 8 characters is rejected with 400 and nothing is saved. | Assignment, Proposed |
| FR-02 | The system shall authenticate users with a username and password. | Critical | A correct login returns 200 and a JWT. A wrong password or an unknown username returns 401\. | Team input |
| FR-03 | The system shall let a logged-in user log out. | High | After logout the frontend no longer holds the token, and protected pages redirect to the login page. | Assignment, Proposed |
| FR-04 | The system shall reject requests to protected endpoints that have no valid token. | Critical | A request without a token, with a malformed Authorization header, or with an expired or tampered token returns 401, never 500\. | Proposed |
| FR-05 | The system shall allow only administrators to perform administrator actions. | Critical | A normal user calling an administrator endpoint receives 403\. | Team input |

### **4.2 Citizens**

| ID | Requirement | Priority | Acceptance criterion | Source |
| :---- | :---- | :---- | :---- | :---- |
| FR-06 | The system shall list citizens with pagination, filtering and sorting. | High | A page size above the maximum is rejected with HTTP 400\. Filtered results contain only matching citizens. Sorting returns the citizens in the requested order. | Team input |
| FR-07 | The system shall show one citizen's details. | High | An existing ID returns 200 with the citizen. An unknown ID returns 404\. | Team input |
| FR-08 | The system shall let an administrator create a citizen. | High | Required fields are a name and a date of birth. A date of birth in the future, or a date of death before the date of birth, is rejected with 400 and nothing is saved. | Team input, Proposed |
| FR-09 | The system shall let an administrator update a citizen. | High | The same validation as FR-08 applies. An unknown ID returns 404\. | Team input |
| FR-10 | The system shall let an administrator retire a citizen while keeping their birth and death history. | Medium | After retiring, the citizen is still retrievable with a date of death. | Team input |
| FR-11 | The system shall show a citizen's age, calculated from the date of birth (or up to the date of death). | Medium | A citizen born exactly 18 years ago today is shown as 18\. A citizen with a date of death shows the age at death. | Proposed |
| FR-12 | The system shall show a citizen's relations, each linking to the other citizen's detail page. | Low | Clicking a relation opens the other citizen's page. The URL contains the citizen's ID. | Team input |

### **4.3 Housing and locations**

| ID | Requirement | Priority | Acceptance criterion | Source |
| :---- | :---- | :---- | :---- | :---- |
| FR-13 | The system shall assign a housing unit to a citizen. | Critical | The citizen's housing changes to the unit. Assigning an occupied unit is rejected (see OI-06). | Team input, Proposed |
| FR-14 | The system shall show who lives in a housing unit. | High | The page lists exactly the assigned citizens. If nobody lives there it shows "No citizens for this housing". | Team input |
| FR-15 | The system shall list companies and institutions, the locations where citizens can work. | Medium | The list contains all companies and institutions; an unknown ID returns 404\. | Team input, Proposed |

### **4.4 Employment**

| ID | Requirement | Priority | Acceptance criterion | Source |
| :---- | :---- | :---- | :---- | :---- |
| FR-16 | The system shall create employment for a citizen at a company or institution with a start date and a salary. | High | The job is saved with its start date and salary. An invalid start date is rejected with 400\. A salary of 0 or less is rejected with 400\. | Team input, Proposed |
| FR-17 | The system shall end an employment and keep the salary history. | High | After ending, earlier salary records are still retrievable. | Team input |

### **4.5 External data from Overpass**

| ID | Requirement | Priority | Acceptance criterion | Source |
| :---- | :---- | :---- | :---- | :---- |
| FR-18 | The system shall enrich a location with nearby places from the Overpass API. | High | For a location with coordinates, the response contains the nearby places (name, type, coordinates) within a configured radius, parsed and transformed from the Overpass response (see OI-02). | Team input, Proposed |
| FR-19 | The system shall keep working when Overpass is unavailable. | High | On timeout (after 10 seconds), HTTP 429, a 5xx error or malformed data, the location is returned without enrichment together with a message that enrichment is unavailable. Nothing is saved from the failed call. | Team input, Proposed |

## **5\. Non-functional requirements**

Eight non-functional requirements cover security, performance, reliability, maintainability and testability. Each has a measurable criterion; the numbers in NFR-04 and NFR-06 are proposals (see OI-05).

| ID | Category | Requirement | Acceptance criterion | Source |
| :---- | :---- | :---- | :---- | :---- |
| NFR-01 | Security | Passwords shall be stored hashed, never in plain text. | The users table contains no plain-text passwords. | Team input |
| NFR-02 | Security | The system shall be protected against SQL injection. | An injection string in a filter or login field returns no extra data and no server error. The ORM uses prepared statements. | Team input |
| NFR-03 | Security | Database credentials shall not be stored in the source code, and users without access shall not reach the database or pgAdmin. | Credentials are read from environment variables. The repository contains no credentials. | Team input |
| NFR-04 | Performance | A list request shall be fast under load. | A citizen list request returns within 500 ms with 100 citizens at 50 concurrent users, shown by the stress test. | Team input, Proposed |
| NFR-05 | Reliability | When the database is unavailable, the system shall fail clearly and keep the data consistent. | The response is HTTP 503 with a clear message. Failed requests are rolled back and no partial data is saved. | Team input |
| NFR-06 | Reliability | The system shall respect the Overpass fair-use limits. | Identical Overpass queries within 24 hours are answered from a cache. Automated tests never call the live Overpass API in the default build. | Team input, Proposed |
| NFR-07 | Maintainability | The backend shall be layered: controllers handle HTTP, data access sits in separate DAO/repository classes. | No controller class accesses the database directly. This is checked in code review and with the static analysis tool. | Team input |
| NFR-08 | Testability | Every push to the repository shall automatically run the unit, integration and API tests. | A push with a deliberately failing test produces a failed GitHub Actions run. A push with passing tests produces a green run. | Proposed |

## **6\. Traceability matrix**

Every requirement has one row. An empty cell is a gap the examiners can ask about. Fill the columns as the work progresses, and keep the IDs identical to sections 4 and 5\. Only the tests that already exist are filled in.

| Requirement | Risk ID | Black-box test cases | Unit / integration tests | API test | E2E test | Acceptance test |
| :---- | :---- | :---- | :---- | :---- | :---- | :---- |
| FR-01 |  |  | UserDAOTest.create, createWithExistingRole, createWithUnknownRole |  |  |  |
| FR-02 |  |  | UserDAOTest.getVerifiedUser, getVerifiedUserWrongPassword, getVerifiedUserUnknownUser; UserTest.verifyPassword |  |  |  |
| FR-03 |  |  |  |  |  |  |
| FR-04 |  |  |  |  |  |  |
| FR-05 |  |  |  |  |  |  |
| FR-06 |  |  |  |  |  |  |
| FR-07 |  |  |  |  |  |  |
| FR-08 |  |  |  |  |  |  |
| FR-09 |  |  |  |  |  |  |
| FR-10 |  |  |  |  |  |  |
| FR-11 |  |  |  |  |  |  |
| FR-12 |  |  |  |  |  |  |
| FR-13 |  |  |  |  |  |  |
| FR-14 |  |  |  |  |  |  |
| FR-15 |  |  |  |  |  |  |
| FR-16 |  |  |  |  |  |  |
| FR-17 |  |  |  |  |  |  |
| FR-18 |  |  |  |  |  |  |
| FR-19 |  |  |  |  |  |  |
| NFR-01 |  |  |   |  |  |  |
| NFR-02 |  |  |  |  |  |  |
| NFR-03 |  |  |  |  |  |  |
| NFR-04 |  |  |  |  |  |  |
| NFR-05 |  |  |  |  |  |  |
| NFR-06 |  |  |  |  |  |  |
| NFR-07 |  |  |  |  |  |  |
| NFR-08 |  |  |  |  |  |  |

## **7\. Open issues**

Thirteen points are open: nine questions about the specification and four tool decisions. The specification review uses the first group, and the review report records who found each issue and how it was resolved.

| \# | Type | Issue | Raised by | Status |
| :---- | :---- | :---- | :---- | :---- |
| OI-01 | Specification | The hand-in deadline is 7 December 2026 in this SRS, but the assignment text says 13 December 2026, 23:59. Which is correct? | SRS review | Open |
| OI-02 | Specification | What exactly does the Overpass enrichment return (which place types, which radius) and where does the user see it? (FR-18) | SRS review | Open |
| OI-03 | Specification | What are the password rules? The minimum of 8 characters in FR-01 is a proposal. | SRS review | Open |
| OI-04 | Specification | Is logout only done in the frontend, or does the backend also invalidate the token? (FR-03) | SRS review | Open |
| OI-05 | Specification | Are 500 ms, 100 citizens and 50 concurrent users the right numbers for NFR-04? | SRS review | Open |
| OI-06 | Specification | Can a housing unit hold only one citizen, or several? This decides what "occupied" means in FR-13. | SRS review | Open |
| OI-07 | Specification | Does "retire" a citizen (FR-10) keep the record, and can an administrator delete one for good? | SRS review | Open |
| OI-08 | Specification | Which entities are in the database and in the tested slice? | SRS review | Open |
| OI-09 | Specification | Do E2E tests cover only happy paths? Is that enough for the assignment? | Team | Open |
| OI-10 | Decision | Decide on the stress performance tool (Apache JMeter, k6 or Locust). | Casper | Open |
| OI-11 | Decision | Decide whether to use an alternative to Swagger, for example Scalar. | Lasse | Open |
| OI-12 | Decision | Decide on the static testing tool and white-box test design (for example SonarQube). | Casper | Open |
| OI-13 | Decision | Decide on the testing approach (London, classical or both). | Christoffer | Open |

