# Verification Report — SCRUM-193

## 1. Verification Summary & Verdict

**Verdict: Passed with accepted exceptions**

The Maven build and all 74 tests pass, and the local HTTP smoke successfully reached the loopback listener and landing/API routes. CR-03/NFR-03 browser smoke remains a formally accepted exception: no actual wide/narrow modern-browser smoke with create, status-change, and validation interactions is evidenced.

## 2. Environment

- **Date:** 2026-11-25
- **Operating system:** Microsoft Windows 11 Enterprise 64-bit, Windows NT 10.0.26200.0, amd64
- **Shell:** Windows PowerShell
- **Java command:** `java -version`
- **Java command result:** `java version "18.0.1.1"`; Java(TM) SE Runtime Environment 18.0.1.1; Java(TM) SE Runtime Environment build 18.0.1.1+2-6
- **Maven Wrapper runtime:** Apache Maven 3.9.9 (8e8579a9e76f7d015ee5ec7bfcdc97d260186937)
- **Maven Wrapper Java runtime:** Java 17.0.12, Oracle Corporation, `C:\Program Files\Java\jdk-17.0.12_windows-x64_bin\jdk-17.0.12`
- **Build compiler target:** Maven compiler plugin used `release 17`
- **Repository:** `Durgaprasad-Cheerla/github_capstone`

The Maven build used Java 17.0.12. The standalone `java -version` command reports Java 18.0.1.1, so the environment has no single unambiguous Java command/runtime record.

## 3. Build & Test Results

**Exact command:**

```powershell
.\mvnw.cmd clean verify
```

**Exit code:** `0`

**Result:** `BUILD SUCCESS`

**Exact Surefire summary line:**

```text
[INFO] Tests run: 74, Failures: 0, Errors: 0, Skipped: 0
```

The exact per-class summaries were:

| Test class | Tests | Failures | Errors | Skipped |
|---|---:|---:|---:|---:|
| `org.example.AppTest` | 4 | 0 | 0 | 0 |
| `org.example.reading.config.ReadingShelfConfigTest` | 6 | 0 | 0 | 0 |
| `org.example.reading.http.BrowserContractIntegrationTest` | 3 | 0 | 0 | 0 |
| `org.example.reading.http.ReadingShelfHandlerTest` | 9 | 0 | 0 | 0 |
| `org.example.reading.http.ReadingShelfHttpServerIntegrationTest` | 3 | 0 | 0 | 0 |
| `org.example.reading.http.ReadingShelfIntegrationTest` | 6 | 0 | 0 | 0 |
| `org.example.reading.persistence.JsonReadingShelfPersistenceIntegrationTest` | 7 | 0 | 0 | 0 |
| `org.example.reading.persistence.JsonReadingShelfPersistenceTest` | 14 | 0 | 0 | 0 |
| `org.example.reading.service.ShelfServiceTest` | 12 | 0 | 0 | 0 |
| `org.example.reading.shelf.BookTest` | 5 | 0 | 0 | 0 |
| `org.example.reading.shelf.ReadingStatusTest` | 5 | 0 | 0 | 0 |

**Failing test names:** None.

## 4. Requirement Coverage Matrix

| Requirement | Verification evidence | Result |
|---|---|---|
| FR-01 — Create a book | `BookTest.createsBookWithAllValues`, `BookTest.rejectsBlankTitleAndAuthor`; `ShelfServiceTest.createReportsSeparateFieldErrors`, `ShelfServiceTest.createSavesAndCommits`; `ReadingShelfHandlerTest.createValidation`, `ReadingShelfHandlerTest.createBook`; `ReadingShelfIntegrationTest.apiCreateListStatusDuplicateAndValidation` | Covered / Passed |
| FR-02 — Display complete inventory | `ShelfServiceTest.creates...`/inventory snapshot assertions; `ReadingShelfHandlerTest.getInventory`; `ReadingShelfIntegrationTest.apiCreateListStatusDuplicateAndValidation` | Covered / Passed |
| FR-03 — Change a book's status | `ReadingStatusTest.representsDirectTransitions`; `ShelfServiceTest.allStatusTransitionsAndReversalsWork`; `ReadingShelfHandlerTest.changeStatus`; `ReadingShelfIntegrationTest.apiCreateListStatusDuplicateAndValidation` | Covered / Passed |
| FR-04 — Prevent duplicate inventory entries | `ShelfServiceTest.duplicateMatchingIsExactAndIgnoresStatus`; `ReadingShelfHandlerTest.duplicate`; `ReadingShelfIntegrationTest.apiCreateListStatusDuplicateAndValidation` | Covered / Passed |
| FR-05 — Provide action feedback | `ShelfServiceTest.createReportsSeparateFieldErrors`; `ReadingShelfHandlerTest.createValidation`; `ReadingShelfHandlerTest.createBook`; `ReadingShelfHandlerTest.changeStatus`; `BrowserContractIntegrationTest.javaScriptContractProvidesActionsAndFeedback` | Covered / Passed |
| FR-06 — Persist inventory and status changes | `JsonReadingShelfPersistenceTest.saveAndLoadRoundTrip`; `JsonReadingShelfPersistenceIntegrationTest.temporarySaveAndLoadStayIsolated`; `ReadingShelfIntegrationTest.inventoryPersistsAcrossRestart`; `supportedDirectorySyncReturnsFullDurability`; `unsupportedDirectorySyncReturnsLimitedSuccess` | Covered / Passed |
| FR-07 — Start and access locally | `AppTest.serverStartupRemainsNonblocking`, `AppTest.runReturnsFailureForRuntimeStartupError`; `ReadingShelfHttpServerIntegrationTest.startsAndServesInventoryFromTemporaryPath`; `ReadingShelfIntegrationTest.startsEphemeralLoopbackServerWithTemporaryData`, `occupiedLoopbackPortFailsWithoutFallback`; README documents the exact command and URL; HTTP smoke returned HTTP 200 | Covered / Passed |
| NFR-01 — Local-only network binding | `ReadingShelfConfigTest.acceptsInjectedValues`; `ReadingShelfIntegrationTest.startsEphemeralLoopbackServerWithTemporaryData`; `ReadingShelfHttpServerIntegrationTest.startsAndServesInventoryFromTemporaryPath`; actual listener probe reported `127.0.0.1:8080` and no wildcard address | Covered / Passed |
| NFR-02 — Explicit persistence-error reporting | `JsonReadingShelfPersistenceTest.malformedJsonFailsWithoutOverwrite`, `unreadableJsonFailsExplicitly`, `flushFailureIsReported`, `replacementFailureIsReportedAndCleaned`, `directorySyncFailureFailsClosed`, `unsupportedDirectorySyncReturnsLimitedSuccess`; `JsonReadingShelfPersistenceIntegrationTest` malformed/unreadable/unwritable/flush/replacement/post-sync cases; `AppTest.runReturnsFailureForPersistenceStartupError` | Covered / Passed |
| NFR-03 — Responsive browser interface | Static CSS/asset contract tests cover `@media`/responsive contract only (`BrowserContractIntegrationTest.cssContractIsResponsive`). README defines 1280×800 and 375×667 manual smoke, but no actual browser run or viewport evidence exists; HTTP smoke is not browser/rendering verification | **Not Covered** |
| NFR-04 — Isolated automated tests | `@TempDir` and injected I/O/port use in persistence and HTTP integration tests; `ReadingShelfConfigTest.acceptsInjectedValues`; `ReadingShelfIntegrationTest.startsEphemeralLoopbackServerWithTemporaryData`; `JsonReadingShelfPersistenceIntegrationTest.temporarySaveAndLoadStayIsolated` | Covered / Passed |

## 5. Integration Test Results

End-to-end and integration behavior was tested with temporary directories, injected JUnit `@TempDir` paths, ephemeral ports, and narrow Java I/O test-double implementations for file read, flush, replacement, directory sync, and startup failure injection. External network services and real credentials were not used.

Integration coverage included:

- Ephemeral loopback HTTP server startup and inventory API operations
- Create/list/status/duplicate/validation behavior
- Persistence across server restart
- Save-failure HTTP envelope behavior
- Malformed and unreadable startup behavior
- Occupied-port startup failure
- Temporary-save isolation and malformed/unreadable/unwritable persistence cases
- Static browser shell, JavaScript contract, and responsive CSS contract

## 6. Local Run & Smoke Check

**Required startup command:** `.\mvnw.cmd compile exec:java`

**Configured URL:** `http://127.0.0.1:8080/`

**Actual startup result:** Started successfully under Maven Exec and remained running.

**Actual HTTP checks:**

- `GET /` returned HTTP 200, `text/html; charset=UTF-8`, and served the Reading Shelf HTML shell.
- `GET /api/books` returned HTTP 200 and `{"books":[]}`.
- `Get-NetTCPConnection -State Listen -LocalPort 8080` reported `127.0.0.1:8080`.
- Independent temporary-port probe: `GET /` and `GET /api/books` succeeded on `127.0.0.1:18080` using a temporary runtime data path. This is HTTP/loopback evidence only.

**Browser automation/manual viewport smoke:** Not performed. No browser automation tool is available in the verification evidence, and no manual viewport recording was supplied. This is an HTTP smoke check only, not a browser-rendering, viewport, create/status, or validation-interaction smoke.

## 7. Document Quality Results

### requirements.md

- **Structure:** Pass — overview, story, FR/NFR, edge cases, out-of-scope, and open-question sections are present.
- **Completeness:** Pass — no unresolved `TBD`, `TODO`, or `Lorem` placeholders; open questions state none.
- **Traceability:** Pass — every FR-01 through FR-07 and NFR-01 through NFR-04 exists and has trace source/evidence.
- **Consistency:** Pass — requirement IDs are reflected consistently in architecture and implementation plan.
- **Status accuracy:** Pass — requirements contain no task/status claims.
- **Open items:** Pass — the document explicitly records no open questions.
- **Local application:** Pass — startup command and URL are complete and match the approved profile.

### architecture.md

- **Structure:** Pass — the required system, traceability, diagram, components, contracts, flows, technology, error handling, risks, open questions, and change-log sections are present.
- **Completeness:** Pass — no `TBD`, `TODO`, or `Lorem` placeholders found.
- **Traceability:** Pass — traceability matrix covers FR-01 through FR-07 and NFR-01 through NFR-04.
- **Consistency:** Pass — component names correspond to implemented packages/classes/assets and the approved technology stack.
- **Status accuracy:** Pass — change log and mitigations correspond to reviewed decisions and implementation evidence.
- **Open items:** Pass — architecture open questions are explicitly empty.
- **Local application:** Pass — loopback, startup, URL, persistence, and manual smoke behavior are documented.

### design-review.md

- **Structure:** Pass — summary, scope, findings, decisions, mitigations, accepted risks, open questions, re-review, and approval sections are present.
- **Completeness:** Pass — no placeholder text found.
- **Traceability:** Pass — F-01 through F-05 reference valid requirement/component areas and mitigation sections.
- **Consistency:** Pass — decisions align with the updated architecture and approved stack.
- **Status accuracy:** Pass — all findings are marked mitigated and CR-01 remains correctly identified for implementation/verification.
- **Open items:** Pass — no design questions remain; the remaining approval gate is separate.
- **Local application:** Not Applicable to a document-level design review except for the documented NFR-03 viewport matrix, which is complete.

### impl-plan.md

- **Structure:** Pass — overview, dependency graph, task summary, detailed tasks, blocked items, and requirements coverage are present.
- **Completeness:** Pass — no placeholder text found.
- **Traceability:** Pass — T-01 through T-12 map to valid requirement IDs.
- **Consistency:** Pass — planned components and tasks correspond to actual repository assets and requirements.
- **Status accuracy:** Pass — all 12 tasks are marked done, and affected build/test evidence is present. T-12 browser-manual evidence is not executable test evidence, and its CR-03 status is accurately unresolved.
- **Open items:** Pass — no implementation task is left blocked in the task breakdown.
- **Local application:** Pass — T-12 specifies the exact startup command, URL, two viewports, interactions, and smoke result expectations.

### code-review.md

- **Structure:** Pass — summary, scope, checklist, findings, requirement coverage, dependency safety, remediation, accepted findings, open questions, and approval gate are present.
- **Completeness:** Pass — no placeholder text found.
- **Traceability:** Pass — CR-01 through CR-04 reference valid files and requirement IDs.
- **Consistency:** Pass — resolved findings align with passing deterministic tests and implementation traces.
- **Status accuracy:** Pass — CR-01, CR-02, and CR-04 are marked resolved with evidence; CR-03 is marked open/deferred and matches the absent browser evidence.
- **Open items:** Fail — human approval and several decisions remain open in the document: confirmation of CR-03 deferral, whether to add a create-warning test, Dependabot status, and final approval. CR-03 is recorded deferred, as requested for this verification, but final approval remains outstanding.
- **Local application:** Fail as a quality/status check — the README/browser smoke procedure is complete, but no actual browser smoke result is supplied, so the document's deferral accurately leaves NFR-03 unevidenced.

## 8. Dependency Scan Results

**Not Run** — no approved dependency-scan mechanism or tool result was supplied for this verification. The repository documents GitHub Dependabot in the approved stack and code-review dependency report, but no current alert status or scan report was available. A Maven build/resolve is not a dependency-vulnerability scan and was not treated as one.

## 9. Failures & Remediation

| Failure | Cause | Route |
|---|---|---|
| NFR-03 / CR-03 is not verified | No browser automation or manual viewport evidence exists for 1280×800 and 375×667 interactions | Route to user/browser tester for evidence; route to Implementation Agent only if the browser exposes layout or interaction defects |
| `code-review.md` open approval/open-items state | Human approval and CR-03 disposition remain unresolved in the review artifact | Route to user for explicit acceptance/approval; no implementation defect is established |

No failing tests or build failures occurred.

## 10. Accepted Exceptions

**CR-03/NFR-03 is formally accepted as an exception**, with the user explicitly approving deferral on 2026-11-25. A future release or verification cycle should perform the documented 1280×800 and 375×667 browser interactions.

**Dependabot alerts:** Not Checked, not accepted as a clean result.

## 11. Open Questions

1. Does the user accept the CR-03/NFR-03 deferral as an exception, or require actual browser smoke at 1280×800 and 375×667?
2. Does the user approve this verification report and handoff as "Passed with accepted exceptions", if the CR-03 exception is accepted?
