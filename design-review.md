# Design Review — SCRUM-193

## 1. Review Summary & Status

**Overall verdict:** Approved with conditions — all findings are mitigated in the architecture; explicit user approval of this review and the updated architecture is still required.

| Severity | Total findings | Open | Mitigated |
|---|---:|---:|---:|
| Critical | 1 | 0 | 1 |
| High | 1 | 0 | 1 |
| Medium | 2 | 0 | 2 |
| Low | 1 | 0 | 1 |
| **Total** | **5** | **0** | **5** |

No finding remains open or unaccepted. This is not a planning handoff: the human approval gate below has not yet been passed.

## 2. Scope of Review

| Document | Status/revision |
|---|---|
| `architecture.md` | SCRUM-193 architecture, updated 2026-11-24 to resolve F-01 through F-05; focused CR-01 durability-policy amendment reviewed below |
| `requirements.md` | SCRUM-193 requirements baseline, fetched 2026-11-24 |
| `.github/standards/TechStack.md` | Approved technologies and local browser application constraints |
| `code-review.md` | SCRUM-193 implementation review; consulted only to scope the CR-01 architecture re-review |
| `.github/standards/AGENT_PROTOCOL.md` | Traceability, human approval, security, and handoff constraints |
| `.github/agents/design-review/skills.md` | Architecture Review, Risk Assessment, Decision Recording |
| `.github/agents/design-review/instructions.md` | Required review workflow and approval gate |

**Review date:** 2026-11-24

### Re-run Review Checklist

- **Requirement coverage:** Rechecked the traceability matrix. FR-01 through FR-07 and NFR-01 through NFR-04 each map to architecture components; no uncovered requirement was found.
- **Component ownership:** Rechecked responsibilities and data ownership. Shelf Service owns domain rules and process inventory; JSON Persistence owns file access; HTTP Handler owns transport mapping. Mutation/read synchronization is now explicit.
- **Security:** Rechecked loopback-only binding, local trust boundary, input/status validation, path handling, safe browser text rendering, no authentication beyond approved scope, and secret-free error reporting. The standardized error contract excludes stack traces and internal file contents.
- **Reliability and error handling:** Rechecked startup, malformed/unreadable data, save/replace/directory-sync failures, validation, duplicate, not-found, malformed HTTP JSON, and occupied-port behavior. Candidate snapshots are not committed on save failure. An explicit unsupported-directory-sync probe permits success only after temporary-file flush and atomic replacement, with a platform-limited durability warning; an actual directory-sync `IOException` after replacement when support was reported remains fail-closed and refuses mutations and inventory reads.
- **Scalability and performance:** Rechecked bounded local inventory operations and concurrent writes. Create and status mutations are serialized through validation, persistence, and commit; reads obtain consistent snapshots. No new scalability finding remains for the single-user scope.
- **Correctness:** Rechecked create, full inventory display, all distinct status transitions, duplicate identity, local persistence, API status/error contracts, and startup behavior against the acceptance criteria. The standardized envelope now removes the API response-shape ambiguity.
- **Testability:** Rechecked injected temporary paths and ephemeral ports and the new deterministic persistence I/O seam. The manual responsive smoke contract now defines concrete viewports and interactions without introducing unapproved test technology.

All seven Architecture Review categories were checked. Findings F-01 through F-05 are mitigated by the architecture updates listed below; no additional finding was identified in this checklist rerun.

## 3. Findings Register

| ID | Component or data flow reference | Requirement IDs affected | Category | Impact | Likelihood | Severity | Status |
|---|---|---|---|---|---|---|---|
| F-01 | Section 4 — Shelf Service; Section 6 — create/status-change flows | FR-06, NFR-02 | Reliability / concurrency | High | Medium | Critical | Mitigated — serialized mutation critical section documented |
| F-02 | Section 5 — Persistence Interface; Section 6 — persistence flows; Section 8 — Persistence behavior | FR-06, NFR-02 | Reliability / crash handling | High | Low | High | Mitigated — file flush and directory sync durability documented |
| F-03 | Section 4 — JSON Persistence/Automated Test Suite; Section 8 — Persistence behavior | NFR-02, NFR-04 | Testability / error injection | Medium | Medium | Medium | Mitigated — injected I/O test seam documented |
| F-04 | Section 4 — Browser UI/Automated Test Suite; Section 9 — Risks | NFR-03, FR-05 | Testability / responsiveness | Medium | Medium | Medium | Mitigated — viewport smoke matrix documented |
| F-05 | Section 5 — HTTP API Routes and HTTP Error Response Contract | FR-01, FR-03, FR-05 | Correctness / interface contract | Low | Medium | Low | Mitigated — standardized error envelope documented |

## 4. Finding Details

### F-01 — Concurrent create/status requests can lose updates

- **Description:** Concurrent mutations could previously validate and persist competing candidate snapshots based on the same inventory.
- **Risk:** A create or status change could be lost, violating persistence and inventory correctness.
- **Evidence:** Original architecture Sections 4–6.
- **Impact/Likelihood/Severity:** High / Medium / Critical.
- **Decision and resolution:** Mitigate with serialized mutations. Architecture Section 4 now defines one service-level mutation lock spanning validation, candidate creation, persistence, and in-memory commit for create and status-change operations. Reads obtain a consistent inventory snapshot under the same synchronization rule. Section 6 reflects the serialized flows.

### F-02 — Atomic replacement did not explicitly define crash durability

- **Description:** Atomic replacement alone did not specify when file contents and the rename were durably recorded.
- **Risk:** A success response could imply persistence durability that was not established.
- **Evidence:** Original architecture Sections 5, 6, and 8.
- **Impact/Likelihood/Severity:** High / Low / High.
- **Decision and resolution:** Mitigate with file flush, atomic replacement, and an explicit platform capability distinction. Architecture Sections 5, 6, and 8 require `FileChannel.force(true)` for the temporary file and atomic replacement. If the capability probe reports parent-directory sync supported, sync must succeed before reporting success. If the probe explicitly reports unsupported (including the Windows limitation under review), success may be reported only after file flush and atomic replacement, with the `PLATFORM_LIMITED_DURABILITY` warning exposed and no claim of full crash durability. A probe error other than explicit unsupported, file-flush failure, or replacement failure remains a save error. If a supported-platform directory sync throws an actual `IOException` after replacement, the service fails closed against further mutations and inventory reads until restart and successful reload; it does not report durable success.

### F-03 — Persistence failure testing lacked injectable I/O behavior

- **Description:** Host filesystem permissions and platform behavior are not a deterministic way to exercise every persistence failure branch.
- **Risk:** Required NFR-02 failure cases could remain untested or depend on the machine running the suite.
- **Evidence:** Original architecture Sections 4, 6, and 8.
- **Impact/Likelihood/Severity:** Medium / Medium / Medium.
- **Decision and resolution:** Mitigate by adding an I/O test seam. Architecture Sections 4 and 8 define a narrow injected Java I/O interface using the approved standard library, with deterministic tests for read, file-flush, atomic-replacement, and directory-sync failures; temporary files cover malformed JSON. Candidate non-commit and post-replacement failure behavior are covered.

### F-04 — Responsiveness had no concrete viewport or test contract

- **Description:** “Wide” and “narrow” viewports did not define concrete sizes or required interactions.
- **Risk:** NFR-03 could be verified inconsistently, missing clipping or unusable controls.
- **Evidence:** Original architecture Sections 4, 8, and 9.
- **Impact/Likelihood/Severity:** Medium / Medium / Medium.
- **Decision and resolution:** Mitigate by defining a viewport smoke matrix. Architecture Section 4 specifies a manual modern-browser check at 1280 × 800 and 375 × 667, with inventory, create, status-change, and invalid-field feedback checks at both sizes; no clipped or overlapped content/controls and no horizontal page scrolling to reach/use controls.

### F-05 — HTTP error JSON shape was not fully standardized

- **Description:** Routes identified statuses and field keys but did not define one complete reusable error envelope.
- **Risk:** Browser handling and tests could parse inconsistent API failures.
- **Evidence:** Original architecture Section 5.
- **Impact/Likelihood/Severity:** Low / Medium / Low.
- **Decision and resolution:** Mitigate by standardizing HTTP error JSON. Architecture Section 5 now defines the stable shape `{ "error": { "code": "...", "message": "...", "fieldErrors": {} } }`, the supported codes, HTTP status mappings, always-present `fieldErrors`, and exclusion of sensitive implementation details.

## 5. Agreed Design Decisions

| Finding | Decision and exact mitigation | Rationale | User who agreed | Date |
|---|---|---|---|---|
| F-01 | **Mitigate:** Serialize create and status-change mutations under one service-level lock spanning validation, candidate snapshot creation, persistence save, and in-memory commit; synchronize inventory reads to return consistent snapshots. | Prevent concurrent requests from losing updates or observing inconsistent state. | User (requesting human) | 2026-11-24 |
| F-02 / CR-01 architecture update | **Mitigate with an explicit limitation:** Flush the temporary file using Java NIO `FileChannel.force(true)` and atomically replace the runtime file. Require parent-directory sync success when the capability probe reports support. When it explicitly reports unsupported, permit success only after those preceding operations and expose `PLATFORM_LIMITED_DURABILITY`. An actual sync `IOException` after replacement on a platform reported as supporting sync remains a save failure and fail-closed state. | Preserve usable saves on Windows where directory sync is unsupported without claiming full crash durability; retain fail-closed behavior for an operational sync failure after replacement. | User (requesting human) | 2026-11-24 |
| F-03 | **Mitigate:** Add a narrow injected Java I/O test seam and deterministic tests for read, file-flush, replacement, and directory-sync failures; use temporary files for malformed JSON and verify candidate state is not committed. | Make persistence-error tests repeatable across operating systems without relying on permission manipulation or real user data. | User (requesting human) | 2026-11-24 |
| F-04 | **Mitigate:** Define a manual modern-browser viewport smoke matrix at 1280 × 800 and 375 × 667, running inventory, create, status-change, and invalid-field checks at both sizes and verifying no clipping/overlap. | Make the NFR-03 smoke check concrete without adding an unapproved browser automation dependency. | User (requesting human) | 2026-11-24 |
| F-05 | **Mitigate:** Standardize all API errors on `{ "error": { "code": string, "message": string, "fieldErrors": object } }`, always include `fieldErrors`, document error codes/status mappings, and exclude stack traces/secrets/internal file contents. | Provide a stable contract for Browser UI behavior and tests while preserving safe, user-facing errors. | User (requesting human) | 2026-11-24 |

## 6. Proposed Mitigations

No mitigations remain proposed or pending. The F-02 / CR-01 architecture decision is reflected in `architecture.md`, using only approved Java 17/NIO, Jackson, JUnit/Mockito, plain browser UI, and manual smoke-check approaches. No new technology was introduced. The implementation finding CR-01 in `code-review.md` remains open pending implementation and verification; this focused architecture re-review does not resolve that code finding.

## 7. Architecture Update Actions

| Finding | Sections changed | Result |
|---|---|---|
| F-01 | Section 4 — Shelf Service; Section 6 — main create/status-change flows | Serialized mutation critical section and consistent reads documented. |
| F-02 / CR-01 architecture update | Sections 2, 5, 6, 8, 9, and Change Log | Distinguished explicit unsupported-sync capability from operational sync failure; documented the warning-bearing success path and preserved supported-sync success and fail-closed behavior. |
| F-03 | Section 4 — JSON Persistence/Automated Test Suite; Section 8 — persistence behavior | Injected I/O seam and deterministic failure coverage documented. |
| F-04 | Section 4 — Browser UI/Automated Test Suite; Section 9 — risks | Concrete manual viewport matrix and acceptance checks documented. |
| F-05 | Section 5 — HTTP API Routes and new HTTP Error Response Contract | Stable error envelope, codes, field-error semantics, and safe response rules documented. |

`architecture.md` ends with a Change Log entry for each finding ID. No source code or tests were edited.

## 8. Accepted Risks

**Accepted design trade-off (F-02 / CR-01 architecture update):** When the capability probe explicitly reports parent-directory sync unsupported, the application may complete the save after successful temporary-file flush and atomic replacement, but cannot guarantee crash durability of the directory entry. The application must expose and record `PLATFORM_LIMITED_DURABILITY`; it must not claim full crash durability. This is the user's explicit decision dated 2026-11-24. This acceptance does not apply to a probe error or an actual post-replacement sync `IOException` when support was reported; those remain failures, with the latter fail-closed.

## 9. Open Questions

No design questions remain from F-01 through F-05. The only remaining gate is explicit user approval of this `design-review.md` and the updated `architecture.md`.

## 10. Targeted CR-01 Architecture Change Re-review

**Scope:** Only the architecture change responding to implementation finding CR-01 in `code-review.md`; this is not a code review and makes no implementation changes.

**Reviewed:** `architecture.md` Sections 2, 5, 6, 8, 9 and its Change Log, checked against FR-06/NFR-02 in `requirements.md`, the F-02 decision recorded above, and approved `.github/standards/TechStack.md`. `code-review.md` was used to identify the Windows runtime failure being addressed.

**User decision reviewed (2026-11-24):** Unsupported Windows parent-directory sync after successful temporary-file flush and atomic replacement may return success with an explicit platform-limited durability warning; successful sync remains required when supported; an actual sync `IOException` after replacement remains fail-closed.

| Check | Result |
|---|---|
| Explicit unsupported capability is distinct from an operational sync failure | Covered: the persistence contract permits skipping sync only for an explicit unsupported probe; unrelated probe errors remain failures. |
| File contents are flushed and target is atomically replaced before warning-bearing success | Covered in the persistence interface and create/status flows. |
| Supported sync success remains a precondition for ordinary success | Covered in Sections 5 and 8. |
| Actual post-replacement sync `IOException` when support was reported | Covered as save failure, no candidate commit, and fail-closed reads/mutations until restart and successful reload. |
| User-visible/recorded warning and honest durability claim | Covered through the response warning, Browser UI display, logging, and explicit limitation on crash durability. |
| Requirement/design compatibility | No conflict found with FR-06/NFR-02 or the earlier F-02 rationale. An explicitly unsupported directory-sync capability is not treated as malformed, unreadable, or unwritable data; an actual write/sync failure is still surfaced as failure. |

**Findings:** None for the targeted architecture change. The architecture Change Log already records the CR-01 decision and its affected contracts; no further architecture edit is required by this re-review.

**Focused review status: Approved with conditions.** Conditions: retain the explicit warning and no-full-durability claim; preserve supported-sync success and actual post-replacement sync-`IOException` fail-closed semantics; implementation and tests must demonstrate both the unsupported-probe success path and supported-probe sync-failure path. The code-review finding CR-01 remains open until implementation and verification.

## 11. Approval Gate

**Current status:** Awaiting explicit user approval. The review and updated architecture are presented for approval; do not proceed to planning unless and until the user approves them. No planning handoff has been made.

**Implementation gate:** Do not begin implementation of CR-01 until the user explicitly approves this focused architecture re-review and the updated architecture. No implementation work has been started by this review.
