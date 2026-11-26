# SCRUM-219 Implementation Plan — Personal Reading Shelf Manager

## Intro

### Scope (what is included)
This plan covers Epic **SCRUM-219** changes for the **Personal Reading Shelf Manager** on base branch **`github_capstone_execution`**, implementing the following approved Jira Tasks and subtasks:

- **SCRUM-223** (Service normalization) + **SCRUM-224**
- **SCRUM-226** (Handler: bounded body read + validation) + **SCRUM-227**, **SCRUM-228**
- **SCRUM-229** (Tests) + **SCRUM-230**, **SCRUM-231**
- **SCRUM-220** (UI feedback persistence) + **SCRUM-221**, **SCRUM-222**
- **SCRUM-225** (Docs updates to `requirements.md` FR-04)

Baseline: **SCRUM-193 implemented on `github_capstone_execution`**; **74 tests passing** per repository verification.

### Out of scope (explicitly excluded)
- CR-03 / NFR-03 browser and viewport smoke evidence
- Delete / edit / search features
- Changing unknown API routes behavior (e.g., INTERNAL_ERROR with 404)
- Security headers
- New frameworks, new test frameworks, browser automation

### Decisions already made (requirements locked)
- **Title max length:** 100 characters **after trimming**
- **Author max length:** 50 characters **after trimming**
- **Oversize body response:** **413 PAYLOAD_TOO_LARGE** (in the existing error envelope)
- **Storage rule:** store **trimmed** values; keep **first-entered casing** (post-trim)
- **Identity compare rule:** compare using `toLowerCase(Locale.ROOT)` on **both sides at read time**, **no migration**

### Git & PR conventions (for later implementation PRs)
> This plan file is intended for a docs-only PR. Implementation work should follow these conventions.

- **Plan branch:** `docs/SCRUM-219-implementation-plan` (from `github_capstone_execution`)
- **Feature branch (later code work):** `feature/SCRUM-219-<short-description>` (exact name decided by implementer; not created by this plan)
- **Commit format:** `<type>(<JIRA_KEY>): <description>`
  - Examples:  
    - `feat(SCRUM-223): normalize identity matching for title/author`  
    - `fix(SCRUM-226): cap request body at 1MB and return 413`  
    - `test(SCRUM-229): add handler validation boundary tests`  
    - `docs(SCRUM-225): update FR-04 duplicate definition`
- **Review checklist**
  - No new frameworks or dependencies.
  - All **74 existing tests still pass**, plus new tests pass.
  - Service: identity normalization is applied consistently for duplicate detection and status lookup.
  - Handler: `readJsonBody` uses a bounded read loop (does not rely on `available()` and does not rely on `Content-Length`).
  - Validation: trim + whitespace-only + max length rules enforced with exact error messages and field names.
  - UI: feedback persistence works only for reload triggered by `changeStatus()`; other loads clear feedback; sequential changes replace message.

---

## Stage 1 — Service normalization (SCRUM-223; subtask SCRUM-224)

### Jira keys
- SCRUM-223
- SCRUM-224

### Goal
Implement trim + case-insensitive identity matching for:
- duplicate detection when adding a book
- status lookup (and any service-level lookup used by status change)

While ensuring:
- stored record keeps **first-entered casing** (but trimmed)
- comparisons normalize **both persisted and request values** at comparison time (no migration)

### Files to change
- `src/main/java/org/example/reading/service/ShelfService.java`
- `src/main/java/org/example/reading/shelf/Book.java` (only if needed to ensure stored title/author are trimmed at write time; do not change public API lightly)
- `src/main/java/org/example/reading/persistence/JsonReadingShelfPersistence.java` (only if trimming must be guaranteed at persistence boundary; otherwise keep logic in service)

### Tasks (in order)
1. **Confirm current service responsibilities** (to confirm during implementation)
   - Where duplicate detection lives in `ShelfService`.
   - Where status lookup / status change resolves a book (by title/author) in `ShelfService`.
   - Whether persistence returns `Book` objects exactly as stored (including whitespace) and whether service currently trusts those values.
2. **Add normalization helpers** in `ShelfService` (SCRUM-224)
   - `normalizeForStorage(String input)`:
     - trims leading/trailing whitespace
     - **does not** change case
     - null-handling: **to confirm during implementation** (do not invent; align with existing contract)
   - `normalizeForIdentity(String input)`:
     - trim + `toLowerCase(Locale.ROOT)`
     - null-handling: **to confirm during implementation**
3. **Apply normalization in add/duplicate detection** (SCRUM-223)
   - When checking if a new book is a duplicate, compare by normalized identity for both title and author.
   - When storing a newly-added book:
     - store title/author using `normalizeForStorage` (trim)
     - preserve the user’s first-entered casing (post-trim)
4. **Apply normalization in status lookup** (SCRUM-223)
   - Ensure lookups by title/author succeed for inputs like `"DUNE "` and `" frank herbert"`.
   - Ensure comparisons normalize both the request values and the existing stored values at lookup time.
5. **Confirm “no migration” behavior** (SCRUM-223)
   - Ensure code does not rewrite all existing persisted values on startup.
   - Ensure comparison-time normalization makes older untrimmed/mixed-case persisted data usable without a migration step.

### Dependencies
- None (this is the first stage and establishes normalization rules others depend on).

### Risks & mitigation
- **Risk:** Only some code paths use normalization (e.g., duplicate detection normalized but status lookup isn’t).  
  **Mitigation:** Centralize comparisons via helper methods and lock behavior with tests in Stage 4.
- **Risk:** Conflicting responsibilities between service vs persistence (where trimming should happen).  
  **Mitigation:** Prefer service-layer trimming for new writes; keep persistence focused on serialization; only adjust persistence if required to satisfy “stored values trimmed”.
- **Risk:** Null-handling contract unknown for title/author in service methods.  
  **Mitigation:** Confirm existing behavior during implementation; keep behavior consistent and add tests for expected behavior only if requirements state it.

### Verification
- Run unit tests and confirm **all 74 existing tests still pass**:  
  - macOS/Linux: `./mvnw test`  
  - Windows: `.\mvnw.cmd test`

---

## Stage 2 — Bounded body read and 413 PAYLOAD_TOO_LARGE (SCRUM-226; subtask SCRUM-227)

### Jira keys
- SCRUM-226
- SCRUM-227

### Goal
Make request body reading reliable and capped at **1,048,576 bytes (1MB)** even when:
- `Content-Length` is missing
- `Content-Length` is wrong

When the body exceeds the limit:
- Stop reading further
- Return **413 PAYLOAD_TOO_LARGE** using the **existing error envelope** and code

### Files to change
- `src/main/java/org/example/reading/http/ReadingShelfHandler.java` (method `readJsonBody`)

### Tasks (in order)
1. **Inspect current `readJsonBody`** implementation (to confirm during implementation)
   - Confirm whether it currently uses `InputStream.available()` or trusts `Content-Length`.
   - Confirm how it signals parse errors vs other errors and how it builds the error envelope.
2. **Implement a bounded read loop** in `readJsonBody` (SCRUM-227)
   - Define `MAX_BODY_BYTES = 1_048_576`.
   - Read from request body stream in chunks into a buffer, tracking total bytes.
   - If total bytes would exceed max:
     - stop / cut off reading
     - return/throw a handler-level signal that results in **413** with existing error envelope
   - Ensure behavior does not rely on `available()` and does not rely on `Content-Length`.
3. **Wire 413 response path** into existing handler error handling (SCRUM-227)
   - Use the **existing error envelope** shape and existing error code for payload too large (`PAYLOAD_TOO_LARGE` per AC).
   - Ensure this error occurs before attempting JSON parsing (avoid parsing partial JSON).
4. **Confirm no changes to routing/ports** (non-goal)
   - Server remains loopback-only; no port changes are introduced by this stage.

### Dependencies
- Stage 1 not required for this stage (they can be independent), but overall epic order keeps Stage 1 first.
- Stage 4 tests depend on this stage.

### Risks & mitigation
- **Risk:** Off-by-one or “exactly 1MB” boundary mishandled.  
  **Mitigation:** Implement precise condition: accept up to exactly 1,048,576 bytes; reject at 1,048,577+; add tests in Stage 4.
- **Risk:** Incorrect envelope or status code returned due to mismatch with existing error handling.  
  **Mitigation:** Reuse existing error response construction in handler; add handler tests asserting status and error code.
- **Risk:** Performance/memory issues if implementation reads entire body even when too large.  
  **Mitigation:** Stop reading as soon as limit is exceeded and immediately return 413.

### Verification
- Run unit tests and confirm **all 74 existing tests still pass**:  
  - macOS/Linux: `./mvnw test`  
  - Windows: `.\mvnw.cmd test`

---

## Stage 3 — Title/author trim, whitespace-only and max-length validation (SCRUM-226; subtask SCRUM-228)

### Jira keys
- SCRUM-226
- SCRUM-228

### Goal
Enforce request validation rules in the handler for title/author **after trimming**:
- title length: **1..100**
- author length: **1..50**
- whitespace-only is invalid (treated as empty after trimming)

On validation failure:
- Return **400** with error code **VALIDATION_ERROR**
- Include a `fieldError` indicating the field and message:
  - `"title must be between 1 and 100 characters"`
  - `"author must be between 1 and 50 characters"`

Boundary acceptance:
- exactly 100/50 accepted
- 101/51 rejected

This story owns the empty/whitespace-only rule.

### Files to change
- `src/main/java/org/example/reading/http/ReadingShelfHandler.java`

### Tasks (in order)
1. **Confirm request DTO shape and parsing flow** (to confirm during implementation)
   - Where title/author come from after JSON parsing.
   - How handler currently reports validation errors (if any).
   - Whether handler already trims inputs anywhere.
2. **Trim for validation** (SCRUM-228)
   - After JSON parse, compute trimmed values:
     - `titleTrimmed = title.trim()`
     - `authorTrimmed = author.trim()`
   - Null-handling: **to confirm during implementation** (e.g., if null fields can occur in parsed objects, decide whether to treat as empty or reject; must align with existing behavior and requirements).
3. **Validate lengths and whitespace-only** (SCRUM-228)
   - Reject if trimmed length is 0 (whitespace-only or empty)
   - Reject if title trimmed length > 100
   - Reject if author trimmed length > 50
   - Use exact messages from AC.
4. **Return correct error envelope** (SCRUM-228)
   - HTTP status 400
   - error code `VALIDATION_ERROR`
   - `fieldError.field` must be `"title"` or `"author"` accordingly
   - Ensure only the relevant field error is returned per failing input (to confirm current envelope capabilities during implementation).
5. **Ensure service still stores trimmed values** (alignment check)
   - Handler validation trims for validation; service trims for storage in Stage 1.
   - Do not double-trim in a way that changes behavior; trimming twice should be harmless but ensure casing preservation is still satisfied (service rule is authoritative).

### Dependencies
- Depends on Stage 2: body must be safely read and parsed before validation.
- Depends on Stage 1 alignment: handler trimming/validation should be compatible with service trimming/storage rules.
- Stage 4 tests depend on this stage.

### Risks & mitigation
- **Risk:** Validation message text or field name does not exactly match AC.  
  **Mitigation:** Use constants for messages; assert exact values in `ReadingShelfHandlerTest` in Stage 4.
- **Risk:** Unknown null-handling contract for title/author.  
  **Mitigation:** Confirm current behavior in handler/service during implementation; document final decision in Stage 6 requirements updates only if it changes FR-04 scope (avoid scope creep).
- **Risk:** Validation applied in wrong endpoint(s) or wrong time (e.g., before trim).  
  **Mitigation:** Validate only after trim; add boundary tests for 100/101 and 50/51 and whitespace-only.

### Verification
- Run unit tests and confirm **all 74 existing tests still pass**:  
  - macOS/Linux: `./mvnw test`  
  - Windows: `.\mvnw.cmd test`

---

## Stage 4 — Tests (SCRUM-229; subtasks SCRUM-230 ShelfService, SCRUM-231 ReadingShelfHandler)

### Jira keys
- SCRUM-229
- SCRUM-230
- SCRUM-231

### Goal
Lock in behavior for:
- Service normalization rules (trim + case-insensitive identity; first-entered casing retained; pre-existing persisted data normalized at comparison time)
- Handler bounded body read / 413
- Handler trim + validation boundaries for title/author

Constraints:
- **Unit tests only**
- Modify only the two existing test classes
- Use existing stack: **JUnit 5 + Mockito** (no new frameworks)

### Files to change
- `src/test/java/org/example/reading/service/ShelfServiceTest.java`
- `src/test/java/org/example/reading/http/ReadingShelfHandlerTest.java`

### Tasks (in order)
1. **Add service normalization tests** (SCRUM-230)
   - Duplicate rejection:
     - Add `"Dune"` by `"Frank Herbert"`
     - Attempt to add `" dune "` by `" frank herbert "` → reject as duplicate
   - First-entered casing retained:
     - Confirm stored record keeps `"Dune"` / `"Frank Herbert"` casing from first entry (post-trim)
   - Stored values trimmed:
     - Confirm stored `Book` title/author have no leading/trailing whitespace after add
   - Pre-existing data behavior without migration:
     - Arrange persistence/service state to include older record with mixed-case/untrimmed title/author (how to arrange depends on current service/persistence APIs; to confirm during implementation).
     - Lookup or status change using differently cased/spaced input succeeds.
2. **Add handler request size tests** (SCRUM-231)
   - Oversize body with correct `Content-Length` → 413 + error code `PAYLOAD_TOO_LARGE`.
   - Oversize body with missing/wrong `Content-Length` → still 413 + error code `PAYLOAD_TOO_LARGE`.
   - Ensure tests verify that error envelope structure matches existing contract (exact envelope fields to confirm during implementation; assert at least HTTP status and error code per AC).
3. **Add handler validation boundary tests** (SCRUM-231)
   - Title length 100 accepted; 101 rejected with:
     - HTTP 400
     - error code `VALIDATION_ERROR`
     - fieldError field `"title"`
     - message `"title must be between 1 and 100 characters"`
   - Author length 50 accepted; 51 rejected with analogous assertions for `"author"`.
   - Whitespace-only title or author rejected with same message and field.
4. **Run full suite and keep tests stable**
   - Ensure tests are deterministic and do not rely on fixed ports.
   - Ensure tests do not require a browser and do not implement any JS harness.

### Dependencies
- Depends on Stage 1 (service behavior exists).
- Depends on Stage 2 and Stage 3 (handler behavior exists).

### Risks & mitigation
- **Risk:** Tests become brittle due to tight coupling to internal implementation (e.g., exact JSON serialization or internal exception types).  
  **Mitigation:** Assert externally observable behavior: HTTP status, error code, field name, and message; avoid asserting incidental implementation details.
- **Risk:** Difficulty constructing “pre-existing persisted untrimmed data” scenario.  
  **Mitigation:** Use `JsonReadingShelfPersistence` and/or service setup hooks if available; if not, seed in-memory state in the most direct supported way (to confirm during implementation).
- **Risk:** Handler tests might currently rely on assumptions about `readJsonBody`.  
  **Mitigation:** Update tests to target the handler’s public behavior rather than helper internals where possible.

### Verification
- Run unit tests and confirm **all 74 existing tests still pass**, plus new tests pass:  
  - macOS/Linux: `./mvnw test`  
  - Windows: `.\mvnw.cmd test`

---

## Stage 5 — UI feedback persistence (SCRUM-220; subtasks SCRUM-221 app.js, SCRUM-222 manual verification steps)

### Jira keys
- SCRUM-220
- SCRUM-221
- SCRUM-222

### Goal
In the UI:
- After `changeStatus()` and the subsequent `loadInventory()` finish, the **“status changed” confirmation** and any **PLATFORM_LIMITED_DURABILITY** warning remain visible.
- Only a reload triggered from `changeStatus()` preserves feedback.
- Initial/other inventory loads clear the feedback.
- Sequential status changes **replace** the message; messages do not accumulate.

### Files to change
- `src/main/resources/web/app.js`
- `src/main/resources/web/index.html` (only if needed for stable message/warning container elements; prefer minimal/no changes)

### Tasks (in order)
1. **Confirm current feedback rendering and clearing behavior** (to confirm during implementation)
   - Where “status changed” message is set.
   - Where PLATFORM_LIMITED_DURABILITY warning is set/cleared.
   - Whether `loadInventory()` currently clears feedback internally or callers clear it.
2. **Adjust `loadInventory` API or caller behavior** (SCRUM-221)
   - Implement one of:
     - `loadInventory({ preserveFeedback: true|false })` (option object), OR
     - move message-clearing responsibility out of `loadInventory()` into callers.
   - Requirement: only `changeStatus()`-triggered reload uses preserve behavior; initial/other loads clear.
3. **Ensure sequential status changes replace message** (SCRUM-220)
   - Use a single message DOM element/state variable; set `.textContent` (or equivalent) rather than appending.
   - Ensure new status change overwrites prior message and warning state, then reload preserves the latest feedback.
4. **Keep UI changes minimal** (guard against scope creep)
   - No browser automation.
   - No new frameworks.

### Dependencies
- Independent of service/handler logic, but best done after Stages 1–4 so backend behavior is stable while verifying UI.

### Risks & mitigation
- **Risk:** `loadInventory()` is used by multiple flows; preserving feedback could leak into other reload paths.  
  **Mitigation:** Default behavior clears feedback; only changeStatus passes an explicit preserve flag.
- **Risk:** Async timing issues cause message to be cleared after being set.  
  **Mitigation:** Set feedback state before calling reload and ensure reload does not clear it when preserve is enabled; test manually following SCRUM-222 steps.

### Verification
- Run unit tests and confirm **all 74 existing tests still pass** (UI changes should not break tests):  
  - macOS/Linux: `./mvnw test`  
  - Windows: `.\mvnw.cmd test`  
- Perform manual UI verification steps (below) in a local run at `http://127.0.0.1:8080/` (final confirmation also repeated in Stage 6).

### SCRUM-222 — Manual UI verification steps (no JS harness; no browser automation)
Perform these steps manually in a browser:

1. **Initial load clears messages**
   - Start the app and open: `http://127.0.0.1:8080/`
   - Confirm that on first inventory load, any “status changed” confirmation message area is empty/cleared.
   - Confirm that PLATFORM_LIMITED_DURABILITY warning area is empty/cleared unless the app explicitly sets it on initial load (to confirm current intended behavior during implementation).

2. **Change status preserves success message after reload**
   - Change the status of a book (use the UI control that triggers `changeStatus()`).
   - Confirm you see a “status changed” confirmation.
   - Confirm the inventory reload occurs (the UI list refreshes).
   - After reload completes, confirm the “status changed” confirmation is **still visible**.

3. **PLATFORM_LIMITED_DURABILITY warning is preserved when applicable**
   - Trigger a status change that causes a PLATFORM_LIMITED_DURABILITY warning (if there is a known way to do so).
   - Confirm the warning appears.
   - After the reload completes, confirm the warning is **still visible**.

4. **Another reload clears the message**
   - Trigger an inventory reload that is **not** initiated by `changeStatus()` (e.g., a manual refresh button if present; otherwise refresh behavior per the app’s existing UI).
   - Confirm that the “status changed” confirmation and any PLATFORM_LIMITED_DURABILITY warning are **cleared** after this reload.

5. **Sequential status changes replace message (no accumulation)**
   - Perform status change A and observe message A.
   - Perform status change B and observe message B.
   - Confirm message B replaces message A (no stacking/accumulation).

---

## Stage 6 — Docs and final verification (SCRUM-225: revise FR-04 in requirements.md; run the full suite)

### Jira keys
- SCRUM-225

### Goal
- Update `requirements.md` to reflect the finalized duplicate definition and normalization/storage rules.
- Run the final full suite and perform a local manual run at `http://127.0.0.1:8080/`.

Important constraint: **In the plan PR, do not modify** `requirements.md`. This stage describes what to do later during implementation.

### Files to change
- `requirements.md` (repo root)
  - Revise FR-04 at **lines 39–43** (as referenced in the requirement)
  - Revise the repeated FR-04 statement at **line 92** (as referenced in the requirement)

### Tasks (in order)
1. **Revise FR-04 duplicate definition (first occurrence, lines 39–43)** (SCRUM-225)
   - Update text to specify:
     - duplicate identity uses title+author after trimming and case-insensitive comparison (Locale.ROOT lower-casing for identity)
     - status is ignored for duplicates
     - stored values are trimmed; first-entered casing retained
     - no migration; comparisons normalize both persisted and request values at read/compare time
2. **Revise repeated FR-04 statement (line 92)** (SCRUM-225)
   - Ensure it matches the updated definition exactly (avoid divergence).
3. **Final full-suite run**
   - Confirm **all 74 existing tests still pass** (and new tests pass):  
     - macOS/Linux: `./mvnw test`  
     - Windows: `.\mvnw.cmd test`
4. **Manual local run**
   - Start the application locally (exact command depends on repo; to confirm during implementation).
   - Open and smoke-check the UI at: `http://127.0.0.1:8080/`
   - Re-run the SCRUM-222 manual UI verification steps to confirm UI behavior in a real run.

### Dependencies
- Depends on completion of Stages 1–5 (docs should reflect actual implemented behavior; final verification requires all changes integrated).

### Risks & mitigation
- **Risk:** `requirements.md` updates drift from actual implementation behavior.  
  **Mitigation:** Perform docs changes last; cross-check against tests and observed runtime behavior before updating both FR-04 occurrences.
- **Risk:** Manual run differs from test environment (data file contents, existing books).  
  **Mitigation:** Use a clean local setup and a known small dataset; document any prerequisites discovered during implementation (without expanding scope).

### Verification
- Run unit tests and confirm **all 74 existing tests still pass**:  
  - macOS/Linux: `./mvnw test`  
  - Windows: `.\mvnw.cmd test`  
- Perform manual local run at `http://127.0.0.1:8080/`.

---

## Open questions (to confirm during implementation; do not guess)

1. **Null-handling contract:** Are `title` or `author` allowed to be `null` in incoming JSON or service calls? If they can be null, should validation treat null as empty (and return VALIDATION_ERROR) or is it already rejected earlier?
2. **Existing error envelope contract:** What are the exact JSON fields for the error envelope and `fieldError` structure used by `ReadingShelfHandler` today (so tests can assert correctly without overfitting)?
3. **`readJsonBody` current behavior:** Does it currently read the full body into memory, and how does it handle malformed JSON vs IO exceptions? (Needed to integrate 413 behavior cleanly.)
4. **How to seed “pre-existing persisted untrimmed/mixed-case data” in tests:** What is the cleanest supported approach in the current codebase—via `JsonReadingShelfPersistence`, via service APIs, or via direct construction of in-memory shelf state?
5. **UI reload triggers:** Besides `changeStatus()`, what other user actions call `loadInventory()` (e.g., initial page load, manual refresh button)? This affects how we validate “only changeStatus-triggered reload preserves feedback.”
