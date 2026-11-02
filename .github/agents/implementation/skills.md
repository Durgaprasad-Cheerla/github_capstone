# Implementation Agent Skills

## Skill 1: Task Selection

### Purpose
Find the next task that is ready to build.

### Inputs
- `impl-plan.md`

### Procedure
1. List every task with its status and dependencies.
2. Keep only tasks marked Open.
3. Keep only Open tasks whose dependencies are all Done.
4. Pick the first remaining task in the plan's order.
5. If no task qualifies, list the Open tasks and what each one is waiting for, then stop.

### Output
Task ID, description, files it will touch, and acceptance criteria.

### Quality Checks
- The selected task has no open dependencies.
- The task is not already In Progress or Done.

---

## Skill 2: Change Proposal

### Purpose
Show the user exactly what will change before anything is written.

### Inputs
- Selected task
- `architecture.md` and `design-review.md`
- `.github/standards/TechStack.md`

### Procedure
1. List each file to create or modify, with its path under `src/main/java/`, `src/test/java/`, or the repository root (`pom.xml`).
2. List each class and public method, with its inputs, outputs, and exceptions.
3. List the JUnit 5 unit tests (`*Test.java`) and integration tests (`*IntegrationTest.java`) needed to verify the task's observable behavior, acceptance criteria, and relevant failure or missing-data cases.
4. For each test, list the requirement IDs (FR-xx, NFR-xx) it verifies.
5. List the Mockito test doubles needed and what each one replaces.
6. If the task needs JSON handling, state which Jackson types are used.
7. List any new Maven dependency with its exact version and check it against `TechStack.md`. If it is not listed, flag it and ask for approval.
8. Note any design decision from `design-review.md` that the change relies on.
9. Ask the user to approve, edit, or reject the proposal.

### Output
A proposal with files, classes, methods, tests, requirement IDs per test, test doubles, dependencies with versions, and related design decisions.

### Quality Checks
- Every file in the proposal maps to the task.
- Each component in `architecture.md` maps to one or more packages or classes in the proposal.
- No dependency is added that is absent from `TechStack.md` without a user decision.
- Every dependency has a pinned version.
- Every test that verifies a requirement lists that requirement's ID.

---

## Skill 3: Build and Verify

### Purpose
Write the approved code and tests, then check that they pass.

### Inputs
- Approved change proposal

### Procedure
1. Write the classes in `src/main/java/` following the approved interfaces.
2. Validate all external input before use: ticket IDs, URLs, file paths, and API responses.
3. Parse JSON with Jackson (`ObjectMapper`). Check that required fields exist before reading them, and handle missing or null fields with a clear error. Handle malformed JSON explicitly.
4. Handle relevant errors explicitly, including invalid input, malformed or missing data, storage failures, and server startup failures. Do not silently report successful persistence when a write failed.
5. Read credentials from environment variables only. Never hardcode or log them.
6. Put the requirement ID in each test's display name, for example `@DisplayName("FR-01: sync adds new pages")`. If a test covers several requirements, list all of their IDs.
7. Write JUnit 5 unit tests (`*Test.java`) in `src/test/java/`. Use Mockito (`@ExtendWith(MockitoExtension.class)` with `@Mock`) to replace external services.
8. Write integration tests (`*IntegrationTest.java`) that exercise several components together, using test doubles for external services and isolated temporary resources. For a local web application, exercise HTTP behavior on an ephemeral loopback port and use temporary storage.
9. Run `mvn test` for the task, then fix any failures.
10. Stop and ask the user after two failed fix attempts.

### Output
Java classes in `src/main/java/`, JUnit and Mockito tests in `src/test/java/`, and the Maven test result.

### Quality Checks
- No secrets appear in any file.
- Tests cover each acceptance criterion and meaningful failure or missing-data behavior; trivial accessors and implementation details do not require standalone tests.
- Unit and integration tests follow the naming convention.
- No test calls a live external service. Local application tests use temporary storage and an ephemeral loopback port rather than fixed ports or user data.
- Every test that verifies a requirement includes that requirement's ID in its `@DisplayName`.
- `mvn test` passes for the task.

---

## Skill 4: Progress Recording

### Purpose
Keep `impl-plan.md` accurate so the next task and the review-agent start from a true state.

### Inputs
- Task result from Build and Verify
- List of changed files

### Procedure
1. Set the task status to `[x]` Done, or `[!]` Blocked with the reason.
2. Add a short note under the task: files changed, tests added, and dependencies added with versions.
3. Update any dependent task that is now unblocked.
4. Report the result to the user.

### Output
Updated `impl-plan.md` and a short summary for the user.

### Quality Checks
- Only tasks whose tests passed are marked Done.
- No task is marked Done with failing tests.