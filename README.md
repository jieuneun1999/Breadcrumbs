# Breadcrumbs

**Leave context behind. Pick up where work stopped.**

Breadcrumbs is an in-development **local-first, offline-capable, Java 17+ console application** for preserving reliable project continuity. B1-B3 provide a custom DLL sandbox, safe initial-project baseline, confirmed project checkpoints and local restart continuity. Later batches add drift comparison and portable handoffs.

> **Status: B3 DEMO_READY on Windows with owner-accepted JDK 25, targeting Java 17 (2026-10-30).** The junction-enabled run passes 3,780 B1, 192 B2 and 138 B3 assertions. Separate Java processes demonstrate B0/checkpoint restart, source-gone import restoration, workspace isolation and corrupt-load handling. See [B3 execution and evidence](docs/B3_EXECUTION.md). Actual JDK 17 runtime and instructor-machine execution remain unverified. B4-B7 remain unimplemented. Feature PRs remain unmerged.

## Build, run, and test

Install or use an existing JDK 17+ with `java` and `javac` on PATH. No Maven,
Gradle, database, IDE, or external dependency is required. From Windows CMD:

```bat
run.bat
scripts\test.bat
run.bat --dsa < tests\cli-demo-input.txt
run.bat --no-color
run.bat "--storage-root=C:\Temp\Breadcrumbs Demo" --color=always
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
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/verify-b2-windows.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/verify-b3-windows.ps1
```

The execution policy override applies only to that process. The application and
its Java test suite do not require PowerShell. See the evidence record for actual
commands, Windows results, and the JDK 17 runtime verification limit.

## Resume and record project context

The normal startup shows a compact ASCII breadcrumb trail and the real number of
saved workspaces. Choose **1** to open a saved project, **2** to create/import,
**3** for the active project, **4** for the independent DSA sandbox, or **0** to exit.
No recent-selection record is maintained; select a workspace explicitly.
Unavailable/corrupt saved projects are disclosed and retained for manual recovery.
Opening a project never scans its original folder or executes source commands.

The active-project menu offers B0/source details, checkpoint creation, latest
checkpoint, forward/backward history, Save, switching, information and DLL diagnostics.
Creation collects objective, progress, an explicit `UNKNOWN`/`NONE_REPORTED`/`BLOCKED`
blocker state, next actions, decisions/rationale, owner and selected B0 evidence IDs.
Blank optional fields stay `UNKNOWN`. Review the preview and type `yes` to append.
Declining leaves the DLL and next ID unchanged. The first ID is `CP-000001`.
IDs continue from a persisted high-water counter, independently of list size.
There is no project-level deletion/edit/archive in B3; historical corrections
and archival are deferred. Sandbox deletion remains available.

Each checkpoint contains its immutable project context directly in the DLL value.
Its workspace, original snapshot, previous checkpoint and evidence IDs are validated.
Source-reported claims retain their original verification, locators and SHA-256
even when a person confirms their present assessment. Saving a checkpoint does
not run tests or rehash source files. Restored context is a **saved observation**;
source freshness remains `UNKNOWN`, including when its original folder disappears.
Drift checks and portable handoff export belong to B4.

## Storage and terminal appearance

Default local storage is `${user.home}/.breadcrumbs/workspaces/<workspaceId>/state.txt`.
Each file is self-contained; there is no required manifest to become inconsistent.
Use `--storage-root=<path>` for separate demo/test storage. Relative overrides
resolve against the Java working directory; `run.bat` sets that to the repository.
Drive/network roots and linked/reparse storage paths are rejected. Storage cannot
be inside the imported project. Tests always inject disposable `build` locations.

After B0 confirmation, consent enables auto-save after confirmed checkpoints.
Explicit Save can enable persistence later. Failed saves remain visibly `UNSAVED`
and retain the in-memory state for retry. Switching and exit require Save or an
explicit discard when dirty. EOF tries an enabled save and warns if unsaved state
will be lost. In session-only mode, nothing survives shutdown.

The [versioned UTF-8 codec](docs/B3_STORAGE_FORMAT.md) preserves baseline, policy,
inventory, original claims, reviews, native chronology and next ID. It uses Base64
fields and SHA-256 integrity, not Java object serialization. It is **not encrypted
or authenticated**. Store only appropriate local context. Conservative sensitive
markers are rejected, but that is not a universal secret detector. Original
documents, binaries, credentials and unrestricted snippets are never copied.
Writes validate the complete data, use a temporary sibling, require atomic file
replacement and verify readback. Unsupported atomic moves fail without the weaker
replacement fallback. No power-loss durability, backup rotation, concurrent writer
coordination, or multi-file crash recovery is claimed. Those are B7 work.

The brand uses warm amber-gold ANSI color `38;5;214` for headings and prompts.
Green/yellow/red mark fixed success/warning/error labels. User/source text stays
in the default foreground and control characters are escaped. Labels also spell
out their meaning. `--no-color`, `NO_COLOR`, redirected output, `TERM=dumb`, and
uncertain Windows CMD environments use plain text. `--color=always` opts into
ANSI even when redirected; `--no-color` and `NO_COLOR` take precedence. Auto color
needs an interactive console and a known terminal; on Windows this includes
Windows Terminal/ANSICON. Terminal palettes can vary. The narrow layout splits
the tagline. No artificial delays, shell VT setup, animation or GUI is required.

```text
o -- . -- o    B R E A D C R U M B S
Leave context behind. Pick up where work stopped.

Storage: Ready
Saved projects: 0
```

The B3 Windows harness records actual separate-process scenarios A-F, ANSI/no-color
and `NO_COLOR`, quoted launcher paths, ACL denial and junction rejection. The B3
suite adds corruption/encoding/reference tests, Unicode/multiline round trips,
checkpoint cancellation, dirty-save recovery and same-workspace switching.
The [execution record](docs/B3_EXECUTION.md) binds results to source hashes.
Reconstructing n checkpoints uses public append with duplicate checking, so it is
O(n²), as is full integrity validation. Serialization adds linear byte I/O and
bounded evidence/inventory reference checks; it is not an O(1) DLL operation.

## Start a project and establish B0

Run `run.bat`, then select **2** to add or create a project:

1. Choose `NEW_EMPTY`, `IMPORT_FOLDER`, or `IMPORT_LEGACY_CONTEXT`.
2. Enter a display name and select `GENERAL`, `ACADEMIC`, `OPERATIONS`, or
   `SOFTWARE`. A blank profile selects `GENERAL`.
3. For a new project, enter objective, deliverable, next action, optional owner,
   and blockers. Blank information stays `UNKNOWN`. An existing empty directory
   is optional; the program does not create directories or invent file evidence.
4. For imports, provide a local directory and type `yes` to approve the displayed
   read-only scan policy. A README-only folder and an empty folder are valid.
5. Review the inventory, coverage, source-reported claims, unknowns, and explicit-ID
   conflicts. Use `c` to confirm a present assessment, `r` to reject, `u` or Enter
   to leave unknown, or `s` to defer the remaining candidates.
6. Type `yes` at the final B0 confirmation prompt. Cancellation preserves the
   previously active workspace and creates no checkpoint.

Choose **3** at startup to open the active project menu, then **1** for B0 with optional paged inventory and
source-claim details. It works with zero native checkpoints. `/cancel` discards a
pending proposal. Scans offer continuation/cancellation after each 25 visited
entries. The time budget includes these pauses; delayed continuation can yield
an explicitly partial scan. There is no background watcher.

All modes converge on one `WorkspaceState`. `BaselineB0` is a separate immutable
type with observation/review timestamps, profile/mode, scan policy, snapshot,
claims, and conflict dispositions. It is never a DLL node. The project's custom
`CheckpointList` stays empty throughout bootstrap. The DSA demonstration retains the B1
sandbox separately; sandbox CP records are not source-linked B3 project history.

After B0 confirmation, explicitly opt into local storage or keep session-only mode.
A B0 report is **not a historical resume**.
Import never automatically creates `CP-000001` or recreates old handoff events.
Confirming an imported claim records a person's present assessment while keeping
its original source verification and fingerprint intact.

## Inspection policy and supported extraction

The inspector reads bounded UTF-8 TXT, Markdown, and CSV. Metadata and SHA-256
fingerprints are observations, not proof of successful work. PDF, Office files,
images, archives, source code, and unfamiliar formats are metadata-only in B2.
No imported code, command, or agent instruction is executed.

| Policy field | Default |
| --- | --- |
| Directory depth below root | 5 |
| Visited filesystem entries, including directories | 300 |
| Bytes per eligible file | 65,536 |
| Total bytes read | 2,097,152 |
| Text characters per file | 65,536 |
| Characters per source line or CSV field | 1,000 |
| Extracted claims | 200 |
| CSV columns | 16 |
| CSV rows, including header | 201 |
| Cooperative scan duration | 5,000 milliseconds |

These values are configurable through the validated `ScanPolicy` constructor.
Inventory reports `SUPPORTED`, `SKIPPED`, `UNSUPPORTED`, `FAILED`, and `UNSTABLE`
separately. Ordinary traversed directories consume the entry budget; skipped
directories appear in the inventory, and their hidden descendant count is unknown.
Partial traversal never claims to cover every file. An empty SHA-256 field means
no trustworthy hash was obtained; an empty file has its real SHA-256 value.

Excluded names include `.git`, dependency/generated/cache directories, `.env*`,
`.ssh`, `.aws`, `.azure`, credential/secret/password/token names, common private
key/keystore extensions, temporary files, and `.breadcrumbs` storage. Matching is
case-insensitive and deliberately conservative. Sensitive-looking text markers
suppress extraction. This is not a universal secret detector.

Markdown recognizes the frozen context headings, explicit labels, strict task
checkboxes, and explicit `TASK-*` or `T-*` identifiers. Code fences are not parsed
as project claims. `AGENTS.md` and `CLAUDE.md` are classified as instructions and
produce no completion claims. State, handoff, task, and decision filenames suggest
artifact roles only. Claimed dates and "tests passed" remain source-reported.

CSV supports quoted commas, escaped quotes, quoted newlines, headers, and strict
row/column validation. Only configured headers create claims: `task_id` supplies
an explicit identity, `status` supplies task status when that identity is valid,
and recognized context headers such as `objective`, `next_action`, `owner`, and
`blockers` map to their literal fields. Arbitrary columns create no inferred facts.
TXT extraction accepts explicit recognized labels such as `Objective:`.

Conflicts require the same explicit entity ID and field, with differing literal
comparison values. Similar words, filename precedence, and newest modification
time do not select winners. Rejected/deferred candidates retain source paths,
line/section locators, fingerprints, source roles and original verification.
Unknowns remain distinct from unsupported or conflicting evidence.

## B2 safety and verification limits

The inspector has no imported-file write, network, or subprocess calls. It
rejects UNC/drive roots and linked/reparse roots, skips symlinks and special entries,
checks resolved containment, and opens eligible files with `NOFOLLOW_LINKS`.
Detected size/time/creation-metadata changes discard the fingerprint and claims
as `UNSTABLE`. Controls and invisible formatting cannot enter terminal snippets;
reports and source detail pages have bounded output.

Actual Windows junction and ACL read-denial fixtures pass. Ordinary symbolic-link
creation was denied by this environment, so that actual fixture remains unverified.
Use quiescent local folders. Portable path checks are not an OS sandbox against
hostile concurrent parent replacement, hard-link aliases, or same-metadata rewrites.
The deadline is cooperative between JDK calls and cannot terminate a blocked OS
operation. Actual JDK 17, Unix, and instructor-machine execution remain unverified.

Run `scripts/verify-b2-windows.ps1` with the command above to generate synthetic
fixtures and reproduce new-empty, README-only, and legacy-conflict workflows.
It checks source-file byte preservation, secret exclusion, instruction inertness,
and zero automatic native checkpoints. Fixture inputs and raw logs stay under
ignored `build/b2-verification`. [Retained evidence](docs/B2_EXECUTION.md) binds
the observed results to source hashes. No real user project or credential is
required for testing.

## Use the B1 menu

Choose startup option **4**, or launch `run.bat --dsa`, for the separate B1 menu.
It starts with an empty custom list. Option 12 loads five explicitly
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
| 13 | Add/create a project and review its initial B0 |
| 14 | View B0, inventory, source locators and review dispositions |
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

**Nothing in the DSA sandbox is saved between runs.** B2 adds directory inspection and B0
through options 13/14. The B1 checkpoint sandbox does not attach source evidence
or implement the complete B3 workflow. Human confirmation in that sandbox records
a current session statement without linking it to the active project.

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
Directory inspection/B0 and limited explicit-ID first-import review are available
in B2 as described above. B3 adds basic persistence described below. Full reconciliation/audit scoring, drift,
handoff, Graph/Hash/Trie, advanced document parsers, Git integration, AI, voice,
and crash recovery remain in later batches. There are no known failing B1 tests.

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

Three supported B2 entry modes:

- **NEW_EMPTY:** start a project and record its stated objective and first-observation baseline.
- **IMPORT_FOLDER:** safely inspect an existing folder even when no Breadcrumbs checkpoints exist.
- **IMPORT_LEGACY_CONTEXT:** recognize supported `state.md`, `HANDOFF.md`, `AGENTS.md`, `CLAUDE.md`, task/decision notes, and related Markdown as *source-reported evidence*, with human review before claims become current-context statements.

**B0** means “what was observed at first import”; it is **not** historical progress. **CP-000001** is the first native Doubly Linked List checkpoint, created only with explicit user approval.

## DSA and delivery plan

The **team-approved implementation sequence** is [Batch Guide v1.0](docs/BREADCRUMBS_IMPLEMENTATION_BATCH_GUIDE_V1.0.md). It reorganizes delivery of the frozen **M0–M10** master plan into reviewable, tested feature batches, without changing the evidence and import contracts.

| Batch | Deliverable | Status |
| --- | --- | --- |
| **B1** | Java 17 CLI scaffold, manually implemented Doubly Linked List, five DEMO checkpoints, six+ operations and tests | PASS / DEMO_READY on owner-accepted Windows/JDK 25; B2 foundation ready |
| **B2** | Three entry modes, read-only project folder inspection and B0 baseline | DEMO_READY on Windows/JDK 25; platform limits disclosed |
| **B3** | User-confirmed checkpoints, local TXT save/load, restart continuity and branded startup | DEMO_READY on Windows/JDK 25; basic persistence only |
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
