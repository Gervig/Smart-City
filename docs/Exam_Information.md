# Software Quality: Exam Information

*Copied from the school's website.*

## Description

The exam is **physical, individual, oral, and internally censored**.

Each student connects their laptop to the classroom's projector. It is recommended to make sure that all relevant files and applications are open and ready before the exam starts (e.g. IDE, browser, API testing tool, CI jobs, stress performance testing tool, PDF files). It is also recommended to check the state of the services right before the exam (e.g. API, CI jobs).

The schedule is available on Wiseflow. According to EK regulations, students can be required to take the exam up to one hour before their scheduled time. **Be ready at least one hour in advance.**

Special exam requests (specific timeslots, online examination) must be sent to eksamen@ek.dk.

## Format

Each exam takes a maximum of 30 minutes:

| Part | Time |
|---|---|
| The student presents their Exam Project | max. 10 min |
| The examiners ask questions | max. 15 min |
| Examiners grade the exam and communicate the grade | 5 min |

(The exam itself is max. 25 minutes: 10 + 15.)

## Materials

- Only those submitted as the Exam Project.
- **No slides, PowerPoint files or similar presentation media are allowed.**

## Questions

### Related to the Exam Project

These are *examples* of potential questions.

**Descriptive**

- What criteria did you use to assign roles for your specification review?
- How did you apply the boundary values technique to the design of test cases?
- Track your unit tests to the test cases found in the black-box design phase, or vice versa.
- Explain how you organised your risk table.
- Explain the follow-up to a specific risk.
- Explain how you documented your acceptance test.
- Explain how you organised the issues found in the review report.
- Did your coverage tool calculate statement coverage or decision coverage?
- Explain what approach to unit testing you used.
- Discuss your use of the AAA pattern for your unit tests.
- Show your use of parameterised tests/data providers.
- Show how you used test doubles or, if you did not, explain why.
- Explain how you used setup/teardown in your tests.
- Explain how you tested the database.
- Explain how you tested the external API.
- Showcase negative API tests.
- Explain the design of your internal API tests.
- Explain the design of your end-to-end tests.
- Explain the design of the stress performance tests.

**Practical**

- Run the unit tests.
- Change the code of the application so that a unit test fails.
- Run the continuous integration job.
- Push to the repository so that the CI job runs automatically.
- Make the CI job fail.
- Run the API tests.
- Make an API test fail.
- Run specific end-to-end UI tests.
- Edit an end-to-end UI test.
- Run a stress performance test.

### Related to the subject contents

These are *actual* questions (not just examples). Each student is asked **3 or 4 of them, randomly chosen, at the end of the exam**. Follow-up questions may be asked.

**Introduction to Software Testing**

- What are the differences between testing and debugging?
- What is static testing and what is dynamic testing?
- What is verification and what is validation?
- What is regression testing?
- Explain at least four general testing principles.

**The V-model and testing in Agile**

- Describe the V-model.
- According to the ISTQB certification (as expressed in the slides), what parts of the V-model correspond to verification and which ones to validation?
- Explain what types of tests fall normally under system testing.
- Explain the test pyramid and discuss its applicability.
- Explain the difficulties of performing acceptance testing in a Scrum setting.

**Test Management**

- What is test monitoring and control?
- What is incident management?
- What is configuration management?

**White Box Test Design**

- Explain the pros and cons of coverage as a test progress measuring indicator.

**Unit Testing**

- Discuss whether private methods must be unit-tested or not.
- Discuss when a unit test can be considered an integration test.
- What are the advantages and disadvantages of the classical approach to unit testing (isolating the unit tests, using test doubles only for shared dependencies)?
- What are the advantages and disadvantages of the London approach to unit testing (isolating the units of code, using test doubles for all but immutable dependencies)?
- Discuss whether mocking should be used for external (unmanaged) dependencies.

**Integration Testing**

- Explain the difference between narrow and broad integration tests.
- Discuss potential issues when writing tests that involve a database.
- List different possibilities when testing against a database.

**Acceptance Testing**

- What is an OAT (Operational Acceptance Testing)?
