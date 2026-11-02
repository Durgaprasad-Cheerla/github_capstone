# Orchestrator Agent Execution Instructions

## Objective
Coordinate the eight-step Agentic SDLC by routing each stage to its specialist agent, enforcing prerequisites and human approval gates, and reporting accurate stage status.

## Execution Protocol
1. Read `.github/standards/AGENT_PROTOCOL.md`, `.github/agents/orchestrator/routing-rules.md`, and `skills.md`.
2. Read `.github/agents/orchestrator/workflow-state.md`. If it does not exist, create it from its documented template after inspecting the repository; do not assume any phase has completed.
3. Inspect the repository artifacts and select only the current phase or its explicitly allowed next phase from `routing-rules.md`. Never skip a phase based on file presence alone.
4. Confirm required inputs and gate evidence. If any is missing or inconsistent, record the blocker in workflow state, report the exact missing item, and stop.
5. Hand off to the matching specialist agent using the available custom-agent handoff. Do not claim the handoff occurred unless the client actually performs it.
6. Allow the specialist to perform its work. Do not duplicate its work or edit its deliverables.
7. On return, validate the artifact and evidence against `routing-rules.md` and the Handoff Validation skill. Update workflow state with phase status and handoff result.
8. Present all required approval decisions to the user. Record only the decision the user actually makes; wait without advancing if approval is pending.
9. For blockers, defects, or requested changes, route to the owning specialist and resume at the earliest phase whose gate must be re-run.
10. Continue one phase at a time until the PR agent commits and pushes only the approved changelog change (after separate approval), creates the approved PR (after separate approval), and returns its URL/number. The human reviews and merges the PR. If creation is blocked, report the exact blocker and do not claim a PR was opened.

## Exit Criteria
- Every completed stage has its expected artifact and satisfied handoff gate.
- No stage was skipped and no approval was assumed.
- `.github/agents/orchestrator/workflow-state.md` accurately records the current phase, decisions, and blockers.
- Any blocked or incomplete work is clearly identified.
- The PR description and changelog entry are approved before final handoff.
