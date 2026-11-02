# PR Agent Skills

## Skill 1: Summary Generation

### Purpose
Synthesize project artifacts into a clear, concise executive summary and file change log for the Pull Request.

### Inputs
- `requirements.md`, `architecture.md`, `impl-plan.md`

### Procedure
1. Extract the core objective from `requirements.md` and `architecture.md`.
2. Write a 2–3 sentence summary explaining what was built and why.
3. Review the actual head-branch diff and cross-check it against `impl-plan.md`. List only files actually changed, with a brief purpose; omit planned but unimplemented changes.

### Output
- Summary section and Changes Made section of the PR description.
- A concise change summary suitable for the `Unreleased` section of `CHANGELOG.md`.

### Quality Checks
- Summary accurately reflects the delivered user story.
- File change list matches the files modified during implementation.

---

## Skill 2: Test Evidence Compilation

### Purpose
Extract verified build and test results to provide undeniable proof of correctness in the PR description.

### Inputs
- `verification-report.md`

### Procedure
1. Extract the command, outcome, and test counts as reported in `verification-report.md`; do not assume a command or test-report format.
2. Present concise evidence in the PR and note accepted exceptions or known limitations from review/verification.

### Output
- Test Evidence and Known Limitations sections of the PR description.

### Quality Checks
- Extracted numbers match `verification-report.md` exactly.
- Limitations accurately reflect deferred or accepted items.

---

## Skill 3: PR Checklist Drafting

### Purpose
Create a structured verification tick-list for the human reviewer before merging the code.

### Inputs
- `code-review.md`
- `verification-report.md`

### Procedure
1. Cover applicable correctness, security, error handling, test coverage, dependency, and documentation checks using `code-review.md`, `verification-report.md`, and the approved requirements.
2. Include local startup/loopback checks only when the requirements call for a local application.
3. Format items as markdown checkboxes (`- [ ]`).

### Output
- Reviewer Checklist section of the PR description.

### Quality Checks
- All standard capstone review areas are represented in the checklist.
