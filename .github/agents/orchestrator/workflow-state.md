# Agentic SDLC Workflow State

This file is the Orchestrator Agent's resumable operational record. Update it after every phase transition, approval decision, blocker, or specialist handoff. Do not use it as a substitute for requirements, design, implementation, review, verification, or PR artifacts.

## Current State

| Field | Value |
|---|---|
| Project/repository | Not started |
| Current phase | `REPOSITORY_INITIALIZATION` |
| Phase status | `NOT_STARTED` |
| Current owner | `orchestrator-agent` |
| Next eligible phase | `REPOSITORY_INITIALIZATION` |
| Last updated | Not started |

Allowed phase statuses: `NOT_STARTED`, `IN_PROGRESS`, `WAITING_FOR_USER`, `BLOCKED`, `DONE`.

## Phase Checklist

Mark a phase `DONE` only after its exit evidence and approval gate are satisfied. The implementation plan tracks task status; record each task-proposal approval and the final implementation approval in the decision and handoff log below.

| Order | Phase | Status | Deliverable/evidence | Approval evidence or blocker |
|---:|---|---|---|---|
| 1 | `REPOSITORY_INITIALIZATION` | `NOT_STARTED` |  |  |
| 2 | `REQUIREMENTS` | `NOT_STARTED` |  |  |
| 3 | `REQUIREMENTS_APPROVAL` | `NOT_STARTED` |  |  |
| 4 | `ARCHITECTURE` | `NOT_STARTED` |  |  |
| 5 | `DESIGN_REVIEW` | `NOT_STARTED` |  |  |
| 6 | `DESIGN_APPROVAL` | `NOT_STARTED` |  |  |
| 7 | `PLANNING` | `NOT_STARTED` |  |  |
| 8 | `PLANNING_APPROVAL` | `NOT_STARTED` |  |  |
| 9 | `IMPLEMENTATION` | `NOT_STARTED` |  |  |
| 10 | `IMPLEMENTATION_APPROVAL` | `NOT_STARTED` |  |  |
| 11 | `REVIEW` | `NOT_STARTED` |  |  |
| 12 | `REVIEW_APPROVAL` | `NOT_STARTED` |  |  |
| 13 | `VERIFICATION` | `NOT_STARTED` |  |  |
| 14 | `VERIFICATION_APPROVAL` | `NOT_STARTED` |  |  |
| 15 | `PR` | `NOT_STARTED` | Approved changelog-only commit/push; approved title/body; created PR URL/number | Separate approvals for exact changelog commit/push and PR creation |
| 16 | `COMPLETED` | `NOT_STARTED` | PR is open and awaiting human review/merge | Confirmed PR URL/number |

## Decision and Handoff Log

| Date | Phase | Artifact/task | Event | Decision/status | Evidence or notes |
|---|---|---|---|---|---|
|  |  |  |  |  |  |

## Active Blockers

| Phase | Blocker | Owner to resolve | Required action |
|---|---|---|---|
| None |  |  |  |

## Maintenance Rules

- Replace `Not started` and `NOT_STARTED` placeholders as the workflow progresses; do not leave contradictory statuses.
- Record concise evidence references (artifact path, test command/result, or user approval statement/date). Never include secrets.
- Record `WAITING_FOR_USER` when approval or a design decision is pending.
- Record `BLOCKED` with the exact prerequisite or failed gate and the agent/owner responsible for resolution.
- Preserve completed history in the log; do not silently erase rejected decisions or prior blockers.
