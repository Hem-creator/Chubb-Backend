# AI Journal

How this project was built with an AI assistant (Claude Code). Prompts came from the requirements you gave
me, one subtask at a time; this journal records what the assistant produced, what survived review, and why.
Where a decision was made by the user, it says so; where it was the assistant's call, that is stated too.

## Summary of the prompts used

| # | Prompt (short) | Result |
|---|---|---|
| 1 | Create Spring Boot 3 / Java 21 / Maven project with listed dependencies | `pom.xml`, `application.yml`, main class, packages |
| 2 | JPA entities `Claim`, `ClaimNote`, `ClaimStatus` | entities, auditing, relationships |
| 3 | Spring Data repositories | `ClaimRepository`, `ClaimNoteRepository` |
| 4 | Claims CRUD service and REST APIs | DTOs, service, controller, global exception handling |
| 5 | Project background and requirements (read only) | gap list against the first version |
| 6 | Re-validate task 1 with the stricter spec | package structure, Swagger, service interfaces |
| 7 | Entity design with validation and `LocalDateTime` | enum and entity rewrite |
| 8 | Repository aggregation queries | open count, exposure, by status, by officer |
| 9 | Claim APIs with `CreateClaimRequest` / `UpdateClaimRequest`, envelope | rewritten CRUD API |
| 10 | State transition workflow | `ClaimTransitionValidator`, lifecycle endpoints |
| 11 | Officer operations: assign and notes | assignment and notes services/controller |
| 12 | Manager dashboard | `GET /api/dashboard/workload` |
| 13 | Kafka events, tests, documentation | five topics and events, integration test, these docs |
| 14 | Build what is still pending | claimant additional-info endpoint, officer personal workload endpoint |

## AI response (what was delivered)

A layered Spring Boot application with CRUD, a validated lifecycle workflow, officer assignment and notes,
a manager dashboard, five Kafka topics, and 71 automated tests (unit, MockMvc, `@DataJpaTest`, and an
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
- **Honest scope.** Features outside the requirements (security, officer entity, claimant self-service for
  additional information) were deliberately left out and listed as future work.

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
