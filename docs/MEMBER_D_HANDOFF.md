# Member D handoff — Alert

## Scope implemented

- T-2.D1: `AlertService.create(targetMemberId, requestId, type, message)` persists all three `AlertType` values.
- T-2.D2: `GET /api/alerts?isRead=` returns only the authenticated member's alerts; `PATCH /api/alerts/{id}/read` marks an owned alert as read.
- The API follows the existing `docs/openapi.yaml`: the list response is an array, not a paginated object.

## Integration contract for Members B and C

Use only the public entry point below from a transactional service method. Do not use `AlertRepository` outside the `alert` feature.

```java
alertService.create(targetMemberId, requestId, AlertType.ASSIGNED, message);
```

Alert types and recipients:

- `HIGH_PRIORITY_REGISTERED`: every ADMIN after a CLIENT creates a HIGH request.
- `ASSIGNED`: the assigned DEVELOPER after an assignment.
- `STATUS_CHANGED`: the CLIENT after a request status changes.

The caller's transaction is preserved, so the request/workflow update and its alert commit or roll back together.

## Files changed

- `BE/src/main/java/com/bzcom/crm/alert/Alert.java`: matches the canonical `alerts` schema, which has `created_at` but no `updated_at`.
- `BE/src/main/java/com/bzcom/crm/alert/AlertRepository.java`: own-alert queries ordered newest first.
- `BE/src/main/java/com/bzcom/crm/alert/AlertService.java`: create, list-own and ownership-safe mark-read use cases.
- `BE/src/main/java/com/bzcom/crm/alert/AlertController.java`: completed endpoints using `@AuthenticationPrincipal CurrentUser`.
- `BE/src/main/java/com/bzcom/crm/alert/dto/AlertResponse.java`: UTC `Instant` response timestamp.
- `BE/src/test/java/com/bzcom/crm/alert/AlertServiceTest.java`: create/list/read/forbidden service tests.

## Verification to run locally

```powershell
cd BE
.\mvnw.cmd -B verify
```

Use Java 21 and start Docker Desktop first. The full verification requires Docker because integration tests use PostgreSQL Testcontainers.

## Remaining Member D work

- T-2.D3 Swagger/runtime contract audit.
- T-3.1 mock LLM now compiles; add dedicated mock tests before marking the task complete.
- T-3.2 LLM APIs, OpenAI provider, fallback and tests remain.
- Run and record the full Maven quality gate before marking T-2.D1/D2 complete in `docs/TASKS.md`.
