# Chubb APAC Claims Management – Backend

Backend platform for motor and property claims across six APAC markets. Claimants report incidents and
track status, claims officers review and settle claims, and managers see workload and liability exposure
in real time. The frontend is out of scope.

## Technology

| Area | Choice |
|---|---|
| Language / framework | Java 21, Spring Boot 3.4 |
| Build | Maven |
| Persistence | Spring Data JPA, H2 (in-memory) for the assessment, PostgreSQL recommended for production |
| Messaging | Apache Kafka (Spring Kafka) |
| API docs | springdoc-openapi (Swagger UI) |
| Ops | Spring Boot Actuator |
| Tests | JUnit 5, Mockito, MockMvc, Spring Kafka embedded broker |

## Architecture

Single Spring Boot application with a layered structure:

```
com.chubb.claims
├── controller     REST endpoints, request validation, response envelope
├── service        service interfaces
│   ├── impl       business logic (CRUD, workflow, assignment, notes)
│   └── workflow   ClaimTransitionValidator – the one place lifecycle rules live
├── repository     Spring Data JPA repositories and aggregation queries
├── entity         Claim, ClaimNote, ClaimStatus
├── dto            request / response records
├── mapper         entity <-> DTO and entity -> Kafka event mapping
├── exception      BusinessException, ResourceNotFoundException, GlobalExceptionHandler
├── config         JPA auditing, Kafka, OpenAPI configuration
├── kafka          ClaimEventProducer and event payloads (kafka.event)
├── dashboard      manager workload dashboard (controller, service, DTO)
└── util           ClaimNumberGenerator
```

Request flow: `controller -> service -> repository -> H2`, with the service publishing events to Kafka
after the database transaction commits.

### Design decisions

- **REST for synchronous operations, Kafka for asynchronous notifications.** Callers get an immediate
  answer; downstream systems (notifications, analytics) react to events without slowing the API.
- **Centralised lifecycle rules.** `ClaimTransitionValidator` holds the only transition map. Every status
  change goes through `ClaimWorkflowServiceImpl.transition()`, so a new rule is a one-line change.
- **Events are published after commit.** A rolled-back transaction never produces an event. Publishing is
  best-effort: a broker outage is logged and does not fail the API call (see trade-offs in
  `docs/architecture.md`).
- **Claim number as Kafka key.** All events for one claim go to the same partition, so they stay ordered.
- **Optimistic locking** (`@Version`) stops two officers silently overwriting each other.
- **Uniform response envelope** `{success, message, data}` for claim APIs and errors. The dashboard
  returns the bare JSON object defined in the requirements.
- **Constructor injection, interfaces for services, records for DTOs** (SOLID, easy to mock).

## Claim lifecycle

```
NEW -> UNDER_REVIEW -> APPROVED -> SETTLED
          |  ^
          v  |
   MORE_INFO_REQUIRED
          |
UNDER_REVIEW -> REJECTED
```

Allowed transitions: `NEW→UNDER_REVIEW`, `UNDER_REVIEW→MORE_INFO_REQUIRED`, `UNDER_REVIEW→APPROVED`,
`UNDER_REVIEW→REJECTED`, `MORE_INFO_REQUIRED→UNDER_REVIEW`, `APPROVED→SETTLED`. Anything else throws
`BusinessException` (HTTP 409). `REJECTED` and `SETTLED` are final: the claim cannot be edited or assigned.

## API endpoints

Swagger UI: http://localhost:8080/swagger-ui.html

### Claims

| Method | Path | Description |
|---|---|---|
| POST | `/api/claims` | Create a claim (status `NEW`, claim number generated) |
| GET | `/api/claims` | Paged list; filters `status`, `assignedOfficer`, `policyNumber`; `page`, `size`, `sort` |
| GET | `/api/claims/{id}` | Get by id |
| GET | `/api/claims/claim-number/{claimNumber}` | Get by claim number |
| PUT | `/api/claims/{id}` | Update claim details |
| DELETE | `/api/claims/{id}` | Delete a claim and its notes |

### Lifecycle (officers)

| Method | Path | Transition |
|---|---|---|
| PUT | `/api/claims/{id}/assign` | Body `{"officer": "John Smith"}` |
| PUT | `/api/claims/{id}/under-review` | `NEW` or `MORE_INFO_REQUIRED` → `UNDER_REVIEW` |
| PUT | `/api/claims/{id}/request-more-info` | `UNDER_REVIEW` → `MORE_INFO_REQUIRED` |
| PUT | `/api/claims/{id}/approve` | `UNDER_REVIEW` → `APPROVED` |
| PUT | `/api/claims/{id}/reject` | `UNDER_REVIEW` → `REJECTED` |
| PUT | `/api/claims/{id}/settle` | `APPROVED` → `SETTLED` |

Each lifecycle call accepts an optional body `{"note": "..."}`; a non-blank note is stored as a claim note
(for example a rejection reason).

### Claimant

| Method | Path | Description |
|---|---|---|
| POST | `/api/claims/{id}/additional-info` | Body `{"information": "..."}`. Only while the claim is `MORE_INFO_REQUIRED`; stored as a note and the claim returns to `UNDER_REVIEW` (409 otherwise) |

Claimants track status with `GET /api/claims/{id}` or `GET /api/claims/claim-number/{claimNumber}`.

### Notes

| Method | Path | Description |
|---|---|---|
| POST | `/api/claims/{id}/notes` | Body `{"note": "Requested repair invoice."}` |
| GET | `/api/claims/{id}/notes` | Notes, oldest first |

### Dashboard

| Method | Path | Description |
|---|---|---|
| GET | `/api/dashboard/workload` | Open claims, total liability exposure, claims by status, claims per officer |
| GET | `/api/officers/{officer}/workload` | One officer's open claims, exposure on them and all their claims by status (exact name match; zeros if none) |

"Open" means `NEW`, `UNDER_REVIEW` or `MORE_INFO_REQUIRED`. `claimsByStatus` covers all claims and always
lists every status (zero when empty).

### Example

```bash
curl -X POST http://localhost:8080/api/claims -H "Content-Type: application/json" -d '{
  "claimantName": "Jane Doe", "claimantEmail": "jane@example.com", "policyNumber": "POL-1001",
  "description": "Rear-end collision", "incidentDate": "2026-10-01",
  "claimAmount": 2000.00, "liabilityExposure": 1500.00 }'

curl -X PUT http://localhost:8080/api/claims/1/assign -H "Content-Type: application/json" -d '{"officer":"John Smith"}'
curl -X PUT http://localhost:8080/api/claims/1/under-review
curl -X PUT http://localhost:8080/api/claims/1/approve
curl -X PUT http://localhost:8080/api/claims/1/settle
curl http://localhost:8080/api/dashboard/workload
```

### Error format

```json
{ "success": false, "message": "Invalid status transition: NEW -> APPROVED. Allowed from NEW: [UNDER_REVIEW]", "data": null }
```

| Status | Meaning |
|---|---|
| 400 | Validation failed (`data` maps field → message) or malformed request |
| 404 | Claim not found |
| 409 | Invalid status transition, claim is final, or concurrent modification |
| 500 | Unexpected error (details only in the log) |

## Run instructions

Requirements: JDK 21, Maven 3.9+. Kafka is optional: the app starts without a broker and only logs a
warning when an event cannot be sent.

```bash
mvn clean install        # build and run all tests
mvn spring-boot:run      # start on http://localhost:8080
```

Optional local Kafka (any broker on `localhost:9092` works):

```bash
docker compose up -d      # uses docker-compose.yml in the project root
```

Use another broker with `KAFKA_BOOTSTRAP_SERVERS=host:port`.

Useful URLs: Swagger UI `/swagger-ui.html`, OpenAPI JSON `/v3/api-docs`, H2 console `/h2-console`,
health `/actuator/health`.

Logs go to the console and to `logs/claims-management.log` (rolling, 10 MB, 14 days).

## Database

- **H2 in-memory**, created at startup; data is lost on restart. `spring.jpa.hibernate.ddl-auto=update`
  builds the schema from the entities.
- **H2 console:** http://localhost:8080/h2-console – JDBC URL `jdbc:h2:mem:claimsdb`, user `sa`, empty password.
- **Tables**
  - `claims` – claim data, status, assigned officer, audit timestamps, `version` for optimistic locking.
    Indexed on policy number, status and officer.
  - `claim_notes` – one row per note, `claim_id` foreign key, `created_at`.
- **Production:** PostgreSQL with Flyway migrations and `ddl-auto=validate`; only the datasource settings
  and the driver change, since the code uses standard JPA/JPQL.

## Kafka topics

| Topic | Event | Published when |
|---|---|---|
| `claim-created` | `ClaimCreatedEvent` | a claim is created |
| `claim-assigned` | `ClaimAssignedEvent` | a claim is assigned to an officer |
| `claim-approved` | `ClaimApprovedEvent` | a claim is approved |
| `claim-rejected` | `ClaimRejectedEvent` | a claim is rejected (includes the reason note) |
| `claim-settled` | `ClaimSettledEvent` | a claim is settled |

- Key: claim number. Value: JSON (ISO-8601 timestamps, no Java type headers). Every event has `eventId`,
  `claimId`, `claimNumber` and `occurredAt`.
- Topics are created at startup (3 partitions, replication factor 1). Names are configurable under
  `app.kafka.topics.*` in `application.yml`.
- Review steps (`under-review`, `request-more-info`) and plain updates do not publish events.
- Delivery is at-least-once from the consumer's point of view; use `eventId` for de-duplication.

## Tests

`mvn test` runs 82 tests:

- unit tests with Mockito for services, mappers' collaborators and the Kafka producer
- MockMvc controller tests (validation, status codes, response envelope)
- `@DataJpaTest` repository tests for the aggregation queries
- `ClaimLifecycleIntegrationTest`: full Spring context, H2 and an embedded Kafka broker – creates, assigns,
  approves, settles and rejects claims over HTTP and checks the events that arrive on each topic

## Project documents

| File | Purpose |
|---|---|
| `docs/README.md` | Index of all project documents |
| `docs/requirements.md` | Business requirements and domain rules this system implements |
| `docs/architecture.md` | Domain model, lifecycle diagram, trade-offs, future enhancements |
| `docs/decisions.md` | Short log of key design decisions |
| `docs/ai-journal.md` | How AI was used: prompts, accepted / rejected / modified, reasoning |
| `openapi/claims-api.yaml` | API contract generated from the running application |
| `postman/claims-management.postman_collection.json` | Manual test collection for the full claim lifecycle |
| `docker-compose.yml` | Local Kafka broker (`docker compose up -d`) |