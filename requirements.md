# Requirements

## 1. Project Overview & Objective

Provide a lightweight local web application for an avid reader to record a personal book inventory, see each book's reading status, and update progress. The application must retain books and status changes across restarts and provide clear feedback for validation and user actions.

## 2. User Story Details

- **Source type:** Jira issue
- **Source:** [SCRUM-193](https://durgaprasad675106.atlassian.net/browse/SCRUM-193)
- **Fetched:** 2026-11-24
- **Title:** Personal Reading Shelf Manager
- **Status when fetched:** To Do
- **Story:** As an avid reader, I want a dedicated digital space to organize my personal book collection and track my reading progress, so that I can easily manage what I plan to read, what I am currently reading, and what I have finished.
- **Acceptance criteria captured:** create books with title, author, and an initial status; display the complete inventory; update status; save data locally across restarts; start locally and use it in a browser; validate input and provide clear feedback.
- **Linked references:** None reported by the Jira issue.
- **Clarification answers:** User answers supplied for this requirements draft on 2026-11-24. They define allowed status transitions, duplicate handling, title/author validation, and accepted architecture-level decisions for persistence location and persistence-error behavior.

## 3. Functional Requirements

### FR-01 — Create a book

- **Requirement:** A user can add a book with a title, author, and initial status selected from **To Read**, **Reading**, or **Finished**.
- **Source:** SCRUM-193, Acceptance Criteria 1.
- **Acceptance criterion:** Submitting a valid title, author, and one of the three allowed statuses adds a book with those values to the inventory. A title or author that is empty or consists only of whitespace is rejected, the incomplete record is not added, and a clear validation message identifies the invalid field. When creating a book, the title (at most 100 characters) and the author (at most 50 characters) are measured after trimming; a value that is empty after trimming, or longer than its limit, is rejected with a field-specific validation error (HTTP 400). Status changes apply no length limit, so previously saved books remain updatable. Exact message wording is not prescribed.

### FR-02 — Display the complete inventory

- **Requirement:** The dashboard displays the user's complete book inventory, including each book's title, author, and current reading status.
- **Source:** SCRUM-193, Acceptance Criteria 2; Acceptance Criteria 1 identifies the book fields.
- **Acceptance criterion:** After adding books with different values and statuses, the dashboard shows every added entry and its title, author, and current status.

### FR-03 — Change a book's status

- **Requirement:** A user can change a book's status directly between any pair of the three statuses, in either direction. No transition sequence is required.
- **Source:** SCRUM-193, Acceptance Criteria 2; user answer 1.
- **Acceptance criterion:** For a book, verify each ordered change between distinct statuses (To Read, Reading, Finished). Each selected status is accepted and displayed as the book's current status, including transitions back to a prior status.

### FR-04 — Prevent duplicate inventory entries

- **Requirement:** The application prevents duplicate entries. A duplicate is defined as an entry whose title and author, after trimming leading and trailing whitespace and comparing case-insensitively (lower-casing with Locale.ROOT), equal those of an existing entry; the reading status is ignored when determining duplicates. New entries are stored with trimmed title and author, keeping the casing of the first entry. Existing saved entries are not migrated; they are normalized the same way at comparison time. The same trimmed, case-insensitive comparison identifies the book whose status is changed. If previously saved data contains several entries that match one another after this normalization, a status change applies to the first matching entry in stored order.
- **Source:** User answer 2, revised by the explicit duplicate-prevention decision for SCRUM-193.
- **Acceptance criterion:** Given an existing entry with a title and author, attempting to add another entry whose title and author match after trimming and case-insensitive comparison (for example " dune " and "Dune") is rejected even when its status differs from the existing entry. The duplicate is not added, and the inventory remains unchanged.

### FR-05 — Provide action feedback

- **Requirement:** The application provides clear feedback for validation and user actions.
- **Source:** SCRUM-193, Acceptance Criteria 5; user answer 4 clarifies field-specific validation messages.
- **Acceptance criterion:** An invalid title or author produces a clear, field-specific validation message. After a valid create or status-change action, the resulting entry or status is visibly reflected in the dashboard. A create or status-change request body larger than 1,048,576 bytes is rejected with HTTP 413 (PAYLOAD_TOO_LARGE) and a clear message. No exact message wording is prescribed.

### FR-06 — Persist inventory and status changes

- **Requirement:** Added books and status updates are saved locally and remain available after the application is closed and restarted. The specific persistence location is not specified in this requirements baseline.
- **Source:** SCRUM-193, Acceptance Criteria 3; user answer 3.
- **Acceptance criterion:** Add books, change a status, stop and restart the application, and verify that the same entries and latest statuses are available. The Architecture Agent defines the data location within `.github/standards/TechStack.md`; this requirement does not prescribe a path.

### FR-07 — Start and access the application locally

- **Requirement:** The application can be started using a documented simple command and accessed through a browser at the local URL specified by the approved technology standards.
- **Source:** SCRUM-193, Acceptance Criteria 4; `.github/standards/TechStack.md`, “Local Browser Application Profile.”
- **Acceptance criterion:** Following the documented startup command starts the application and its interface loads in a browser at `http://127.0.0.1:8080/`, when the configured port is available.

## 4. Non-Functional Requirements

### NFR-01 — Local-only network binding

- **Requirement:** The local web server binds to the loopback interface only; it must not expose the application on a non-loopback network interface.
- **Source:** `.github/standards/TechStack.md`, “Local Browser Application Profile”; SCRUM-193, Acceptance Criteria 4 requires local access.
- **Acceptance criterion:** When the application is running, verify that its listener is bound to `127.0.0.1` and not to `0.0.0.0` or another non-loopback interface.

### NFR-02 — Explicit persistence-error reporting

- **Requirement:** Malformed, unreadable, or unwritable persistence data must be handled explicitly. Exact recovery behavior and message content are not specified here and are for the Architecture Agent to define within the approved explicit-error-handling constraint.
- **Source:** User answer 5; `.github/standards/TechStack.md`, “Local Browser Application Profile.”
- **Acceptance criterion:** Exercise malformed, unreadable, and unwritable persistence conditions separately. In each case, verify that the failure is explicitly surfaced and is not reported as a successful load or save. Architecture must define and test the specific recovery behavior.

### NFR-03 — Responsive browser interface

- **Requirement:** The application provides the responsive web interface described in the story's business objectives and is usable in a modern browser.
- **Source:** SCRUM-193, Business Value & Objectives (“Personal Organization” specifies a lightweight, responsive web interface; “Accessibility” specifies access from any modern browser).
- **Acceptance criterion:** At both a wide and a narrow viewport in a modern browser, verify that inventory content and book-entry/status controls are not clipped or overlapped and that a user can add a book and change its status. The specific browser support matrix and viewport sizes are not prescribed by the story.

### NFR-04 — Isolated automated tests

- **Requirement:** Tests that exercise persistence and the local server must be able to use a temporary data path and an ephemeral server port, rather than the user's data file or a dependency on port 8080.
- **Source:** `.github/standards/TechStack.md`, “Local Browser Application Profile.”
- **Acceptance criterion:** Run the persistence and server tests using a temporary data location and an ephemeral port; verify they do not read or modify the user's runtime data and do not require port 8080.

## 5. Edge Cases & Error Handling

- Empty and whitespace-only title and author values are rejected separately with clear, field-specific validation feedback. (FR-01; SCRUM-193, Acceptance Criteria 1; user answer 4.)
- Submitting an entry whose title and author match an existing entry after trimming and case-insensitive comparison (Locale.ROOT) is rejected as a duplicate, regardless of the submitted status. Status does not participate in duplicate detection. New entries are stored trimmed with the casing of the first entry; existing saved entries are not migrated. (FR-04; explicit user decision.)
- On create, title is limited to 100 characters and author to 50 characters after trimming; status changes apply no length limit. (FR-01; explicit user decision.)
- Create and status-change request bodies over 1,048,576 bytes are rejected with HTTP 413. (FR-05; explicit user decision.)
- Status changes may go directly between any distinct statuses, including returning to a previous status. (FR-03; user answer 1.)
- Malformed, unreadable, and unwritable persistence data must produce explicit error handling. The precise user-facing behavior and recovery strategy are **Not Found** and are to be defined by the Architecture Agent within `.github/standards/TechStack.md` constraints. (NFR-02; user answer 5.)
- The persistence location is **Not Found** and is to be defined by the Architecture Agent within `.github/standards/TechStack.md` constraints. No path or other storage-location implementation assumption is made here. (FR-06; user answer 3.)
- If the default local port is occupied, the approved technology standards require a clear startup failure rather than silently binding to an arbitrary interface or claiming success. (SCRUM-193, Acceptance Criteria 4; `.github/standards/TechStack.md`, “Local Browser Application Profile.”)

## 6. Out of Scope

- Authentication or network exposure beyond the loopback-only local application profile. Source: `.github/standards/TechStack.md`, “Local Browser Application Profile.”
- Additional book fields, workflow states, or inventory capabilities not specified in SCRUM-193 or the recorded user answers. No specific additional feature is implied by this exclusion.
- Choosing a persistence path or prescribing a persistence-error recovery strategy in this requirements document; these are accepted architecture decisions, not implementation assumptions. Source: user answers 3 and 5.

## 7. Open Questions

None. All identified gaps are answered or accepted as Not Found for architecture.
