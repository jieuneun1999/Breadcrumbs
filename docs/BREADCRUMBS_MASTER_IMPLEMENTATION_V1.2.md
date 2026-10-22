# BREADCRUMBS — MASTER IMPLEMENTATION PLAN v1.2
**Status:** FROZEN ARCHITECTURE BASELINE — REVISION v1.2  
**Date:** 2026-10-22  
**Implementation language:** Java 17+ (JDK only for the academic core)  
**Interface:** CLI / terminal only, permanently  
**Source of this specification:** The user-supplied 42-page report, *Breadcrumbs: Research-Backed Product, Architecture, DSA, and Implementation Blueprint*, combined with the agreed Breadcrumbs master plan and explicit user overrides.  
**Document purpose:** Partner-friendly project introduction, authoritative build specification, milestone tracker, risk register, reusable-technology register, and future-feature backlog.
**Revision notes:** v1.2 formalizes three zero-history onboarding routes, legacy agent-context ingestion, strict first-import evidence contracts, cross-source conflict review, and B0 vs native checkpoint semantics. v1.1's partner overview and M7A Trie + Inverted Index remain. The research, primary DLL baseline, source provenance, and permanent CLI-only scope are preserved.


> Change-control rule: “Frozen” means decisions cannot silently drift. Bugs, discovered constraints, and approved refinements may change the design through a documented architecture decision record (ADR). This is a specification, not a claim that the software has already been implemented or tested.


---


## START HERE — PROJECT OVERVIEW & PARTNER PITCH


### What is Breadcrumbs?


**Breadcrumbs is a Java console application that helps people understand, resume, and hand off unfinished work.** It can begin with an empty new project, an existing folder that has never used Breadcrumbs, or an existing project with legacy `state.md` and handoff files. It reads only *available supported evidence*, asks the user to verify missing or conflicting context, establishes a current-state baseline, then maintains confirmed checkpoints and explains later changes.


Think of it as a **continuity companion for ongoing projects**: when you return after days or weeks, you do not need to reconstruct everything from scattered notes, documents, and memory.


**One-sentence pitch:** “Breadcrumbs helps you leave reliable context behind today so you—or a teammate—can pick up work tomorrow without guessing.”


**Why the name?** Breadcrumbs are small, intentional traces that help someone find their way back. In this application, those traces are checkpoints, evidence references, decisions, blockers, and next actions.


### The problem we want to solve


A student pauses a thesis, an event organizer changes teammates, or a developer returns to a half-finished application. The files still exist, but the useful *working context* may not: What were we trying to finish? Which draft is current? Why did we choose this approach? Are we waiting on someone? What should happen next?


Existing tools solve pieces of this: Git tracks file history, task boards track tasks, and notes preserve what someone wrote. Breadcrumbs combines **documented project evidence + human decisions + continuity checkpoints + freshness checks** in a guided, offline-first CLI. It does **not** replace those tools or claim to know facts that were never recorded.


### Who can use it?


- **Students and researchers:** resume papers, track adviser feedback, distinguish an actual next revision from a pile of draft files.
- **Event and operations teams:** capture decisions, owners, supplier blockers, and explicitly declared task dependencies.
- **Developers:** preserve why code changed, which tests were *documented* as run, and what needs review after new edits.
- **Solo workers or handoff partners:** record a clear next action and share a source-linked summary without requiring Git or an AI service.


### First-time onboarding: three valid entry points

| Entry path | User starts with | Immediate benefit | Historical limitation |
|---|---|---|---|
| `NEW_EMPTY` | A new or empty folder and a project goal | Guided objective, deliverable, owner if relevant, first action; establish B0 | No earlier history exists |
| `IMPORT_FOLDER` | An existing folder without Breadcrumbs records | Read-only inventory, supported evidence extraction, gap audit, suggested next actions, B0 | No verified pre-import chronology |
| `IMPORT_LEGACY_CONTEXT` | Existing `state.md`, `HANDOFF.md`, `tasks.md`, `AGENTS.md`, ADR notes, etc. | Legacy evidence review, contradictions, first confirmed baseline, optional draft checkpoint | Agent-reported progress is not implementation verification; dates do not prove history |

**No existing Breadcrumbs checkpoint or agentic workflow is required.** The importer should automatically *suggest* the legacy route when context files are found, but the user always confirms the selected project and import scope. A B0 baseline is a present-time observed snapshot; it is **not** CP-000001. The first approved checkpoint is a separate user-confirmed event.

### A concrete example — messy thesis folder


Suppose a project folder contains research-proposal.docx, chapter-3-notes.md, references.pdf, and revisions.txt.


1. **Import:** Breadcrumbs scans safely. It extracts supported text from DOCX, Markdown, and TXT; the PDF is recorded as present but its contents are **not** interpreted in the dependency-free core.
2. **Inspect and audit:** It finds an objective and revision notes, but cannot find a confirmed next action or whether adviser feedback has been addressed. It reports *unknown* instead of inventing an answer.
3. **Search (planned M7A):** Typing “meth” offers “methodology” as a Trie suggestion. Choosing the term returns indexed evidence locations through the inverted index. This searches extracted text only, not the PDF's hidden contents.
4. **Checkpoint:** The student explicitly records: “Revised methodology draft; waiting for adviser response; next: review comments when received.” The checkpoint is linked into the custom Doubly Linked List.
5. **Return later:** Breadcrumbs compares saved file fingerprints with the current folder, warns if referenced material changed, and displays the checkpoint plus relevant evidence.
6. **Handoff:** It produces a readable summary of the documented state, decisions, blockers, next actions, sources, and uncertainties.


**What it does not do:** It does not claim the thesis is “82% complete,” certify that the methodology is correct, read unsupported PDF text, or reconstruct history before the first import.


### What the user actually sees — CLI only


~~~text
BREADCRUMBS — Leave context behind. Pick up where work stopped.


[1] Continue a project
[2] Add or create a project (first-time guided setup)
[3] Check project context
[4] Save a progress checkpoint
[5] Create a handoff
[6] View checkpoint history
[7] Review tasks and dependencies
[8] View changes since last checkpoint
[9] Project settings and DSA diagnostics
[10] Search project context (planned, if approved)
[0] Exit
~~~


The interface uses numbered choices, readable explanations, Back/Cancel, and targeted questions. It is intentionally **not a GUI**; even future enhancements remain terminal-based.


### Why the data structures matter to the product


| Structure | Real user benefit | Implementation status |
|---|---|---|
| Custom Doubly Linked List | Reliable chronological checkpoint history; forward/backward browsing | Academic primary DSA |
| Custom Directed Graph | See which declared tasks may be affected by a blocked prerequisite | Planned M6; professor approval required |
| Custom Hash Table | Fast exact lookup of checkpoints, tasks, evidence, paths, and search postings | Planned M7; professor approval required |
| Custom Trie (Prefix Tree) | Suggest indexed terms from partial queries | Proposed M7A; professor approval required |
| Inverted Index (Hash-backed posting lists) | Show which supported evidence records/checkpoints contain a term | Proposed M7A; shares M7 Hash Table |


**Important:** The Trie and Inverted Index are complementary: *term discovery* versus *source retrieval*. They form **one search subsystem**, not two unrelated databases. Search is an index of canonical evidence, never a replacement for evidence or a new source of truth.


### What we would build together


The project is intentionally split into deliverable milestones: first the JDK-only CLI and core DLL, then safe inspection, audit, checkpoints, drift, and handoff; next the Graph, Hash Table, and Context Search Engine **if approved and within schedule**; finally persistence, security, tests, documentation, and demo readiness.


Possible partner work split (to agree on, not pre-assigned):
- **Track A — Evidence and user experience:** safe folder scanning, text adapters, audit explanations, guided CLI, handoff formatting, nontechnical testing.
- **Track B — Continuity and algorithms:** checkpoint DLL, Graph, Hash Table, Trie/inverted index, drift algorithms, invariants, persistence/recovery.
- **Shared integration:** domain contracts, test fixtures, security, documentation, and final defense. Both partners should understand and be able to explain all DSAs.


**Minimum viable academic success:** A user can create an empty project or import an existing folder (including common Markdown context artifacts), distinguish evidence from claims, establish B0, confirm CP-000001, compare later changes, resume, and hand off through a clean Java console application, with six or more DLL operations. Additional DSAs must not endanger that baseline. A full generic legacy-format migration engine is *not* a prerequisite.


### 45-second partner pitch


> “Breadcrumbs is a Java CLI tool that makes unfinished projects easier to resume. You don't need previous Breadcrumbs checkpoints or an AI setup to start. For an existing thesis or codebase it inspects supported files and existing handoff notes, shows what is documented versus unknown or contradictory, and lets the user confirm a reliable starting baseline. From there it records checkpoints in a custom Doubly Linked List, detects file changes, and creates traceable handoffs. Approved Graph, Hash Table, and Trie-plus-Inverted-Index features add dependency insight and search. It's offline-first and doesn't pretend documented claims prove the software works.”


### What we need to decide as partners


1. Confirm Sir's actual approved DSA list and whether multiple custom structures, local file persistence, and optional libraries are allowed.
2. Agree on the academic baseline versus approved stretch milestones; protect the 10-minute demo.
3. Choose realistic test folders: thesis/research, operations/events, software, and unstructured/folderless.
4. Divide ownership without splitting the canonical data model into incompatible implementations.
5. Treat search, Graph, and Hash Table as purposeful integrations—not requirements to inflate the DSA count.


---


## 0. Executive implementation decision


Breadcrumbs is a **local-first, offline-capable, evidence-backed work-continuity assistant**. It helps a person resume or transfer an ongoing project by inspecting available files, identifying which continuation facts are evidenced or unknown, recording explicit historical checkpoints, detecting change since those checkpoints, identifying the downstream impact of declared task dependencies, and exporting portable human/agent handoffs.


**Tagline:** Leave context behind. Pick up where work stopped.  
**Primary question:** “Can I safely pick this work up from where it stopped?”


**Permanent nonnegotiables**
1. Java, terminal/console only. No GUI, desktop app, web UI, dashboard, IDE-only interface, or future GUI roadmap.
2. Useful without Git, internet, an LLM, external libraries, an existing agentic workflow, or an existing Breadcrumbs history. All three entry modes support the same canonical WorkspaceState.
3. Works for general work, academic/research, events/operations, and software workspaces; software is a profile, not the product identity.
4. Evidence and explicit human statements are authoritative; summaries, scores, and AI drafts are derived.
5. Never claim project completion percentage, correctness, or undocumented intent from file names, timestamps, checkboxes, or source-code existence.
6. Main DSA: **manually implemented Doubly Linked List (DLL)**. Cohesive, approval-dependent enhancements: **manually implemented Directed Graph**, **manually implemented Hash Table**, and a **custom Trie + Hash-backed Inverted Index** as one Context Search Engine.
7. Treat academic constraints as hard requirements: at least six menu operations, custom nodes, at least five initial records with three or more fields, visible DSA structure and time complexity, clean JDK-only build, README, validation, and professor confirmation for uncertain persistence/multiple-DSA/external-integration rules.
8. Imported project files are read-only by default. Privacy and scanner limits are mandatory.
9. Milestones, not “gates,” define delivery progress. Every milestone has artifacts, tests, and acceptance criteria.


**Final academic target:** A new or existing folder can be bootstrapped into a truthful B0 baseline, optionally using supported legacy Markdown evidence; the user can audit/confirm gaps, create a native checkpoint, compare later changes, resume, and hand off from a guided CLI. Graph and Hash Table add dependency impact and fast indexing when permitted and feasible. Proposed M7A adds prefix-term discovery and evidence retrieval only after evidence and indexing contracts are stable and instructor approval is confirmed.


**Distinctiveness:** Unlike Git (“what changed in files?”), a task manager (“what tasks are assigned?”), a notes app (“what was written?”), or agent memory (“what information might be relevant?”), Breadcrumbs answers: “What continuation evidence exists, what is unknown, what changed, and what should be reviewed before work resumes?” With M7A enabled, it also answers: “Where does the documented project context mention this term?” without claiming semantic understanding.


---


## 1. Research findings and competitive positioning


The research found support for the interruption/resumption problem in empirical software-engineering literature, especially the cognitive costs of task interruption and reconstructing prior reasoning. This supports the problem statement, **not** a claim of proven demand across every nontechnical market.


Closest adjacent systems and lessons:
- **Linear:** concise project updates, health, challenges, next steps, history, reminders, and staleness. Learn short structured checkpoint UX; do not imitate a full SaaS project manager.
- **GitHub Issues/Projects:** typed work fields and explicit “blocked by / blocking” relationships. Consume such evidence; do not rebuild GitHub Projects.
- **Jira:** dependency impact is useful; enterprise-scale planning complexity is not appropriate.
- **GitHub Spec Kit:** specification, plan, tasks, and implementation are durable process artifacts. Consume them when present.
- **AGENTS.md and GitHub Copilot instructions:** stable agent instructions are useful, but dynamic checkpoint state and handoff are a separate need. Export complementary context.
- **MADR/Architecture Decision Records:** preserve context, alternatives, decision, rationale, and status; a one-line decision is insufficient.
- **Claude-Mem, Mem0, Letta:** cross-session state matters, but AI conversational memory is not identical to evidence-backed project continuity.
- **Apache Maka:** event/evidence-first, derived-projection-second architecture. Adopt the principle without importing an event-sourcing platform.


The research did not find a mature direct match centered on the *entire* local-first, cross-domain inspect → audit → checkpoint → drift → handoff workflow. This is a limited landscape inference, **not** proof that no competitor exists.


**Reject these product claims:** automatic true completion percentages; arbitrary undocumented semantic understanding; universal source-code intelligence; automatic fuzzy dependency inference; AI as the source of truth; voice-first workflows; Git as a prerequisite; PDF semantic parsing in the dependency-free academic core.


---


## 2. Scope, user profiles, and language


### 2.1 Project profiles
| Profile | Typical evidence | Profile-specific audit considerations |
|---|---|---|
| GENERAL | TXT, Markdown, DOCX, loose checklists | Explicit objective, current state, next action; owner optional for solo work |
| ACADEMIC | Thesis DOCX, revisions MD, notes TXT, references PDF | Deliverable, revisions, adviser feedback, next step; PDF may be unsupported |
| OPERATIONS | Supplier CSV, schedule, budget, meeting notes | Task status, owners, explicit blockers, dependency chain |
| SOFTWARE | Java, README, tasks, ADRs, optional Git | Documented work, decisions, explicit tests/status, code change evidence; no correctness inference |


Profile detection is heuristic and user-confirmed. GENERAL is the fallback. Users may change profile; doing so recalculates the audit but does not rewrite historical checkpoints.


### 2.2 Human-friendly terminology
| Internal term | Default CLI text |
|---|---|
| EvidenceRecord | Information found / source |
| AuditFinding | Context finding |
| Snapshot | Saved file state |
| Drift | Changes since last checkpoint |
| Directed Graph | Task dependencies |
| Downstream impact | Work that may be affected |
| Provenance | Where this information came from |
| Unsupported | File found, contents not inspected |


Normal mode prioritizes plain-language instructions. DSA Demonstration mode shows nodes, links, algorithms, and complexity. Color is optional; no ANSI capability is required. Every menu has Back/Cancel; Enter-to-skip is allowed for nonessential questions.


### 2.3 First-import truth and onboarding semantics

**B0 is a baseline snapshot, not a historical checkpoint**: B0 records the files seen, extraction coverage, declared scope, scan policy, and *observed current* context at first import. Without earlier trusted snapshots or evidence, Breadcrumbs cannot reconstruct what happened beforehand. File modification time is not proof of task completion or sequence.

- `NEW_EMPTY`: no files to scan is a legitimate result. Prompt for objective, deliverable, owner when relevant, next action, and explicitly UNKNOWN blockers when omitted. User statements are EXPLICIT/USER_CONFIRMED, not externally verified.
- `IMPORT_FOLDER`: scan read-only, display per-format coverage, extract explicit facts and context gaps, then offer to confirm the present state.
- `IMPORT_LEGACY_CONTEXT`: detect known documentation cues, parse only supported headings/markers, surface conflicts/staleness, review before mapping to current claims. Old handoffs remain *legacy evidence*, not automatically DLL checkpoints.

A native CP-000001 appears **only when the user explicitly accepts a checkpoint**; appending it to the DLL then establishes checkpoint chronology. Where persistence is disallowed, B0 and CP events are session-only and must not be marketed as durable cross-session recovery.


---


## 3. Product requirements and out-of-scope items


### 3.1 Functional requirements
| ID | Requirement | Priority |
|---|---|---|
| FR-01 | Add existing folder and confirm project profile | Must |
| FR-02 | Safe bounded discovery, classification, and scan diagnostics | Must |
| FR-03 | Normalize explicit source facts into evidence records | Must |
| FR-04 | Build SHA-256 snapshots and deterministic change reports | Must |
| FR-05 | Explain continuity readiness with source references and unknowns | Must |
| FR-06 | Ask targeted questions for important missing human context | Must |
| FR-07 | Create, find, insert, remove, traverse, and display DLL checkpoints | Must |
| FR-08 | Resume from latest checkpoint with drift warning | Must |
| FR-09 | Produce human-readable handoff with provenance and uncertainty | Must |
| FR-10 | Manage explicitly declared task dependencies | Planned Graph |
| FR-11 | Report BFS/DFS downstream impact and detect cycles | Planned Graph |
| FR-12 | Fast lookup by checkpoint, task, evidence, and path IDs | Planned Hash Table |
| FR-13 | Persistent local state with recovery, if instructor approves | Conditional |
| FR-14 | Optional Git evidence adapter, if approved | Future |
| FR-15 | Scriptable CLI and JSON handoff | Future |
| FR-16 | Optional evidence-constrained AI drafting, with consent | Future |
| FR-17 | Suggest normalized indexed terms for a typed prefix | Proposed M7A; conditional |
| FR-18 | Search supported evidence/checkpoints by exact term and source location | Proposed M7A; conditional |
| FR-19 | Rebuild/validate search projections after changes; no stale results | Proposed M7A; conditional |
| FR-20 | Show coverage limits and never search unextracted unsupported content | Proposed M7A; conditional |
| FR-21 | Create a project from no files and establish an honest B0 | Must |
| FR-22 | Bootstrap a never-before-imported existing folder without checkpoint prerequisites | Must |
| FR-23 | Recognize common legacy agent-context files and extract explicitly mapped Markdown fields | Must, narrowly scoped |
| FR-24 | Keep legacy claims, observations, and current user confirmations distinct | Must |
| FR-25 | Surface supported cross-source contradictions and staleness for user review | Must, deterministic rules only |
| FR-26 | Produce source-backed next-action candidates only when explicitly documented | Must |
| FR-27 | Require confirmation before creating CP-000001; do not invent prior checkpoints | Must |


### 3.2 Nonfunctional requirements
- Offline-first; zero network calls in core.
- No extra installs beyond an available compatible JDK.
- Read-only imported workspaces; application storage outside imported root by default.
- Stable IDs, deterministic rule evaluation, reproducible fingerprints, traceable reports.
- Bounded memory, file size, traversal depth, ZIP expansion, and terminal output.
- Explicit uncertainty and unsupported states.
- Terminal-safe rendering of untrusted content.
- Recoverable state when persistence is enabled.
- No hidden dependency on Git, codebase structure, or AI.
- Plain-language CLI usable by nontechnical people.
- Accessible keyboard interaction, consistent error/help screens, non-color fallback.


### 3.3 Deferred / excluded
**Deferred:** rich PDF parsing, XLSX/PPTX extraction, broad document indexing beyond supported normalized evidence, semantic retrieval, AI drafting, multi-device sync, fuzzy rename detection, file watcher optimization, advanced Java analysis, Git integration, Stack undo, Heap prioritization, cross-workspace search, generic arbitrary agent-history reconstruction, and unvalidated JavaScript/n8n/Docker semantic adapters. **Proposed M7A** adds only deterministic, bounded exact-term/prefix search over extracted canonical evidence and checkpoints; it is not broad document indexing or semantic search.  
**Excluded by permanent decision:** GUI/web/desktop interface, autonomous completion claims, full Jira competitor, mandatory voice, mandatory AI, arbitrary dependency invention.


---


## 4. Architecture and single source of truth


~~~text
User / Guided CLI
       |
Project Bootstrap Service: NEW_EMPTY | IMPORT_FOLDER | IMPORT_LEGACY_CONTEXT
       |                         |
       v                         v
Safe Inspector             Legacy Markdown Context Adapter
       |                         |
       +-------> Normalized Evidence <--------+
                        |
          Conflict Reconciler + Context Audit
                        |
                 Review and confirm B0
                        |
            Create CP-000001 ONLY on approval
                        |
          Continuity Engine: DLL checkpoint history
                        +-- Directed Graph: declared dependencies (M6)
                        +-- Custom Hash Table: exact indexes (M7)
                        +-- Trie + Inverted Index: lexical search (M7A)
                        |
             Drift / Resume / Handoff / Persistence
~~~


**One canonical WorkspaceState** owns EvidenceRecords, WorkItems, Decisions, Snapshots, and Checkpoints. DLL, Graph, Hash Table, Trie, and inverted posting lists are complementary structures/projections, not competing stores of business truth. Hash entries reference canonical objects; Graph vertices reference stable WorkItem IDs; search postings reference canonical EvidenceRecord/Checkpoint IDs and locators; checkpoints capture immutable historical views or references with recorded versions. Rebuildable search indexes are never authoritative.


**Dependency direction:** CLI → application services → domain/DSA/inspector/audit. Domain and DSA must never import CLI code. Adapters depend on the normalized evidence contract, not on audit rules.


**Evidence-first principle:** save observations/explicit statements, then derive audit, resume, relevance, and handoff views. A prose summary is never the sole historical record. **Bootstrap is an application service**, not a new business-state database; first-import, legacy-import, and normal continuity all write/refer to the same canonical model. Original project files remain read-only.


---


## 5. Domain model and contracts


Use enums instead of unrestricted status strings where possible. IDs are workspace-scoped or globally prefixed; all references validate workspace ownership.


### 5.1 Core entities
~~~text
Workspace:
  workspaceId, displayName, rootPath, profile, createdAt,
  lastScanId, latestCheckpointId, configurationVersion


EvidenceRecord:
  evidenceId, workspaceId, kind, sourceType, sourcePath,
  locator, observedAt, sourceFingerprint, claim,
  provenance, verificationState, sensitivity,
  derivedFromEvidenceIds, scanId


WorkItem:
  workItemId, workspaceId, title, description, explicitStatus,
  owner, dueDateOptional, sourceEvidenceIds, createdAt, updatedAt


DecisionRecord:
  decisionId, workspaceId, statement, context, optionsConsidered,
  rationale, status, recordedAt, evidenceIds


Checkpoint:
  checkpointId, workspaceId, createdAt, owner, objective,
  progressSummary, explicitBlockerState, blockerDescriptions,
  nextActions, decisionIds, evidenceIds, snapshotId,
  referencedWorkItemVersions, dependencyRevisionId,
  tags, previousCheckpointId, supersedesId, active


Snapshot:
  snapshotId, workspaceId, observedAt, entries, scanDiagnostics,
  completeness, policyVersion


SnapshotEntry:
  relativePath, sizeBytes, lastModified, sha256,
  adapterType, contentReadable, sensitiveFlag, scanStatus


DependencyEdge:
  prerequisiteWorkItemId, dependentWorkItemId,
  relationshipType, provenanceEvidenceId, createdAt, active


AuditFinding:
  ruleId, dimension, resultState, explanation,
  evidenceIds, weight, recommendedAction, severity


AuditReport:
  workspaceId, profile, evaluatedAt, findings,
  dimensionScores, assessableCoverage, overallScoreOptional,
  readinessLabel, criticalWarnings
~~~


### 5.2 Provenance and claim truth
- OBSERVED: a direct measurement (file path, hash, timestamp, supported parser output).
- EXPLICIT: a statement explicitly present in a document or entered by a human.
- DERIVED: a deterministic result with references to input evidence.
- AI_PROPOSED: unaccepted future AI interpretation.


**Verification is separate from provenance.** A CSV saying “supplier confirmed” is an explicit statement; Breadcrumbs did not independently confirm the supplier. Proposed verification states: SOURCE_OBSERVED, USER_CONFIRMED, UNVERIFIED, STALE, CONFLICTING, UNREADABLE.


**Sensitivity:** PUBLIC, INTERNAL, SENSITIVE, EXCLUDED. Export defaults exclude SENSITIVE/EXCLUDED content and require explicit review of INTERNAL snippets.


### 5.3 Historical truth and deletion
A checkpoint is an immutable historical fact once confirmed. A correction creates a new checkpoint with supersedesId. Academic “delete” unlinks a node from the active DLL and its index; if persistence is approved, a tombstone/archived record preserves that a deletion occurred. Graph/task changes do not rewrite historical checkpoint state. On reload, rebuild indexes from canonical active records.


### 5.4 ID format
Illustrative workspace W001, checkpoint CP-000001, work item T-000001, evidence EV-000001, snapshot S-000001, decision D-000001. IDs are monotonic within workspace, not derived from list position; deleting a record does not recycle its ID.


---


### 5.5 Bootstrap, imported legacy evidence, and review contracts (v1.2)

~~~text
BootstrapRequest:
  workspaceRoot, entryMode (NEW_EMPTY|IMPORT_FOLDER|IMPORT_LEGACY_CONTEXT),
  userSelectedProfile, scanPolicyVersion, excludePatterns, consentFlags
BootstrapReport:
  workspaceId, entryMode, scanIdOptional, fileInventory,
  extractionCoverageByAdapter, legacyArtifactsDetected, candidateFacts,
  conflictGroups, unknowns, proposedNextActions, warnings
LegacyArtifact:
  sourcePath, sha256, observedAt, artifactRole
  (INSTRUCTIONS|CURRENT_STATE|HANDOFF|TASK_LIST|DECISION|UNKNOWN),
  recognizedSectionLocators, unsupportedPortions, sourceDateClaimOptional
ClaimCandidate:
  fieldKey, explicitEntityIdOptional, literalValue, sourceEvidenceId,
  sourceLocator, sourceRole, sourceStatedAtOptional,
  verificationState (UNVERIFIED|SOURCE_OBSERVED|CONFLICTING|STALE),
  reviewDisposition (PENDING|CONFIRMED|REJECTED|LEFT_UNKNOWN)
ConflictGroup:
  fieldKey, explicitEntityIdOptional, candidateEvidenceIds,
  triggerRule, disposition (UNRESOLVED|USER_RESOLVED)
BaselineB0:
  workspaceId, observedAt, scanIdOptional, sourceFingerprintSet,
  extractionCoverage, reviewedCurrentFacts, unresolvedGaps,
  selectedPolicyVersion, userConfirmedAtOptional
~~~ 

`BaselineB0` is a conceptual record attached to the first snapshot/review, **not a Checkpoint node**. Avoid a second persistence truth source: record baseline metadata through WorkspaceState + initial Snapshot/Audit/ReviewEvent structures. The `sourceStatedAt` field is an asserted timestamp when parsed from text; it is *not* verified historical chronology. Imported source file contents are never overwritten or executed. Human confirmation may mark a **current assessment** USER_CONFIRMED but does not retroactively verify any old implementation or test result.


### 5.5 Search projection contracts (M7A, conditional)


~~~text
SearchPosting:
  term, workspaceId, recordType (EVIDENCE|CHECKPOINT),
  recordId, fieldName, sourceLocator, occurrenceCount,
  evidenceFingerprint, indexRevision


SearchIndexRevision:
  workspaceId, scanId, checkpointRevision, tokenizerVersion,
  policyVersion, builtAt, indexedRecordCount, skippedRecordCount


SearchResult:
  query, normalizedTerm, matchingRecordId, recordType,
  displaySource, locator, snippetOptional, freshness,
  provenanceEvidenceId, limitations
~~~


These are **derived, rebuildable projections**, not new canonical domain entities. A SearchPosting never stores an independent truth claim. Every search hit resolves through the canonical record and is filtered again by workspace, sensitivity, and freshness before display.


**Indexability contract:** index only bounded, supported, explicitly extracted text in EvidenceRecords and approved checkpoint fields. Metadata-only PDFs/images, excluded secrets, failed/unstable scans, and unreadable content do not become searchable text. The CLI must display how many records/files were unsupported or excluded.


**Tokenizer v1:** Unicode-normalize (NFKC), lowercase with Locale.ROOT, tokenize bounded letter/digit sequences, limit token length and per-record token count, avoid indexing secrets and control characters, and record tokenizerVersion for rebuilds. These are deterministic policy choices, not linguistic understanding.


---


## 6. Project Inspector — supported files and exact limits


### 6.1 MVP file adapter matrix
| Format | Supported extraction | Must not claim |
|---|---|---|
| .txt | Bounded UTF-8 text; configured labels/sections | Hidden meaning |
| .md | Headings, strict - [ ] and - [x] checkboxes, configured markers, ADR-like headings; bounded legacy `state.md` / `HANDOFF.md` / `tasks.md` sections | Arbitrary Markdown semantic correctness; undocumented task identity |
| .csv | Bounded CSV parser with quoted fields, escaped quotes, configured column mapping | Implicit blocker status without explicit schema |
| .docx | Safe ZIP + secure XML extraction of main word/document.xml paragraphs/runs | Full Word fidelity, comments, images, track changes |
| .java | Text markers; optional JDK Compiler Tree package/class/method structure | Behavioral correctness or successful tests |
| .json/.xml/.yaml/.yml | Narrow configured known-key/marker extraction; otherwise metadata-only | General schema semantics; n8n node behavior, Docker runtime state, or external platform status |
| .pdf | Metadata, size, hash when within limits | PDF content meaning |
| .xlsx/.pptx/images/other archives | Metadata-only | Hidden document content |
| Unknown binary | Metadata-only | Any semantic claim |


Use a strict adapter interface:
~~~java
interface EvidenceAdapter {
    boolean supports(FileDescriptor file);
    ExtractionResult extract(FileDescriptor file, ScanPolicy policy);
}
~~~
ExtractionResult includes evidence, warnings, unsupportedReasons, bytesRead, and scanStatus. Do not return unbounded raw document text to the audit engine.


### 6.2 Safe scan sequence
1. User selects folder; canonicalize/normalize and confirm directory.
2. Apply exclusions and NOFOLLOW_LINKS; do not leave selected root.
3. Bound depth, file count, file bytes, time, ZIP entries, and extracted text.
4. Classify files by supported extension plus safe content checks.
5. Hash with streaming SHA-256 where policy allows; distinguish skipped hash from hash of empty content.
6. Extract only explicit supported facts; attach path, locator, source hash, observed time.
7. Record skipped, unsupported, failed, unstable, and successful files separately; only supported, non-sensitive extracted text may later enter the M7A search projection.
8. Build deterministic snapshot and scan summary.
9. Show user profile suggestion and request confirmation.
10. Never execute imported code, scripts, Office macros, or shell commands.
11. Classify potential legacy-context artifacts by normalized filename/path and safe section markers; **filenames alone establish document role candidates, never project progress**.
12. For an empty project, skip content claims and move to guided user-state capture instead of treating zero files as a fatal error.


### 6.3 Exclusions
Default exclude .git object internals, node_modules, build, target, dist, .venv, venv, dependency caches, temp files, .env, private keys, credential files, and Breadcrumbs' own storage. Do not treat excluded content as absent project evidence.


### 6.4 DOCX hardening
ZIP entry-count and compression-ratio bounds; reject path traversal entries; only inspect known package member word/document.xml; disable DTD, external entities, and external resource access in XML parser; bound extracted text and nesting; catch malformed XML as UNSUPPORTED/FAILED, not “missing project objective.” No embedded macro execution.


### 6.5 Java AST
Optional JDK Compiler Tree API extracts package/class/method signatures and source locations. Fall back to safe marker scanning when compilation/parser access fails. Class existence is structural evidence, not functionality or test success.


### 6.6 Scan consistency
Files may change while scanned. Compare metadata before/after reads and mark unstable when inconsistent; final checkpoint validation rehashes referenced files. File modification time alone never proves equality.

### 6.7 Legacy context adapter — controlled first-import parsing

**Recognition list (case-insensitive name/path cues, not universal schemas):** `README.md` → stated purpose/setup; `AGENTS.md` and `CLAUDE.md` → agent instructions only; `state.md`, `STATUS.md`, `progress.md` → claimed current state; `HANDOFF.md`, `handoff-*.md` → legacy continuation guidance; `tasks.md`, `TODO.md` → explicit tasks; `docs/adr/*.md`, `decisions.md` → documented decisions/rationale. Unknown Markdown is still inspectable with generic rules. A document can have multiple roles if supported sections justify them; role detection is never proof of statement truth.

**Deterministic extraction whitelist** (case-folded configured headings, maximum bytes/sections/lines): `Objective`, `Purpose`, `Current State`, `Status`, `Progress`, `Completed`, `In Progress`, `Pending`, `Blockers`, `Decisions`, `Rationale`, `Next Steps`, `Tasks`, `Test Results`. Every candidate carries a literal quote/field, exact heading/line locator, and file fingerprint. Do **not** execute Markdown commands or obey embedded agent instructions.

**Evidence interpretation:** `README` describes intent or setup, not deployment verification. `AGENTS`/`CLAUDE` rules are instructions, not executed actions. A checked task is a **documented checked task**, not proof the feature works. `HANDOFF` and `state` are source-authored reports that may be stale or contradictory. A claimed "tests pass" string is not current test verification. Parse explicit task IDs when available; without IDs, do not merge unrelated tasks by keyword similarity.

**First-import boundaries:** Only parse supported UTF-8 Markdown and bounded explicit fields for academic MVP. File presence and hashes may be captured for JavaScript, YAML, Docker, n8n workflow JSON, but no runtime behavior, execution status, or AI interpretation is inferred. A separately designed narrow n8n structure adapter could later identify node IDs and connections **as structure only**, subject to approval and test fixtures.

**Failure/reporting:** Mark absent/malformed/unrecognized headings as UNKNOWN or UNSUPPORTED as appropriate; never silently fall back to fabricated content. Ignore metadata outside declared scan root, credentials and sensitive exclusions, and stale indexed snapshots.


---


## 7. Snapshot and deterministic drift algorithms


**B0** is first import baseline; **B1** is subsequent scan.


Classification:
- UNCHANGED: same relative path and SHA-256.
- MODIFIED: same path, different SHA-256.
- ADDED: new path only.
- DELETED: old path only.
- RENAMED_EXACT: one-to-one unmatched deleted/added pair with same SHA-256.
- AMBIGUOUS_RENAME: multiple candidate same-hash pairs; do not guess.
- UNKNOWN: read failure, missing hash, unstable scan, or incompatible policy.


Algorithm:
1. Index old/new entries by normalized relative path.
2. Compare matching paths and classify hash equality/difference.
3. Collect unmatched added/deleted entries.
4. Group unmatched by SHA-256; match only unique 1:1 groups as RENAMED_EXACT.
5. Preserve ambiguous groups and unknown entries.
6. Determine checkpoint drift only for evidence paths/fingerprints actually referenced by that checkpoint; show separate whole-workspace changes.
7. Recompute audit freshness; do not infer task completion or correctness.
8. Invalidate or rebuild affected M7A search postings after a new scan or checkpoint mutation. Never show a stale posting as a current match; fall back to live linear search or display INDEX_OUTDATED until rebuilding finishes.


Complexity: with Hash Table, expected O(F) comparisons and matching plus O(F) memory; without index, sorted arrays can support O(F log F), or nested scans O(F²) for small academic fixtures. SHA-256 streaming costs O(total bytes hashed), independent of map lookup complexity.


Metadata-first incremental hashing and WatchService are future optimizations; final evidence validation requires content hashes. Fuzzy rename matching is future and must report similarity uncertainty.


---


## 8. Context Audit — deterministic rule engine


### 8.1 Four dimensions
- Continuability: 40% — objective, deliverable, state, next action.
- Traceability: 25% — evidence and decision rationale.
- Coordination: 20% — owner, blocker, responsibilities.
- Freshness: 15% — checkpoint/evidence consistency.


### 8.2 Rule registry (from research)
| Rule ID | Requirement | Rule weight | Suggested dimension |
|---|---|---:|---|
| CTX-PURPOSE | Explicit objective | 12 | Continuability |
| CTX-DELIV | Expected deliverable | 10 | Continuability |
| CTX-STATE | Documented current state | 13 | Continuability |
| CTX-NEXT | Explicit actionable next step | 16 | Continuability |
| CTX-BLOCK | Explicit blockers or explicit “none” | 11 | Coordination |
| CTX-DECISION | Relevant decision and rationale | 10 | Traceability |
| CTX-OWNER | Responsible owner where applicable | 10 | Coordination |
| CTX-EVIDENCE | Key claims have source references | 8 | Traceability |
| CTX-FRESH | Referenced evidence unchanged or reviewed | 10 | Freshness |


These are **proposed product policy weights**, not scientifically validated quality measures. Normalize within each dimension, then apply 40/25/20/15 across assessable dimensions. Profiles determine applicability; CTX-OWNER may be N/A for solo work; CTX-FRESH is N/A on a first baseline with no historical checkpoint.


### 8.3 Finding states
EVIDENCED (1.0), PARTIAL (0.5), STALE (0.25), MISSING (0.0), CONFLICTING (0.0 until reviewed), UNKNOWN (excluded), UNSUPPORTED (excluded), NOT_APPLICABLE (excluded).


**Critical distinction:** MISSING means required *documentation* is confirmed absent within assessable sources/confirmed user inputs, not proof that a real-world task/decision does not exist. UNKNOWN means insufficient evidence; UNSUPPORTED means evidence may be in unparsed formats. A missing blocker declaration usually means blocker state UNKNOWN, not “no blockers.”


### 8.4 Score and confidence
For each dimension:
- applicableWeight = sum weights of profile-applicable rules.
- assessableWeight = sum weights excluding UNKNOWN, UNSUPPORTED, NOT_APPLICABLE.
- dimensionScore = 100 × sum(weight × multiplier) / assessableWeight, if assessableWeight > 0; else N/A.
- dimensionCoverage = assessableWeight / applicableWeight.
Overall = weighted mean of available dimensionScores using the four dimension weights renormalized over assessable dimensions.
Overall assessable coverage = weighted proportion of assessable applicable rules.


**Initial policy proposal:** if assessable coverage < 60%, show INSUFFICIENT EVIDENCE rather than an authoritative readiness score. Otherwise READY TO RESUME requires score ≥ 80 and no critical unresolved stale/conflict/next-action finding; NEEDS CONTEXT for other insufficient continuity; REVIEW CHANGES if critical referenced evidence drift is present. These thresholds are configurable product choices and must be tested, not marketed as objective scientific truth.


Display the reason and highest-priority gaps before the score. Always show “This does not measure project completion or quality.”


### 8.5 Explainability and determinism
Every finding answers:
1. Why was this state assigned?
2. Which evidence IDs and source locators support it?
3. What user action would improve continuation?


Identical evidence + profile + policy version → identical findings and ranking. Avoid system-clock-dependent “recent” rules without an explicit evaluation timestamp. **Search popularity or number of term hits must never affect the Context Coverage Score**; an indexed mention is not proof that a required fact is true.


### 8.6 First-import reconciliation and next-action policy (v1.2)

The audit consumes ClaimCandidates and source EvidenceRecords; it **does not** execute code or ask AI to resolve contradictions. Deterministic grouping requires the **same explicit fieldKey and entity/work-item ID**, or another configured unambiguous mapping. A shared English word, matching filenames, or close modification times are insufficient to claim a conflict or completion.

- **Evidence classification:** `OBSERVED` file facts (path/hash/structure), `EXPLICIT` document or user statements, `DERIVED` rules; separately annotate `SOURCE_REPORTED` vs `USER_CONFIRMED` verification. Instructions from `AGENTS.md`/`CLAUDE.md` cannot be promoted to performed work.
- **Cross-source contradiction:** for matching explicit keys, incompatible statuses or values generate `CONFLICTING` with links to all original sources. If matching identity is ambiguous, show **potential overlap / needs review**, not a proven conflict.
- **Staleness:** any imported source may be old; source-stated dates are claimed. Compare hash to B0 only after a later scan, and never use newest mtime alone to pick a winner. Archived claims remain historical evidence, not "current confirmed" state.
- **Next actions:** extract only literal unchecked tasks, `Next Steps` entries, or user-declared next actions with source locators. Show them as **candidate continuations**; do not invent task dependencies, priorities, or owners.
- **Missing context:** ask the highest-value targeted question about current state, next action, or blocker, with Back/Skip. Skips remain UNKNOWN; an empty source is not automatically a MISSING real-world action.
- **Review:** present source-specific claims, conflicting/unclear fields, and unsupported coverage for the user to confirm, reject, or leave unknown. Confirmation updates present-time canonical assessment without rewriting original evidence.

**Scores and labels:** No native checkpoints on B0 is a separate *history availability* flag, not a penalty proving poor documentation. `CTX-FRESH` is NOT_APPLICABLE on first import without a prior comparable snapshot. Checklists are claims, not test verification; the score is continuity readiness, not progress completion.


### 8.6 Checkpoint completeness is separate
Evaluate objective, progress, blocker status, next action, decision rationale when relevant, and evidence references for the proposed checkpoint. Ask at most one or two high-value missing-context questions at a time. Never block all checkpoint saving merely because an optional field is unknown; mark uncertainty explicitly.


---


## 9. Custom DSA #1: Doubly Linked List


**First-import invariant:** B0 is **not** a DLL checkpoint and must not be inserted simply because the folder was scanned. CP-000001 is appended only after user approval of a checkpoint review, with clear provenance for imported claims, new user statements, and unresolved unknowns. Five synthetic demo nodes for academic assessment must be clearly labeled as demo fixtures, not falsely generated real project history.


### 9.1 Ownership and invariants
The custom DLL owns checkpoint **chronological active ordering**. Each Workspace has a separate list. Node = {Checkpoint value, previous, next}. List = {head, tail, size}. Checkpoints must contain ≥3 fields; demo records use the full domain contract.


Invariants:
- Empty ⇒ head == null, tail == null, size == 0.
- head.previous == null; tail.next == null.
- For each link A.next == B, B.previous == A.
- Forward/backward traversal counts equal size; no cycles.
- IDs unique within workspace; time ordering is stable with checkpointId tie-breaker.
- Every active checkpoint in DLL has matching canonical record; no inactive checkpoint is linked.


### 9.2 Required API
~~~java
final class CheckpointList {
    void append(Checkpoint checkpoint);             // O(1)
    void prepend(Checkpoint checkpoint);            // O(1)
    void insertAt(int index, Checkpoint checkpoint); // O(n) locating
    Checkpoint findById(String checkpointId);       // O(n) without index
    boolean removeById(String checkpointId);       // O(n) locate + O(1) unlink
    Checkpoint latest();                            // O(1)
    Checkpoint oldest();                            // O(1)
    void traverseForward(CheckpointVisitor visitor);// O(n)
    void traverseBackward(CheckpointVisitor visitor);// O(n)
    String displayLinks();                          // O(n)
    boolean validateInvariants();                   // O(n)
    int size();                                      // O(1)
}
~~~


After each DSA demonstration operation, show:
~~~text
NULL <- CP-001 <-> CP-002 <-> CP-003 -> NULL
Operation: append at tail
Time complexity: O(1)
Space added: O(1)
~~~
Deletion in persistent mode creates an archive/tombstone record while unlinking the active DLL node.


### 9.3 Retrieval integration
When Hash Table enabled, checkpointId → CheckpointNode index permits expected O(1) locate + O(1) unlink, but the underlying DLL remains authoritative for sequence. When Hash Table disabled, O(n) lookup remains fully functional. Report the actual mode-specific complexity. M7A indexes approved checkpoint text as a secondary projection; editing/removing an active checkpoint updates or rebuilds its postings without rewriting historical facts.


---


## 10. Custom DSA #2: Directed Graph


### 10.1 Purpose
Represent **explicit task prerequisites** and answer “what other work may be affected if this prerequisite is blocked?” This is the strongest additional functional DSA from the research.


Edge direction: **prerequisite → dependent**. Example: Approve budget → Book supplier → Finalize event. Edges are never guessed from file names, modified timestamps, or AI suggestions.


### 10.2 Structure
~~~java
final class GraphVertex {
    String workItemId;       // reference canonical WorkItem
    EdgeNode firstOutgoing;
}
final class EdgeNode {
    String dependentWorkItemId;
    String sourceEvidenceId;
    EdgeNode next;
}
final class TaskDependencyGraph {
    boolean addWorkItem(String workItemId);
    boolean addDependency(String prerequisiteId, String dependentId);
    boolean removeDependency(String prerequisiteId, String dependentId);
    ImpactReport downstreamImpact(String workItemId); // BFS
    boolean hasCycle();                               // DFS colors
    TraversalReport depthFirst(String workItemId);
    String displayAdjacencyList();
    boolean validateReferences();
}
~~~


### 10.3 Graph rules
- Vertices reference canonical WorkItem IDs; Graph does not own duplicate status/owner data.
- Reject self-dependencies, duplicate edges, and cross-workspace references.
- Reject cycles at insertion or flag existing imported cycles before enabling impact reports.
- Preserve provenance of every edge.
- Report **potentially affected**, not automatically “blocked.”
- Use visited sets to avoid repeated traversal.
- Store graph revision ID with checkpoints; historical reports use historical revision, not current graph.
- M7A term matches may help users *find* task evidence, but **never create Graph edges** from co-occurring words or search suggestions.
- If the Hash Table is not yet available, vertex lookup can be linear; no functional dependence on the third DSA.


### 10.4 Complexity
Adjacency list storage O(V+E); BFS/DFS/cycle detection O(V+E). Add/remove edge O(outdegree) after vertex lookup for duplicate/removal checks; lookup O(1) expected with Hash Table, O(V) without. Topological ordering O(V+E) is an optional useful enhancement only for acyclic graphs.


### 10.5 UX
~~~text
TASK DEPENDENCIES
Blocked task: Approve budget


Potentially affected:
  -> Book supplier
      -> Finalize event


2 downstream tasks may be affected.
These dependencies were explicitly recorded.
[1] View source  [2] Edit link  [3] Back
~~~


---


## 11. Custom DSA #3: Hash Table


### 11.1 Purpose
Fast secondary indexes for checkpointId → DLL node, workItemId → WorkItem, evidenceId → EvidenceRecord, relativePath → SnapshotEntry, hash → rename candidates, and (M7A) normalized term → posting-list head. It improves responsiveness and enables efficient evidence/drift/search navigation but never owns the canonical business state.


### 11.2 Structure and API
Use a manually implemented array of bucket heads, custom linked HashEntry nodes, separate chaining, load factor initially 0.75, resize/rehash when threshold exceeded.


~~~java
final class HashEntry<V> {
    String key;
    V value;
    HashEntry<V> next;
}
final class CustomHashTable<V> {
    void put(String key, V value);
    V get(String key);
    boolean containsKey(String key);
    V remove(String key);
    int size();
    void clear();
    boolean validate();
}
~~~


Choose deterministic string hashing (Java String.hashCode with a documented bit-spread and nonnegative index); handle null keys explicitly, collision chains, negative hash codes, and rehashing. Do not imply cryptographic resistance from a basic hash function; SHA-256 is separately used for file fingerprints.


### 11.3 Complexity and consistency
Expected insert/find/delete O(1), worst O(n), resize O(n), storage O(n). On every canonical mutation, update the relevant index transactionally or rebuild it on detected mismatch. On load, rebuild indexes from canonical persisted data and validate sizes/references. For rename candidates, one hash may map to multiple files; never overwrite duplicate-hash candidates. For M7A, one term maps to **many postings**, not one overwritten record. Index records by compound workspace-scoped keys; rebuild search indexes on tokenizer or policy version changes.


---


## 11A. Custom DSA #4: Context Search Engine — Trie + Inverted Index (proposed M7A)


**Decision:** Combine the structures as one user-facing subsystem, not two redundant search products. The Trie discovers **terms** from a prefix; the inverted index retrieves **record locations** for a selected exact term. The existing custom Hash Table supplies both the term dictionary and record resolution. This enhancement remains **conditional on instructor approval and M7 readiness**.


### 11A.1 User problem and search promise


When users have dozens of notes, documents, and checkpoints, they need to find *where a documented concept appears* without opening every file. Example: type “meth,” see “method,” “methods,” “methodology,” choose “methodology,” and receive source-linked matches such as a DOCX paragraph, Markdown line, or checkpoint field.


**Search is lexical, not semantic.** It does not automatically understand synonyms, infer intent, answer factual questions from unsupported PDFs, or certify that a source claim is correct. The system searches only text the Inspector/Checkpoint engine explicitly captured and permitted for indexing.


### 11A.2 Data structures and ownership


~~~text
Canonical WorkspaceState (unchanged)
  EvidenceRecord EV-018  ---> indexed fields / locator / fingerprint
  Checkpoint CP-004      ---> indexed approved fields
                 |
                 v
SearchIndexBuilder (rebuildable projection)
    |
    +-- Trie:
    |     m -> e -> t -> h -> o -> d -> o -> l -> o -> g -> y [terminal]
    |     terminal stores normalized term ID, not evidence truth
    |
    +-- Existing CustomHashTable<PostingList>:
    |     "methodology" -> PostingNode(EV-018) -> PostingNode(CP-004)
    |
    +-- Existing CustomHashTable<CanonicalRecord>:
          EV-018 -> source-backed evidence; CP-004 -> checkpoint
~~~


Trie terminal nodes hold term keys or references; **do not duplicate full posting lists inside Trie nodes**. A posting contains stable record ID, record type, field, source locator, count, and index revision. Canonical records remain authoritative.


### 11A.3 Proposed Java contracts


~~~java
final class TrieNode {
    char character;
    boolean terminal;
    String normalizedTerm;    // set only for terminal nodes
    TrieChildNode firstChild;  // manually linked child edges
}
final class TrieChildNode {
    char character;
    TrieNode child;
    TrieChildNode nextSibling;
}
final class PrefixTrie {
    void insert(String normalizedTerm);
    boolean contains(String normalizedTerm);
    List<String> suggest(String prefix, int limit);
    void clear();
    boolean validate();
}
final class PostingNode {
    String recordId;
    String recordType;
    String fieldName;
    String sourceLocator;
    int occurrenceCount;
    PostingNode next;
}
final class ContextSearchIndex {
    void rebuild(WorkspaceState state, SearchPolicy policy);
    List<String> suggestTerms(String prefix, int limit);
    SearchReport searchExact(String term, int resultLimit);
    void invalidate(String reason);
    boolean isCurrent(WorkspaceState state);
    boolean validateAgainstCanonicalState();
}
~~~


List<String> in return types is only a **transport/result container**; the Trie, posting chains, and Hash Table are custom structures. If Sir disallows helper collections, return a custom bounded result object instead. A sibling-linked Trie uses no built-in map for child ownership, but child lookup scans siblings; the complexity must reflect the chosen child representation.


### 11A.4 Build algorithm


1. Verify scan completeness and sensitivity policy; select canonical EvidenceRecords and approved checkpoint fields.
2. Normalize/tokenize bounded text; deduplicate repeated occurrences within one record/field while preserving occurrence count.
3. For each unique normalized term, insert it into the Trie **once** and find/create its posting list using the existing custom Hash Table.
4. Append a posting referencing the canonical record ID, field, locator, and index revision.
5. Store the index revision tied to workspace scan, checkpoint revision, tokenizer, and policy.
6. Validate posting references, sensitivity exclusions, term counts, and Trie/Hash dictionary agreement.
7. Mark index READY only after a complete successful build. On cancellation/failure keep previous index only if its revision still matches; otherwise show INDEX_OUTDATED and offer rebuild or bounded live scan.


For a first implementation, **rebuild the whole per-workspace index after changes**. Incremental deletion, posting compaction, and live watcher updates are future optimizations and must not create stale results.


### 11A.5 Query algorithm and terminal UX


~~~text
SEARCH PROJECT CONTEXT
Enter a word or prefix: meth


Suggestions (Trie):
  [1] method
  [2] methods
  [3] methodology
  [0] Back


Choose: 3
Exact term: methodology


Matches (Inverted Index -> Hash Table -> canonical records):
  [1] chapter-3-notes.md, line 18 (EV-018)
  [2] research-proposal.docx, paragraph 9 (EV-022)
  [3] Checkpoint CP-004, decision summary


3 indexed matches. 1 PDF is metadata-only and not text-searchable.
[1-3] Open evidence  [R] Refine  [0] Back
~~~


This is an **illustrative demo**, not a claim about any actual user's files. In a normal console, numbered suggestions avoid requiring arrow-key or raw-terminal dependencies. Prefix suggestions are capped and deterministic (alphabetical by default; frequency ranking only if explicitly implemented and tested).


### 11A.6 Complexity, bounds, and alternatives


Let L = term length, P = prefix length, K = visited Trie nodes while collecting bounded suggestions, H = number of postings returned, and T = total normalized token characters indexed.


| Operation | Expected complexity | Caveat |
|---|---|---|
| Trie insert / exact lookup | O(L * C) with linked siblings | C = max siblings scanned per character; bounded alphabet yields O(L), but document the representation |
| Prefix suggestions | O(P * C + K) | Limit result count and visited nodes; traversal can still be large |
| Inverted term lookup | Expected O(1) hash + O(H) results | Worst O(U) hash lookup with collisions; U = unique terms |
| Resolve each hit | Expected O(1) per ID | Recheck canonical sensitivity and freshness |
| Full index build | O(T * C + postings) expected | Includes tokenization; file extraction/hash cost is separate |
| Memory | O(Trie nodes + unique terms + postings) | Bounded by scan/index policy, not unlimited |


**Tradeoff:** A Trie is not required for exact-term search: the Hash-backed inverted index alone can do that. The Trie earns its place only if **prefix suggestions** measurably improve the CLI. For tiny workspaces, bounded linear scanning is an acceptable simpler fallback. If the schedule or academic rules forbid extra DSAs, M7A is omitted without breaking inspect/audit/checkpoint/handoff.


### 11A.7 Privacy, consistency, and acceptance


- Index only supported, extracted, allowed text. Never index EXCLUDED or SENSITIVE fields, credential contents, or unapproved document snippets.
- Search hits show **source + locator + evidence state**, not an unsourced answer.
- Modified/deleted files, archived checkpoints, changed policies, and changed tokenizer versions invalidate/rebuild postings.
- No cross-workspace results without a separately approved multi-workspace feature.
- Reject overly long/empty queries; bound tokens, postings, suggestions, memory, and output; sanitize terminal snippets.
- Validate duplicates, collision chains, shared terms, prefix overlap, Unicode normalization, and deterministic ordering.
- M7A is complete only when no stale/unauthorized hit can be returned after a workspace update.


### 11A.8 DSA defense value


Demonstrate term insertion and prefix traversal in the Trie; term-to-posting retrieval in the Hash Table; posting-chain traversal to source-linked results; and index rebuild after a modified document. Explain why the DLL remains chronological history, Graph remains explicit dependencies, Hash Table remains lookup, and search projections remain derived.


---


## 12. Other DSA / algorithm recommendations (preserved)


| Structure/algorithm | Possible feature | Decision |
|---|---|---|
| Stack | Undo recent mutation | Deferred until persistence/recovery is trustworthy |
| Heap/Priority Queue | Prioritize tasks by explicitly supplied urgency/deadline/impact | Conditional future, not decorative |
| BST | Ordered date/deadline search | Lowest priority; chronological DLL already covers history |
| Queue | BFS traversal worklist | Internal Graph algorithm helper |
| Trie (Prefix Tree) | Bounded prefix suggestions over indexed evidence terms | Proposed M7A, custom implementation, approval required |
| Inverted Index / posting chains | Term → canonical evidence/checkpoint locations | Proposed M7A, reuses existing custom Hash Table |
| DFS | Directory traversal, Graph cycle/impact exploration | Required supporting algorithm |
| SHA-256 streaming | Reliable file fingerprints | Required supporting algorithm |
| Sorting/ranking | Deterministic checkpoint relevance | Advanced continuity feature |
| WatchService | Incremental change hints | Future; always verify by rescan |
| Myers/text diff | Human-readable changed lines | Future java-diff-utils option |
| Fuzzy rename matching | Recognize moved-and-edited files | Future, bounded and explicitly uncertain |


Do not add additional DSAs merely to raise the count.


---


## 13. CLI design and complete menu mapping


### 13.1 Default menu
~~~text
BREADCRUMBS — Leave context behind. Pick up where work stopped.


[1] Continue a project
[2] Add or create a project (first-time guided setup)
[3] Check project context
[4] Save a progress checkpoint
[5] Create a handoff
[6] View checkpoint history
[7] Review tasks and dependencies
[8] View changes since last checkpoint
[9] Project settings and DSA diagnostics
[10] Search project context (M7A, if approved)
[0] Exit
~~~


### 13.1A First-time setup sub-menu (v1.2)

~~~text
ADD OR CREATE PROJECT
[1] Start a new empty project
[2] Import an existing folder
[3] Import existing notes and agent handoff context
[0] Back

If known legacy files are detected, show: "I found possible state/handoff
notes. Would you like a review of that context?" Never auto-promote
its statements to confirmed current progress.
~~~

A first import displays scan limits, supported/unsupported counts, identified file roles, exact source links, and unknowns **before** offering a baseline review and CP-000001. Users can cancel, correct profile or parsing scope, or decline checkpoint creation without losing the scan report (when persistence is allowed).


### 13.2 Menu-to-service mapping
| Menu | Controller | Main methods | DSA / algorithm |
|---|---|---|---|
| 1 | ResumeController | resumeLatest, compareReferencedEvidence | DLL tail, drift, optional Hash |
| 2 | WorkspaceController | importFolder, scan, suggestProfile | DFS, adapters, SHA-256 |
| 3 | AuditController | evaluate, showSources, answerGap | deterministic rules |
| 4 | CheckpointController | propose, validate, confirm, append | DLL append, optional Hash |
| 5 | HandoffController | build, preview, export | DLL retrieval, Graph, audit |
| 6 | HistoryController | search, insert, delete, forward, backward | DLL; Hash acceleration |
| 7 | DependencyController | addTask, addEdge, removeEdge, impact, cycle | Graph BFS/DFS |
| 8 | DriftController | rescan, compare, review | SHA-256, snapshot Hash |
| 9 | SettingsController | profile, scan policy, diagnostics, recovery | all structures |
| 10 | SearchController | suggestTerms, searchExact, showSources, rebuildIndex | Trie, inverted postings, existing Hash Table (M7A conditional) |
| 0 | Main | confirm pending changes, exit | none |


History submenu must explicitly expose **at least six** DSA operations and display the structure/complexity after each demonstration operation. Include initial five demo checkpoints with at least three fields each, labeled synthetic.


### 13.3 Input rules
- Trim/validate menu input; handle EOF, blank, nonnumeric, out-of-range, huge strings.
- No unexpected destructive default; ask confirmation.
- Folder selection explains read-only scanning and excluded files.
- Show a progress summary for long scans; allow cancellation between files.
- Use plain ASCII-safe output; optionally support Unicode when terminal does.
- Sanitize control characters and limit printed lines from untrusted documents.
- Guided prompts allow “unknown” or “answer later.”
- Avoid long mandatory checkpoint forms; auto-fill supported observations and ask targeted human questions.
- Help and examples always available; scripting mode is future and never required for ordinary use.


### 13.4 Demo checkpoint seed data
Use five **synthetic** records, each with ID, timestamp, objective, progress, next action, owner, and blocker status:
- CP-001: Draft proposal; outline completed; review objectives; Alex; none.
- CP-002: Collect references; source list prepared; verify citations; Bea; none.
- CP-003: Revise methodology; draft updated; request adviser review; Alex; awaiting feedback.
- CP-004: Confirm event venue; venue shortlist created; obtain quotation; Casey; pending supplier reply.
- CP-005: Prepare handoff; current state recorded; verify latest files; Bea; unknown.
These may be assigned to separate demo workspaces or one coherent scenario; for DLL demo use **one** workspace and consistent chronology, preferably a single academic project with five sequential states. Do not present synthetic records as imported history.


---


## 14. Guided checkpoint, resume, and handoff workflows


### 14.0 Project bootstrap before any checkpoint (v1.2)

1. User chooses one of the three entry modes; `IMPORT_FOLDER` may suggest legacy review if known artifacts are present.
2. `NEW_EMPTY` asks for a minimal user-supplied objective, deliverable, next step, and optional owner/blockers; all are explicit current statements.
3. Import modes safely scan, classify agent instructions versus reported state, normalize only supported evidence, and build a B0 snapshot.
4. Reconciler presents documented state, next-action candidates, conflicts, unknowns, and unsupported coverage. User approves/rejects individual current-state claims; no historical timeline is fabricated.
5. B0 records scan + review metadata. **Do not call this a checkpoint** or count it as a native historical node.
6. User can independently choose "Create first confirmed checkpoint." Only then save CP-000001 with user-approved current context; preserve the original legacy evidence IDs.
7. Subsequent scans compare against B0 or CP-linked snapshots as applicable; no "changed since last checkpoint" is shown when no checkpoint exists.

For existing `state.md` and `HANDOFF.md`, **backfill is opt-in only** and requires sufficiently explicit dates/IDs and separate human confirmation per item. Treat ordinary imports as legacy evidence, not automatically ordered DLL history. A source-stated date never creates a verified historical event by itself.


### 14.1 Guided checkpoint creation
1. Retrieve current scan and latest checkpoint.
2. Auto-fill observable changes and explicit task markers.
3. Ask only for unknown human facts: why, blocker status, next action, owner where relevant.
4. Attach evidence IDs, snapshot ID, graph revision ID, and provenance.
5. Run checkpoint completeness validator.
6. Preview source-backed claims, uncertainty, and privacy redactions.
7. Confirm; append DLL; update Hash index; persist only if permitted.
8. Display structure and actual complexity.


### 14.1A First-use immediate handoff

A user may request a *current-context report* from B0 before saving any checkpoint. It contains documented purpose, file coverage, exact source quotes/references, approved current user statements, unknowns/conflicts, and proposed next actions. Label it **"Baseline context report — not a historical resume"**. A normal resume/diff packet requires a confirmed checkpoint or prior comparable snapshot.

### 14.2 Resume
1. Select workspace.
2. Retrieve latest active checkpoint (DLL tail).
3. Rescan referenced files and compare snapshots.
4. Mark stale/unknown evidence and calculate audit findings.
5. Retrieve next actions, explicit blockers, decisions, and Graph impact.
6. Show current objective, known status, changes, uncertainty, and suggested review.
7. Offer review drift, new checkpoint, prior checkpoint, or handoff.


### 14.3 Deterministic relevant checkpoint retrieval
No semantic-search claim. Require same workspace, then rank by explicitly matching tags, shared evidence IDs, freshness, and recency. Example initial weights: +4 tag match, +3 shared evidence, +2 fresh, +0–1 recency. Stable tie-breaker: timestamp then checkpoint ID. Explain which factors caused the rank. No LLM-generated ranking in core.


### 14.4 Handoff packet
Human-readable Markdown/plain text:
~~~text
# Breadcrumbs Handoff
Project / Profile / Checkpoint / Created / Recipient
Current objective
Documented progress (with evidence references)
Current blockers and explicit “none”/unknown distinction
Decisions, alternatives, rationale
Next actions and owners
Declared dependencies and potential downstream impact
Changes since checkpoint / freshness warning
Unknown, conflicting, and unsupported information
Evidence references (path, locator, hash, observed time)
Privacy/redaction notice
~~~
Preview before export. If writing is not approved, print to terminal for copying. Future JSON output uses the same canonical evidence schema; agent-ready means **portable**, not automatically integrated. M7A search results may help users choose evidence references, but selected search hits are not automatically verified facts or new handoff claims.


---


## 15. Persistence and crash recovery (conditional on instructor approval)


### 15.1 Local-only storage
Prefer storage outside imported root, e.g. user home .breadcrumbs:
~~~text
.breadcrumbs/
  manifest.txt
  workspaces/
    W001/
      current.txt
      generations/
        G0001/
          workspace.txt
          checkpoints.txt
          workitems.txt
          decisions.txt
          dependencies.txt
          evidence.txt
          snapshots/
            S0001.txt
        G0002/...
      backups/
~~~
All files start with FORMAT_VERSION=1 and stable encoding UTF-8. Define escaping/length-prefix rules so delimiters/newlines in user input cannot corrupt parsing. Do not serialize raw unbounded file content. Keep minimal provenance snippets and hashes, respecting sensitivity. **M7A Trie and inverted posting lists are derived indexes:** rebuild them from approved canonical evidence/checkpoints on startup or persist them only as validated disposable caches with index-version checks.


### 15.2 Atomic generation protocol
1. Serialize entire new workspace generation to staging.
2. Flush/close; validate schema, checksums, references, and counts.
3. Move staging into immutable generation directory.
4. Atomically replace current-generation pointer where supported.
5. Retain previous generation as backup.
6. On startup validate pointer/generation; if corrupt, offer read-only recovery from last valid generation.


ATOMIC_MOVE is only guaranteed where supported; otherwise report reduced guarantees and preserve backup. Never claim multi-file atomicity merely because one file was atomically renamed.


### 15.3 In-memory fallback
If file-writing permission is unclear or denied, run fully in memory with five seeded records and scan data, and display handoff in terminal. Do not claim cross-run persistence. Persistence is a product capability conditional on approval, not a hidden academic assumption.


### 15.4 Optional append-only record principle
Inspired by Apache Maka: historical checkpoint mutations may be logged as append-only events (CREATED, SUPERSEDED, ARCHIVED) while active DLL is a projection. This is a future refinement, not an academic requirement for a full event-sourcing framework.


---


## 16. Security, reliability, and privacy requirements


| Risk | Mitigation / invariant |
|---|---|
| Symlink escape / path traversal | Normalize root; NOFOLLOW_LINKS; containment checks |
| Secret leakage | Exclude .env, keys, credentials; redact snippets; no automatic uploads |
| Untrusted DOCX/XML | ZIP limits, secure XML, disable DTD/entities, bound text |
| Untrusted terminal text | Escape control characters, ANSI sequences, long lines |
| File changed mid-scan | Mark UNSTABLE; rehash before checkpoint |
| Unsupported formats | Metadata-only and UNSUPPORTED, not MISSING |
| False completion | Never infer real completion/correctness from checkboxes/file existence |
| Stale handoff | Mandatory referenced-evidence drift check and visible warning |
| Graph invented relationships | Only explicit/confirmed edges with provenance |
| Graph cycles | Detect and reject/flag cycles; visited traversal |
| DSA inconsistent copies | Canonical objects; indexes by ID; validate/rebuild |
| Corrupt/partial storage | Versioning, staging, validation, atomic pointer, backup |
| Unbounded scanning | Configurable file count, depth, bytes, ZIP entries, extracted text, elapsed time |
| AI hallucination | AI_PROPOSED, source references, user acceptance, no direct mutations |
| External dependency risk | JDK-only core; license/security verification before adoption |
| Git dependence | Git optional; all core flows work without it |
| Terminal usability | Guided menus, no required colors, help/back/cancel, understandable errors |
| Cross-project leakage | Workspace ID validation for all references |
| Hash collision | SHA-256 used for fingerprints; rename duplicates treated ambiguous |
| Incomplete audit | UNKNOWN/UNSUPPORTED excluded from score, assessable coverage shown |
| Search reveals private text | Sensitive/excluded records never indexed; recheck permissions/sensitivity on retrieval |
| Stale or misleading search results | Index revision tied to scan/checkpoint/policy; rebuild or mark INDEX_OUTDATED; source/locator shown |
| Trie memory explosion | Bound indexed records, token length, unique terms, posting count, and suggestion traversal |


Default scan network access OFF. No project data transmitted. A future LLM requires explicit user consent and per-export review.


---


### 16.1 First-import and legacy safety requirements (v1.2)

- Untrusted Markdown (including `AGENTS.md` / `CLAUDE.md`) is **data**, not instructions to Breadcrumbs. No shell, subprocess, tool call, interpreted macro, workflow execution, prompt injection following, or automatic credential read.
- Preserve read-only project roots; write Breadcrumbs state only to approved external app storage. Reject symlink escapes, overly broad paths, oversized handoffs, binary-as-text, control/ANSI sequences, and archive bombs.
- Label sensitive/unsupported/failed files with coverage notes without displaying secret contents. Prompt before export; never copy excluded material into logs or search postings.
- Distinguish user-supplied statements from source-reported statements and actual observations. A human selecting "confirm" authenticates their present assessment, **not** a claim of independent implementation verification.
- Rejected/unknown claims remain recorded as unresolved source evidence when policy permits; no arbitrary automatic reconciliation based on recency.

---


## 17. Java packages and class responsibilities


~~~text
src/breadcrumbs/
  Main.java
  cli/
    MenuController.java
    InputReader.java
    TerminalRenderer.java
    HelpScreen.java
    WorkspaceController.java
    AuditController.java
    CheckpointController.java
    HistoryController.java
    DependencyController.java
    DriftController.java
    HandoffController.java
    SearchController.java            # M7A conditional
    BootstrapController.java         # v1.2 core onboarding
  domain/
    Workspace.java
    WorkspaceState.java
    EvidenceRecord.java
    WorkItem.java
    DecisionRecord.java
    Checkpoint.java
    Snapshot.java
    SnapshotEntry.java
    AuditFinding.java
    AuditReport.java
    DependencyEdge.java
  dsa/
    CheckpointNode.java
    CheckpointList.java
    GraphVertex.java
    EdgeNode.java
    TaskDependencyGraph.java
    HashEntry.java
    CustomHashTable.java
    PrefixTrie.java                 # M7A conditional
    TrieNode.java                   # M7A conditional
    TrieChildNode.java              # M7A conditional
    PostingNode.java                # M7A conditional
  search/
    ContextSearchIndex.java         # M7A conditional
    SearchIndexBuilder.java         # M7A conditional
    SearchPolicy.java               # M7A conditional
    SearchReport.java               # M7A conditional
    Tokenizer.java                  # M7A conditional
  bootstrap/
    ProjectBootstrapService.java      # NEW_EMPTY / IMPORT_FOLDER / IMPORT_LEGACY_CONTEXT
    BootstrapReport.java              # first-import explanation/coverage
    BaselineReviewService.java        # guided approval before CP-000001
  legacy/
    LegacyArtifactClassifier.java     # role candidates by path/heading
    LegacyMarkdownAdapter.java        # allowlisted section extraction
    ClaimReconciler.java              # explicit field/ID conflict detection
  inspector/
    WorkspaceScanner.java
    SafePathWalker.java
    FileClassifier.java
    EvidenceNormalizer.java
    SnapshotBuilder.java
    ScanPolicy.java
    ScanDiagnostics.java
    adapters/
      EvidenceAdapter.java
      TextAdapter.java
      MarkdownAdapter.java
      CsvAdapter.java
      DocxAdapter.java
      JavaAdapter.java
      StructuredTextAdapter.java
      MetadataOnlyAdapter.java
  audit/
    ProjectProfile.java
    AuditRule.java
    RuleEvaluator.java
    ReadinessCalculator.java
    CheckpointCompletenessValidator.java
  continuity/
    CheckpointService.java
    DriftDetector.java
    ResumeService.java
    RelevanceRanker.java
    DependencyImpactService.java
  persistence/
    RecordCodec.java
    WorkspaceRepository.java
    RecoveryManager.java
  export/
    HandoffBuilder.java
    TextExporter.java
    MarkdownExporter.java
  security/
    PathValidator.java
    ContentSanitizer.java
    SensitivePathPolicy.java
  demo/
    DemoData.java
test/
  ...mirrored unit and scenario tests...
README.md
LICENSES.md            # only if external components are adopted
ARCHITECTURE_DECISIONS.md
~~~


**Contracts**
- WorkspaceScanner.scan(root, policy) → ScanResult(evidence, snapshot, diagnostics).
- EvidenceNormalizer.normalize(extractionResults) → deduplicated, traceable EvidenceRecords.
- RuleEvaluator.evaluate(workspaceState, profile, evaluationTime) → AuditReport.
- CheckpointService.propose/confirm → Checkpoint; confirm mutates DLL and indexes.
- DriftDetector.compare(oldSnapshot, newSnapshot) → DriftReport.
- ResumeService.resume(workspaceId) → ResumeReport.
- DependencyImpactService.analyze(workItemId) → ImpactReport.
- HandoffBuilder.build(workspaceId, checkpointId, recipient) → HandoffPacket.
- WorkspaceRepository.save/load → versioned state; only when persistence permitted.
- ContextSearchIndex.rebuild/suggestTerms/searchExact → source-linked SearchReport; only after M7A approval, index readiness, and sensitivity checks.


All services return typed results with explicit failure/unknown statuses rather than throwing unhandled exceptions into menus.


### 17.1 JDK-only compile/run
From project root, with JDK 17+:
~~~bash
javac -d out -sourcepath src src/breadcrumbs/Main.java
java -cp out breadcrumbs.Main
~~~
README must include Windows and Unix-compatible instructions and avoid requiring Maven/Gradle/third-party installs. Verify that all referenced classes compile with the chosen JDK version; do not assume a JRE-only installation includes javac or jdk.compiler.


---


## 18. Milestone-driven implementation roadmap


**Milestones replace all prior “gates.”** Every milestone records status (NOT_STARTED / IN_PROGRESS / BLOCKED / COMPLETE), owner, evidence, tests, and acceptance date. Suggested eight-week schedule is illustrative, not a claim about course deadlines.


### M0 — Academic alignment and frozen contracts (Week 1)
Deliverables: professor question list; requirements checklist; repo skeleton; ADR-001..012; domain enums; CLI shell; five synthetic checkpoints; basic DLL prototype. Confirm whether multiple custom DSAs (specifically Graph, Hash Table, Trie, and posting-list inverted index), local file writes, DOCX ZIP/XML extraction, Git, and external dependencies are permitted. Freeze the three entry-mode bootstrap contracts, B0-vs-CP semantics, and legacy-safe parsing whitelist.  
Acceptance: JDK-only compile/run, six+ menu operations mapped, seed records have ≥3 fields, initial DLL append/traverse works, architecture reviewed.  
Fallback: disable unapproved modules without breaking baseline.


### M1 — Safe Project Inspector (Weeks 2–3)
Deliverables: SafePathWalker, exclusions, classification, bounded TXT/MD/CSV extraction, SHA-256, snapshots, scan diagnostics, profile suggestion, `NEW_EMPTY` onboarding, and `IMPORT_FOLDER` first-import B0; narrow recognition of `state.md`/`HANDOFF.md`/task files plus allowlisted headings. DOCX and Java AST are stretch subdeliverables after core stability.  
Acceptance: developer, academic, operations, unstructured, and empty fixtures start without modifying input; supported/unsupported/skipped/failed separated; repeatable hashes; no symlink escape; agent instructions never become completion evidence; first-import B0 is not a checkpoint.


### M2 — Context Audit and guided gaps (Week 4)
Deliverables: nine-rule registry, four dimensions, profile applicability, result states, provenance display, coverage/score, one-question guided completion; legacy current-state claim candidates, deterministic cross-source conflict groups, source-backed next-action candidates, first-import review decisions.  
Acceptance: identical evidence yields identical report; UNKNOWN/UNSUPPORTED not treated as MISSING; N/A handled; no completion percentage; every finding links to a source and next action; `AGENTS.md` instructions not interpreted as performed tasks; conflicting same-ID status isn't silently resolved; no history penalty on B0.


### M3 — Full DLL checkpoint engine (Week 5)
Deliverables: append/prepend/insert/find/delete/forward/backward/latest/display; invariants; checkpoint completeness; resume retrieval; user-approved first CP-000001 distinct from B0; optional unconfirmed legacy history remains evidence-only.  
Acceptance: empty/single/multi-node tests; structure and complexity after operations; five explicitly synthetic seed records; historical checkpoints do not mutate with current task edits; importing a legacy handoff never manufactures a native checkpoint.


### M4 — Drift and freshness (Week 6A)
Deliverables: old/new snapshot comparison, exact rename handling, ambiguity/unknown handling, checkpoint-referenced drift, freshness audit.  
Acceptance: added/deleted/modified/unchanged/exact-rename/unknown fixtures; stale handoff warning; no “feature broken/completed” claims.


### M5 — Retrieval and portable handoff (Week 6B)
Deliverables: deterministic relevant-checkpoint ranker; terminal resume packet; Markdown/text handoff preview/export when permitted; privacy redactions.  
Acceptance: source-linked claims, decisions/rationale, next actions, blockers, changed evidence, unknowns; usable without Breadcrumbs; deterministic rank explanation.


### M6 — Directed Graph integration (Week 7, if approved)
Deliverables: custom vertices/adjacency nodes; add/remove edge; BFS/DFS impact; cycle detection; Graph CLI; graph revision references in checkpoints.  
Acceptance: no inferred edges, no cross-workspace edges, correct downstream count, cycle rejection/flagging, Graph output in audit/handoff.


### M7 — Hash Table integration (Week 8, if approved)
Deliverables: separate-chaining custom table, collision/resize tests, checkpoint/task/evidence/path indexes, index rebuild and consistency checks.  
Acceptance: same outputs with/without index; correct updates after DLL deletion and Graph mutation; expected O(1) average lookup described honestly.


### M7A — Context Search Engine: Trie + Inverted Index (conditional, after M7)
Deliverables: custom sibling-linked Trie and prefix suggestions; Hash-backed term-to-posting index; source-linked exact-term search; CLI search menu; tokenizer and privacy policy; versioned rebuild/invalidation; fallback to bounded linear search if indexing is unavailable.
Acceptance: term suggestions and source locations are correct; multiple matches are preserved; unsupported PDFs are not falsely searched; changed/deleted files and archived checkpoints do not yield stale hits; secrets stay excluded; collisions, Unicode, limits, and deterministic ordering pass; all search hits resolve to canonical evidence.
Dependencies: M1 Inspector, M3 checkpoints, M7 custom Hash Table, M0 instructor approval. This is **one integrated search subsystem**, not an independent product or a new source of truth.


### M8 — Persistence and recovery (conditional approval; schedule after M3)
Deliverables: versioned codec, generation staging, pointer commit, backup, recovery, corruption tests.  
Acceptance: restart preserves active timeline and historical revisions; interrupted writes recover last valid generation; fallback remains in-memory if denied.


### M9 — Four-profile hardening and CLI accessibility (final buffer)
Deliverables: end-to-end fixtures, help/errors, sanitized output, docs, defense script, complexity table, privacy checks, source/license registry; test `NEW_EMPTY`, `IMPORT_FOLDER`, `IMPORT_LEGACY_CONTEXT`, and native returning-project flows; when M7A is approved, include search accessibility, coverage disclosure, and index-rebuild tests.  
Acceptance: all four scenarios complete inspect → audit → checkpoint → drift → resume → handoff; no Git/AI dependency; clean build on supported JDK; no unhandled exceptions.


### M10 — Future product extensions (not required for academic completion)
JGit, commonmark-java, POI/Tika, java-diff-utils, JavaParser, Gitleaks, WatchService, JSON/noninteractive CLI, AGENTS.md-compatible handoff, optional LLM, cross-workspace indexing, team sync, undo, priority queue, incremental search-index updates, phrase/boolean search, typo tolerance, and semantic search (separately consented/approved). Each requires a separate ADR and maintenance/security review. No GUI.


### 18.1 Milestone status template
~~~text
Milestone ID:
Owner:
Status:
Start / target date:
Dependencies:
Deliverables completed:
Tests passed:
Known limitations:
Risks and mitigations:
Evidence / commit references:
Acceptance decision:
~~~


---


## 19. Verification matrix and tests


### 19.1 DSA unit tests
DLL: empty list, one node, head/tail/middle insert, duplicate ID rejection, head/tail/middle delete, forward/backward traversal, invariant checker, size accuracy, no cycles, latest O(1).  
Graph: isolated vertex, disconnected components, duplicate/self edge, missing ID, cross-workspace rejection, chain, diamond, multi-parent impact, BFS/DFS, cycle, provenance.  
Hash: empty, insert/get/update/remove, deliberate collisions, negative hash, resize, duplicate keys, missing key, canonical index rebuild, stale pointer detection.
Trie (M7A): empty tree, duplicate term, prefix vs exact term, shared prefixes, mixed case, Unicode normalization, sibling traversal, bounded suggestions, missing prefix, deterministic order.
Inverted Index (M7A): repeated term in one record, one term in many records, collisions, correct source locator, unsupported/excluded records not indexed, deleted/changed file invalidation, checkpoint archive invalidation, index version mismatch, safe rebuild and no stale results.


### 19.2 Inspector and audit tests
- Nested source tree with .git/build exclusions, symlink escape attempt.
- Markdown checked/unchecked tasks and ADR rationale.
- CSV quoted commas/escaped quotes, missing configured headers.
- Valid DOCX, malformed ZIP, zip-bomb-like fixture, XXE-like XML (must not resolve external entities).
- PDF and image metadata-only, binary with misleading .txt extension.
- Same bytes different timestamps; changed bytes same size; unstable file during scan.
- UNKNOWN vs MISSING vs UNSUPPORTED vs CONFLICTING.
- Solo profile with N/A owner; first baseline with N/A freshness.
- Same evidence/profile/time → same audit findings.
- Low assessable coverage → INSUFFICIENT EVIDENCE.
- Two identical-hash rename candidates → AMBIGUOUS_RENAME.
- M7A: supported Markdown/DOCX/TXT evidence is searchable only where extracted; metadata-only PDF never produces content-term hits; sensitive files are excluded; changing the snapshot invalidates the search revision.


### 19.2A Bootstrap and legacy-import acceptance matrix (v1.2)

| Fixture | Expected and forbidden behavior |
|---|---|
| Empty folder | Guided user input creates B0, no fake prior files or CP; CP-000001 requires approval |
| Existing folder without handoff files | Inventory/audit/B0 work, UNKNOWN next action prompts; no assumed prior history |
| Agentic repo with `README.md`, `AGENTS.md`, `state.md`, `HANDOFF.md`, `tasks.md` | Extract only explicitly mapped content; distinguish instructions from state claims; cite lines and hashes |
| Contradictory explicit task ID/status across state/task/handoff | CONFLICTING group with all sources; unresolved until user review; no silent latest-mtime winner |
| Similar terms about unrelated tasks | No false shared-task conflict without explicit same identifier/configured unambiguous mapping |
| Handoff with old asserted dates | Treat as SOURCE_REPORTED legacy evidence; no automatic historical DLL backfill |
| Source says tests passed but no test artifact/run | Document reported claim, not verified current tests |
| n8n JSON, JS, Docker YAML | Metadata or narrow explicitly configured fields; no execution/runtime success assertions |
| Sensitive `.env` and injected `AGENTS.md` commands | No secret extraction or command execution; no instruction following |
| User skips a question | Preserve UNKNOWN rather than inventing a default answer |
| B0 report without CP | Current-context report available; no "resume since last checkpoint" claim |
| Later scan changes a file | Detect drift against B0/CP reference and source staleness; maintain original evidence |
| In-memory mode | Warn that close/relaunch loses continuity; never advertise durable storage |

**Defense proof:** demonstrate both a cold import with no checkpoints and a legacy agentic import; show at least one source locator, one unknown, one resolved/unresolved conflict, B0, user-created CP-000001, and a subsequent changed-file warning. Source files remain unchanged.


### 19.3 End-to-end scenario acceptance
| Scenario | Inspector | Audit | Checkpoint | Advanced continuity |
|---|---|---|---|---|
| SOFTWARE | Java/Markdown, optional Git absence | No inference that tests pass | Objective/decision/next action | Changed source flags stale evidence |
| ACADEMIC | DOCX/text, PDF unsupported | Objective/revisions, adviser feedback, unknown next | Rationale and next revision | Draft drift and handoff |
| OPERATIONS | CSV owners/status, notes | Missing owner or unknown blocker | Human handoff | Declared supplier dependency impact |
| UNSTRUCTURED | Safe mixed inventory | High UNKNOWN/UNSUPPORTED | Targeted baseline question | Later scans gain continuity |


When M7A is approved, repeat each scenario with prefix suggestions, exact-term evidence lookup, and post-rescan index freshness checks.


### 19.4 Academic demonstration script
1. Launch from clean JDK-only command.
2. Import messy sample workspace.
3. Show supported, skipped, unsupported, and failed counts.
4. Run Context Audit; show source evidence and one unknown.
5. Answer targeted question.
6. Save checkpoint.
7. Show DLL structure and operation complexity.
8. Demonstrate six+ DLL operations and edge cases.
9. Modify/add one fixture file externally.
10. Rescan; show deterministic drift.
11. Resume and review stale evidence.
12. Add explicit dependency; show Graph impact and cycle protection if approved.
13. Find checkpoint/evidence by Hash index if approved.
14. If M7A is approved, type a partial term, choose a Trie suggestion, inspect source-linked inverted-index matches, then change a fixture and demonstrate index rebuild.
15. Generate source-backed human/agent handoff.
16. Explain uncertainty, privacy, and scope limits.


---


## 20. Risk register and mitigations


| Risk | Probability / impact from research | Mitigation |
|---|---|---|
| Glorified notes app | High / High | Immediate folder inspection, audit, drift before product is considered complete |
| Pretends to understand undocumented meaning | High / High | Explicit provenance and unknown states |
| Universal parsing scope explosion | High / High | Strict adapter whitelist and metadata-only fallback |
| CLI intimidating for nontechnical users | Medium / High | Guided numbered menus, examples, progressive disclosure, no required colors |
| False completion/correctness | High / High | Continuity readiness only; never percent-complete |
| Checkpoint friction | Medium / High | Auto-fill observed facts; one or two targeted prompts |
| Stale handoff | High / Medium | Mandatory freshness review |
| Multiple DSAs disagree | Medium / High | Canonical state; indexes by ID; graph revisioning; invariants |
| Git requirement undermines generality | Medium / High | Optional Git adapter |
| Extra dependencies break school build | Medium / High | JDK-only academic core |
| Symlink/path escape | Medium / High | NOFOLLOW_LINKS, containment checks |
| Secret leakage | Medium / Critical | Sensitive exclusions, redaction, consent before export/AI |
| DOCX/XML parser attack | Low–Medium / High | ZIP/XML hardening and bounds |
| Partial/corrupt persistence | Medium / High | Versioning, generation commits, backups |
| LLM hallucination | Future / High | AI_PROPOSED, citations, human confirmation |
| Agent cannot consume CLI output | Medium / Medium | Future JSON and noninteractive mode |
| Overengineered DSA count | Medium / High | DLL chronology, Graph dependency, Hash indexing only |
| Unknown score misrepresented as confidence | High / High | Assessable coverage, N/A and conservative readiness |
| Historical state silently changes | Medium / High | Immutable checkpoints and graph revisions |
| Terminal escape injection | Medium / High | Sanitize imported content before rendering |
| Search index out of sync | Medium / High | Revision checks, rebuildable postings, fail closed on stale indexes |
| Sensitive terms exposed by suggestions | Medium / High | Exclude sensitive/excluded content before tokenization; revalidate on result display |
| Too many DSAs for academic scope | High / High | Protect DLL baseline; make M6/M7/M7A approval- and schedule-dependent |
| First-time project appears useless | High / High | Three onboarding modes and B0 current-context report before checkpoints |
| Agent instructions mistaken for finished work | High / High | Artifact-role separation, explicit statement provenance, human review |
| Legacy handoff is stale or inaccurate | High / High | Preserve asserted dates, treat as unverified, show conflicts and unknowns |
| Fake historical checkpoint backfill | Medium / High | Distinguish B0 from CP; opt-in verified chronology only; synthetic seeds labeled |
| False cross-source contradictions | Medium / High | Match explicit field/entity IDs; ambiguous overlap prompts review rather than conflict |
| No persistence under course rules | High / High | Explicit in-memory session-only operation and honest restart limitations |


---


## 21. Reusable open-source technology register


**Policy:** Academic v1 uses the JDK standard library. External components are optional future adapters and require exact-version license/security verification, instructor approval where relevant, and NOTICE compliance. Research findings are not independent current license verification. **M7A requires no additional library:** Trie, posting lists, and tokenizer are implemented with JDK facilities and the project's existing custom Hash Table.


| Priority | Repository | Research-reported license and verification | Java fit | Decision / risk |
|---|---|---|---|---|
| 1 | https://github.com/eclipse-jgit/jgit | EDL 1.0 / BSD-3-Clause-equivalent terms; repo updated Aug 2026 in report | Pure Java Git core | Optional future Git evidence; check GHSA-vrpq-qp53-qv56 |
| 2 | https://github.com/commonmark/commonmark-java | BSD-2-Clause, canonical LICENSE.txt referenced | Java 11+, core no dependencies | Extended Markdown AST; strict parser for MVP |
| 3 | https://github.com/javaparser/javaparser | Dual Apache-2.0 / LGPL; choose Apache option | Java AST | Future; JDK Compiler Tree for MVP |
| 4 | https://github.com/apache/poi | Apache-2.0 canonical legal LICENSE | Java Office documents | Future richer Office formats; dependency footprint |
| 5 | https://github.com/apache/tika | Apache-2.0 plus third-party notices | Broad Java extraction | Future only; security review, GHSA-f58c-gq56-vjjf |
| 6 | https://github.com/java-diff-utils/java-diff-utils | Apache-2.0 reported; beware older archived forks | Java diff algorithms | Future readable drift |
| 7 | https://github.com/gitleaks/gitleaks | MIT in canonical GitHub metadata; strict LICENSE not opened in research | Go external CLI | Optional release security tool; not core runtime |
| 8 | https://github.com/apache/maka | Apache-2.0 verified in research | TypeScript, not Java dependency | Study append-only event/projection architecture |


**Related permissive-format references**
- GitHub Spec Kit: https://github.github.com/spec-kit/index.html — MIT reported, LICENSE-file verification partial.
- AGENTS.md: https://github.com/agentsmd/agents.md — MIT reported, LICENSE contents not independently checked in report.
- MADR: https://adr.github.io/madr/decisions/ — license not fully verified in research; use conceptual convention, not copied templates without checking.


**License intake record**
~~~text
Component / repository / exact version / commit:
Canonical LICENSE URL and verified date:
SPDX identifier and copyright holders:
NOTICE and transitive license obligations:
Security advisories / patched release:
What code or artifact will be reused:
Professor approval / reviewer:
Decision: APPROVED / DEFERRED / REJECTED
~~~
Do not treat “GitHub repository” as proof of MIT licensing. Copying code, depending on a library, and merely learning a design pattern have different obligations.


---


## 22. Competitor and interoperability reference register


| Reference | URL | Use |
|---|---|---|
| Linear project updates | https://linear.app/docs/initiative-and-project-updates | Short, structured progress and staleness UX |
| GitHub Issue Fields | https://github.blog/changelog/2026-07-02-issue-fields-are-now-generally-available/ | Typed work metadata |
| Jira dependencies | https://support.atlassian.com/jira-software-cloud/docs/view-and-manage-dependencies-on-the-program-board/ | Declared dependency impact |
| GitHub Spec Kit | https://github.github.com/spec-kit/index.html | Durable spec/plan/task artifacts |
| AGENTS.md organization | https://github.com/agentsmd | Agent instruction ecosystem |
| AGENTS.md repository | https://github.com/agentsmd/agents.md | Predictable agent-readable format |
| GitHub Copilot instructions | https://docs.github.com/en/copilot/tutorials/customization-library/custom-instructions/your-first-custom-instructions | Static instructions vs dynamic handoff |
| MADR decisions | https://adr.github.io/madr/decisions/ | Decision context and rationale |
| Claude-Mem | https://github.com/thedotmack/claude-mem | Agent session continuity; Apache-2.0 reported |
| Mem0 | https://github.com/mem0ai/mem0 | Agent memory infrastructure; Apache-2.0 reported, LICENSE check partial |
| Letta | https://github.com/letta-ai/letta | Persistent agent state; Apache-2.0 verified in report |
| Apache Maka | https://github.com/apache/maka/blob/main/README.md | Event log and projection architecture |
| Apache Maka LICENSE | https://github.com/apache/maka/blob/main/LICENSE | Verified Apache-2.0 in research |


These are competitive/architecture references, not dependencies. The goal is interoperability with existing artifacts, not replacing their ecosystems.


---


## 23. Standards, official documentation, and research sources


1. Interruption research — https://arxiv.org/abs/1805.05508
2. Java Files API — https://docs.oracle.com/en/java/javase/24/docs/api/java.base/java/nio/file/Files.html
3. WordprocessingML main document structure — https://learn.microsoft.com/en-us/office/open-xml/word/structure-of-a-wordprocessingml-document
4. Eclipse JGit organization — https://github.com/eclipse-jgit
5. JGit XXE advisory — https://github.com/advisories/GHSA-vrpq-qp53-qv56
6. CommonMark organization — https://github.com/commonmark
7. JavaParser LICENSE — https://github.com/javaparser/javaparser/blob/master/LICENSE
8. JavaParser organization — https://github.com/javaparser
9. Apache POI LICENSE — https://github.com/apache/poi/blob/trunk/legal/LICENSE
10. Apache Software Foundation GitHub — https://github.com/apache
11. Apache Tika LICENSE — https://github.com/apache/tika/blob/main/LICENSE.txt
12. Apache Tika Docker changes — https://github.com/apache/tika-docker/blob/main/CHANGES.md
13. Apache Tika XXE advisory — https://github.com/advisories/GHSA-f58c-gq56-vjjf
14. Apache Maka LICENSE — https://github.com/apache/maka/blob/main/LICENSE
15. JDK Compiler Tree API — https://docs.oracle.com/en/java/javase/25/docs/api/jdk.compiler/com/sun/source/tree/Tree.html
16. Git diff/rename documentation — https://git-scm.com/docs/git-diff
17. Java WatchService — https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/nio/file/WatchService.html
18. CLI Guidelines — https://clig.dev/
19. CLI Spec — https://clispec.dev/spec/v0.2/
20. Java StandardCopyOption — https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/nio/file/StandardCopyOption.html
21. Apache POI releases — https://poi.apache.org/download.cgi
22. Gitleaks GitHub organization — https://github.com/gitleaks/
23. java-diff-utils repository — https://github.com/java-diff-utils/java-diff-utils


All links above are preserved from the uploaded research's reference section or the frozen master plan. Version dates, licenses, and security conditions must be rechecked when actually integrating external code.


---


## 24. Future enhancement register — preserved suggestions


**Legacy agentic workflow adapters:** Broader `STATE.md`, `HANDOFF.md`, `AGENTS.md`, tool-specific agent memory, JSON schema adapters, n8n node/edge structure, and Git-based historical signals are optional *future* adapters with explicit versions/tests. Academic v1.2 includes only bounded deterministic Markdown headings and file-role recognition. Never present external workflow runtime state as proven merely from files.

**Context Search Engine (M7A):** Trie + Hash-backed inverted index are now a documented conditional implementation milestone, not an untracked future suggestion. Future *extensions* include incremental updates, boolean/phrase search, typo tolerance, term frequency ranking, and cross-workspace search; none are implied by lexical v1 search.


**CLI automation:** stable subcommands, --format json, noninteractive mode, documented exit codes, stdin/stdout pipelines, deterministic machine-readable errors. Still terminal-only.


**Agent handoff:** export a separate dynamic handoff alongside existing AGENTS.md, never silently overwrite agent instructions; future JSON schema; optional CLI/MCP integration only as an automation adapter, not a GUI.


**Git:** optional read-only Git metadata (branch, HEAD, dirty files, commit summaries, rename evidence) through approved JGit. Absence of Git is normal.


**Documents:** commonmark-java, POI, Tika, JavaParser when scope and security justify them; PDFs remain metadata-only until explicitly supported.


**Diff and monitoring:** java-diff-utils for readable text changes; WatchService as an optimization; exact hash-based truth remains primary.


**Security:** optional Gitleaks during packaging/release; not an extra install required of ordinary users.


**AI:** user-opt-in, selected sanitized evidence only, draft current state/decisions/blockers/next actions, evidence ID per proposed claim, AI_PROPOSED status, user approval; no invented dependencies or direct mutations.


**Team / multi-workspace:** cross-workspace indexing, collaboration and synchronization only after conflict resolution, permissions, and persistence are mature.


**Additional DSAs:** Stack undo and Heap priority queue only when their user benefits are proven; BST remains low priority.


**No future GUI.** Public-product usability improvements must be achieved through guided CLI, discoverable help, readable reports, accessible terminal behavior, and scriptable automation.


---


## 25. Architecture Decision Record (ADR) index


- ADR-001: CLI-only forever; no GUI/web/desktop roadmap.
- ADR-002: Evidence-backed work continuity is the product identity.
- ADR-003: Deterministic offline core; AI optional and never authoritative.
- ADR-004: DLL owns checkpoint chronological ordering.
- ADR-005: Directed Graph owns declared dependency relationships.
- ADR-006: Hash Table owns secondary indexes only.
- ADR-007: Four project profiles share one canonical domain model.
- ADR-008: UNKNOWN, UNSUPPORTED, MISSING, STALE, and CONFLICTING are distinct.
- ADR-009: Continuity readiness is not project completion or correctness.
- ADR-010: Read-only scanning with bounded safe defaults.
- ADR-011: Versioned local persistence conditional on academic approval.
- ADR-012: Handoffs preserve evidence provenance and freshness.
- ADR-013: Historical checkpoints are immutable; corrections supersede.
- ADR-014: Graph edges require explicit provenance.
- ADR-015: External licenses and security advisories checked at exact-version adoption.
- ADR-016: M7A Context Search uses a custom Trie for term discovery and a Hash-backed inverted index for evidence retrieval; both are derived projections, not canonical truth.
- ADR-017: Search only supported extracted and approved text; version, invalidate, rebuild, and disclose index coverage.
- ADR-018: One Project Bootstrap Service supports NEW_EMPTY, IMPORT_FOLDER, and IMPORT_LEGACY_CONTEXT; all modes share WorkspaceState.
- ADR-019: B0 is a first-observation/current-context baseline, never an automatically created DLL checkpoint; CP-000001 needs explicit approval.
- ADR-020: Legacy notes and agent instructions are source-scoped evidence, not verified implementation, runtime results, or trustworthy historical event chronology.
- ADR-021: Deterministic cross-source claim reconciliation requires explicit shared identity; unresolved contradictions stay visible.
- ADR-022: First-use context reports and source-backed next-action candidates are allowed before checkpoint creation, with limitations disclosed.


**Change request template**
~~~text
Change ID:
Problem:
Affected milestone(s):
Affected modules and DSA invariants:
Why frozen design is insufficient:
New functionality / dependency / risk:
Impact on academic compliance:
Migration / backward compatibility:
Tests:
Decision: ACCEPT / DEFER / REJECT
Approved by / date:
~~~


---


## 26. Final acceptance milestones and definition of done


The previous “gates” are **now tracked as milestones**:


| Milestone | Completion evidence |
|---|---|
| M0 Academic alignment | Professor constraints documented; JDK-only shell; five demo records |
| M1 Inspector / bootstrap | Three entry modes, supported legacy heading extraction, B0, safe four-profile inventory and honest scan diagnostics |
| M2 Audit / reconciliation | Explainable deterministic findings, conflict/unknown/source review, and uncertainty coverage |
| M3 DLL | Custom linked structure, six+ operations, B0 distinct from approved CP-000001, invariants, complexities |
| M4 Drift | Reproducible file comparisons and stale-checkpoint warnings |
| M5 Handoff | Evidence-backed portable resume and handoff |
| M6 Graph | Explicit dependency management, BFS/DFS, cycle handling |
| M7 Hash | Collision-safe indexing, resize, rebuild, cross-DSA consistency |
| M7A Context Search | Prefix Trie, Hash-backed inverted postings, source-linked results, safe rebuild; conditional approval |
| M8 Persistence | Approved versioned recovery or explicit in-memory fallback |
| M9 Usability and release | Four profiles × zero-history/legacy/returning fixtures, help/errors, README, privacy, defense demo |
| M10 Future evolution | Documented optional integrations, each separately approved |


**Academic minimum release:** M0–M5 and M9 including `NEW_EMPTY` and `IMPORT_FOLDER`, bounded supported legacy Markdown parsing, B0 review, and user-confirmed checkpoint initiation; M8 has an explicit in-memory fallback when file writes are not authorized.  
**Full frozen implementation target:** M0–M9, plus M7A when approved; M6/M7/M7A are integrated but explicitly conditional on instructor approval, schedule, and passing core acceptance. M10 remains a maintained backlog.  
**No unapproved dependency may block the minimum release.**


### Final frozen statement
> Breadcrumbs is a Java, local-first, CLI-only work-continuity application that bootstraps new or existing projects without prior Breadcrumbs checkpoints; safely inspects supported files and legacy agent handoffs as evidence, reconciles documented gaps and contradictions, establishes a truthful first-observation B0, and creates native DLL history only from explicitly accepted checkpoints. It audits continuation readiness through explainable rules, detects later drift through snapshots, and produces source-linked baseline-context and resume/handoff reports. A custom Directed Graph explains downstream effects of explicitly declared task dependencies; a custom Hash Table accelerates lookup while preserving one canonical project model. A proposed custom Trie plus Hash-backed Inverted Index makes supported evidence and checkpoints discoverable by prefix and exact term, with source locations and safe rebuilds. The application remains usable for technical and nontechnical projects, offline, without Git or AI. Milestones and ADRs control delivery and changes.


**Next implementation artifact:** A separate code-level contract pack (exact Java classes/interfaces/enums, serializer grammar, test fixtures, and pseudocode) may be generated from this document without changing its product decisions. M7A's detailed Trie/posting-list contracts and acceptance tests are now part of this master specification.


---


## 27. Source provenance and completeness note


This document intentionally preserves the uploaded report's: problem evidence; competitor categories; open-source shortlists and license caveats; Inspector architecture; supported-file matrix; DOCX/Java/JDK feasibility; snapshot/rename/Git/WatchService recommendations; EvidenceRecord and provenance model; four-dimension audit; nine rules and score treatment; four worked workspace scenarios; DLL/Graph/Hash/Stack/Heap/BST prioritization; CLI UX; handoff format; persistence; security; four implementation priorities; acceptance tests; risk register; and public-product extension suggestions.


**Explicit user override:** The report recommended a possible future GUI. That suggestion is **not adopted**. All academic and future product work stays CLI-only.


**Implementation elaborations beyond the report:** Canonical WorkspaceState, Graph revision references, collision/ambiguous-rename handling, audit assessable-coverage safeguard, detailed Java APIs, generation-based persistence, exact milestone statuses, and ADR-013..015 are design recommendations added to make the frozen architecture cohesive and testable. They are not claims that the original research empirically validated those choices.


**v1.1 additions (user-directed, not findings from the uploaded research):** Partner-facing project overview, concrete thesis-folder journey, short pitch, collaboration discussion, proposed Context Search Engine using Trie + inverted index + existing Hash Table, M7A, new search contracts/risks/tests, and ADR-016..017. The proposed search enhancement is not silently elevated to professor-approved or minimum-release scope.

**v1.2 additions (user-directed design refinement, not findings from the uploaded research):** Three-entry Project Bootstrap Service, explicit B0 versus CP semantics, controlled legacy state/handoff/agent-context Markdown parsing, artifact-role and provenance distinctions, deterministic conflict policy, source-backed next actions, first-use baseline report, security requirements, acceptance fixtures, and ADR-018..022. No code implementation, verified integrations, restored historical timeline, or professor approval is claimed.


**Final warning:** This document is an implementation plan, not a verified dependency license audit, a completed software test report, or a professor approval record. Track those as milestone evidence.
