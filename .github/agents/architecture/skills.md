# Architecture Agent Skills

## Skill 1: Architecture Design

### Purpose
Derive system components, interfaces, and data flows from approved requirements.

### Inputs
- `requirements.md`
- User answers to design questions

### Procedure
1. Group requirements by responsibility (for example: input ingestion, change detection, transformation, output writing, error reporting, configuration).
2. Assign each group to one component. Give each component a single, clear responsibility.
3. Define the interface for each component: what it receives, what it returns, and what errors it raises.
4. Define the main data flow as an ordered sequence, then add a separate sequence for each error path.
5. Draw a Mermaid component diagram showing components and data flows.
6. Check that no component overlaps another's responsibility.

### Output
- Component list with responsibilities
- Interface definitions
- Mermaid diagram
- Sequence descriptions for the happy path and error paths

### Quality Checks
- Each component serves at least one requirement.
- Each NFR (for example security, performance, reliability) is addressed by a component or by a documented design decision.

---

## Skill 2: Technology Selection

### Purpose
Choose one approved technology for each component and justify the choice against the requirements.

### Inputs
- Component list from Architecture Design
- `.github/standards/TechStack.md`
- Relevant NFRs from `requirements.md`

### Procedure
1. For each component, list the approved options from `TechStack.md` that could fit.
2. Compare the options using these criteria: fit to requirement, simplicity, testability, maintenance, and security.
3. Select one option. Record the reason in one or two sentences, citing the requirement IDs it supports.
4. If no approved option fits, do not choose a new tool. Record the gap and ask the user how to proceed.

### Output
Table with columns: Component | Technology | Justification | Requirement IDs | Standards Status

### Quality Checks
- Every "Standards Status" entry reads "Approved", or the gap is recorded.
- No technology appears that is absent from `TechStack.md`.