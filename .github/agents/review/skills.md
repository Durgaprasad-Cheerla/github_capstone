# Review Agent Skills

## Skill 1: Checklist Evaluation

### Purpose
Check the code against each review area and the requirements.

### Inputs
- `src/main/java/`, `src/test/java/`, `pom.xml`
- `requirements.md`, `architecture.md`, `design-review.md`

### Procedure
1. **Correctness:** For each FR-xx and NFR-xx, find the code that implements it and the test that verifies it. Check that the code matches the acceptance criteria and the interfaces in `architecture.md`.
2. **Security:** Check that no secrets are hardcoded or logged. Check that relevant external/user input and file paths are validated, writes stay within intended locations, and persisted content is handled safely. For a local-only app, confirm the listener binds only to loopback.
3. **Error Handling:** Check that relevant API, startup, persistence, invalid-input, missing-file, empty-data, and malformed-data failures produce clear errors. Check that exceptions are not swallowed silently.
4. **Test Coverage:** Check that tests verify each requirement's observable acceptance criteria, assert results rather than merely executing code, and cover relevant Not Found, missing-field, or empty-input behavior. Do not flag trivial accessors or implementation details solely for lacking standalone tests.
5. **Code Clarity:** Check that class, method, and variable names describe their purpose and that each method does one thing. Flag logic that needs comments to be understood.
6. **DRY:** Look for repeated logic across classes that could move into a shared method or utility.
7. **Dependency Safety:** Check each Maven dependency and plugin in `pom.xml` against `TechStack.md`. Confirm each version is pinned. Record the Dependabot alert status for each dependency as provided by the user (Dependabot runs on GitHub, so the agent cannot run it directly). Report any known-vulnerable version and the fixed version.
8. **Local runtime (when required):** Check that the README/run instructions match the actual startup configuration, that the app is reachable at its documented loopback URL, and that runtime user data is not committed.

### Output
A result for each of the seven areas: Pass, Findings, or Not Applicable with a reason.

### Quality Checks
- Every area has a result.
- Every requirement has been traced to code and a test, or has a finding.

---

## Skill 2: Finding Classification

### Purpose
Give each problem a clear ID, location, and severity so it can be tracked and fixed.

### Inputs
- Results from Checklist Evaluation

### Procedure
1. Assign an ID in the form `CR-01`, `CR-02`, and so on.
2. Record the file and line number, and the requirement IDs affected.
3. Assign severity using these rules:
   - **Critical:** a security exposure, a data-loss risk, or a core requirement that does not work
   - **High:** a requirement only partly met, a missing failure-path handler, or a known-vulnerable dependency
   - **Medium:** a test gap, unclear code, or duplicated logic that affects maintenance
   - **Low:** naming or style issues with no functional effect
4. Write the problem, the risk, and the suggested fix.

### Output
Findings register entries with ID, location, requirements, category, severity, and fix.

### Quality Checks
- Every finding cites a file and line and at least one rule or requirement ID.
- No finding is rated Critical without a stated risk.

---

## Skill 3: Remediation Routing

### Purpose
Decide how each finding gets fixed, without the Review Agent changing code itself.

### Inputs
- Classified findings
- User decisions

### Procedure
1. For each finding, record one decision: Fix (route to the Implementation Agent), Accept (with a reason), or Defer (with an open question).
2. Group Fix items by file so the Implementation Agent can address them together.
3. Order Fix items by severity: Critical first, then High, then Medium.
4. Pass each Fix item to the Implementation Agent as a task with its finding ID, file, and suggested change. The Implementation Agent proposes the change and waits for approval, as it does for every task.
5. After fixes, re-check only the affected findings and update their status to Resolved or Still Open.

### Output
Remediation plan with decision, owner, and order for each finding.

### Quality Checks
- Every finding has exactly one decision.
- No Critical or High finding is marked Accept without the user's explicit agreement.