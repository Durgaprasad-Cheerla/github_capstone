# Design Review Agent Skills

## Skill 1: Architecture Review

### Purpose
Check `architecture.md` against `requirements.md` and across the review categories.

### Inputs
- `architecture.md`
- `requirements.md`
- `.github/standards/` (if present)

### Procedure
1. **Requirement coverage:** Check that every FR-xx and NFR-xx appears in the traceability matrix and maps to a component. Flag any gap.
2. **Component ownership:** Check that each component has one clear responsibility and no two components own the same data.
3. **Security:** Check secrets handling, authentication only when required by the story, validation of user input and file paths, access boundaries, and safe handling of persisted data. For a loopback-only app, confirm it binds only to localhost and does not imply production-grade authentication.
4. **Reliability & error handling:** Check relevant API failures, timeouts, missing or malformed data, empty inputs, partial failures, persistence failures, and startup errors. For each error path in `architecture.md`, confirm user data is not silently lost or reported as saved when it was not.
5. **Scalability & performance:** Check for bottlenecks, unbounded loops or memory use, and concurrency conflicts, such as two syncs writing the same file.
6. **Correctness:** Check that data flows match the requirements' acceptance criteria and that interfaces define their inputs, outputs, and errors.
7. **Testability:** Check that each component can be tested in isolation, with external dependencies and persistent storage replaceable by test doubles or temporary resources. For a local web app, check that server behavior can be tested without a fixed port.

### Output
A list of candidate findings, each with category, component or data flow reference, and affected requirement IDs.

### Quality Checks
- All seven categories have been checked, and each is either a finding or marked "No issues found".
- Every finding cites a specific part of `architecture.md`.

---

## Skill 2: Risk Assessment

### Purpose
Rate each finding so the user can decide what to fix first.

### Inputs
- Candidate findings from Architecture Review

### Procedure
1. Rate **impact** as High, Medium, or Low:
   - High: data loss, wrong documentation published, security exposure, or a core requirement missed
   - Medium: a feature is degraded or fails with poor messaging
   - Low: minor inconsistency or maintainability issue
2. Rate **likelihood** as High, Medium, or Low.
3. Assign severity:
   - **Critical:** High impact and at least Medium likelihood
   - **High:** High impact with Low likelihood, or Medium impact with High likelihood
   - **Medium:** Medium impact with Medium or Low likelihood
   - **Low:** Low impact
4. Sort findings by severity.

### Output
Findings register with severity for each finding.

### Quality Checks
- Every finding has an impact, likelihood, and severity.
- No critical finding is rated below High.

---

## Skill 3: Decision Recording

### Purpose
Record each user decision so that the planning-agent and later reviewers can trace why the design changed.

### Inputs
- Findings register
- User responses

### Procedure
1. For each finding, record one of: Mitigate (with the chosen mitigation), Accept (with the reason), or Defer (with the open question).
2. For each mitigation, state the exact `architecture.md` section to change and the change to make.
3. Record the decision, rationale, user name or role, and date.
4. Add each change to the change log at the end of `architecture.md` with the date and the finding ID it resolves.

### Output
- Agreed Design Decisions section
- Architecture Update Actions section
- Change log entries in `architecture.md`

### Quality Checks
- Every finding has exactly one decision: Mitigate, Accept, or Defer.
- Every change in `architecture.md` has a matching finding ID.