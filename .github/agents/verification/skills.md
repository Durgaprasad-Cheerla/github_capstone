# Verification Agent Skills

## Skill 1: Build and Test Execution

### Purpose
Run the full build and tests and record exact results.

### Inputs
- `pom.xml`
- `src/main/java/`, `src/test/java/`

### Procedure
1. Record the environment: Java version (`java -version`), Maven version (`mvn -version`), and operating system.
2. Run `mvn clean verify` from the repository root.
3. Record the command, the exit code, and the "Tests run" summary line from the Surefire output.
4. If any test fails, record its class and method name and the first line of its failure message.
5. Record the Dependabot alert status as provided by the user. If the status is not available, record "Not Checked" and the reason.

### Output
Environment record, command and exit code, test totals, and failure details.

### Quality Checks
- The build result comes from actual command output, not an assumption.
- Test totals match the build output exactly.

---

## Skill 2: Requirement Coverage Check

### Purpose
Prove that every requirement is implemented and tested.

### Inputs
- `requirements.md`
- Test results from Build and Test Execution
- `src/test/java/` test classes

### Procedure
1. List every FR-xx and NFR-xx ID from `requirements.md`.
2. For each ID, find the test class and method that verifies its acceptance criterion. Search the test names and the requirement ID references in test comments or display names.
3. Mark the requirement:
   - **Covered:** a test exists and passed
   - **Failed:** a test exists and failed
   - **Not Covered:** no test verifies it
   - **Not Testable:** the requirement cannot be verified by an automated test, with the reason
4. For each NFR, confirm the measurable target in `requirements.md` is checked by a test or documented as a manual check.

### Output
Requirement Coverage Matrix with one row per FR-xx and NFR-xx.

### Quality Checks
- Every requirement ID appears in the matrix.
- No requirement is marked Covered without a named passing test.

---

## Skill 3: Document Quality Check

### Purpose
Confirm that each SDLC artifact is complete, consistent, and ready for the next reader.

### Inputs
- `requirements.md`, `architecture.md`, `design-review.md`, `impl-plan.md`, `code-review.md`

### Procedure
For each document, check:
1. **Structure:** all required sections from its agent definition are present.
2. **Completeness:** no placeholders such as "TBD", "TODO", or "Lorem" remain, except items recorded under Open Questions or marked "Not Found".
3. **Traceability:** every ID referenced (FR-xx, NFR-xx, component names, task IDs, finding IDs) exists in the document it points to or in the source document.
4. **Consistency across artifacts:** the requirement IDs in `architecture.md`, `impl-plan.md`, and `code-review.md` match those in `requirements.md`. Components in `architecture.md` match the packages/assets in the implementation.
5. **Status accuracy:** tasks marked Done in `impl-plan.md` have passing tests. Findings marked Resolved in `code-review.md` are actually fixed.
6. **Open items:** every item in an Open Questions section is either answered or accepted by the user.
7. **Runnable local application (when required):** confirm `README.md` documents the startup command and URL; start the application in the verification environment; confirm it binds only to loopback; and check the landing page and required user flows using available browser automation. If browser automation is unavailable, run and record a direct HTTP smoke check, clearly mark browser verification as not run, and do not claim browser verification passed.

### Output
Pass or Fail for each check in each document, with the specific line or section for each failure.

### Quality Checks
- Every failure cites the document, the section, and the exact problem.
- Consistency checks compare the actual files, not memory of them.