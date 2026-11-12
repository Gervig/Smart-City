# Software Quality: Exam Project (Second Mandatory Assignment)

*Afleveringsopgave. Copied from the school's website.*

The Second Mandatory Assignment constitutes the Exam Project.

The Exam Project is developed in **groups of 4 or 5 students**. If major circumstances prevent you from working in a group, request a dispensation from Head of Education Marc Kluge (klu@ek.dk). Write your names in *Groups – Exam Project.xlsx*.

Each group performs diverse forms of testing and produces several deliverables on an application of their choice (built specifically for this project, a repurposed app, or even someone else's code). The topic, business idea, specifications, type of application (web, mobile, standalone), programming language(s), DBMSs, test tools, continuous integration tools and IDEs are decided by the students.

## The application must include

- A frontend
- A backend the frontend connects to via an API
- A database
- A connection to an **external, public API** relevant to the application
- Sign up, login and logout functionality
- Business logic that allows for testing

Feel free to reuse and/or adapt the Databaser project or any other project.

If you reuse an existing project:

- Only one database is necessary. Consider removing extra databases.
- A simple login approach is recommended (e.g. username and password), since handling auth tokens in tests might be complicated.

## Task list

### 1. Review

- One member of the group writes an **SRS** (Software Requirements Specification) in the format chosen by the students. It can be a traditional verbose SRS, a list of user stories, or a brief description plus a list of functional and non-functional requirements. Choose the format with in mind that it is the basis for both the review and the acceptance tests.
- The remaining members conduct a **formal review** and write a **formal review report**.

**Deliverables**

- The SRS (PDF file)
- The review report (PDF file)

### 2. Risk assessment

**Deliverables** (both in a single PDF file)

- **Risk table(s).** Either one table that includes the follow-ups to the risks throughout development (consider dividing it into scope-based tables if it becomes too big), or several tables at different stages of development (then at least an initial table, one created mid-development, and a final one).
- **Risk matrices.** Several, illustrating the state of the risks at several phases of the project.

### 3. Black-box test design

Black-box techniques help identify test cases.

**Deliverables** (all in a single PDF file)

- Equivalence partitioning analysis, including the resulting test cases
- Boundary value analysis, including the resulting test cases

### 4. Static testing tools and white-box test design

Use the static testing tool(s) of your choice, besides linters and IDE extensions (e.g. SonarQube), and document their contribution. Include coverage calculation.

**Deliverables** (all in a single PDF file)

- Static testing tool(s) reports/screenshots and a brief explanation of results
- Coverage report and a brief explanation of how it helped design unit tests

### 5. Unit testing and integration testing

Design and implement:

- Unit tests
- Integration tests in code (unless you decide to mock all dependencies)

Pay attention to the following:

- Comprehensive assertions and test cases are expected.
- The test cases identified in the black-box design phase must be present in the unit tests. Ensure **bidirectional traceability**.
- Use parameterised tests/data providers.

Reflect on:

- Your approach to test design (Classical, London, neither, both, a combination)
- What to mock and what not to mock
- How to test against the database

**Deliverables**

- Source code of the application
- Source code of the unit tests and integration tests

### 6. Continuous testing

Create an online code repository and a continuous integration job or pipeline that checks for potential integration errors. The CI job must automate the execution of the unit tests, integration tests in code, and API tests.

**Deliverables**

- CI job script(s) (YAML file or similar)
- Output of the integration project (if the CI tool does not generate a file, copy the output or take a screenshot and paste it into a PDF file)

### 7. API testing

Use the tool of your choice (e.g. Postman, Thunder Client, Insomnia, Playwright) to design and implement test scripts on your **internal** API.

- Test all your endpoints for different elements (e.g. HTTP status codes, execution time, JSON content).
- Positive and negative tests are expected.

**Deliverables**

- Collection and environment in JSON format, or a similar output

### 8. End-to-end UI testing

Design and implement a series of end-to-end tests using a UI automation tool (e.g. Selenium WebDriver, Cypress, Playwright).

- The tests must be **in code**. Scripting tools like Selenium IDE are **forbidden**.

**Deliverables**

- Source code of the end-to-end tests

### 9. Stress performance testing

Implement a **stress test, load tests and a spike test** on the application with a stress performance testing application (e.g. Apache JMeter, k6, Locust).

**Deliverables**

- Reports or screenshots documenting the tests

### 10. Acceptance testing

Design and document (but **do not run**) an acceptance test based on the SRS.

**Deliverables**

- A PDF file containing the acceptance test document that would be distributed to the application users and that they would subsequently fill in when running the test

## Project delivery

The final project is structured in folders, **one for each task**. It is delivered to this Itslearning assignment as a **single zip file** per group.

**Do not include links** (e.g. to GitHub repositories).

**Deadline: 13 December 2026, 23:59**
