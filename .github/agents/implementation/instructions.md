# Implementation Agent Execution Instructions

## Objective
Build the tasks in `impl-plan.md` in dependency order as a Java 17 Maven project, with one approved change at a time and passing tests after each task.

## Per-Task Protocol
1. Confirm `impl-plan.md`, `architecture.md`, and `design-review.md` exist in the repository root.
2. Read `skills.md` and apply the Task Selection skill to pick the next Open task with all dependencies Done.
3. Confirm the task against `architecture.md` and `design-review.md`. Ask the user if anything conflicts.
4. Apply the Change Proposal skill and show the proposal. Wait for approval.
5. After approval, apply the Build and Verify skill to write the code in `src/main/java/`, the tests in `src/test/java/`, and update `pom.xml` if needed. Then run `mvn test`.
6. If tests fail, fix them. After two failed attempts, stop and ask the user.
7. Present the task result and test evidence to the user. After explicit approval, apply the Progress Recording skill to mark the task Done and summarise the change.
8. Repeat for each ready task. Once every task is Done, run `mvn verify`, present the final implementation summary and result, and obtain explicit user approval before handing off to the review-agent.

## Exit Criteria
- Every task in `impl-plan.md` is Done, or Blocked with a documented reason the user has accepted.
- `mvn verify` passes with no failures.
- Unit tests follow the `*Test.java` naming and integration tests follow the `*IntegrationTest.java` naming.
- No secrets appear in code, tests, `pom.xml`, or logs.
- Every public method has happy-path and failure-path tests.
- Every dependency in `pom.xml` is on the approved list and has a pinned version.
- The user has approved the final state, and the agent has handed off to the review-agent.