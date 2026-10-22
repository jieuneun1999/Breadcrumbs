# Breadcrumbs

**Leave context behind. Pick up where work stopped.**

Breadcrumbs is a proposed **local-first, offline-capable, Java 17+ console application** for preserving reliable project continuity. It helps users inspect a new or existing workspace, identify documented facts and missing context, create confirmed checkpoints, compare later file changes, and produce source-backed handoffs.

> **Status: implementation planning (v1.2).** This repository currently contains the design specifications, not a finished or tested application. The academic core is terminal-only and JDK-only; no GUI, network, Git, or AI subscription is required.

## Start here

| Document | Purpose |
| --- | --- |
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

- **Primary academic DSA:** custom Doubly Linked List (checkpoint order and traversal).
- **Approval-dependent enhancements:** directed Graph (declared task dependencies), Hash Table (exact lookup), and Trie plus Hash-backed Inverted Index (lexical prefix discovery and source retrieval).
- Core implementation stays **Java CLI-only**; the workspace inspector is read-only by default. Imported handoff claims are not equivalent to successful tests or verified implementation.
- The current implementation plan defines milestones **M0–M10**, with **M7A** for the optional combined Context Search Engine. The minimal academic scope protects checkpoint/history operations and required demo criteria.

## Before implementation: instructor confirmation

1. **One DSA versus multiple:** May a custom DLL be the graded primary structure with supporting Graph/Hash Table and a custom Trie/Inverted Index, given the rubric selects one approved structure and requires instructor approval for out-of-table structures?
2. **Local persistence and inspection:** Are read-only workspace scans plus local text/file persistence for B0/checkpoints permitted under the console-only/no-database requirement? If not, we will implement the in-memory fallback.
3. **Built-in parsing and scope:** Are Java JDK file utilities (`java.nio.file`, ZIP/XML for DOCX) and bounded legacy Markdown imports acceptable, and may extra non-DSA workflow menus remain provided the six required DSA operations remain clear?

See the master plan's **M0** and academic acceptance sections before coding. No instructor approval is implied.

## Working principle

**Evidence first.** Report what can be observed; separately label what source documents claim and what users confirm. Preserve unknowns, conflicts, stale information, and unsupported formats. Do not execute imported workflows or expose secrets.

---

Documentation baseline: **v1.2, 2026-10-22**.
