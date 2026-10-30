# B2 execution record

## Active run packet

Owner request is the attached B2 implementation task received 2026-10-30, plus
the subsequent instruction to use meaningful academic comments and readable
names. This packet authorizes B2 implementation, fixture-only testing, scoped
commits, publishing `feat/b2-safe-project-bootstrap`, and a reviewable stacked PR.
It does not authorize merging PR #1, writing its branch, changing main, release,
deployment, actual credential access, unrelated communication, or global changes.

Repository identity verified as `jieuneun1999/Breadcrumbs`. Live main remains
`d1f13c2bb84c9e0841e3b204e32cdbc403b78613`. PR #1 remains open/unmerged with head
`8f09b99414dc4618a2bb03d6368e1ceb7a5ab3aa`. Local state was clean. B2 branches
from that exact B1 head and targets `feat/m0-dll-foundation`. Recheck before PR
publication. Do not silently retarget or merge either branch.

STANDARD bounded local feature with explicit safety requirements. One owner and
one writer, no delegated writers, model panels, or new worktrees. Owner performs
grounding, design, implementation and separate compliance/quality self-reviews.
No independent review is claimed. Six repair attempts maximum, no more than two
under the same hypothesis. Stop at an actual authority or unresolved safety
boundary. Preserve unrelated files and historical evidence. Recovery uses scoped
follow-up changes, never destructive Git. Expected result is DEMO_READY.

Scope is three entry modes, bounded read-only local inspection, TXT/Markdown/CSV,
source provenance, minimal explicit-ID conflicts/review, and confirmed in-memory
B0. B3-B7, persistence, drift/audit scoring, advanced formats and integrations are
excluded. Test only generated synthetic fixtures, not actual user projects.
Windows/JDK 25 is owner-accepted; compile with `--release 17`. JDK 17 runtime and
instructor-machine execution are not implied.

## Authority and methodology

Read relevant sections of canonical Manual 01, Workflow 02, Routing Guide 05 and
Craft Guide 06, supplied AGENTS, then README, Batch Guide, Master v1.2 relevant
sections 3/5/6/13/14/16/M1/19.2A, First-Import Contract and B1 execution record.
Inspected existing source/tests/launchers. No project verification skill exists;
the committed Java suite and Windows harness are the equivalent verification
path and will be extended for B2.

Installed Pstack 1.16.0 catalog path remains
`C:/Users/Papa/.codex/plugins/cache/open-pstack/pstack/1.16.0/skills/`.
Complete router read was dedicated, followed by Feature playbook, architect,
design-red-flags, boundary-discipline and sequence-verifiable-units. Previously
loaded how, model-the-domain, prove-it-works, test-behavior-not-implementation,
technical-writing, unslop, Codex mapping and Opening a PR remain applicable.
Loading, application, and verification are distinct evidence claims.

Craft Guide adaptation preserves one owner instead of mandatory delegated
writing/arena/worktrees. Architecture comparison is direct and bounded. Owner's
academic-comment request overrides no-comments defaults. Comments will explain
algorithms, safety decisions and truth boundaries without repeating obvious code.

## Feature plan and throughput checkpoint

1. `how` over the affected subsystem.
   Done. Main launches ConsoleMenu, which owns the B1 session-only demonstration
   DLL. B2 adds bootstrap/report options without replacing DLL algorithms.
2. `architect` for parallel design exploration.
   Adapted to direct comparison under one-owner topology. See decisions below.
3. Write the throughput checkpoint as four todo items.
   - Blocking first steps. Live Git/PR authority and B1 regression checks.
   - Independent workstreams. n/a: one coupled state/inspection/CLI increment.
   - Shared mutable state. WorkspaceState alone activates confirmed B0. Drafts
     and source records are immutable values; pointer ownership remains DLL-local.
   - Smallest safe decomposition. Domain/inspector, extraction/review, CLI, then
     full verification/evidence, each with behavior checks before proceeding.
4. Delegate code-writing through provider dispatch.
   Adapted. Primary owner writes directly under Craft Guide, no topology widening.
5. Verify on the matching surface.
   Passed. Full B1 regression, B2 fixtures, actual Windows ACL/junction checks,
   three real CMD workflows, launcher paths and failure handling. See evidence.
6. Rebase into small, ordered commits. Stack follow-ups.
   Adapted. Focused new commits on B1 base; no rewrite of published history.
7. If the design is contested, `interrogate` before shipping.
   Conditional. No contested design currently; no panel is implied.
8. Run **Opening a PR**.
   Pending. Stacked PR, never automatic merge.

## Architecture decisions

Compared a large mutable per-mode controller with a single canonical state plus
immutable bootstrap proposals. Chose the latter. All modes share one confirmation
path, so a cancelled scan/review cannot replace the active baseline. No independent
state store per mode, no auto-created checkpoint.

- WorkspaceState owns the active B0 and an empty project CheckpointList ready for
  B3. B1's existing sandbox remains explicitly separate from project history;
  its manually confirmed session records are not source-linked B3 checkpoints.
- Separate BaselineB0 type captures scan/policy/coverage, reviewed claims and
  unresolved facts. It can never enter CheckpointList. No imported chronology.
- Source observations and claims retain independent provenance, verification and
  review enums. Confirming a claim records a present assessment and preserves
  original literal/source/fingerprint; it does not execute or verify old tests.
- Inspector owns all filesystem access. Adapters parse bounded bytes as pure
  transforms. Arrays transport bounded results; no collection replaces the DLL.
- Root/entry validation rejects symlinks, other/reparse entries, UNC roots,
  sensitive/generated paths, and containment escapes. Before/after attributes
  and fingerprints report observed instability. Reads use NOFOLLOW_LINKS.
- Limits cover depth, entries, per-file and total bytes, text/line length, claims,
  CSV dimensions and elapsed time. Cancellation is checked between entries and
  reads. CLI offers continuation/cancellation between bounded scan batches.
- Static links/junctions will be exercised on Windows where permitted. Portable
  JDK path checks do not promise an OS sandbox against hostile concurrent parent
  replacement or indistinguishable hard links. Imports should use a quiescent
  local folder. This limitation must remain explicit, not a race-free claim.

Primary references consulted for link semantics are the
[Java 17 SecureDirectoryStream API](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/nio/file/SecureDirectoryStream.html)
and [OpenJDK Windows file attributes](https://raw.githubusercontent.com/openjdk/jdk17u/master/src/java.base/windows/classes/sun/nio/fs/WindowsFileAttributes.java).
No external code was copied. Windows other/reparse attributes and actual fixture
tests decide junction handling. Source/platform observations do not prove all
adversarial races impossible.

## Verification evidence

Baseline `cmd.exe /d /c scripts\test.bat` passed before implementation.
Observed 3,721 DLL/domain assertions and 59 CLI assertions on accepted JDK 25.
Final source was exercised on Windows 10/JDK 25 with Java 17-target compilation.
The original B1 test files, Checkpoint domain model, node class and demo data are
unchanged. DLL algorithms have only explanatory academic comments added.
Console integration adds options 13/14 without removing the B1 sandbox.

Actual commands executed:

```text
cmd.exe /d /c scripts\test.bat
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/verify-b2-windows.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/verify-windows.ps1
```

The B2 harness compiled using the existing test/build launchers, then ran the
suite with an actual generated junction fixture using:

```text
java -Dbreadcrumbs.junction.fixture="<generated junction root>" -cp build/test breadcrumbs.AllTests
```

The retained summary records the exact generated path and actual command.
All Java compilation uses `javac --release 17 -encoding UTF-8 -Xlint:all -Werror`.
The initial inspector behavior test was compiled before its implementation and
failed because ProjectInspector did not exist. Implementation then passed it.
Later checks added adapters, review/state, actual permission denial and CLI flows.
One shell-only attempt used unsupported PowerShell redirection syntax; it did
not compile or exercise code and was replaced by the normal assertion runner.
No failing final implementation test remains.

Observed final junction-enabled output:

```text
PASS: DLL/domain (3721 assertions)
PASS: CLI (59 assertions)
PASS: all B1 test groups
SKIP: symbolic-link creation unavailable: FileSystemException
PASS: actual Windows ACL read-denial fixture
PASS: actual Windows junction fixture
PASS: B2 inspector (91 assertions)
PASS: B2 bootstrap/review (62 assertions)
PASS: B2 CLI (38 assertions)
PASS: all B2 test groups
```

That is 3,780 original B1 assertions plus 191 B2 assertions in the junction-enabled
run, not 3,971 distinct scenarios. The normal suite reports 90 inspector assertions
and a junction skip unless the harness supplies the fixture property. The separate
junction-enabled run closes that skip. Windows denied ordinary symbolic-link
creation; that actual symlink fixture remains unverified. No privilege, trust,
authentication or system configuration was changed to enable it.

Security/edge fixtures cover malformed UTF-8, binary-as-text, malformed/quoted/
multiline CSV, exact line references, source instruction inertness, terminal
controls, sensitive names/content suppression, unknown columns, byte/depth/entry/
line/claim/time budgets, cancellation between entries, ordinary source mutation
and disappearance, empty-file SHA-256, and actual ACL read denial. Cases use only
synthetic files under ignored build output. The Windows harness creates its own
junction with CMD; the application itself has no subprocess API calls.

The three actual `run.bat` demonstrations establish B0 for NEW_EMPTY, a README-only
folder, and legacy notes with conflicting TASK-12 statuses. They show zero project
native checkpoints. The legacy report retains both source line references, an
old asserted date, source-reported verification, unresolved conflict, and unknowns.
Fixture SHA-256 comparisons prove imported bytes unchanged. The synthetic .env
value never appears in output, and the injected instruction marker was not created.

The existing Windows harness was updated to require both B1 and B2 completion
markers. It passed full tests and application launch from the repository root,
another directory, and a copied path with spaces. Missing-JDK and compilation
failure fixtures returned exit 1 without running stale classes.

Retained evidence normalizes CRLF to LF and removes trailing prompt spaces for
Git whitespace checks. Raw captures remain in ignored build output; substantive
text and source fingerprint values are unchanged.

- [Windows B2 commands, timestamps and results](evidence/b2-windows-summary.txt).
- [Launcher compatibility and failure-path results](evidence/b2-launcher-summary.txt).
- [Actual new-project console output](evidence/b2-new-empty-demo.txt).
- [Actual README-only import output](evidence/b2-readme-demo.txt).
- [Actual legacy conflict import output](evidence/b2-legacy-demo.txt).
- [Exercised source/test/script SHA-256 manifest](evidence/b2-source-sha256.txt).

The manifest was captured before both final harnesses and matched afterward.
It includes all relevant initially untracked implementation inputs, excluding
generated fixtures/classes and documentation. Documentation-only follow-ups do
not change those exercised inputs. Historical B1 evidence retains its own source
and date limits. No claim is made that old B1 manifests match these B2 changes.

## Compliance and quality review

Specification-compliance self-review passes the three entry modes, canonical
WorkspaceState, explicit scan and baseline confirmation, typed truth/review
states, supported bounded extraction, explicit-ID conflict handling, paged reports,
B0/DLL separation, no automatic CP, preserved B1 operations and academic comments.
Frozen master v1.2 and First-Import Contract remain unchanged. Minimal CSV mappings
and narrower format support follow the B2 packet, not the full future adapter list.

Engineering-quality self-review passes explicit source ownership, immutable
proposal/baseline values with defensive array snapshots, source/assessment
separation, bounded allocation, no-follow/reparse/containment checks, safe output,
controlled file failures, and behavior assertions on actual state. Static source
scan found no Files write/create/delete/move/copy, network, subprocess, or forbidden
DSA collection calls. JDK List transport appears only in the ACL test because
the standard ACL API requires it; application result storage uses bounded arrays.
No independent review or proof against every hostile filesystem race is claimed.

## Result and B3 readiness

B2 is DEMO_READY for owner-accepted Windows/JDK 25, with Java 17-target compilation.
Actual JDK 17, Unix, instructor-machine and actual symbolic-link execution remain
unverified. Junction and ACL behavior were observed on Windows. Read-only means
the application issues no imported-file writes; OS-managed access metadata is not
claimed immutable. Use quiescent local folders: portable path checks and metadata
comparisons cannot provide an OS sandbox or detect every adversarial replacement,
hard-link alias, or same-metadata rewrite. Deadlines are cooperative between JDK
operations and include scan-continuation prompts; blocked OS calls are not forcibly
terminated. Conservative name/marker exclusions are not a universal secret detector.

B3 can build on the exercised immutable B0, explicit identities, review records,
single confirmation path and project CheckpointList ownership. Cancellation and
snapshot-isolation tests support that conclusion beyond compilation. B3 must
still define a versioned escaped storage codec, approved external storage location,
round-trip/corruption handling and source-linked confirmed checkpoint workflow.
None of that persistence or continuity is implemented or validated in B2.
B3-B7 remain NOT_STARTED. No RELEASE_READY, merge, deployment or full-product
continuity claim is made.

## Publication

Implementation/evidence commit `22c64e0` contains the tested B2 source, integration,
academic DLL comments, regression additions and source-bound evidence. Staged
byte identity, whitespace checks and narrow token/key/private-key scanning passed.
Retained console prompt trailing whitespace was normalized before commit; the
failed whitespace gate prevented an earlier commit and no source fix was needed.
README/status updates, stacked PR creation, remote base/head readback and final
clean-tree verification follow. B1 branch and main must remain untouched.
