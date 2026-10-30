# Breadcrumbs

**Leave context behind. Pick up where work stopped.**

Breadcrumbs is a proposed **local-first, offline-capable, Java 17+ console application** for preserving reliable project continuity. It helps users inspect a new or existing workspace, identify documented facts and missing context, create confirmed checkpoints, compare later file changes, and produce source-backed handoffs.

> **Status: B1 initial implementation queued (2026-10-30).** This repository has implementation plans, not a finished or tested Java application. The team has adopted an incremental batch execution guide; no batch should be marked complete without executable code and test evidence. The academic core remains terminal-only and JDK-only; no GUI, network, Git, or AI subscription is required.

## Start here

| Document | Purpose |
| --- | --- |
| **[Implementation Batch Guide v1.0](docs/BREADCRUMBS_IMPLEMENTATION_BATCH_GUIDE_V1.0.md)** | **Execution order, Codex PR scope, dependency grouping, acceptance tests and live batch status; start here before coding** |
| [Master Implementation Plan v1.2](docs/BREADCRUMBS_MASTER_IMPLEMENTATION_V1.2.md) | Full product definition, Java architecture, data structures, milestones, security controls, acceptance tests, and design decisions |
| [Professional PDF edition](docs/BREADCRUMBS_MASTER_IMPLEMENTATION_V1.2.pdf) | Readable, shareable PDF version of the same master plan |
| [First-Import Implementation Contract v1.2](docs/BREADCRUMBS_FIRST_IMPORT_CONTRACT_V1.2.md) | Exact contracts for day-one onboarding, legacy handoff parsing, conflict resolution, first-observation B0, and first confirmed checkpoint |

## The problem

After an interruption, the files for a thesis, operations plan, or software project often remain but the *working context* is missing: what was decided, which status claims are current, what is blocked, and what to do next. Breadcrumbs makes those questions explicit and traceable instead of inventing progress.

## Day-one experience

Three proposed entry modes:

- **NEW_EMPTY:** start a project and record its stated objective and first-observation baseline.
- **IMPORT_FOLDER:** safely inspect an existing folder even when no Breadcrumbs checkpoints exist.
- **IMPORT_LEGACY_CONTEXT:** recognize supported `state.md`, `HANDOFF.md`, `AGENTS.md`, `CLAUDE.md`, task/decision notes, and related Markdown as *source-reported evidence*, with human review before claims become current-context statements.

**B0** means “what was observed at first import”; it is **not** historical progress. **CP-000001** is the first native Doubly Linked List checkpoint, created only with explicit user approval.

## DSA and delivery plan

The **team-approved implementation sequence** is [Batch Guide v1.0](docs/BREADCRUMBS_IMPLEMENTATION_BATCH_GUIDE_V1.0.md). It reorganizes delivery of the frozen **M0–M10** master plan into reviewable, tested feature batches, without changing the evidence and import contracts.

| Batch | Deliverable | Status |
| --- | --- | --- |
| **B1** | Java 17 CLI scaffold, manually implemented Doubly Linked List, five DEMO checkpoints, six+ operations and tests | NOT_STARTED — next |
| **B2** | Three entry modes, read-only project folder inspection and B0 baseline | NOT_STARTED |
| **B3** | User-confirmed checkpoints, basic local TXT save/load and restart continuity | NOT_STARTED |
| **B4** | Evidence audit, drift detection, checkpoint retrieval and handoff | NOT_STARTED |
| **B5** | Manually implemented Directed Graph and Hash Table in separate testable units | NOT_STARTED |
| **B6** | Custom Trie + Hash-backed Inverted Index | NOT_STARTED |
| **B7** | Full crash recovery, end-to-end hardening and selectively packaged optional integrations | NOT_STARTED |

**First Codex action:** use a branch such as `feat/m0-dll-foundation` and submit a focused PR for **B1 only**. Do not mix inspector, persistence, Graph/Hash/Trie, external parsing libraries or voice AI into the first code change. Document commands actually executed and test results. Do not claim a Windows validation unless it was performed.

**Academic and scope boundary:** The primary DLL and all proposed supporting DSAs are manually implemented, not Java's built-in `LinkedList`/`HashMap` or third-party DSA substitutes. Java standard APIs for file I/O and hashing are support utilities, not graded DSA replacements. The team is proceeding with the full roadmap as its **internal development scope**; this is **not a claim that the instructor has formally approved every advanced DSA, external library or feature**. The Java-only, dependency-free academic core must remain independently runnable.

**Continuity invariant:** **B0 is the observed initial baseline, not a DLL checkpoint**. **CP-000001** requires explicit human confirmation. Any TXT save/load functionality must restore checkpoint records into the custom DLL rather than replace it with a built-in collection. Read-only import may never execute source project files.

## Working principle

**Evidence first.** Report what can be observed; separately label what source documents claim and what users confirm. Preserve unknowns, conflicts, stale information, and unsupported formats. Do not execute imported workflows or expose secrets.

---

Documentation baseline: **v1.2, 2026-10-22**.
