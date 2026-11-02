# Verification Agent Execution Instructions

## Objective
Confirm that the code builds, passes all tests, meets every requirement, and that the project documents are complete and consistent. Record the results in `verification-report.md`.

## Execution Protocol
1. Confirm `pom.xml`, the source and test folders, and all five artifacts exist in the repository root.
2. Read `skills.md` and apply the Build and Test Execution skill. Run `mvn clean verify` and record the results.
3. Apply the Requirement Coverage Check skill to map every FR-xx and NFR-xx to a passing test or check.
4. Apply the Document Quality Check skill to `requirements.md`, `architecture.md`, `design-review.md`, `impl-plan.md`, and `code-review.md`.
5. Classify each failure as a code defect, a document defect, or an environment issue.
6. Present the results and the proposed routing. Wait for the user's decision on each failure.
7. Fix routed defects through the owning agent, then re-run the affected checks and the full suite.
8. Write `verification-report.md` in the required structure.
9. Ask the user to approve the report. Apply changes until approved.
10. Report completion and handoff readiness to the Orchestrator Agent.

## Exit Criteria
- `mvn clean verify` completes with no failing tests.
- Every FR-xx and NFR-xx has a passing test or documented check.
- Every document passes the Document Quality Check, or has an accepted exception.
- The dependency scan is run, or its absence is recorded with a reason.
- The user has approved `verification-report.md`.