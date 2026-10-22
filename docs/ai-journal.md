# AI Journal

How this project was built with an AI assistant (Claude Code). The developer gave the assistant the requirements
one subtask at a time; this journal records the prompts, what the assistant produced, what survived review, and why.
Where a decision was made by the developer, it says so; where it was the assistant's call, that is stated too.

## Prompt Used

All 26 developer prompts of the build session (16 Oct 2026) are listed below in the order they were sent. The
wording is reproduced **verbatim from the Claude Code session transcript**, so spelling mistakes and informal
phrasing are kept as typed. Only three things were changed: the markers the tool adds around pasted text were
removed, surplus blank lines were trimmed, and the long terminal output pasted in prompts 25 and 26 is shortened
(marked in place). The screenshot attached to prompt 21 is not reproduced. Times are IST.

### Prompt index

Several prompts were first drafts that the developer later replaced with a stricter version (for example the
entities, repositories and APIs were each specified twice). Both versions are kept because the second one
explains many of the entries under *Modified* below.

| Prompts | Task | Result |
|---|---|---|
| 1–4 | Create the Spring Boot 3 / Java 21 / Maven project with the listed dependencies; install the packages; do not commit | `pom.xml`, `application.yml`, main class, packages |
| 5–6 | JPA entities `Claim`, `ClaimNote`, `ClaimStatus` (first version) | entities, auditing, relationships |
| 7 | Spring Data repositories (first version) | `ClaimRepository`, `ClaimNoteRepository` |
| 8 | Claims CRUD service and REST APIs (first version) | DTOs, service, controller, global exception handling |
| 9 | Project background and requirements (read only) | gap list against the first version |
| 10–11 | Re-validate task 1 against the stricter spec, then apply the changes | package structure, Swagger, service interfaces |
| 12 | Entity design with validation and `LocalDateTime` | enum and entity rewrite |
| 13 | Repository aggregation queries | open count, exposure, by status, by officer |
| 14 | Claim APIs with `CreateClaimRequest` / `UpdateClaimRequest`, response envelope | rewritten CRUD API |
| 15 | State transition workflow (the developer's "task 5") | `ClaimTransitionValidator`, lifecycle endpoints |
| 16 | Officer operations: assign and notes (the developer's "task 6") | assignment and notes services/controller |
| 17 | Manager dashboard (the developer's "task 7") | `GET /api/dashboard/workload` |
| 18 | Kafka events, tests, documentation | five topics and events, integration test, these docs |
| 19–20 | Check what is pending, then build it | claimant additional-info endpoint, officer personal workload endpoint |
| 21–22 | Check against the SDLC guidelines (screenshot); keep the README and AI files in `docs/` | `docs/` folder with an index in `docs/README.md` |
| 23–26 | Housekeeping: virtual-env question, push readiness, git line-ending warnings and status | questions, not build tasks |

### Verbatim prompts

#### Prompt 1 - Task 1 - project setup

*16 Oct 2026, 14:58 IST*

```text
Create a production-ready Spring Boot 3 application using Java 21 and Maven for a claims management platform.

Requirements:
- Layered architecture
- Packages:
  controller
  service
  repository
  entity
  dto
  mapper
  exception
  config

Dependencies:
- Spring Web
- Spring Data JPA
- H2 Database
- Validation
- Lombok
- Spring Kafka
- Spring Boot Actuator

Create:
- pom.xml
- application.yml
- main application class

Configure:
- H2 console
- JPA auto create-update
- Logging

Provide complete folder structure and code.
```

#### Prompt 2 - Follow-up (queued while task 1 ran) - install the packages

*16 Oct 2026, 15:00 IST*

```text
I have not installed any pacakages in the project please install the necessary pacakages that is required
```

#### Prompt 3 - Status check

*16 Oct 2026, 15:07 IST*

```text
task one is completed ?
```

#### Prompt 4 - Instruction - do not commit

*16 Oct 2026, 15:09 IST*

```text
dont commit any chnages just do the changes or implement the instaill steup which i have mentioned
```

#### Prompt 5 - Lead-in to the entities task

*16 Oct 2026, 15:09 IST*

```text
now since we ahve setup the instail setup we will impelment this task 2:
```

#### Prompt 6 - JPA entities (first version)

*16 Oct 2026, 15:10 IST*

```text
task 2:

Create JPA entities for a claims management system.

Entities:

Claim
- id
- claimNumber
- claimantName
- claimantEmail
- policyNumber
- description
- incidentDate
- claimAmount
- liabilityExposure
- status
- assignedOfficer
- createdAt
- updatedAt

ClaimNote
- id
- claimId
- note
- createdAt

Create:
- Entity classes
- Enum ClaimStatus
- JPA annotations
- Relationships

Use Lombok.
```

#### Prompt 7 - Repositories (first version)

*16 Oct 2026, 15:12 IST*

```text
if the task 1 and 2 is completed please implemnt this task 3:

Create Spring Data JPA repositories.

Requirements:

ClaimRepository:
- findByStatus
- findByAssignedOfficer
- findByClaimNumber

ClaimNoteRepository

Return complete code.
```

#### Prompt 8 - Claims service and REST APIs (first version)

*16 Oct 2026, 15:14 IST*

```text
once you completed at the edn of the chat mentioend i have compelted the task 3 or whatever number is the first three tasks is comeplted please implement this

Create service and REST APIs for Claims.

Endpoints:

POST /api/claims
GET /api/claims/{id}
GET /api/claims
PUT /api/claims/{id}
DELETE /api/claims/{id}

Requirements:
- DTOs
- Validation
- Global exception handling
- Clean architecture

Return full code.
```

#### Prompt 9 - Project requirements (context, read only)

*16 Oct 2026, 15:18 IST*

```text
before that jjust to tell you the context this is the over all project requiremnts that we need to impleement for that i ahve devided the subtask now just read this dont implement it anything once you read this requirements i will tell you to do the next subtask afetr the apis which we ahve cerated :

PROJECT: CHUBB APAC CLAIMS MANAGEMENT BACKEND

BACKGROUND
----------
Chubb APAC processes motor and property claims across six markets.

Current Problems:
- Claimants submit claims through phone or email.
- Claimants have no visibility into claim status.
- Claims staff manage work through shared inboxes and spreadsheets.
- No consolidated workload view.
- Managers have no real-time visibility into outstanding liability exposure.
- Frontend is out of scope.
- Need to design and build the backend platform.

BUSINESS REQUIREMENTS
---------------------

Claimants should be able to:
1. Report an incident.
2. Create a claim.
3. Track claim status.
4. Submit additional information when requested.
5. Receive claim decisions.

Claims Officers should be able to:
1. View incoming claims.
2. Assign claims.
3. Review claims.
4. Request additional information.
5. Approve claims.
6. Reject claims.
7. Settle approved claims.
8. View personal workload.

Managers should be able to:
1. View team workload.
2. View claims by status.
3. View total liability exposure.
4. Monitor claim processing progress.

TECHNOLOGY DECISIONS
--------------------

Backend:
- Java 21
- Spring Boot 3

Database:
- H2 (for assessment)
- Can explain PostgreSQL for production

Communication:

REST APIs:
- Create claim
- Get claim
- Update claim
- Assign claim
- Dashboard APIs

Kafka Events:
- ClaimCreated
- ClaimAssigned
- ClaimApproved
- ClaimRejected
- ClaimSettled

Reason:
REST = synchronous operations
Kafka = asynchronous notifications and integrations

ARCHITECTURE
------------

Single Spring Boot Application

Package Structure:

com.chubb.claims

├── controller
├── service
├── repository
├── entity
├── dto
├── mapper
├── exception
├── config
├── kafka
└── dashboard

CORE DOMAIN MODEL
-----------------

Claim
-----
id
claimNumber
claimantName
claimantEmail
policyNumber
description
incidentDate
claimAmount
liabilityExposure
status
assignedOfficer
createdAt
updatedAt

ClaimNote
---------
id
claimId
note
createdAt

ClaimStatus Enum
----------------

NEW

UNDER_REVIEW

MORE_INFO_REQUIRED

APPROVED

REJECTED

SETTLED

CLAIM LIFECYCLE
---------------

NEW
 ↓
UNDER_REVIEW
 ↓
MORE_INFO_REQUIRED
 ↓
UNDER_REVIEW
 ↓
APPROVED
 ↓
SETTLED

OR

NEW
 ↓
UNDER_REVIEW
 ↓
REJECTED

Allowed Transitions

NEW → UNDER_REVIEW

UNDER_REVIEW → MORE_INFO_REQUIRED

UNDER_REVIEW → APPROVED

UNDER_REVIEW → REJECTED

APPROVED → SETTLED

Any other transition should throw BusinessException.

DATABASE TABLES
---------------

claims

claim_notes

API ENDPOINTS
-------------

Claim APIs

POST   /api/claims

GET    /api/claims

GET    /api/claims/{id}

PUT    /api/claims/{id}

DELETE /api/claims/{id}

Lifecycle APIs

PUT /api/claims/{id}/assign

PUT /api/claims/{id}/under-review

PUT /api/claims/{id}/request-more-info

PUT /api/claims/{id}/approve

PUT /api/claims/{id}/reject

PUT /api/claims/{id}/settle

Notes APIs

POST /api/claims/{id}/notes

GET  /api/claims/{id}/notes

Dashboard APIs

GET /api/dashboard/workload

Dashboard Response

{
  "totalOpenClaims": 25,
  "totalLiabilityExposure": 1200000,
  "claimsByStatus": {
      "NEW":5,
      "UNDER_REVIEW":10,
```

#### Prompt 10 - Re-validate task 1 against the stricter specification

*16 Oct 2026, 15:22 IST*

```text
before moving forward we will verify the intiall task is comeplted as per the requiremts or not task 1:

You are a Senior Java Architect.

Create a production-ready Spring Boot 3 backend application for a Claims Management System.

Project Name:
Chubb-Backend

Requirements:

Technology:
- Java 21
- Spring Boot 3
- Maven

Dependencies:
- Spring Web
- Spring Data JPA
- Spring Validation
- H2 Database
- Lombok
- Spring Kafka
- Spring Boot Actuator
- Swagger/OpenAPI
- JUnit 5
- Mockito

Architecture:
- Layered Architecture
- Follows SOLID principles

Package Structure:

com.chubb.claims
├── controller
├── service
│   ├── impl
├── repository
├── entity
├── dto
│   ├── request
│   ├── response
├── exception
├── mapper
├── config
├── kafka
├── dashboard
└── util

Tasks:

1. Generate pom.xml
2. Generate application.yml
3. Configure H2 Database
4. Configure Kafka placeholders
5. Configure Swagger
6. Configure Actuator
7. Configure Logging
8. Generate main application class
9. Generate complete folder structure

Output:
- Complete source code
- Complete file structure
- Configuration details

 can you pelase valdiate and confirm me we hace comeplted it or not
```

#### Prompt 11 - Instruction - apply the changes

*16 Oct 2026, 15:23 IST*

```text
please do the nesesscaery changes
```

#### Prompt 12 - Domain model (stricter version of the entities task)

*16 Oct 2026, 15:50 IST*

```text
You are designing the domain model for a Claims Management Platform.

Create JPA entities with proper relationships and annotations.

Entity 1: Claim

Fields:
- id (Long)
- claimNumber (String)
- claimantName (String)
- claimantEmail (String)
- policyNumber (String)
- description (String)
- incidentDate (LocalDate)
- claimAmount (BigDecimal)
- liabilityExposure (BigDecimal)
- status (ClaimStatus)
- assignedOfficer (String)
- createdAt (LocalDateTime)
- updatedAt (LocalDateTime)

Entity 2: ClaimNote

Fields:
- id (Long)
- claim (ManyToOne)
- note (String)
- createdAt (LocalDateTime)

Create Enum:

ClaimStatus

Values:
- NEW
- UNDER_REVIEW
- MORE_INFO_REQUIRED
- APPROVED
- REJECTED
- SETTLED

Requirements:
- Use Lombok
- Use JPA annotations
- Use auditing for createdAt and updatedAt
- Add validation annotations
- Explain relationship design
- Generate complete code

 now this task 2
```

#### Prompt 13 - Repository layer and aggregation queries

*16 Oct 2026, 16:35 IST*

```text
Generate repository layer for the Claims Management application.

Create:

1. ClaimRepository
2. ClaimNoteRepository

ClaimRepository should support:

- findByStatus()
- findByAssignedOfficer()
- findByClaimNumber()
- findByClaimantEmail()

Custom Queries:

1. Open Claims Count
2. Total Liability Exposure
3. Claims By Status
4. Claims Count By Officer

Use:

- Spring Data JPA
- JPQL where required

Generate:

- Repository interfaces
- Aggregation queries
- Explanation of each query

 now can you check this
```

#### Prompt 14 - Claim management APIs

*16 Oct 2026, 16:37 IST*

```text
after this can you please check this :

Implement Claim Management APIs.

Generate DTOs, Service Layer, Controller Layer and Mapper Layer.

DTOs:

CreateClaimRequest
UpdateClaimRequest
ClaimResponse

API Endpoints:

POST /api/claims

GET /api/claims

GET /api/claims/{id}

GET /api/claims/claim-number/{claimNumber}

PUT /api/claims/{id}

DELETE /api/claims/{id}

Requirements:

For Create Claim:
- Auto generate claim number
- Initial status should be NEW

Validation:

- claimantName mandatory
- email validation
- claimAmount > 0
- liabilityExposure > 0

Response Structure:

{
  "success": true,
  "message": "...",
  "data": {}
}

Implement:

- Controller
- Service Interface
- Service Implementation
- Mapper
- DTOs

Generate complete code.
```

#### Prompt 15 - State transition workflow ("task 5")

*16 Oct 2026, 16:39 IST*

```text
now can you please implement this:

This is the most important business requirement.

Implement claim state transition workflow.

States:

NEW

UNDER_REVIEW

MORE_INFO_REQUIRED

APPROVED

REJECTED

SETTLED

Valid Transitions:

NEW → UNDER_REVIEW

UNDER_REVIEW → MORE_INFO_REQUIRED

UNDER_REVIEW → APPROVED

UNDER_REVIEW → REJECTED

MORE_INFO_REQUIRED → UNDER_REVIEW

APPROVED → SETTLED

Invalid transitions must throw BusinessException.

Create:

Service Methods:
- moveToReview()
- requestMoreInfo()
- approveClaim()
- rejectClaim()
- settleClaim()

REST Endpoints:

PUT /api/claims/{id}/under-review

PUT /api/claims/{id}/request-more-info

PUT /api/claims/{id}/approve

PUT /api/claims/{id}/reject

PUT /api/claims/{id}/settle

Requirements:
- Centralized transition validation
- Reusable business rules
- Proper exception handling

Generate complete implementation and design explanation.

 if this task is not implemented it this si the task 5
```

#### Prompt 16 - Claims officer operations ("task 6")

*16 Oct 2026, 16:42 IST*

```text
Implement Claims Officer operations.

Requirements:

Claim Assignment

Endpoint:
PUT /api/claims/{id}/assign

Request:
{
  "officer":"John Smith"
}

Updates assignedOfficer.

Claim Notes

Endpoint:
POST /api/claims/{id}/notes

Request:
{
  "note":"Requested repair invoice."
}

View Notes

Endpoint:
GET /api/claims/{id}/notes

Requirements:
- Claim can have multiple notes
- Notes should be stored separately
- Use ClaimNote entity
- Return notes chronologically

Generate:

DTOs
Repositories
Services
Controllers
Implementation

Generate complete code.

 now you can implement this task 6
```

#### Prompt 17 - Manager dashboard ("task 7")

*16 Oct 2026, 16:44 IST*

```text
Implement Manager Dashboard APIs.

Problem Statement:

Managers need visibility into:

- Team workload
- Open claims
- Liability exposure
- Claims status distribution

Create Dashboard API:

GET /api/dashboard/workload

Response:

{
  "totalOpenClaims":15,
  "totalLiabilityExposure":100000,
  "claimsByStatus":{
      "NEW":4,
      "UNDER_REVIEW":5,
      "APPROVED":3
  },
  "claimsPerOfficer":{
      "John":5,
      "Mike":4
  }
}

Requirements:

Open Claims:
- NEW
- UNDER_REVIEW
- MORE_INFO_REQUIRED

Metrics:

1. Open Claims Count
2. Claims By Status
3. Claims Per Officer
4. Total Liability Exposure

Implementation:
- Aggregation queries
- Service Layer
- DTOs
- Controller

Generate complete code.

 now you can implement this task 7
```

#### Prompt 18 - Kafka events, tests and documentation

*16 Oct 2026, 16:47 IST*

```text
Implement event-driven architecture features.

PART 1 - Kafka

Create Kafka Producer.

Publish events:

1. ClaimCreatedEvent
2. ClaimAssignedEvent
3. ClaimApprovedEvent
4. ClaimRejectedEvent
5. ClaimSettledEvent

Topics:

claim-created
claim-assigned
claim-approved
claim-rejected
claim-settled

Generate:

- Event DTOs
- Producer Service
- Kafka Configuration
- Service Integration

PART 2 - Testing

Generate JUnit 5 tests for:

- Create Claim
- Approve Claim
- Reject Claim
- Settle Claim
- Invalid State Transition
- Dashboard Summary

Use:
- Mockito
- MockMvc

PART 3 - Documentation

Generate:

README.md

Include:

- Project Overview
- Architecture
- Design Decisions
- API Endpoints
- Run Instructions
- Database Information
- Kafka Topics

Generate:

architecture.md

Include:

- Domain Model
- Lifecycle Diagram
- Tradeoffs
- Future Enhancements

Generate:

ai-journal.md

Include sections:

- Prompt Used
- AI Response
- Accepted
- Rejected
- Modified
- Reasoning

Output all files.

 now you implemeent this task
```

#### Prompt 19 - Status check - is anything pending?

*16 Oct 2026, 16:56 IST*

```text
now we have implemented the all the 7 subtasks right ? are is there anyhting is pending
```

#### Prompt 20 - Build the pending items

*16 Oct 2026, 16:58 IST*

```text
build the task whichever is pending and needs to build and tell me what is pending or if you need any information
```

#### Prompt 21 - SDLC guidelines check (screenshot attached)

*16 Oct 2026, 17:08 IST*

```text
[Image #14] you can refer the image for the sdlc guidlines that we need to follow i hope we alreday followed the same can you please confirm me on this
[Screenshot attached: 'Screenshot 2026-10-16 170629.png' - the SDLC guidelines. Not reproduced here.]
```

#### Prompt 22 - Where the documentation should live

*16 Oct 2026, 17:09 IST*

```text
yes correct all the readme and ai files which we ahve cerated for the referenec needs to be there int he doc folder
```

#### Prompt 23 - Environment question

*16 Oct 2026, 17:16 IST*

```text
did we installed the virtual env in this project ?
```

#### Prompt 24 - Push readiness

*16 Oct 2026, 17:17 IST*

```text
no if we dont need them right now its fine now i will commit all the changes to git you dont do it just tell me we are good to push the chnages
```

#### Prompt 25 - Git line-ending warnings (terminal output)

*16 Oct 2026, 17:18 IST*

```text
i ma getting this error :

Microsoft Windows [Version 10.0.26100.9457]
(c) Microsoft Corporation. All rights reserved.

C:\Users\hemanthkum\IdeaProjects\Chubb-Backend>git add .
warning: in the working copy of 'README.md', LF will be replaced by CRLF the next time Git touches it
warning: in the working copy of '.gitignore', LF will be replaced by CRLF the next time Git touches it

[... 76 further lines of pasted terminal output omitted from this journal ...]
```

#### Prompt 26 - Git status (terminal output)

*16 Oct 2026, 17:20 IST*

```text
C:\Users\hemanthkum\IdeaProjects\Chubb-Backend>git status
On branch main
Your branch is up to date with 'origin/main'.

Changes to be committed:
  (use "git restore --staged <file>..." to unstage)
        new file:   .gitignore
        modified:   README.md

[... 77 further lines of pasted terminal output omitted from this journal ...]
```

## AI response (what was delivered)

A layered Spring Boot application with CRUD, a validated lifecycle workflow, officer assignment and notes,
a manager dashboard, five Kafka topics, and 82 automated tests (unit, MockMvc, `@DataJpaTest`, and an
end-to-end test with an embedded Kafka broker). Each task was compiled and tested with Maven before being
reported as done. What was **not** done: a manual run against an external Kafka broker, load testing,
security, and anything outside the written requirements.

## Accepted (kept as generated)

- Layering and package structure, interface + `impl` services, records for DTOs, constructor injection.
- Centralised transition rules in one class, used by every status change.
- JPQL aggregation queries returning interface projections.
- Publishing Kafka events only after the database commit, keyed by claim number.
- Optimistic locking with `@Version` on `Claim`.
- Hand-written mappers (the dependency list did not include MapStruct).
- `PageResponse` wrapper for list endpoints so the JSON shape does not depend on Spring's `Page` internals.

## Rejected

- **MapStruct** – not in the required dependency list; explicit mappers are small enough.
- **Lombok `@Data` on entities** – generated `equals/hashCode/toString` interact badly with lazy relations;
  used `@Getter/@Setter/@Builder` and excluded relations from `toString`.
- **A single generic `claim-events` topic** (first version) – replaced by one topic per event as required.
- **A transactional outbox now** – correct for production, but disproportionate for the assessment; documented
  as a future enhancement instead.
- **`PATCH /{id}/status`** (first version) – removed when the requirements defined dedicated lifecycle endpoints.
- **Mocking the Kafka layer only** – a mock cannot find serializer problems, so an embedded-broker test was added.

## Modified (changed after first draft)

| Change | Why |
|---|---|
| Status enum `SUBMITTED/PAID/CLOSED` → `NEW/…/SETTLED` | The requirements were given after the first version; the enum and transition map were rewritten to match. |
| "Open claims" = not rejected/settled → `NEW`, `UNDER_REVIEW`, `MORE_INFO_REQUIRED` | The dashboard requirement defines it explicitly; `APPROVED` is no longer counted as open. |
| `claimId` field on `ClaimNote` → `@ManyToOne claim` | Proper JPA relationship; `claimId` is exposed in the response DTO instead. |
| `Instant` timestamps → `LocalDateTime` | Requested field types. |
| Generic `ClaimRequest` → `CreateClaimRequest` / `UpdateClaimRequest` | Requested DTOs; policy number is immutable after creation. |
| `liabilityExposure` optional → required and `> 0` | Requested validation. |
| Error body `ErrorResponse` → shared `{success, message, data}` envelope | Consistent response structure. |
| `InvalidClaimStateException` → `BusinessException` | Name required by the spec; mapped to HTTP 409. |
| Kafka config: autoconfigured serializer → custom `ProducerFactory` | ISO-8601 dates and no Java type headers. |
| Dashboard returns bare JSON, not the envelope | The requirement shows the exact response shape. |

## Mistakes found and fixed during the work

- **Byte-order marks in Java files.** A scripted bulk rewrite on Windows PowerShell wrote UTF-8 with BOM and
  broke compilation; fixed by stripping the BOM and writing files without it afterwards.
- **Auditing missing in the repository test slice.** `@DataJpaTest` did not load `JpaConfig`, so `createdAt`
  was null; fixed with `@Import(JpaConfig.class)`.
- **Kafka serializer configured twice.** The first embedded-broker run showed `JsonSerializer must be
  configured with property setters, or via configuration properties; not both`: the producer silently failed
  in production code paths because failures are logged, not thrown. Removing the YAML copy fixed it. This is
  the main reason the integration test exists.
- **Wrong test expectation.** The integration test first asserted on all events on a topic, which included
  events from other tests; it now filters to its own claims.
- **Shell left inside `target/`.** A `cd` into the reports folder blocked `mvn clean`; the working directory
  was reset.

## Reasoning

- **Verify, don't assume.** Every task ended with `mvn clean install`; the Kafka work only counted as done once
  messages were seen arriving on a real (embedded) broker.
- **Requirements over first guesses.** When the written requirements contradicted the assistant's earlier
  choices (status names, open-claim definition, endpoint style) the requirements won and the code was changed.
- **One place per rule.** Transition rules, the open-status set and topic names each exist exactly once, so
  later changes are small and testable.
- **Best-effort events.** For an assessment, an unavailable broker must not stop claims being processed; the
  loss window this creates is stated in `docs/architecture.md` together with the outbox remedy.
- **Honest scope.** Features outside the requirements (security, an officer entity) were deliberately left out
  and listed as future work.

## Open items for the reviewer

- `MORE_INFO_REQUIRED → UNDER_REVIEW` appears in the lifecycle diagram and the transition task, so it is
  allowed; it was missing from the first "allowed transitions" list in the background document.
- Whether `APPROVED` claims should count towards "total liability exposure" is a business decision; the
  current implementation follows the written definition of open claims.
- Claimant additional information and the officer personal workload were found missing when the work was
  reviewed against the business requirements, and were added at the end (additional-info endpoint, workload
  endpoint). Both follow the existing patterns; the officer workload returns bare JSON like the dashboard.
- Notifications to claimants: events are published but no consumer sends anything.
- No authentication or roles: any caller can use any endpoint.
