# PR Agent Skills

## Skill 1: Summary Generation

### Purpose
Synthesize project artifacts into a clear, concise executive summary and file change log for the Pull Request.

### Inputs
- `requirements.md`, `architecture.md`, `impl-plan.md`

### Procedure
1. Extract the core objective from `requirements.md` and `architecture.md`.
2. Write a 2–3 sentence summary explaining what was built and why.
3. Review `impl-plan.md` and list all added or modified production classes, test classes, and configuration files with a brief explanation of their purpose.

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
1. Locate the build and test summary in `verification-report.md`.
2. Extract the exact command run (`mvn clean verify`), exit code, and Surefire test counts (total, passed, failed, skipped).
3. Format the extracted evidence into a clear block for the PR description.
4. Note any accepted exceptions or known limitations documented during review or verification.

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
1. Build a reviewer checklist covering key quality gates:
   - **Correctness:** Behavior matches `requirements.md` acceptance criteria.
   - **Security:** No secrets or unvalidated inputs present.
   - **Error Handling:** Clear handling of relevant validation, persistence, startup, and integration failures.
   - **Test Coverage:** JUnit 5 tests cover happy paths and failure/missing-field edge cases.
   - **Dependency Safety:** All dependencies are approved in `TechStack.md` with pinned versions, and Dependabot status is accurately reported when available.
   - **Local Run (when applicable):** The README startup command and localhost URL are accurate, and the app binds only to loopback.
2. Format the checklist with markdown checkboxes (`- [ ]`).

### Output
- Reviewer Checklist section of the PR description.

### Quality Checks
- All standard capstone review areas are represented in the checklist.

## Skill 4: Approved Changelog Commit and Push

### Purpose
Include the changelog entry in the branch that will back the Pull Request without committing unrelated user changes.

### Procedure
1. Inspect worktree status and the exact `CHANGELOG.md` diff.
2. Stop if the changelog contains unrelated pre-existing changes or if the intended remote/branch is unclear.
3. Present the exact diff, commit message, remote, and branch; obtain explicit approval to commit and push this change.
4. Stage only `CHANGELOG.md`, create a changelog-only commit with the approved message ending in `Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>`, then push to the approved branch.
5. Verify the commit and push results. Never amend commits or stage unrelated files.

### Quality Checks
- The user approved the exact diff, commit message, remote, branch, and commit/push action.
- Only the approved changelog change is committed.
- Report failures without claiming the branch was updated.

## Skill 5: Pull Request Creation

### Purpose
Create the approved Pull Request using an authorized GitHub integration or authenticated GitHub CLI and verify the result.

### Procedure
1. Confirm verification passed, required artifacts are approved, and no blocking finding remains unresolved.
2. Confirm the approved changelog commit is pushed, and confirm repository, head branch, and base branch. Ask the user to resolve any ambiguity.
3. Present the exact PR title, description, base/head branches, and changelog entry to the user.
4. Wait for explicit approval to create this PR. A prior approval of the PR text alone is insufficient if its branch targets change.
5. Use an authorized GitHub PR-creation integration or authenticated GitHub CLI (`gh`) available in the environment. Do not substitute a fabricated URL or claim success without a successful tool response.
6. Record the returned PR URL and number and report them to the Orchestrator Agent.
7. If no authorized creation tool is available or a prerequisite is missing, report a blocker and provide the approved draft for manual creation.

### Quality Checks
- PR content and branch targets match the user's explicit approval.
- The GitHub tool confirms creation before the result is reported as created.
- The PR is left open; merging remains a human action.