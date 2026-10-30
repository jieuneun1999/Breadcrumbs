# Breadcrumbs — Implementation Batch Execution Guide v1.0

**Decision date:** 2026-10-30  
**Status:** Team-approved implementation sequence; NOT a claim of instructor approval or completed functionality.  
**Canonical product specification:** [Master Implementation v1.2](BREADCRUMBS_MASTER_IMPLEMENTATION_V1.2.md)  
**Import and truth semantics:** [First-Import Contract v1.2](BREADCRUMBS_FIRST_IMPORT_CONTRACT_V1.2.md)

## Why this guide exists

This guide translates the M0–M10 product milestone roadmap into **reviewable Codex implementation batches**. It controls **build order and scope**, not underlying product truth or DSA requirements. In the event of conflicting behavior/semantics, follow the master plan and first-import contract; document deviations as ADRs, not silent changes. Do not rewrite the frozen v1.2 master/PDF just to reorder engineering work.

**Current observed repository status (2026-10-30):** documentation-only: README, master Markdown/PDF, first-import contract. No Java source, tests, or launch scripts were present at assessment. All batches below are **NOT_STARTED** until code/tests/evidence establish otherwise.

**Academic delivery:** Java 17+, CLI/terminal-only, JDK-only launchable core, no IDE/database/internet/Docker required. All **graded DSA operations** must use manually written node/algorithm implementations; do not delegate to `LinkedList`, `HashMap`, `TreeMap`, a graph package, a Trie package, or a search engine. Standard JDK primitives, arrays, strings and I/O utilities are for supporting operations, not substitutes for DSAs. Validate Windows CMD portability. The team has chosen to pursue the full architecture; this is **not an assertion of formal instructor authorization**. Protect a compliant, independent DLL-based academic core if permitted scope later changes.

## Batch order at a glance

| Batch | Maps to source milestones | Bundle together | Prerequisites | Completion evidence |
| --- | --- | --- | --- | --- |
| **B1 — DLL foundation** | M0 + DLL prototype of M3 | Java scaffold, domain model, custom DLL, demo fixtures, CLI, validation, test harness, Windows launcher | None | JDK-only runnable CLI + custom DLL tests |
| **B2 — Safe bootstrap** | M1 + first-import contracts | NEW_EMPTY, IMPORT_FOLDER, IMPORT_LEGACY_CONTEXT, read-only bounded inspector, TXT/MD/CSV, file inventory, SHA-256, B0 | B1 | Safe import fixture suite, provenance, no fabricated CP |
| **B3 — Checkpoint continuity** | Complete M3 + basic M8 | Human-confirmed CP workflow, basic versioned local-file save/load, DLL reconstruction on restart, latest checkpoint resume | B1; B2 B0 model | Restart/round-trip and corruption/error tests |
| **B4 — Context, drift and handoff** | M2, M4, M5 | Evidence-driven audit, UNKNOWN/CONFLICTING reconciliation, old/new fingerprint drift, retrieval, text/Markdown handoff | B2 + B3 | Deterministic source-linked scenario tests |
| **B5 — Dependency and retrieval structures** | M6 then M7 | Custom Directed Graph, BFS/DFS/cycle detection; independently custom Hash Table and canonical indexes | B3; stable domain IDs | Separate DSA tests; consistency after mutation |
| **B6 — Context search engine** | M7A | Custom Trie plus Hash-backed Inverted Index, exact-term and prefix results, source positions, invalidation/rebuild | B2 + B3 + B5 Hash Table | Search freshness and exclusion tests |
| **B7 — Hardening and extension** | Full M8 recovery, M9, selective M10 | Crash-safe storage, compatibility, report/demo, advanced parsers and optional integrations *only after core* | B1–B6 as applicable | Clean-environment release tests and evidence |

**Sequencing adjustment:** Basic local TXT/structured-text persistence is intentionally implemented with checkpoint workflow in **B3**, earlier than the full **M8 crash recovery**. B3 is not complete M8: atomic generation staging, backups, corruption recovery and recovery testing are deferred to B7. The master milestone identifiers remain unchanged.

## B1 — First Codex PR: DLL foundation

**Suggested branch:** `feat/m0-dll-foundation`  
**Suggested PR:** `Bootstrap Breadcrumbs Java CLI and implement custom DLL checkpoint operations`

**Implement in one coherent PR:**

1. Minimal Java 17 source tree, `Main`, console menu, domain types, `dsa/dll`, lightweight no-dependency tests, README run instructions and `run.bat`. Keep source portable and avoid premature abstractions.
2. Immutable or safely encapsulated `Checkpoint` data with meaningful fields (ID, title, summary, timestamp, status/origin as appropriate). Validate identifiers and input. Clearly label **five synthetic demo records**; never treat as imported user history.
3. **Manual `CheckpointNode` and `CheckpointList`** with `head`, `tail`, `previous`, `next`, `size`; append, prepend/insert, find, delete, traverse forward/backward, latest, render structure and invariant validation. Own all pointer mutations; enforce ID uniqueness.
4. At least **six independently demonstrable DSA operations** in CLI with consistent validation. After each relevant operation display node ordering/links and honest time complexity (e.g. append O(1) with tail, find O(n), removal by known node O(1) but locating by ID O(n)).
5. Tests for empty/single/multi-node, head/middle/tail insertion and deletion, duplicate IDs, missing IDs, both traversal orders, null boundary links, size, no cycles, invalid console input.
6. Document precise `javac`/`java` commands and demonstrate a **clean JDK 17 Windows CMD launch**. Compile/run and tests must be reported with actual commands and outcomes, not assumed.

**Do not implement in B1:** folder ingestion, legacy parsing, storage/persistence, PDFs, external Java dependencies, Directed Graph, Hash Table, Trie, voice AI, network services, or a huge framework. Interfaces may be documented but avoid nonfunctional placeholder subsystems.

**B1 acceptance checklist:**
- [ ] Clean JDK 17 compile and execution without Maven/Gradle, database, network, or IDE.
- [ ] All required DLL node/link logic is custom; no built-in collection replaces required operations.
- [ ] Five synthetic records with >=3 fields; >=6 CLI DSA operations.
- [ ] Visible structure + accurate complexity after operations.
- [ ] All specified edge-case tests pass; actual output/screenshots are recorded.
- [ ] No claim of directory import, B0 persistence, or resume functionality yet.
- [ ] Codex opens a reviewable PR instead of directly pushing the implementation into `main`.

## B2 — Project entry and safe inspector

Implement the three explicit entry modes and record a **B0 first-observation baseline**, which is never a DLL checkpoint. Read-only folder traversal must prevent symlink escapes; bound file sizes/depth/time; exclude secrets (`.env`), `.git`, binaries, and generated directories. TXT/MD/CSV only initially. Distinguish supported/skipped/unsupported/failed. SHA-256 identifies exact observed bytes, not project correctness. Recognize `AGENTS.md`/`CLAUDE.md` as instruction files, not proof of work. Imported `state.md`/`HANDOFF.md` statements are SOURCE_REPORTED; no execution or automatic CP backfill.

**Exit:** Empty folder, README-only, mixed files, symlink traversal, secrets, and legacy handoff fixtures produce truthful, source-located output. First scan creates B0 but **not CP-000001**.

## B3 — Checkpoint creation, TXT persistence and restart

After B0 and core DLL contracts stabilize, add explicit user-reviewed first native `CP-000001`; checkpoint insertion remains DLL-owned. Define a documented, escaped/versioned local text storage codec and safe application data directory separate from the read-only imported root. Read checkpoints from files and **rebuild the custom DLL** on startup; never use storage records as the in-memory DSA. Test empty state, ordered round-trip, duplicate/corrupt rows, partial files and error reporting. B3 provides basic working continuity, but does **not** promise crash-safe atomic multi-file transactions (B7).

**Exit:** Create → close → relaunch → restore → traverse/resume with identical checkpoint order; no evidence claims invented.

## B4 — Audits, drift and handoffs

Combine M2/M4/M5 only once inspection and durable checkpoints exist. Reconcile literal claims by explicit IDs; distinguish OBSERVED, SOURCE_REPORTED, USER_CONFIRMED, DERIVED, UNKNOWN, UNSUPPORTED, CONFLICTING and STALE. Do not promote timestamps, checkboxes, old handoff text, or agent instructions into verified outcomes. Compare fingerprinted snapshots; report modified/added/deleted/unchanged and ambiguous renames honestly. Export human-readable source-linked resumption and handoff summaries without leaking sensitive paths/values.

**Exit:** Deterministic fixtures with conflicting same-ID status, unknowns, drift, and handoff provenance.

## B5 — Graph and Hash (separate internal PRs)

Implement custom Graph nodes, directed adjacency, declared task dependencies, BFS/DFS, cycle handling, and impact analyses. Test isolated/disconnected/diamond/cycle/cross-workspace cases. Then implement custom Hash Table buckets and chaining, collision, negative hashing, resize, lookup/removal, and index rebuild. Graph/Hash are derived/support structures: they must not diverge from canonical workspace/checkpoint state. **Do not merge both untested implementations into a single opaque Codex change.**

## B6 — Trie + inverted index

Custom prefix-tree nodes and custom Hash-backed term-to-source postings; bound suggestions and extractable text. Index only evidence actually extracted; source locator and checkpoint ID must resolve to current canonical data. Require index versioning/rebuilding when files/checkpoints change and secret exclusion. This is **lexical/prefix search**, not semantic retrieval.

## B7 — Delivery hardening and optional enhancements

Complete M8 staging/backups/recovery and M9 four-profile/Windows demo, screenshots, complexity proof, source/license inventory and end-to-end test suite. Select advanced DOCX/XLSX/PPTX/PDF parsers, Git, and voice AI only as independently packaged optional adapters; the JDK-only academic core must still start without them. Never claim the app is verified on Sir's laptop without testing it there.

## Codex operating contract for every batch

- **Read first:** README, this batch guide, master v1.2, first-import contract. Cite any spec conflict in a short ADR/PR description.
- **Reviewable change:** feature branch + focused PR; no surprise refactors of frozen specification docs.
- **Evidence before completion:** report modified paths, commands actually executed, test pass/fail, limitations, and commit/PR URL; do not infer success merely from code generation.
- **Independent operations:** no use of built-in Java collection classes to *implement* required DSAs. JDK I/O/utilities are allowed as supporting implementation choices in the project plan; if classroom rules prohibit them, academic-compliance decisions supersede this internal sequencing guide.
- **Safety:** imported roots are read-only, never run untrusted code, never load credentials, never invent prior progress.
- **Complexity:** annotate true complexity including lookup costs; keep a deterministic console demo after each batch.
- **Release gate:** complete B1 before beginning B2. Use real acceptance evidence rather than marking future batches complete on the basis of this plan.

## Status log

| Batch | Status | Evidence |
| --- | --- | --- |
| B1 | NOT_STARTED | Documentation-approved scope only |
| B2 | NOT_STARTED | — |
| B3 | NOT_STARTED | — |
| B4 | NOT_STARTED | — |
| B5 | NOT_STARTED | — |
| B6 | NOT_STARTED | — |
| B7 | NOT_STARTED | — |

Update this log only with actual verified PRs/commits/tests. Post concise implementation changes in Breadcrumbs **#dev-updates**; use **#current-milestone** for current goals and acceptance state.
