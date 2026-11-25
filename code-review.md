# Code Review — SCRUM-193

## 1. Review Summary & Verdict

**Verdict: Approved with conditions**

| Severity | Count | Open | Accepted | Resolved |
|---|---:|---:|---:|---:|
| Critical | 1 | 0 | 0 | 1 |
| High | 1 | 0 | 0 | 1 |
| Medium | 2 | 1 | 0 | 1 |
| Low | 0 | 0 | 0 | 0 |
| **Total** | **4** | **1** | **0** | **3** |

CR-01, CR-02, and CR-04 have been remediated and re-reviewed. `UnsupportedOperationException` is now handled as platform-limited success only after flush and atomic replacement, while `IOException` and runtime failures from directory synchronization remain fail-closed. `App.run` returns a nonzero result and `App.main` exits nonzero with a one-line, secret-safe stderr message. CR-04 is resolved: create success now renders the `PLATFORM_LIMITED_DURABILITY` warning in the Browser UI. CR-03 remains deferred because no browser smoke result is recorded.

## 2. Scope

- **Review date:** 2026-11-24
- **Repository:** `Durgaprasad-Cheerla/github_capstone`
- **Working-tree/commit reference:** HEAD `7b45a9bcaa4378bdc2b7bdfb5979ce345e689857` (working tree contains implementation changes). The implementation was working-tree code rather than a clean committed baseline.
- **Requirements/design inputs:** `requirements.md`, `architecture.md`, `design-review.md`, `impl-plan.md`, `.github/standards/TechStack.md`, `.github/dependabot.yml`
- **Files reviewed:** `pom.xml`, `README.md`, `src/main/java/**/*.java`, `src/main/resources/web/**`, `src/test/java/**/*.java`, and `.gitignore`
- **Implementation-plan validation:** All 12 tasks in `impl-plan.md` are marked `[x]` Done.
- **Local checks:** The reconciled Surefire report set contains 74 tests, 0 failures, 0 errors, and 0 skipped. The current reports correspond to `BUILD SUCCESS`; this re-review did not rerun Maven or perform a browser smoke.
- **Runtime check:** No browser or live runtime probe is claimed by this re-review.

## 3. Checklist Results

| Area | Result | Reason |
|---|---|---|
| Correctness | Pass | CR-01 is resolved in persistence code and deterministic tests; CR-04 is resolved in create/status warning handling. |
| Security | Pass | No hardcoded credentials or secret logging was found. Static rendering uses `textContent`; API errors omit internal details. Binding is fixed to loopback. |
| Error Handling | Pass | Startup failures now return nonzero and persistence distinguishes unsupported sync from operational sync `IOException`; save/load paths remain explicit. |
| Test Coverage | Findings | The reconciled 74-test result covers CR-01/CR-02 branches and core UI contracts, but it does not contain an executable browser-smoke result for NFR-03 and has no dedicated create-warning assertion for CR-04. |
| Code Clarity | Pass | Component names, route names, methods, and immutable model types clearly express their purposes. |
| DRY | Pass | No material cross-class duplication requiring extraction was identified. |
| Dependency Safety | Findings | Dependencies/plugins are approved and pinned; Dependabot alert status was not supplied, so vulnerability status is unverified rather than asserted clean. |

## 4. Findings Register

| ID | File and line | Requirement IDs | Category | Severity | Status |
|---|---|---|---|---|---|
| CR-01 | `src/main/java/org/example/reading/persistence/JsonReadingShelfPersistence.java:161-169`; `src/main/java/org/example/reading/persistence/ReadingShelfIo.java:82-87`; tests at `JsonReadingShelfPersistenceTest.java:166-204` | FR-01, FR-06, NFR-02 | Correctness / durability | Critical | Resolved |
| CR-02 | `src/main/java/org/example/App.java:9-31`; `src/test/java/org/example/AppTest.java:31-72` | FR-07, NFR-02 | Error Handling / startup | High | Resolved |
| CR-03 | `README.md:50-64`; `src/test/java/org/example/reading/http/BrowserContractIntegrationTest.java` | NFR-03 | Test Coverage / responsiveness | Medium | Open |
| CR-04 | `src/main/resources/web/app.js:220-229` | FR-05, FR-06, NFR-02 | UI warning handling | Medium | Resolved |

## 5. Finding Details

### CR-01 — Real Windows saves appear to fail at directory synchronization

- **Resolution:** Resolved. `JsonReadingShelfPersistence.saveWithResult` performs file preparation and flush, then atomic replacement, before calling `syncDirectory` (`JsonReadingShelfPersistence.java:161-169`). It returns `PLATFORM_LIMITED_DURABILITY` only for an explicit `UnsupportedOperationException`; an actual `IOException` or runtime exception after replacement sets `failClosed` and throws `PERSISTENCE_ERROR`.
- **Evidence:** `unsupportedDirectorySyncReturnsLimitedSuccess` verifies the limited-success result and that it does not fail closed (`JsonReadingShelfPersistenceTest.java:166-184`). `directorySyncFailureFailsClosed` verifies load and save remain blocked after an injected sync `IOException` (`JsonReadingShelfPersistenceTest.java:145-163`). `supportedDirectorySyncReturnsFullDurability` verifies the directory sync call for full-durability success (`JsonReadingShelfPersistenceTest.java:188-204`). `ShelfService` commits the inventory and exposes the warning only after the save result succeeds (`ShelfService.java:98-104,160-166`); the handler surfaces `durabilityWarning` in successful create/status responses (`ReadingShelfHandler.java:126-131,178-183`) and the UI renders it safely.
- **Residual blocker:** None for CR-01. The implementation does not yet use the exact same Windows NIO directory-open behavior in a real runtime create test, but the dedicated seam verifies the two required branches. This is not a browser-smoke claim.

### CR-02 — Startup failures do not set a failing process exit status

- **Resolution:** Resolved. `App.run` returns `1` for startup/runtime exceptions and `0` on success (`App.java:16-24`). `App.main` calls `System.exit(exitCode)` when nonzero (`App.java:9-13`). `safeMessage` reduces exception text to one sanitized line and falls back to the exception type.
- **Evidence:** App tests verify invalid-port and malformed-persistence startup each return `1`, emit one stderr line, and do not include stack traces (`AppTest.java:31-62`). The subprocess test verifies `App.main` emits one safe stderr line and exits with status `1` (`AppTest.java:64-72`).
- **Residual blocker:** None for CR-02.

### CR-03 — Responsive browser smoke is not evidenced

- **Problem:** The README defines a two-viewport manual smoke procedure, but no recorded result or evidence is present in the repository. `BrowserContractIntegrationTest.java` performs asset-contract checks, not actual browser rendering at 1280 × 800 or 375 × 667.
- **Risk:** NFR-03 can remain unmet through clipping, overlap, or inaccessible controls despite passing static HTML/CSS assertions.
- **Evidence:** `README.md:50-64` documents the manual procedure and says automated tests do not replace it. The browser contract test is the executable evidence, and it does not report a viewport rendering pass.
- **Suggested fix:** Perform the documented manual smoke in a modern browser and record pass/fail evidence, including both viewport sizes and create/status/validation interactions. Route to implementation only if the browser reveals a layout defect.

### CR-04 — Create success does not display the platform-limited durability warning

- **Resolution:** Resolved. The create success branch now checks `body.durabilityWarning` and renders a platform-limited crash-durability warning together with the successful create feedback (`src/main/resources/web/app.js:220-229`).
- **Problem:** The create handler emits `durabilityWarning` in successful POST responses, but `app.js`' create success branch only reports "Added … to the shelf" and does not inspect `body.durabilityWarning` (`src/main/resources/web/app.js:220-229`).
- **Risk:** On an explicit unsupported directory-sync platform, create can succeed with reduced durability while the UI falsely presents only ordinary success, contrary to the architecture's requirement to expose `PLATFORM_LIMITED_DURABILITY`.
- **Evidence:** The status-change branch explicitly renders the warning (`app.js:165-180`). The create branch now parses `body.durabilityWarning` after `body = await response.json()` and uses `setFeedback` with success styling (`app.js:220-229`). The handler adds `durabilityWarning` to successful create responses (`ReadingShelfHandler.java:126-131`), and the service sets the warning after a platform-limited save (`ShelfService.java:98-104`).
- **Severity rationale:** Medium: a testable warning-contract gap affecting clear feedback and honest durability claims, but create and persistence remain functional.
- **Residual blocker:** None for CR-04. A dedicated automated assertion for create-warning propagation is still advisable, but the implementation and handler contract are aligned.

## 6. Requirement Coverage

| Requirement | Code trace | Test trace | Status |
|---|---|---|---|
| FR-01 | `Book`, `ShelfService.createBook`, `ReadingShelfHandler.handleCreate` | `ShelfServiceTest`, `ReadingShelfHandlerTest`, `ReadingShelfIntegrationTest` | Pass; CR-04 resolved by create warning feedback |
| FR-02 | `ShelfService.listBooks`, handler GET route, UI renderer | inventory assertions in service/handler/integration tests | Pass |
| FR-03 | `ShelfService.changeStatus`, status PATCH route, UI status control | all distinct transitions and integration assertions | Pass |
| FR-04 | exact duplicate lookup in `ShelfService.createBook` | duplicate tests in service/integration | Pass |
| FR-05 | validation exceptions, standardized error envelope, UI feedback | field-error and HTTP tests | Pass |
| FR-06 | Jackson persistence, temporary save, atomic replacement, runtime path | persistence unit/integration and restart test | Resolved CR-01 |
| FR-07 | `App`, loopback `HttpServer`, README command/URL | startup and ephemeral-loopback tests | Resolved CR-02 |
| NFR-01 | fixed host `127.0.0.1`, loopback address construction | loopback and wildcard exclusion assertions | Pass |
| NFR-02 | explicit load/save envelopes and fail-closed behavior | deterministic malformed/read/save/failclosed tests | Resolved CR-01 and CR-02; CR-04 resolved for warning disclosure |
| NFR-03 | responsive CSS and plain browser UI | asset contract; manual smoke not recorded | Finding CR-03 |
| NFR-04 | temp paths and ephemeral/constructor port injection | isolated tests use temp/ephemeral values | Pass |

## 7. Dependency Safety Report

All direct dependencies and plugins are approved under `TechStack.md` and pinned in `pom.xml`. Versions shown are the effective resolved properties or plugin versions.

| Dependency or plugin | Version | Approved | Pinned | Known vulnerability status |
|---|---|---|---|---|
| `com.fasterxml.jackson.core:jackson-databind` | 2.22.4 | Yes | Yes | Not verified: no Dependabot alert report supplied |
| `org.junit.jupiter:junit-jupiter` | 5.14.4 | Yes | Yes | Not verified: no Dependabot alert report supplied |
| `org.mockito:mockito-core` | 5.24.0 | Yes | Yes | Not verified: no Dependabot alert report supplied |
| `org.mockito:mockito-junit-jupiter` | 5.24.0 | Yes | Yes | Not verified: no Dependabot alert report supplied |
| `org.apache.maven.plugins:maven-compiler-plugin` | 3.15.0 | Yes | Yes | Not verified: no Dependabot alert report supplied |
| `org.apache.maven.plugins:maven-surefire-plugin` | 3.6.1 | Yes | Yes | Not verified: no Dependabot alert report supplied |
| `org.codehaus.mojo:exec-maven-plugin` | 3.5.0 | Yes | Yes | Not verified: no Dependabot alert report supplied |

`.github/dependabot.yml` configures Maven weekly scans and a five-PR limit. It does not provide current alert results. No dependency version in the application is unpinned.

## 8. Remediation Plan

User decisions are pending; no finding is fix-routed or accepted without explicit approval.

| Finding | Decision | Owner | Route order |
|---|---|---|---|
| CR-01 | Resolved after re-review | Implementation complete | Closed |
| CR-02 | Resolved after re-review | Implementation complete | Closed |
| CR-03 | Defer pending browser evidence | User / browser tester | 2 — Medium |
| CR-04 | Resolved by create-warning UI remediation | Implementation complete | Closed |

No finding is currently accepted. A re-review will be limited to affected findings after implementation changes.

## 9. Accepted Findings

CR-03 remains deferred for browser smoke evidence. CR-01, CR-02, and CR-04 are resolved rather than accepted. Human approval is still required before the final review verdict and verification handoff.

## 10. Open Questions

1. Does the user confirm that CR-03 remains deferred pending NFR-03 browser smoke evidence?
2. Should a dedicated create-warning contract test be added before verification?
3. What is the current Dependabot alert status for the pinned Maven dependencies/plugins?
4. Does the user approve the final review verdict after CR-04 and any browser evidence are resolved or formally deferred?

## 11. Approval Gate

**Approval status:** Awaiting explicit user approval.

This review is **Approved with conditions**. CR-01, CR-02, and CR-04 are resolved; CR-03 remains an open Medium finding but is explicitly deferred as unexecuted browser smoke. The reconciled test reports contain 74 tests with 0 failures and 0 errors. The Critical/High handoff gate is clear, but this final review awaits human approval before the verification handoff.
