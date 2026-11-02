# Planning Agent Skills

## Skill 1: Task Decomposition

### Purpose
Break system components, interfaces, and design decisions down into small, buildable task units for a Java 17 Maven project.

### Inputs
- `architecture.md` (components, interfaces, data flows)
- `design-review.md` (decisions and mitigations)
- `requirements.md` (FR-xx and NFR-xx)
- `.github/standards/TechStack.md`

### Procedure
1. Create Task `T-01` for project setup: configuring `pom.xml` for Java 17, the standard Maven layout (`src/main/java`, `src/main/resources`, `src/test/java`, `src/test/resources`), and only the approved dependencies/plugins actually required by the architecture, each with a pinned version.
2. For each architectural component and interface defined in `architecture.md`, create atomic tasks (`T-02`, `T-03`, ...) mapping to 1-2 production classes and their corresponding tests.
3. Incorporate any specific mitigations or requirements mandated in `design-review.md`.
4. Define production file paths (`src/main/java/...` and, when needed, `src/main/resources/...`), test file paths (`src/test/java/...` using `*Test.java` or `*IntegrationTest.java`), and clear acceptance criteria for each task. For a local web application, include runnable startup configuration, root `README.md` with the exact command and URL, persistence location, and smoke-test task(s).

### Output
- Detailed task breakdown entries (`T-01`, `T-02`, ...) with full file paths and criteria.

### Quality Checks
- Each task is small enough to be implemented and verified in a single step.
- Every task references its associated requirement IDs (FR-xx / NFR-xx).

---

## Skill 2: Dependency Ordering

### Purpose
Sequence tasks into a strict, acyclic build order and construct the dependency graph.

### Inputs
- Deconstructed task list

### Procedure
1. Identify logical execution prerequisites from the approved architecture. Typical ordering is Project Setup (`T-01`) -> Data Models & Configuration -> Core Components -> Interfaces/Presentation -> Persistence or External Integrations -> Integration Tests.
2. Assign predecessor task IDs as dependencies for each task (or "None" for setup).
3. Construct a Mermaid dependency graph (`graph TD`) visualizing task execution flow.
4. Build the Task Order Summary table listing task IDs, descriptions, and initial statuses (`[ ]` Open).

### Output
- Mermaid dependency graph
- Ordered task summary table

### Quality Checks
- No task depends on a task that comes after it in the build order.
- Blocked tasks or tasks waiting on external inputs are appropriately marked.

---

## Skill 3: Plan Validation

### Purpose
Perform a rigorous final check of `impl-plan.md` against requirements and standards before presenting it to the user.

### Inputs
- Draft `impl-plan.md`
- `requirements.md`
- `TechStack.md`

### Procedure
1. **Requirements Coverage Check:** Verify that every single FR-xx and NFR-xx from `requirements.md` appears in the Requirements Coverage section and is mapped to at least one task.
2. **Tech Stack Compliance Check:** Verify that every library/plugin referenced in any task is explicitly approved in `TechStack.md` and has a pinned version (no version ranges or blanks).
3. **Naming Convention Check:** Confirm that unit test files end in `*Test.java` and integration test files end in `*IntegrationTest.java`.
4. **Format Verification:** Confirm all 6 required sections of `impl-plan.md` are fully populated.

### Output
- Validation report confirming zero orphaned requirements and 100% standards compliance.

### Quality Checks
- Zero unmapped requirements.
- Zero unapproved technologies or unpinned versions.