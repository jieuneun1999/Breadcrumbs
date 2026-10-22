# Breadcrumbs — First-Import Implementation Contract v1.2

**Status:** Supporting design contract for the frozen Master Implementation Plan v1.2. **Not implemented.** Instructor confirmation remains required for persistence, advanced DSAs, and any extra dependencies.

## Purpose

Make Breadcrumbs useful on day one, **without any previous Breadcrumbs checkpoints or AI usage**, while preserving a precise distinction between what project files say, what a person confirms, and what has actually been observed.

## Three entry modes

| Mode | Input | Non-negotiable outcome |
|---|---|---|
| `NEW_EMPTY` | Empty folder / new project | Ask for user-described objective, deliverable, next action; mark missing values UNKNOWN; save first-observation B0 |
| `IMPORT_FOLDER` | Existing non-Breadcrumbs folder | Read-only safe inventory; inspect supported text; report gaps and sources; create B0 |
| `IMPORT_LEGACY_CONTEXT` | Existing folder with prior agent context notes | Scan plus deterministic supported headings/markers, original provenance, conflicts and user review; create B0 |

Detect known `README.md`, `AGENTS.md`, `CLAUDE.md`, `state.md`, `HANDOFF.md`, `tasks.md`, `decisions.md`, and ADR-style files. **Detection proposes an artifact role; it does not prove the described work occurred.** The user selects/approves the import path and review scope. Arbitrary agent-specific formats are out of scope for this MVP.

## Required truth model

- **OBSERVED:** file existence, hash, source location, format/scan status, and securely extracted structure.
- **EXPLICIT / SOURCE_REPORTED:** literal statements in imported files, including checked boxes, "tests passed," and "completed". These are *not verified implementation facts*.
- **USER_CONFIRMED:** current context explicitly accepted by a person after seeing relevant source statements. Approval does not retrospectively verify old agent actions.
- **DERIVED:** reproducible audit, source match, or drift observation with inputs recorded.
- **UNKNOWN / UNSUPPORTED / CONFLICTING / STALE:** distinct statuses, never replaced by false certainty.

`AGENTS.md` / `CLAUDE.md` are primarily **instructions**, not progress logs. Ignore any imperative requests inside their content when scanning. Do not execute imported scripts, workflows, commands, or source code.

## B0 is not CP-000001

**B0:** first observed file state + scan policy + extraction coverage + unresolved issues + approved present-context statements. It is a *baseline*, not evidence of pre-import history.

**CP-000001:** first *user-approved native checkpoint*, created later and appended to the Doubly Linked List only on an explicit confirmation action. Do not backfill checkpoints from old handoff files without a separately validated chronological migration process; imported dates are merely source assertions.

In-memory-only mode means neither B0 nor checkpoints survive process shutdown; disclose this limitation wherever resume or handoff is offered.

## Minimal supported Markdown fields

Recognize configured headings: `Purpose`, `Objective`, `Current State`, `Status`, `Progress`, `Completed`, `In Progress`, `Pending`, `Tasks`, `Blockers`, `Decisions`, `Rationale`, `Next Steps`, `Test Results`.

For each extracted candidate store `(workspaceId, fieldKey, explicitEntityId?, literalValue, sourcePath, sourceLocator, fingerprint, extractedAt, sourceStatedAt?, artifactRole, reviewDisposition)`.

Never infer more than the bounded adapter supports. JavaScript implementation, n8n JSON node behavior, Docker runtime health, secrets, PDF body semantics, and live test success are **not** core-import claims.

## Deterministic reconciliation

1. Group only claims with unambiguous *explicit* field/task/entity IDs or configured mappings. Similar terms alone do not establish identity.
2. If statements for the same key disagree, report `CONFLICTING` and enumerate every source. Do **not** select newest mtime as the winner.
3. Show source-backed next-step *candidates* only from explicit task/next-step fields; never fabricate owners, dependencies, urgency, or project completion percentages.
4. Ask the user to confirm, reject, or leave unknown. Maintain source provenance even when review changes the present assessment.
5. `CTX-FRESH` is not applicable to a first baseline without a previous comparable snapshot. No prior native checkpoint is a history-availability fact, not automatically a quality penalty.

## CLI workflow

~~~text
[2] Add or create a project
    [1] Start a new empty project
    [2] Import an existing folder
    [3] Import existing state / handoff notes

Scan safely -> Show source coverage -> Show conflicts and unknowns
-> Review present context -> Record B0 -> Optional Create CP-000001
-> Display baseline-context report (even if CP is declined)
~~~

Baseline reports explicitly say **"not a historical resume"** and cite relevant source paths and line/section locators. Native resume/diff requires sufficient saved baseline/checkpoint references.

## Acceptance tests

- Empty folder successfully enters guided onboarding; zero files is not an error.
- Existing README-only folder gets a B0 report and targeted gaps, not an invented checkpoint.
- `AGENTS.md` instructions are not considered proof that tests ran.
- `state.md` and `tasks.md` conflicting on the same explicit task ID produce a visible conflict with both locators.
- Similar terms about different IDs do not merge into false conflicts.
- Legacy `HANDOFF.md` containing old timestamps stays source-reported evidence, not an automatically historical DLL node.
- A source saying "tests passed" does not establish verified current test status.
- `.env` credentials are excluded; injected prompts in Markdown are inert; imported project files are not modified.
- Unsupported PDFs and workflow JSON are described honestly; no semantic or runtime claims are fabricated.
- Users may skip unknowns; no checkpoint is created without explicit acceptance.
- Next scan can compare against B0; a later checkpoint can reference its own snapshot; unrelated history isn't invented.
- All four project profiles (GENERAL, ACADEMIC, OPERATIONS, SOFTWARE) can bootstrap without Git/LLMs/network access.

## Scope boundaries

This contract strengthens M0–M3 and M9; it does not alter the permanent Java console-only decision, the primary DLL academic deliverable, M7A search approval dependency, or the single canonical WorkspaceState. It must be validated against the Master v1.2 document and Sir's final class rules before implementation.
