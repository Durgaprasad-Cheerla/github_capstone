# Architecture Agent Execution Instructions

## Objective
Produce `architecture.md` from `requirements.md` that is traceable, standards-compliant, and ready for design review.

## Execution Protocol
1. Confirm `requirements.md` exists in the repository root. If it is missing, stop and report.
2. Read `.github/standards/TechStack.md` and `.github/standards/AGENT_PROTOCOL.md`.
3. List all FR-xx and NFR-xx IDs from `requirements.md`.
4. Read `skills.md` and apply the Architecture Design skill.
5. Apply the Technology Selection skill from the same file.
6. Ask the user about any open trade-off or ambiguous requirement. Record each answer.
7. Write `architecture.md` in the required structure.
8. Check the traceability matrix. Fix any requirement without a component or any component without a requirement.
9. Ask the user to approve the draft. Apply changes until approved.
10. Report completion and handoff readiness to the Orchestrator Agent.

## Exit Criteria
- Every FR-xx and NFR-xx is mapped to a component.
- Every technology appears in `TechStack.md`.
- All open questions are either resolved or listed under Assumptions, Risks & Open Questions.
- The user has approved the document.