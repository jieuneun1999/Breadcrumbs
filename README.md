# Breadcrumbs

**Leave context behind. Pick up where work stopped.**

Breadcrumbs is a proposed **local-first, offline-capable, Java 17+ console application** for preserving reliable project continuity. It helps users inspect a new or existing workspace, identify documented facts and missing context, create confirmed checkpoints, compare later file changes, and produce source-backed handoffs.

> **Status: B1 PASS / DEMO_READY on Windows with owner-accepted JDK 25, targeting Java 17 (2026-10-30).** The runnable session-local CLI and custom DLL pass 3,780 assertions. Deeper mixed-operation and fault-injection checks also pass. Actual JDK 17 runtime execution remains unverified and is no longer a B2 blocker after owner acceptance of the installed runtime. See [B1 execution and evidence](docs/B1_EXECUTION.md). The academic core is terminal-only and JDK-only; no GUI, network, Git, or AI subscription is required. B2-B7 remain unimplemented.

## Build, run, and test B1

Install or use an existing JDK 17+ with `java` and `javac` on PATH. No Maven,
Gradle, database, IDE, or external dependency is required. From Windows CMD:

```bat
run.bat
scripts\test.bat
run.bat < tests\cli-demo-input.txt
```

Both batch launchers resolve their own location. You can invoke them by full
path from another directory, including a path containing spaces. Compilation
targets Java 17 and writes only ignored `build` output. A compiler failure
returns exit code 1 and prevents execution of stale classes.

Equivalent manual commands from the repository root work in Windows CMD,
PowerShell, and Unix shells:

```text
javac --release 17 -encoding UTF-8 -Xlint:all -Werror -d build/manual -sourcepath src src/breadcrumbs/Main.java
java -cp build/manual breadcrumbs.Main
```

Compile and run the dependency-free tests in Windows CMD or PowerShell:

```text
javac --release 17 -encoding UTF-8 -Xlint:all -Werror -d build/manual-tests -sourcepath "src;tests" tests/breadcrumbs/AllTests.java
java -cp build/manual-tests breadcrumbs.AllTests
```

On Unix, replace `"src;tests"` with `"src:tests"`. Unix execution has not been
tested. On Windows, the optional broader launcher verification uses the bundled
PowerShell and writes transcripts and source hashes under `build/verification`:

```text
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/verify-windows.ps1
```

The execution policy override applies only to that process. The application and
its Java test suite do not require PowerShell. See the evidence record for actual
commands, Windows results, and the JDK 17 runtime verification limit.

## Use the B1 menu

The application starts with an empty custom list. Option 12 loads five explicitly
synthetic checkpoints for a fictional study planner journey. Repeated loading
skips existing fixture IDs without replacing records. These fixtures do not
describe Breadcrumbs development progress.

| Option | Behavior |
| --- | --- |
| 1 | Preview and confirm a session-local native checkpoint, then append it |
| 2 | Prepend a synthetic `DEMO-*` checkpoint |
| 3 | Insert a synthetic checkpoint at a zero-based position from 0 through size |
| 4 | Find an immutable checkpoint by exact ID |
| 5 | Confirm removal of a checkpoint from this session |
| 6, 7 | Traverse forward or backward and display checkpoint fields |
| 8 | Read the tail checkpoint |
| 9 | Render the DLL links |
| 10 | Display size and empty state |
| 11 | Check boundaries, reciprocal links, counts, cycles, and ID uniqueness |
| 12 | Load or replenish the five synthetic fixtures |
| 0 | Exit and discard the session |

Enter `/cancel` at a prompt to return to the menu. Only `yes` confirms creation
or deletion. EOF cancels the pending operation and exits cleanly. Invalid menu
values, positions, blank required fields, unknown statuses, control characters,
oversized lines, duplicates, and missing IDs produce controlled messages.
Titles allow 120 characters and summaries allow 600. A blank summary becomes
`UNKNOWN`. Status values are `PLANNED`, `IN_PROGRESS`, `COMPLETED`, `BLOCKED`,
and `UNKNOWN`. Status is a user's statement or a synthetic field, not test proof.

Native IDs start at `CP-000001` and increase after confirmed creation. Deletion
does not recycle IDs during that session. Synthetic IDs use `DEMO-` followed by
1-40 uppercase letters, digits, or hyphens. B0 is rejected as a checkpoint ID.
Native creation is append-only. Synthetic positional exercises illustrate link
order, not verified event chronology. The tail is latest in list order, which can
differ from timestamp order when synthetic records are inserted or loaded.

**Nothing is saved between program runs.** B1 does not scan a directory, generate
B0, import old files, attach source evidence, or implement the complete B3
checkpoint workflow. Human confirmation here records a current session statement.

## Custom DLL and complexity

`Checkpoint` is an immutable Java record with validated ID, title, summary, enum
status, timestamp, and enum origin. `CheckpointList` owns private head, tail, and
size fields. Package-private `CheckpointNode` owns explicit previous and next
references. Append, prepend, positional insertion, and deletion manipulate these
references manually. The CLI uses this list as its only checkpoint store.
No Java collection implements or indexes the required DSA.

Search returns an immutable value or null. Deletion returns a boolean. Invalid
API inputs throw `IllegalArgumentException`, which the menu handles. Traversals
return fresh arrays of immutable values; mutable nodes never escape to the CLI.

Let n be the active node count. Bounds treat validated, length-limited field
comparisons as constant cost. The table reports worst-case core operation costs.

| Operation | Time | Auxiliary space | Explanation |
| --- | --- | --- | --- |
| `append` | O(n) | O(1) | Linear duplicate check, then O(1) tail pointer changes |
| `prepend` | O(n) | O(1) | Linear duplicate check, then O(1) head pointer changes |
| `insertAt` | O(n) | O(1) | Validate position, check duplicates, locate node, then O(1) link changes |
| `findById` | O(n) | O(1) | Manual linear search |
| `removeById` | O(n) | O(1) | Linear search, then O(1) unlink |
| `traverseForward` | O(n) | O(n) | Follow next links into a result array |
| `traverseBackward` | O(n) | O(n) | Follow previous links into a result array |
| `getLatest`, `getOldest` | O(1) | O(1) | Read tail or head |
| `size`, `isEmpty` | O(1) | O(1) | Read stored count |
| `displayStructure` | O(n) | O(n) | Render each active ID |
| `validateIntegrity` | O(n²) | O(1) | O(n) cycle/link/count checks plus pairwise duplicate checks |
| Five-fixture demo load | O(n) | O(1) | Constant five candidates with linear lookup/insertion checks |

Every relevant CLI operation also prints the structure, which adds O(n) time
and O(n) output space. A failed validation can return earlier than the listed
worst-case bound. The frozen master plan illustrates O(1) append/prepend pointer
work and O(n) invariant checking. B1 reports the additional uniqueness work
explicitly; the planned B5 Hash Table is not implemented early to hide that cost.

## Verified behavior and limits

On Windows 10 with JDK 25, compilation with `--release 17 -Xlint:all -Werror`
passes. The test runner passes 3,721 DLL/domain assertions and 59 CLI assertions.
Tests cover empty, singleton, and multi-node mutations, both traversal orders,
duplicates, pointer corruption, cycles, invalid forms, cancellation, EOF, native
ID non-reuse, repeated demo loading, and reset on a new session. Actual CMD
launches pass from the root, from another directory, and through paths containing
spaces. Missing JDK and compiler-failure paths return exit 1.

Class files target Java 17 major version 61. Actual JDK 17 runtime execution,
Unix execution, and the instructor's laptop remain unverified. This is the B1
academic DLL foundation, not completion of M0-M9 or product release acceptance.
Persistence, directory inspection/B0, source reconciliation, drift, handoff,
Graph/Hash/Trie, document parsers, Git integration, AI, voice, and crash recovery
remain in their later batches. There are no known failing B1 automated tests.

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
| **B1** | Java 17 CLI scaffold, manually implemented Doubly Linked List, five DEMO checkpoints, six+ operations and tests | PASS / DEMO_READY on owner-accepted Windows/JDK 25; B2 foundation ready |
| **B2** | Three entry modes, read-only project folder inspection and B0 baseline | NOT_STARTED |
| **B3** | User-confirmed checkpoints, basic local TXT save/load and restart continuity | NOT_STARTED |
| **B4** | Evidence audit, drift detection, checkpoint retrieval and handoff | NOT_STARTED |
| **B5** | Manually implemented Directed Graph and Hash Table in separate testable units | NOT_STARTED |
| **B6** | Custom Trie + Hash-backed Inverted Index | NOT_STARTED |
| **B7** | Full crash recovery, end-to-end hardening and selectively packaged optional integrations | NOT_STARTED |

**B1 implementation branch:** `feat/m0-dll-foundation`. See [B1 execution and evidence](docs/B1_EXECUTION.md) for the implemented scope, decisions, tests, and review limits. Follow-up work keeps inspector, persistence, Graph/Hash/Trie, external parsing libraries, and voice AI in their planned batches.

**Academic and scope boundary:** The primary DLL and all proposed supporting DSAs are manually implemented, not Java's built-in `LinkedList`/`HashMap` or third-party DSA substitutes. Java standard APIs for file I/O and hashing are support utilities, not graded DSA replacements. The team is proceeding with the full roadmap as its **internal development scope**; this is **not a claim that the instructor has formally approved every advanced DSA, external library or feature**. The Java-only, dependency-free academic core must remain independently runnable.

**Continuity invariant:** **B0 is the observed initial baseline, not a DLL checkpoint**. **CP-000001** requires explicit human confirmation. Any TXT save/load functionality must restore checkpoint records into the custom DLL rather than replace it with a built-in collection. Read-only import may never execute source project files.

## Working principle

**Evidence first.** Report what can be observed; separately label what source documents claim and what users confirm. Preserve unknowns, conflicts, stale information, and unsupported formats. Do not execute imported workflows or expose secrets.

---

Documentation baseline: **v1.2, 2026-10-22**.
