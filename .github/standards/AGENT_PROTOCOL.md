# Agentic SDLC Protocol & Standards

## Overview
This document defines the mandatory communication protocols, handoff contracts, quality gates, and safety constraints governing all GitHub Copilot agents in the capstone project. The product scope is defined by the currently approved requirements and Jira story; do not assume the original documentation-sync example is the product being built.

---

## 1. Orchestration and Inter-Agent Handoff Contracts
The **Orchestrator Agent** owns sequencing and routes each stage to the appropriate specialist agent. The human remains the approval authority at the gates below.

Copilot's agent handoffs provide the routing points between the Orchestrator Agent and each specialist. After a specialist finishes or becomes blocked, it can hand the conversation back to the Orchestrator Agent with its status; the Orchestrator Agent validates the gate before presenting the next eligible handoff. The exact no-skip phase sequence, approval loops, and recovery routes are defined in `.github/agents/orchestrator/routing-rules.md`; resumable progress is recorded in `.github/agents/orchestrator/workflow-state.md`.

To ensure seamless execution across the 8-step SDLC pipeline, every specialist agent must adhere to strict input prerequisites, output deliverables, and handoff gates:

The Orchestrator Agent coordinates these stages but is not counted as a delivery stage.

| Step | Agent | Mandatory Input Prerequisites | Mandatory Output Deliverable | Handoff Gate |
|---|---|---|---|---|
| **1** | Requirements Agent | Jira/Confluence reference or user-pasted story content | `requirements.md` | Human approval |
| **2** | Architecture Agent | `requirements.md`, `TechStack.md` | `architecture.md` | Human approval |
| **3** | Design Review Agent | `architecture.md`, `requirements.md` | `design-review.md`, updated `architecture.md` | Zero unaccepted Critical/High findings & approval |
| **4** | Planning Agent | `architecture.md`, `design-review.md`, `requirements.md` | `impl-plan.md` | 100% requirement coverage & approval |
| **5** | Implementation Agent | `impl-plan.md`, `architecture.md`, `design-review.md` | Application code, tests, build configuration, updated `impl-plan.md` | Passing tests (`mvn test`) & task approval |
| **6** | Review Agent | Source code, Tests, `pom.xml`, SDLC artifacts | `code-review.md` | Zero unaccepted Critical/High findings |
| **7** | Verification Agent | Codebase, `pom.xml`, all SDLC artifacts | `verification-report.md` | `mvn clean verify` passes & 100% requirement coverage |
| **8** | PR Agent | `verification-report.md`, `code-review.md`, `impl-plan.md`, `requirements.md`, `architecture.md`, approved changelog-only commit/push and PR content/branch targets | Created Pull Request & Changelog | Separate approval for changelog commit/push and PR creation; tool-confirmed PR URL/number |

### Orchestrator Agent and human approval authority
- **Orchestrator Agent** means `.github/agents/orchestrator-agent.agent.md`. It coordinates the workflow and routes work to specialist agents; it is not the owner of their deliverables.
- The Orchestrator Agent validates each specialist's output and gate before routing to the next stage.
- The human remains the approval authority for requirements, design decisions, implementation proposals, reviews, verification exceptions, and final PR materials.
- The PR Agent prepares the PR description and `CHANGELOG.md` entry, commits/pushes only that changelog change after separate explicit approval, then creates the Pull Request after approval using the authorized GitHub MCP PR-creation tool when available, or authenticated GitHub CLI as a fallback. It does not merge; the human operator reviews and merges the PR.

---

## 2. Core Operational Rules

### **Human-in-the-Loop (HITL)**
- No agent may mark any artifact or code change complete without explicit human review and approval.
- The Implementation Agent must present every code change proposal and wait for user approval before modifying files.

### **Traceability**
- Complete bi-directional traceability is mandatory. Every Functional Requirement (FR-xx) and Non-Functional Requirement (NFR-xx) must be traceable from its source story through architecture, planning, code tests, review, and verification.

### **Security & Compliance**
- **Zero Secrets:** No credentials, API tokens, passwords, or keys may ever be hardcoded, logged, or included in any SDLC document, code file, or review report.
- **Approved Tech Stack Only:** All code must target Java 17 LTS and Apache Maven, and use only libraries, plugins, and runtime technologies listed in `TechStack.md`.
- **Runnable local application:** When approved requirements call for a local browser application, architecture, planning, implementation, and verification must include the startup command, loopback URL/port, persistent-data location, and a smoke check. Follow the local-app security constraints in `TechStack.md`.
- **Read-Only Enforcement:** Review, Verification, and PR agents operate in strict read-only mode regarding source code and tests. They cannot modify code; all defects must be routed back to the Implementation Agent.

### **Error Handling & Resilience**
- External inputs (ticket IDs, URLs, file paths, API responses) must be validated before use.
- Relevant API failures, missing or malformed data, empty inputs, persistence failures, and server startup errors must be explicitly handled with clear error reporting (no silent failures).