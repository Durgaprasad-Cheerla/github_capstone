# PR Agent Execution Instructions

## Objective
Compile all SDLC verification reports and test evidence into a Pull Request, with a repository-root `CHANGELOG.md` entry and reviewer checklist. After separate explicit approval, commit and push only the changelog update; create the PR only after explicit approval of the exact PR title, body, base branch, and head branch. The human remains responsible for reviewing and merging it.

## Execution Protocol
1. Confirm `verification-report.md`, `code-review.md`, `impl-plan.md`, `requirements.md`, and `architecture.md` exist in the repository root and reflect a passing status. If verification is incomplete or failing, stop and report.
2. Read `skills.md` and apply the **Summary Generation** skill to write the PR overview and file changes list.
3. Apply the **Test Evidence Compilation** skill to format test run metrics and execution proof.
4. Apply the **PR Checklist Drafting** skill to generate the mandatory reviewer checklist.
5. Draft the complete Pull Request title and description following the 5 required sections.
6. Add a concise entry under `Unreleased` in the repository-root `CHANGELOG.md`, creating the file if needed and preserving existing content.
7. Inspect the worktree and exact `CHANGELOG.md` diff. If it contains unrelated pre-existing changes, stop and ask the user to resolve them; never stage them. Prepare a changelog-only commit message ending with `Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>`.
8. Present the exact changelog diff, commit message, remote, and branch. Wait for separate explicit approval to stage only `CHANGELOG.md`, create the commit, and push it. Do not amend commits.
9. After approval, stage only the approved changelog change, commit using the approved message, and push to the approved remote/branch. Verify each result. Do not claim success if any command fails.
10. Confirm the repository, pushed head branch, base branch, and an authorized PR-creation integration or authenticated GitHub CLI (`gh`) are available. Ask the user to resolve branch ambiguity.
11. Present the exact PR title/body and base/head branches. Wait for explicit approval to create the PR. Apply requested edits and seek approval again.
12. After approval, create the PR using the authorized GitHub integration or authenticated `gh` CLI and verify the success response. Record its URL and number. If the tool or prerequisites are unavailable, report a blocker and do not claim creation.
13. Report commit/push status and the PR URL/number to the Orchestrator Agent. Never merge the PR.

## Exit Criteria
- The Pull Request description contains all 5 required sections (Summary, Changes Made, Test Evidence, Known Limitations, Reviewer Checklist).
- Test evidence matches `verification-report.md` exactly.
- The changelog entry is under `Unreleased` in `CHANGELOG.md`.
- The changelog-only diff, exact commit message, remote/branch, and commit/push action were explicitly approved separately.
- Only the approved changelog change was committed and pushed.
- The user has explicitly approved the exact PR content and branches before creation.
- The PR URL and number are recorded only after the GitHub tool confirms creation.
- The PR remains open for human review and merge.