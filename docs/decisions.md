# Design decisions

Short log, newest context last. Format: decision – reason – consequence.

1. **Single Spring Boot application, layered packages.** The brief asks for one service; layering keeps
   controllers thin and rules testable. Module boundaries are by convention, not enforced.
2. **REST for commands and queries, Kafka for notifications.** Synchronous callers need an immediate result;
   downstream systems should not slow the API down.
3. **One class owns the lifecycle** (`ClaimTransitionValidator`). All status changes go through one private
   method in `ClaimWorkflowServiceImpl`. Adding a state or rule is a local change.
4. **Invalid transitions → `BusinessException` → HTTP 409.** The request is well-formed but conflicts with the
   claim's current state.
5. **`REJECTED` and `SETTLED` are final.** Such claims cannot be edited or assigned; notes can still be added so
   the history stays complete.
6. **`MORE_INFO_REQUIRED → UNDER_REVIEW` is allowed**, and is also what the claimant's additional-info call does.
7. **Open claims = NEW, UNDER_REVIEW, MORE_INFO_REQUIRED** (from the brief). `APPROVED` is therefore excluded from
   open count and exposure. Defined once in `ClaimRepository.OPEN_STATUSES`.
8. **Dashboard metrics are JPQL aggregations**, computed on demand, never by loading claim entities.
9. **Notes are separate rows** (`claim_notes`), many-to-one to the claim, returned oldest first with id as tie-breaker.
10. **Kafka: one topic per event, key = claim number.** Per-claim ordering, simple consumers.
11. **Events are sent after the DB commit and are best-effort.** No events for rolled-back data; a broker outage
    never fails the API. Known cost: an event can be lost (outbox listed as future work). When the broker is
    unreachable, each send waits up to `max.block.ms` (5 s) on the request thread.
12. **Optimistic locking (`@Version`)** protects against concurrent officers; the loser gets 409.
13. **Response envelope `{success, message, data}`** for claim APIs and errors; dashboard and officer workload return
    bare JSON as specified.
14. **Hand-written mappers, records for DTOs, constructor injection.** No MapStruct (not in the dependency list).
15. **H2 for the assessment, PostgreSQL + Flyway for production.** `ddl-auto=update` is a development convenience.
16. **Officer is a plain string.** Matches the specified model; the workload endpoint matches the exact name.
17. **No authentication in this version.** Out of the written requirements; listed under future enhancements.
