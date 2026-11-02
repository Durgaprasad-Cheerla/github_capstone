# Planning Agent Execution Instructions

## Objective
Convert `architecture.md`, `design-review.md`, and `requirements.md` into a structured, dependency-ordered Java 17 Maven implementation plan (`impl-plan.md`).

## Execution Protocol
1. Confirm `architecture.md`, `design-review.md`, and `requirements.md` all exist in the repository root. If any is missing, stop immediately and report the missing prerequisite.
2. Read `.github/standards/TechStack.md` to load approved libraries, plugins, test naming conventions, and project structure rules.
3. Read `skills.md` and apply the **Task Decomposition** skill to break down components into atomic, testable tasks (starting with Maven project setup and `pom.xml`). Include startup configuration, local URL, persistent-data handling, and a smoke check when required by the story.
4. Apply the **Dependency Ordering** skill to sequence tasks logically, building a Mermaid dependency graph and status summary table.
5. Apply the **Plan Validation** skill to verify requirement coverage, test naming conventions (`*Test.java` / `*IntegrationTest.java`), and pinned dependency versions.
6. Draft `impl-plan.md` strictly following the six sections defined in `planning-agent.agent.md`.
7. Present the draft plan to the user and request approval. Iterate and apply changes until explicit user approval is granted.
8. Report completion and handoff readiness for `impl-plan.md` to the Orchestrator Agent.

## Exit Criteria
- `impl-plan.md` exists in the repository root with status marked "Ready for Implementation".
- Every task has a unique ID, status, dependencies, requirement IDs, file paths, test files, acceptance criteria, and pinned Maven dependencies.
- Every FR-xx and NFR-xx from `requirements.md` is mapped to at least one task.
- All dependencies strictly conform to `TechStack.md`.
- The user has formally approved the plan.