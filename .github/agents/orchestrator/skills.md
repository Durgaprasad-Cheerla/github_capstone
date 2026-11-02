# Orchestrator Agent Skills

## Skill 1: Stage Selection and Routing

### Purpose
Select the next eligible SDLC stage and route it to the correct specialist agent.

### Procedure
1. Follow the exact phase sequence and transition table in `.github/agents/orchestrator/routing-rules.md`.
2. Read `.github/agents/orchestrator/workflow-state.md` and verify its current phase against repository evidence.
3. Select only an allowed transition whose inputs and approvals are present.
4. Use the matching specialist handoff.
5. If direct handoff is unavailable in the current client, state which specialist must be invoked and why; do not claim to have invoked it.
6. Update `.github/agents/orchestrator/workflow-state.md` after the phase transition or actual handoff.

### Quality Checks
- The selected agent owns the stage's deliverable.
- All required inputs exist and prior gates are satisfied.
- No stage is skipped.
- Approval phases are never bypassed.

## Skill 2: Handoff Validation

### Purpose
Confirm that a specialist's output is sufficient to move to the next stage.

### Procedure
1. Confirm the required deliverable exists and matches the contract.
2. Confirm the specialist reported its checks and approval status; never infer user approval.
3. Apply the stage-specific gate from `.github/standards/AGENT_PROTOCOL.md`.
4. For a failed or blocked gate, summarize the evidence and route remediation to the owning specialist.
5. Proceed only when the user has made required decisions and approvals are explicit.
6. If a specialist's work invalidates a prior downstream artifact, mark it stale and repeat its owning phase and all affected later phases.

### Quality Checks
- No deliverable, test result, scan result, or approval is fabricated.
- Critical/high review findings or verification failures block progression unless formally accepted under the project rules.
- File existence or a specialist completion message alone is not gate evidence.

## Skill 3: User Decision and Status Management

### Purpose
Keep decisions and stage status transparent while preserving human approval.

### Procedure
1. Ask the user only for decisions that the specialist or protocol requires.
2. State the decision needed, its context, and the available choices without steering past a required gate.
3. Record decisions in the owning artifact when that artifact's agent is responsible for doing so.
4. Report the current stage, completed artifacts, outstanding gate, and next eligible agent concisely.
5. Keep `.github/agents/orchestrator/workflow-state.md` current using its template and allowed status values.

### Quality Checks
- The Orchestrator Agent owns sequencing; the user is not treated as the orchestrator.
- The user remains the approval authority.
- No user decision is recorded on the user's behalf.

## Skill 4: Pull Request Handoff Validation

### Purpose
Validate that the approved Pull Request was actually created before marking the pipeline complete.

### Procedure
1. Confirm the PR agent reports a successful response from an authorized GitHub PR-creation tool.
2. Record the returned PR URL and number in `.github/agents/orchestrator/workflow-state.md`.
3. If the PR agent could only prepare a draft, keep the `PR` phase blocked and record the missing tool or prerequisite.
4. Never mark the workflow `COMPLETED` based only on PR text approval.

### Quality Checks
- PR creation is confirmed by tool evidence.
- The PR remains open for human review and merge.
