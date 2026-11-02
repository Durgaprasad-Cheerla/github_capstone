# Orchestrator Routing Rules

These rules are mandatory. The Orchestrator Agent must follow the phase order and transition rules below. A specialist handoff is not permission to skip a phase or an approval gate.

## Phase Order

The normal forward-only sequence is:

```text
REPOSITORY_INITIALIZATION
  -> REQUIREMENTS
  -> REQUIREMENTS_APPROVAL
  -> ARCHITECTURE
  -> DESIGN_REVIEW
  -> DESIGN_APPROVAL
  -> PLANNING
  -> PLANNING_APPROVAL
  -> IMPLEMENTATION
  -> IMPLEMENTATION_APPROVAL
  -> REVIEW
  -> REVIEW_APPROVAL
  -> VERIFICATION
  -> VERIFICATION_APPROVAL
  -> PR
  -> COMPLETED
```

No phase may be skipped. Approval phases are explicit gates, not implied by the specialist completing work or producing a file.

## Phase Contracts and Allowed Transitions

| Current phase | Required action and exit evidence | Allowed next phase |
|---|---|---|
| `REPOSITORY_INITIALIZATION` | Confirm repository root, available specialist profiles and standards, current branch/worktree status, and whether required project/story inputs are present. A story may be supplied as a Jira/Confluence reference or pasted text. Do not discard or overwrite user changes. Record missing prerequisites; do not fabricate repository initialization or a story reference. | `REQUIREMENTS` when a story reference or user-provided story content is available; otherwise remain blocked here and request it. |
| `REQUIREMENTS` | Route to Requirements Agent. Require its `requirements.md` draft and a clear statement that user approval is pending. | `REQUIREMENTS_APPROVAL` |
| `REQUIREMENTS_APPROVAL` | Present the requirements draft for explicit user approval. Record the decision in workflow state. | `ARCHITECTURE` on approval; `REQUIREMENTS` if edits are requested or approval is declined. |
| `ARCHITECTURE` | Route to Architecture Agent only after approved requirements exist. Require `architecture.md` and the agent's readiness report. | `DESIGN_REVIEW` |
| `DESIGN_REVIEW` | Route to Design Review Agent with both `architecture.md` and `requirements.md`. Require `design-review.md`, finding decisions, and any agreed architecture updates. | `DESIGN_APPROVAL` |
| `DESIGN_APPROVAL` | Present the review and updated design for explicit user approval. Critical or high findings must be resolved or formally accepted as specified by project policy. | `PLANNING` on approval; `ARCHITECTURE` for agreed design changes; `DESIGN_REVIEW` for review corrections. |
| `PLANNING` | Route to Planning Agent only after approved requirements, architecture, and design review exist. Require `impl-plan.md` with full requirement coverage. | `PLANNING_APPROVAL` |
| `PLANNING_APPROVAL` | Present `impl-plan.md` for explicit user approval. | `IMPLEMENTATION` on approval; `PLANNING` if edits are requested or approval is declined. |
| `IMPLEMENTATION` | Route to Implementation Agent for the next ready task only. The agent must propose each task's changes and wait for explicit user approval before editing. Require task-level test results and accurate plan status updates. After all tasks are Done, require successful `mvn verify`, required local startup/smoke-check evidence from the approved plan, and a final implementation summary. | `IMPLEMENTATION_APPROVAL` when all tasks are Done, `mvn verify` succeeds, required local runtime evidence is recorded, and the final implementation summary is ready; remain here while tasks remain or work is blocked. |
| `IMPLEMENTATION_APPROVAL` | Present the completed implementation summary and verification evidence for explicit user approval. Per-task change proposals must already have been approved before edits. Record final approval. | `REVIEW` on approval; `IMPLEMENTATION` if changes or fixes are requested. |
| `REVIEW` | Route to Review Agent only when implementation is complete. Require `code-review.md` and a finding register. | `REVIEW_APPROVAL` |
| `REVIEW_APPROVAL` | Present findings and requested decisions. Critical/high issues must be fixed or formally accepted under project policy. If fixes are made, route through implementation approval and then repeat review. | `VERIFICATION` when review is approved and blocking findings are resolved or formally accepted; `IMPLEMENTATION` for approved code fixes; `REVIEW` for review corrections. |
| `VERIFICATION` | Route to Verification Agent only after review approval. Require actual command results, requirement coverage, and document checks in `verification-report.md`. | `VERIFICATION_APPROVAL` |
| `VERIFICATION_APPROVAL` | Present the report, failures, and exceptions for explicit user approval. A failing check or uncovered requirement blocks progression unless project policy explicitly allows a documented accepted exception. | `PR` when approved and the gate passes; otherwise route to the owning phase (`IMPLEMENTATION`, `REVIEW`, or the document's owning phase), then return to `VERIFICATION`. |
| `PR` | Route to PR Agent only after verification approval. Require the PR title/body, `CHANGELOG.md` entry, repository, remote, pushed head branch, and base branch. First, present the exact changelog diff, commit message, remote/branch, and commit/push action for separate user approval; stage only `CHANGELOG.md`, commit, and push only after that approval. Then present exact PR content and branch targets for separate approval. Prefer the authorized GitHub MCP PR-creation tool (such as `github-create_pull_request`); use authenticated `gh` only if MCP creation is unavailable. Do not merge. If a tool or prerequisite is unavailable, record a blocker and do not claim creation. | `COMPLETED` only after the approved changelog commit is pushed and a PR-creation tool confirms success with the PR URL/number; remain in `PR` for edits or a blocker. |
| `COMPLETED` | Record the created PR URL/number and that it is awaiting human review/merge. Do not state that it was merged unless verified separately. | Terminal phase. A new project/story begins a new workflow state. |

## Approval Rules

- Approval is explicit only when the user clearly approves the named artifact, task proposal, review, verification result, or PR materials. Silence, file presence, a successful specialist handoff, or an earlier approval is not approval.
- Record each approval or rejection in `.github/agents/orchestrator/workflow-state.md` with the phase, artifact/task, decision, and date. Do not invent the user's identity or decision.
- If the user requests changes, route back to the agent that owns the affected deliverable. Re-present the revised output for approval.
- The Orchestrator Agent coordinates and validates; it must not make specialist deliverables appear approved on the user's behalf.

## Blockers, Failures, and Recovery

- If required evidence, a configured agent handoff, or a required tool is unavailable, keep the current phase active or mark it `BLOCKED`, state the exact blocker, and do not advance.
- PR creation is a separate side effect from drafting. Obtain the user's explicit approval of the final title/body and creation action before calling a PR-creation tool. If no such tool is available, stop with `PR` blocked and provide the approved draft for manual creation.
- Committing and pushing the changelog is a separate side effect from PR creation. Obtain separate approval for the exact changelog diff, commit message, remote/branch, and push action. Stage only `CHANGELOG.md`; never amend or include unrelated changes.
- Route code defects to `IMPLEMENTATION`; architecture/design defects to `ARCHITECTURE` or `DESIGN_REVIEW`; plan defects to `PLANNING`; verification-report defects to `VERIFICATION`; PR-material defects to `PR`.
- After remediation, resume at the earliest phase whose gate must be re-run. Re-run downstream stages whose evidence may have been invalidated.
- Never infer success from a specialist's completion message alone. Check the required artifact and evidence.
- Preserve existing repository changes. Do not run destructive cleanup or overwrite user work to make a phase pass.

## State Persistence

Maintain `.github/agents/orchestrator/workflow-state.md` using the template in that file. Update it after each phase transition, decision, blocker, or handoff. It is an operational record, not a substitute for the SDLC deliverables or their approval gates.
