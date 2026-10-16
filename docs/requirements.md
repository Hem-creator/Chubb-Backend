# Requirements

Source: the project brief supplied for this assessment, reproduced in structured form.

## Background

Chubb APAC processes motor and property claims across six markets.

Current problems:

- Claimants submit claims by phone or email.
- Claimants have no visibility into claim status.
- Claims staff manage work through shared inboxes and spreadsheets.
- There is no consolidated workload view.
- Managers have no real-time view of outstanding liability exposure.

The frontend is out of scope; this project is the backend platform.

## Business requirements

**Claimants** can: report an incident, create a claim, track claim status, submit additional information when
requested, receive claim decisions.

**Claims officers** can: view incoming claims, assign claims, review claims, request additional information,
approve, reject, settle approved claims, view their personal workload.

**Managers** can: view team workload, view claims by status, view total liability exposure, monitor
claim processing progress.

## Technology decisions

- Java 21, Spring Boot 3
- H2 for the assessment (PostgreSQL explained for production)
- REST for synchronous operations (create, get, update, assign, dashboard)
- Kafka for asynchronous notifications and integrations: ClaimCreated, ClaimAssigned, ClaimApproved,
  ClaimRejected, ClaimSettled
- Single Spring Boot application; packages `controller, service, repository, entity, dto, mapper, exception,
  config, kafka, dashboard`

## Domain model

**Claim**: id, claimNumber, claimantName, claimantEmail, policyNumber, description, incidentDate, claimAmount,
liabilityExposure, status, assignedOfficer, createdAt, updatedAt

**ClaimNote**: id, claimId, note, createdAt

**ClaimStatus**: NEW, UNDER_REVIEW, MORE_INFO_REQUIRED, APPROVED, REJECTED, SETTLED

## Lifecycle

```
NEW -> UNDER_REVIEW -> MORE_INFO_REQUIRED -> UNDER_REVIEW -> APPROVED -> SETTLED
NEW -> UNDER_REVIEW -> REJECTED
```

Allowed transitions: NEW→UNDER_REVIEW, UNDER_REVIEW→MORE_INFO_REQUIRED, UNDER_REVIEW→APPROVED,
UNDER_REVIEW→REJECTED, APPROVED→SETTLED. Any other transition throws `BusinessException`.
(The diagram and the transition task also include MORE_INFO_REQUIRED→UNDER_REVIEW, which is implemented.)

## Database tables

`claims`, `claim_notes`

## API endpoints

| Group | Endpoints |
|---|---|
| Claims | `POST /api/claims`, `GET /api/claims`, `GET /api/claims/{id}`, `PUT /api/claims/{id}`, `DELETE /api/claims/{id}` (also `GET /api/claims/claim-number/{claimNumber}`) |
| Lifecycle | `PUT /api/claims/{id}/assign`, `/under-review`, `/request-more-info`, `/approve`, `/reject`, `/settle` |
| Notes | `POST /api/claims/{id}/notes`, `GET /api/claims/{id}/notes` |
| Dashboard | `GET /api/dashboard/workload` |

Dashboard response:

```json
{
  "totalOpenClaims": 25,
  "totalLiabilityExposure": 1200000,
  "claimsByStatus": { "NEW": 5, "UNDER_REVIEW": 10 },
  "claimsPerOfficer": { "John": 5, "Mike": 4 }
}
```

Open claims are NEW, UNDER_REVIEW and MORE_INFO_REQUIRED. (The brief's example was cut off after
`"UNDER_REVIEW": 10`; the remaining statuses and the per-officer map follow the dashboard task definition.)

## Subtask requirements added later

- Entities with validation, `LocalDateTime` auditing and a `@ManyToOne` from `ClaimNote` to `Claim`.
- Repository aggregation queries: open claims count, total liability exposure, claims by status, claims per officer.
- Create/Update DTOs with validation (claimantName mandatory, valid email, claimAmount > 0, liabilityExposure > 0),
  response envelope `{success, message, data}`, claim number auto-generated, initial status NEW.
- Centralised transition validation, reusable rules, `BusinessException` for invalid transitions.
- Kafka topics `claim-created`, `claim-assigned`, `claim-approved`, `claim-rejected`, `claim-settled`.
- Tests with JUnit 5, Mockito and MockMvc; README, architecture and AI journal documents.

## Added to close gaps against the business requirements

- Claimant submits additional information: `POST /api/claims/{id}/additional-info`.
- Officer personal workload: `GET /api/officers/{officer}/workload`.
