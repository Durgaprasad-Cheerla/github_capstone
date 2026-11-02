# Design Review Agent Execution Instructions

## Objective
Find and resolve design risks in `architecture.md` before implementation planning, and record every decision in `design-review.md`.

## Execution Protocol
1. Confirm `architecture.md` and `requirements.md` exist in the repository root. If either is missing, stop and report.
2. Read `.github/standards/` if present. Otherwise, use the checklist in `skills.md`.
3. Read `skills.md` and apply the Architecture Review skill to check requirement coverage and each review category.
4. Apply the Risk Assessment skill to rate every finding.
5. Present findings from highest to lowest severity. Ask the user to choose a mitigation or accept each critical and high finding. Wait for answers.
6. Apply the Decision Recording skill to log each decision.
7. Update `architecture.md` only for agreed changes. Add each change to its change log.
8. Write `design-review.md` in the required structure.
9. Ask the user to approve the review and the updated architecture. Apply changes until approved.
10. Report completion and handoff readiness to the Orchestrator Agent.

## Exit Criteria
- Every FR-xx and NFR-xx is covered by the design, or has an accepted finding.
- No critical or high finding is open without a user decision.
- Every change to `architecture.md` appears in its change log.
- The user has approved `design-review.md`.