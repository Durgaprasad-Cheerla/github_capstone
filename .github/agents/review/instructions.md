# Review Agent Execution Instructions

## Objective
Review the Java implementation for correctness, security, reliability, test coverage, clarity, duplication, and dependency safety. Record the results in `code-review.md`. Do not change any code.

## Execution Protocol
1. Confirm `src/main/java/`, `src/test/java/`, `pom.xml`, `requirements.md`, `architecture.md`, `design-review.md`, and `impl-plan.md` exist. Confirm all tasks in `impl-plan.md` are Done.
2. Read `skills.md` and apply the Checklist Evaluation skill to check requirement coverage and every listed review area, including local runtime checks when applicable.
3. Apply the Finding Classification skill to record each finding with its file, line, category, and severity.
4. Present findings from highest to lowest severity. Wait for the user's response to each Critical and High finding.
5. Apply the Remediation Routing skill to send approved fixes to the Implementation Agent.
6. After fixes, re-check the affected findings only.
7. Write `code-review.md` in the required structure.
8. Ask the user to approve the review. Apply changes until approved.
9. Report completion and handoff readiness to the Orchestrator Agent.

## Exit Criteria
- Every review area in `skills.md` is checked and has a result.
- Every FR-xx and NFR-xx is covered by code and tests, or has a finding.
- No Critical or High finding is open without a user decision.
- The dependency safety report is complete.
- The user has approved `code-review.md`.