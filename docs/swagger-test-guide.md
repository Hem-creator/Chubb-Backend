# Swagger test guide

Step-by-step test script for the Claims API in Swagger UI. Every request body can be pasted as it is, and every
step lists the status code and result you should see. All steps were run against a fresh local instance of this
project and the results below are what came back.

Companion docs: [api-flow.md](api-flow.md) (endpoint reference and lifecycle),
[`postman/`](../postman) (the same API as a Postman collection).

## Before you start

1. Start the app (`mvn spring-boot:run`, or run `ClaimsManagementApplication` in IntelliJ).
2. **Restart it for a clean run.** The database is in-memory H2, so a restart wipes all data and ids start at 1
   again. This guide assumes a fresh start: the four claims you create below get ids **1, 2, 3 and 4**.
3. Open <http://localhost:8080/swagger-ui.html>.
4. In Swagger UI, open an endpoint, click **Try it out**, paste the body, click **Execute**, then compare the
   *Server response* with the **Expect** line.

### Three Swagger quirks to know first

| Quirk | What to do |
|---|---|
| **List claims (`GET /api/claims`) fails with 500 if you click Execute straight away.** Swagger pre-fills the `pageable` box with `page` and `size` of 1073741824 and `sort: ["string"]`, and `string` is not a real field. | Replace the whole `pageable` box with `{"page":0,"size":20}` (or `{"page":0,"size":2,"sort":["createdAt,desc"]}`). |
| **Workflow endpoints** (`under-review`, `request-more-info`, `approve`, `reject`, `settle`) pre-fill the body with `{"note":"string"}`. Left as it is, the claim gets a real note with the text `string`. | Type a real note, or replace the body with `{}`. The body is optional. |
| **No Kafka broker running.** Every call that publishes an event (create, assign, approve, reject, settle) takes about 5 to 6 seconds, and the console shows Kafka warnings. The API result is still correct. | Ignore it, or run `docker compose up -d` first for fast responses. |

## Test data

| Claim | id | Who | Policy | claimAmount | liabilityExposure | Planned path |
|---|---|---|---|---|---|---|
| A | 1 | Asha Verma | POL-100234 | 45000.00 | 30000.00 | assign, review, approve, settle |
| B | 2 | Rohan Mehta | POL-200567 | 120000.00 | 90000.00 | review, more info, back to review, reject |
| C | 3 | Priya Nair | POL-300891 | 8000.00 | 5000.00 | assign, stays UNDER_REVIEW (an open claim) |
| D | 4 | Karan Shah | POL-400112 | 15000.00 | 12000.00 | stays NEW and unassigned |

---

## Part 1. Create the four claims

Tag **Claims**, `POST /api/claims`. Run it four times, one body each. Keep the `claimNumber` from claim A
(it is random, you need it in step 6).

**Step 1: claim A.** Expect **201**, `"message": "Claim created successfully"`, `data.id` 1, `data.status`
`NEW`, a `claimNumber` like `CLM-AEB9543816E1`, and no `assignedOfficer` field yet.

```json
{
  "claimantName": "Asha Verma",
  "claimantEmail": "asha.verma@example.com",
  "policyNumber": "POL-100234",
  "description": "Rear-end collision at traffic signal, bumper and boot damage",
  "incidentDate": "2026-09-15",
  "claimAmount": 45000.00,
  "liabilityExposure": 30000.00
}
```

**Step 2: claim B.** Expect **201**, `data.id` 2.

```json
{
  "claimantName": "Rohan Mehta",
  "claimantEmail": "rohan.mehta@example.com",
  "policyNumber": "POL-200567",
  "description": "Water damage to warehouse stock after pipe burst",
  "incidentDate": "2026-09-20",
  "claimAmount": 120000.00,
  "liabilityExposure": 90000.00
}
```

**Step 3: claim C.** Expect **201**, `data.id` 3.

```json
{
  "claimantName": "Priya Nair",
  "claimantEmail": "priya.nair@example.com",
  "policyNumber": "POL-300891",
  "description": "Laptop stolen from office",
  "incidentDate": "2026-10-05",
  "claimAmount": 8000.00,
  "liabilityExposure": 5000.00
}
```

**Step 4: claim D.** Expect **201**, `data.id` 4.

```json
{
  "claimantName": "Karan Shah",
  "claimantEmail": "karan.shah@example.com",
  "policyNumber": "POL-400112",
  "description": "Minor fire damage to kitchen",
  "incidentDate": "2026-10-10",
  "claimAmount": 15000.00,
  "liabilityExposure": 12000.00
}
```

## Part 2. Claim A: the full happy path (NEW to SETTLED)

| Step | Tag, endpoint | `id` | Body | Expect |
|---|---|---|---|---|
| 5 | Claims, `GET /api/claims/{id}` | 1 | none | **200**, `status` `NEW` |
| 6 | Claims, `GET /api/claims/claim-number/{claimNumber}` | the `claimNumber` from step 1 | none | **200**, the same claim as step 5 |
| 7 | Claims Officer, `PUT /api/claims/{id}/assign` | 1 | `{"officer": "John Smith"}` | **200**, `assignedOfficer` `John Smith`, `status` still `NEW` |
| 8 | Claims Officer, `POST /api/claims/{id}/notes` | 1 | `{"note": "Police report received and verified."}` | **201**, `"message": "Note added successfully"`, `claimId` 1 |
| 9 | Claims Officer, `GET /api/claims/{id}/notes` | 1 | none | **200**, a list with 1 note |
| 10 | Claim Workflow, `PUT /api/claims/{id}/under-review` | 1 | `{"note": "Starting review"}` | **200**, `status` `UNDER_REVIEW` |
| 11 | Claim Workflow, `PUT /api/claims/{id}/approve` | 1 | `{"note": "Documents verified, approved for payout"}` | **200**, `status` `APPROVED` |
| 12 | Claim Workflow, `PUT /api/claims/{id}/settle` | 1 | `{"note": "Paid via bank transfer"}` | **200**, `status` `SETTLED` |
| 13 | Claims Officer, `GET /api/claims/{id}/notes` | 1 | none | **200**, 4 notes, oldest first: the police report, then the three workflow notes |

Claim A is now final: it cannot be edited, reassigned or moved again (see Part 6).

## Part 3. Claim B: more information, then rejection

| Step | Endpoint | `id` | Body | Expect |
|---|---|---|---|---|
| 14 | `PUT /api/claims/{id}/assign` | 2 | `{"officer": "Mary Jones"}` | **200**, `assignedOfficer` `Mary Jones` |
| 15 | `PUT /api/claims/{id}/under-review` | 2 | `{}` | **200**, `UNDER_REVIEW` |
| 16 | `PUT /api/claims/{id}/request-more-info` | 2 | `{"note": "Please send plumber invoice and photos"}` | **200**, `MORE_INFO_REQUIRED` |
| 17 | Claim Workflow, `POST /api/claims/{id}/additional-info` | 2 | `{"information": "Plumber invoice #4471 and 12 photos uploaded to the shared drive"}` | **200**, `"message": "Additional information received, claim is back under review"`, `UNDER_REVIEW` |
| 18 | `PUT /api/claims/{id}/reject` | 2 | `{"note": "Damage pre-dates policy start date"}` | **200**, `REJECTED` |
| 19 | `GET /api/claims/{id}/notes` | 2 | none | **200**, 3 notes: the info request, then `Additional information from claimant: Plumber invoice #4471 ...`, then the rejection reason |

## Part 4. Claim C: an open claim, and claim D left alone

| Step | Endpoint | `id` | Body | Expect |
|---|---|---|---|---|
| 20 | `PUT /api/claims/{id}/assign` | 3 | `{"officer": "John Smith"}` | **200** |
| 21 | `PUT /api/claims/{id}/under-review` | 3 | `{}` | **200**, `UNDER_REVIEW` |

Do nothing with claim D (id 4). It stays `NEW` and unassigned.

## Part 5. Lists and filters

Endpoint `GET /api/claims`. Remember to replace the `pageable` box (see the quirks above).

| Step | Parameters | Expect |
|---|---|---|
| 22 | `pageable` = `{"page":0,"size":20}` | **200**, `totalElements` 4, newest first (ids 4, 3, 2, 1) |
| 23 | `pageable` = `{"page":0,"size":2}` | **200**, 2 claims, `totalElements` 4, `totalPages` 2 |
| 24 | `status` = `NEW`, `pageable` = `{"page":0,"size":20}` | **200**, only claim D |
| 25 | `assignedOfficer` = `John Smith`, same `pageable` | **200**, claims C and A (`totalElements` 2) |
| 26 | `policyNumber` = `POL-200567`, same `pageable` | **200**, only claim B, `REJECTED` |
| 27 | `status` = `NEW` **and** `assignedOfficer` = `John Smith`, same `pageable` | **200**, only claim D. Only one filter applies, in the order status, officer, policy. |

## Part 6. Dashboard

Tag **Dashboard**. These two endpoints return plain JSON, without the `success` and `message` wrapper.

**Step 28: `GET /api/dashboard/workload`.** Expect **200**:

```json
{
  "totalOpenClaims": 2,
  "totalLiabilityExposure": 17000.00,
  "claimsByStatus": {
    "NEW": 1,
    "UNDER_REVIEW": 1,
    "MORE_INFO_REQUIRED": 0,
    "APPROVED": 0,
    "REJECTED": 1,
    "SETTLED": 1
  },
  "claimsPerOfficer": { "John Smith": 1 }
}
```

Open claims are `NEW`, `UNDER_REVIEW` and `MORE_INFO_REQUIRED`, so only C (5000) and D (12000) count. Claim D is
unassigned, so it is not in `claimsPerOfficer`.

**Step 29: `GET /api/officers/{officer}/workload`**, `officer` = `John Smith`. Expect **200**:

```json
{
  "officer": "John Smith",
  "openClaims": 1,
  "totalLiabilityExposure": 5000.00,
  "claimsByStatus": {
    "NEW": 0, "UNDER_REVIEW": 1, "MORE_INFO_REQUIRED": 0,
    "APPROVED": 0, "REJECTED": 0, "SETTLED": 1
  }
}
```

**Step 30:** the same endpoint with `officer` = `Nobody`. Expect **200** with `openClaims` 0, exposure 0 and
every status 0. An unknown officer is not an error.

## Part 7. Update a claim

**Step 31: `PUT /api/claims/{id}`**, `id` = 4 (claim D is still `NEW`). The policy number cannot be changed, so it
is not in this body. Expect **200**, `"message": "Claim updated successfully"`, new `description`, `claimAmount`
18000.00, `liabilityExposure` 14000.00, `status` still `NEW`.

```json
{
  "claimantName": "Karan Shah",
  "claimantEmail": "karan.shah@example.com",
  "description": "Fire damage to kitchen and dining area (updated)",
  "incidentDate": "2026-10-10",
  "claimAmount": 18000.00,
  "liabilityExposure": 14000.00
}
```

## Part 8. Negative tests

Run these after Part 7. Expected errors use the envelope `{"success": false, "message": "...", "data": ...}`.

### Validation (400)

**Step 32: `POST /api/claims` with every field wrong.** Expect **400**, `"message": "Validation failed"`, and a
`data` object with one message per field.

```json
{
  "claimantName": "",
  "claimantEmail": "not-an-email",
  "policyNumber": "",
  "description": "",
  "incidentDate": "2999-01-01",
  "claimAmount": 0,
  "liabilityExposure": -5
}
```

Expected `data` messages: `claimantName is mandatory`, `claimantEmail must be a valid email address`,
`policyNumber is mandatory`, `description is mandatory`, `incidentDate cannot be in the future`,
`claimAmount must be greater than 0`, `liabilityExposure must be greater than 0`.

| Step | Request | Expect |
|---|---|---|
| 33 | `POST /api/claims` with body `{}` | **400**, `Validation failed`, every field reported as `... is mandatory` |
| 34 | `POST /api/claims` with a broken body such as `{"claimantName":"x"` | **400**, `Malformed request` |
| 35 | `PUT /api/claims/4/assign` with `{"officer": ""}` | **400**, `data.officer` = `officer is mandatory` |
| 36 | `POST /api/claims/4/notes` with `{"note": ""}` | **400**, `data.note` = `note is mandatory` |
| 37 | `POST /api/claims/4/additional-info` with `{"information": ""}` | **400**, `data.information` = `information is mandatory` |
| 38 | `GET /api/claims/abc` (text where the id should be) | **400**, `Malformed request` |
| 39 | `GET /api/claims` with `status` = `FOO` | **400**, `Malformed request` |

### Not found (404)

| Step | Request | Expect |
|---|---|---|
| 40 | `GET /api/claims/{id}`, `id` = 9999 | **404**, `Claim not found with id: 9999` |
| 41 | `GET /api/claims/claim-number/CLM-DOESNOTEXIST` | **404**, `Claim not found with claim number: CLM-DOESNOTEXIST` |
| 42 | `PUT /api/claims/9999/approve` with `{}` | **404**, `Claim not found with id: 9999` |

### Workflow rules (409), using claim D (id 4, status `NEW`)

| Step | Request | Expect |
|---|---|---|
| 43 | `PUT /api/claims/4/approve` with `{}` | **409**, `Invalid status transition: NEW -> APPROVED. Allowed from NEW: [UNDER_REVIEW]` |
| 44 | `PUT /api/claims/4/settle` with `{}` | **409**, `Invalid status transition: NEW -> SETTLED. Allowed from NEW: [UNDER_REVIEW]` |
| 45 | `PUT /api/claims/4/request-more-info` with `{}` | **409**, `Invalid status transition: NEW -> MORE_INFO_REQUIRED. Allowed from NEW: [UNDER_REVIEW]` |
| 46 | `POST /api/claims/4/additional-info` with `{"information": "hello"}` | **409**, `Additional information can only be submitted while the claim is MORE_INFO_REQUIRED (current: NEW)` |

### Final claims (409), using claim A (id 1, status `SETTLED`)

| Step | Request | Expect |
|---|---|---|
| 47 | `PUT /api/claims/1` with the body from step 31 | **409**, `Claim 1 can no longer be edited (status: SETTLED)` |
| 48 | `PUT /api/claims/1/assign` with `{"officer": "Someone Else"}` | **409**, `Claim 1 cannot be assigned because it is SETTLED` |
| 49 | `PUT /api/claims/1/under-review` with `{}` | **409**, `Invalid status transition: SETTLED -> UNDER_REVIEW. Allowed from SETTLED: []` |
| 50 | `POST /api/claims/1/notes` with `{"note": "post-settlement note still allowed"}` | **201**. Notes can still be added to final claims. |

## Part 9. Delete and final check

| Step | Request | Expect |
|---|---|---|
| 51 | `DELETE /api/claims/{id}`, `id` = 4 | **200**, `Claim deleted successfully` |
| 52 | `GET /api/claims/{id}`, `id` = 4 | **404**, `Claim not found with id: 4` |
| 53 | `DELETE /api/claims/{id}`, `id` = 4 again | **404** |
| 54 | `GET /api/dashboard/workload` | **200**, `totalOpenClaims` 1, `totalLiabilityExposure` 5000.00, `claimsByStatus`: `NEW` 0, `UNDER_REVIEW` 1, `REJECTED` 1, `SETTLED` 1, others 0, `claimsPerOfficer` `{"John Smith": 1}` |

## Quick checklist

- [ ] 4 claims created with ids 1 to 4 (steps 1-4)
- [ ] A reaches `SETTLED` with 4 notes (steps 5-13)
- [ ] B goes through the more-info loop and ends `REJECTED` (steps 14-19)
- [ ] C is `UNDER_REVIEW`, D stays `NEW` (steps 20-21)
- [ ] Lists and filters return the right claims (steps 22-27)
- [ ] Dashboard shows 2 open claims and 17000.00 exposure (steps 28-30)
- [ ] Update works on a `NEW` claim (step 31)
- [ ] Every 400, 404 and 409 case returns the message listed (steps 32-50)
- [ ] Delete removes the claim and its notes, dashboard drops to 1 open claim (steps 51-54)

## Known behaviour found while testing

- **An invalid sort value returns 500, not 400.** `GET /api/claims?sort=string` fails with
  `An unexpected error occurred`, because the unknown field surfaces as an unhandled data-access error. This is
  the 500 you get from Swagger's default `pageable` box.
- Assigning a claim does not change its status, and `assign` works in any non-final status.
- `DELETE` has no status check: it removes a claim in any state, together with its notes.
