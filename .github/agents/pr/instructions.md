# PR Workflow

1. Confirm required root artifacts exist: `verification-report.md`, `code-review.md`, `impl-plan.md`, `requirements.md`, and `architecture.md`. Stop if verification fails or a blocking review finding remains unresolved.
2. Draft the PR using `.github/agents/pr/skills.md`. Include **Summary**, **Changes Made**, **Test Evidence**, **Known Limitations**, and **Reviewer Checklist**. Derive changed files from the actual head-branch diff and cross-check against `impl-plan.md`; use exact evidence from `verification-report.md`.
3. Add a concise, evidence-based entry under `Unreleased` in `CHANGELOG.md`, preserving existing content. Inspect the worktree and exact diff; stop if the changelog has unrelated changes.
4. Present the exact changelog diff, commit message (including the Copilot co-author trailer), remote, and branch. Wait for explicit approval of these details and the commit/push action. Stage only `CHANGELOG.md`; verify both operations and report failures.
5. Confirm the pushed head branch, repository, and base branch. Present the exact PR title, body, and branches; wait for separate explicit approval. Re-present and obtain approval again if any PR content or target changes. If a target is ambiguous, ask before proceeding.
6. After approval, prefer the authorized GitHub MCP PR-creation tool, such as `github-create_pull_request`. Use authenticated `gh` only if MCP creation is unavailable. Verify the response and record the PR URL/number; if creation is unavailable, report the blocker without claiming success.
7. Hand off commit/push status and the confirmed PR URL/number to the Orchestrator. Do not modify application code or tests, stage unrelated changes, amend commits, or merge the PR.
