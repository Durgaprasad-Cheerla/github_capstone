# Requirements Agent Skills

## Skill 1: Story Ingestion

### Purpose
Ingest the user story and its acceptance criteria from an explicitly provided Jira/Confluence reference or user-pasted text.

### Inputs
- Jira ticket ID (for example `PROJ-123`) or Jira ticket URL
- Confluence page URL
- User-pasted story text, including acceptance criteria and relevant context

### Procedure
1. Determine whether the user supplied a Jira ID, Jira URL, Confluence URL, or pasted story text.
2. For Jira/Confluence references only, call the matching MCP tool with the exact ID or URL supplied. Do not modify it.
3. For pasted text, use only the content supplied in the conversation; do not make an MCP or external fetch.
4. Capture available title, description, acceptance criteria, status, and linked references without inventing missing details.
5. Record the source type and provenance. For a fetched story, include the ID/URL and fetch date. For pasted text, record "User-provided pasted story" and do not invent an ID, URL, or fetch date.
6. If an MCP result is an error or "Not Found", stop. Report the exact message and ask for a corrected reference or pasted content.

### Output
- Story title, description, acceptance criteria, and linked references when provided
- Source metadata (type and provenance; ID/URL and fetch date only when fetched)

### Quality Checks
- Retrieved content matches the exact reference provided, or pasted content is faithfully represented.
- No external fetch is made when the user supplied pasted content.
- No credentials or tokens appear in the output.

---

## Skill 2: Gap Analysis

### Purpose
Identify what the story leaves undefined before requirements are written.

### Inputs
- Output of Story Ingestion

### Procedure
1. Check each relevant area: functional behaviour, acceptance criteria, input validation, error handling, authentication only when in scope, secrets, data limits, performance, concurrency, persistence, and security.
2. For each gap, write one focused question. Group the questions by area.
3. Ask the questions in batches of no more than five. Wait for answers.
4. Record each answer with the question it resolves. If the user has no answer, mark the item as "Not Found".

### Output
- Gap list grouped by area
- Recorded answers and "Not Found" items

### Quality Checks
- Every gap is either answered, marked "Not Found", or moved to Out of Scope with the user's agreement.

---

## Skill 3: Requirements Drafting

### Purpose
Write `requirements.md` from the fetched story and the recorded answers.

### Inputs
- Story Ingestion output
- Gap Analysis answers

### Procedure
1. Write the Project Overview & Objective in one short paragraph.
2. Fill in User Story Details with the source metadata.
3. Convert each agreed behaviour into an FR-xx entry. Give each one an acceptance criterion that a test can check.
4. Convert each agreed quality constraint into an NFR-xx entry, with a measurable target where the user gave one.
5. List edge cases and their expected handling under Edge Cases & Error Handling.
6. List excluded items under Out of Scope.
7. Carry any unresolved item into Open Questions.

### Output
`requirements.md` in the required structure.

### Quality Checks
- Each FR-xx and NFR-xx has a source reference and an acceptance criterion.
- No requirement uses vague terms such as "fast" or "secure" without a measurable target.
- Open Questions is empty, or every item is marked "Not Found".