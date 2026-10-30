# B3 execution record

## Active packet and baseline

Owner's attached B3 task plus bread-gold ANSI request authorizes scoped B3 code,
tests, documentation, commits, branch publication and a stacked reviewable PR.
No merge, release, deployment, credential access, unrelated communication or
global configuration change. School-oriented comments and readable names remain
required. Frozen master/PDF and import contract are preserved.

STANDARD local feature. One owner/writer in this checkout, no agents, panels or
new worktrees. Full Access is the actual runtime profile, not containment.
Tests use synthetic build fixtures and injected storage, never real home data.
Six repair iterations per failing gate, two per hypothesis before re-grounding.
Recover through scoped edits; never reset unrelated work. Target DEMO_READY.

Live PR #1 and #2 are open/unmerged. Main remains d1f13c2. B2 local/remote head
c7567fdf34139e5829a4470ebe44ab6bd33704b0 is the clean starting point.
Branch feat/b3-persistent-project-continuity targets feat/b2-safe-project-bootstrap.

## Authority and actual loading

Read supplied AGENTS, relevant Manual 01, Workflow 02, Routing 05 and Craft 06
sections, README, Batch Guide, master domain/DLL/workflow/storage/security
sections, complete First-Import Contract, B1/B2 execution records and live code.
Pstack 1.16.0 router was read completely in its dedicated call, followed by the
Feature playbook in its own call. Supporting how, architect/design-red-flags,
model-the-domain, boundary-discipline, prove-it-works, test-behavior-not-
implementation, unslop, technical-writing and Codex mapping were read.
No project verification skill exists; extend the actual Java/Windows verification
path. Craft Guide adapts mandatory delegation/arena to one-owner direct design.
Comments follow the owner's academic requirement. No independent review claimed.

## Feature plan and throughput checkpoint

1. `how` over the affected subsystem. Grounded: Main launches B1 ConsoleMenu;
   BootstrapConsole prepares immutable drafts then activates B0. WorkspaceState
   currently resets history on activate. ScanResult retains original claims,
   while BaselineB0 retains reviewed claims. Both must round-trip separately.
2. `architect` for parallel design exploration. Adapted to direct alternatives.
   Rejected companion checkpoint metadata store because IDs could diverge.
   Extend Checkpoint with one immutable optional ProjectContext, retaining the
   six-argument B1 constructor. Rejected multi-file manifest/state transactions
   as premature B7 work. One self-contained UTF-8 file per workspace allows full
   prevalidation/reconstruction; enumerate bounded records for discovery.
3. Write the throughput checkpoint as four todo items.
   - Blocking first steps. Live ancestry/PRs, authority, B1/B2 regression.
   - Independent workstreams. n/a: one coupled persistence/domain/CLI feature.
   - Shared mutable state. WorkspaceState owns B0, DLL and next-ID high water.
   - Smallest safe decomposition. Model/service, codec/storage, shell/theme,
     behavior tests, real restart scenarios, review/docs/publication.
4. Delegate code-writing through provider dispatch. Adapted to owner writing
   under Craft Guide's explicit one-writer policy; no topology expansion.
5. Verify on the matching surface. Java suites and separate-process Windows
   scenarios A-F passed. Final refreshed evidence follows review below.
6. Rebase into small, ordered commits. New focused commits, no published rewrite.
7. If the design is contested, `interrogate` before shipping. Conditional.
8. Run **Opening a PR**. [PR #3](https://github.com/jieuneun1999/Breadcrumbs/pull/3)
   is open, non-draft and unmerged against the verified B2 branch.

## Design decisions

Model the Domain puts checkpoint details in each immutable DLL value rather than
an independently mutable metadata list. Boundary Discipline keeps strict codec
validation at the file boundary. Prove It Works requires actual separate-process
restart demonstrations. Test Behavior Not Implementation checks restored context,
source truth, order, errors and preserved active state.

Project deletion/correction is deferred rather than losing immutable history.
IDs use a saved high-water counter, independent of DLL size. Workspace UUIDs
avoid restart collisions; existing B2 service defaults remain test-compatible.
Explicit persistence consent follows B0 confirmation. Confirmed mutations auto-save
when enabled; failed saves stay dirty. Switching requires save or explicit discard.
Startup enumerates only app storage, never imported folders. All reopened context
is a saved observation with freshness UNKNOWN. B4 rehash/drift remains deferred.

Warm bread-gold brand overrides cyan suggestion. Central trusted SGR only,
no-color/NO_COLOR and redirected fallback. No shell VT configuration or delays.

Impeccable 4.6.0 entrypoint/craft-floor read and its installed context command
ran against the existing CLI. No product/design files existed. Adapted the
web-oriented new-world/interview/detector flow to this explicitly specified ASCII
terminal enhancement; no new web design system, hooks, assets or global setup.
Existing product specs and the owner's warm-gold direction are the visual brief.
Inspected actual emitted logo/menu/ANSI and plain transcripts; terminal-specific
palette appearance still depends on the host. No pixel/independent visual claim.

## Implementation and verification decisions

One self-contained file per workspace avoids a B3 multi-file manifest transaction.
No recent-selection record exists, so startup says to select a workspace rather
than inventing a recent project. NEW_EMPTY and imported baselines persist with
separate original-source and reviewed arrays. Conflict groups reconstruct from
those canonical values. New checkpoints keep explicit UNKNOWN/current statements,
B0 evidence IDs and the original observation snapshot. B4 rescan/drift is deferred;
saved context is never called fresh verification. These are B3 packet decisions,
not edits to frozen v1.2 architecture. See B3_STORAGE_FORMAT.md for exact bounds.

The Windows harness initially stopped on PowerShell's native stderr treatment of
`java -version`; corrected version capture through CMD, without changing runtime
tests or silencing their failures. Final script includes source-bound checks.

Quality review found a real same-workspace switch ordering issue. Loading before
the dirty-save guard could activate pre-save state after a successful retry.
The shell now validates before leaving, then reloads after a possible save. A
behavior test simulates corrupt storage, retains the confirmed unsaved checkpoint,
restores access, saves while switching to the same project, and asserts both
the active and durable checkpoint. A disposable source copy with the previous
ordering compiles but fails that exact assertion with exit 1. The fixed full suite
passes. No tracked source was reverted for the probe. Retain the short failure
log as test-sensitivity evidence, not an exhaustive mutation score.

Manual code review renamed codec locals and split ordered field writes for
academic readability. It also moved conflict reconstruction outside the per-claim
validation loop, avoiding unnecessary cubic repeated work. DLL algorithms remain
unchanged. Storage discovery uses bounded sorted arrays; required chronology uses
the actual manual DLL. No built-in DSA substitute or third-party dependency added.

## Observed checks and remaining boundaries

Final command output, source manifest and demo transcripts are recorded under
docs/evidence/b3-*.txt after the last source change. Raw synthetic fixtures stay
under ignored build/b3-verification-* directories. Tests inject storage roots and
never write the user's real .breadcrumbs directory.

Actual JDK 17 runtime, Unix, instructor-machine and symbolic-link creation remain
unverified. Windows/JDK 25 is the existing owner-approved runtime. Windows ACL
read/save denial and storage junction rejection were observed. Checks do not
prove safety against hostile filesystem races/hard links or every secret pattern.
Single process per storage root; no encryption/authentication, crash recovery,
backup rotation, concurrent writer locking or power-loss guarantee. Atomic moves
are required and fail closed when unsupported. B4-B7 remain NOT_STARTED.

Final B3 Windows command:

```text
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/verify-b3-windows.ps1
```

2026-10-30 20:45+08 run compiled with `--release 17 -encoding UTF-8 -Xlint:all
-Werror`, passed the ordinary suite and a second run with both junction fixtures.
Counts are assertions, not distinct scenarios: 3,721 DLL/domain + 59 B1 CLI +
91 B2 inspector + 62 B2 review + 39 B2 CLI + 65 B3 continuity/storage + 73 B3
CLI/brand = **4,110**. The ordinary run omits the two optional junction assertions.
ACL rejection is exercised on Windows. Symbolic-link creation remains an explicit
SKIP. No claim is made that these assertions exhaust every filesystem/codec state.

The harness launches twelve separate Java processes for new B0 save/restart,
README import and reopen after its synthetic source disappears, native CP save/
restart, independent histories/switching, corrupt-load preservation, plain/gold/
no-color/NO_COLOR startup. It also runs the real run.bat from an external directory
with quoted storage path. All exit 0. Source bytes and all exercised input hashes
remain unchanged. Imported synthetic secret values are absent from output.

Retained evidence:

- [Final Windows summary](evidence/b3-windows-summary.txt).
- [Source/test/script SHA-256 manifest](evidence/b3-source-sha256.txt).
- [Scenarios A-E](evidence/b3-continuity-demo.txt).
- [Actual brand and fallback output](evidence/b3-brand-demo.txt).
- [Final Windows launcher results](evidence/b3-launcher-summary.txt).
- [Selected old-ordering defect rejection](evidence/b3-switch-regression.txt).

Transcript retention normalizes CRLF to LF and strips prompt trailing spaces;
the brand record displays actual ESC bytes as literal `\u001b`. Raw outputs and
synthetic fixtures remain under ignored build. No old B1/B2 evidence was rewritten.

`cmd.exe /d /c scripts\test.bat` passed directly during implementation. The
broader `powershell.exe -NoProfile -ExecutionPolicy Bypass -File
scripts/verify-windows.ps1` passed root/external/spaced-path launches, copied
fixture suites, missing-JDK exit 1, and compile-error exit 1 without stale launch.
`powershell.exe -NoProfile -ExecutionPolicy Bypass -File
scripts/verify-b2-windows.ps1` passed original new-empty/README/legacy-conflict
demonstrations with source bytes unchanged and imported instructions inert.
The old harnesses now select --dsa explicitly and keep their original assertions.
The launcher run is refreshed after the final three reference assertions.

## Review and acceptance

Specification-compliance self-review: PASS for B3. Native project history uses
WorkspaceState's custom DLL, B0 remains separate, creation requires preview and
explicit confirmation, evidence truth survives restoration, and IDs remain monotonic.
Save/load/switch failures preserve the active state and expose unsaved status.
All required presentation and no-color paths were exercised. No B4/B7 claim.

Engineering-quality self-review: PASS for the bounded single-process local use.
Codec allocation, checksums, reference validation, complete-before-activate load,
observed storage links, corruption and permission errors are tested. Source review
found no network/subprocess/object-deserialization or forbidden collection use in
application code. Only test ACL APIs use JDK List as required transport. DLL source
and original B1/B2 tests are unchanged; frozen master Markdown/PDF and First-Import
Contract match the verified B2 base. No independent review is claimed.

B3 result: DEMO_READY under owner-accepted Windows/JDK 25. B4 can begin from the
saved baseline/provenance/reference foundation. Its later snapshots, drift/audit,
handoff and explicit storage migration/version strategy still need B4 design and
tests. This is not RELEASE_READY. No automatic merge or release.

## Publication receipt

Implementation commit `13f6f65` contains 21 scoped source/test/script files.
All 47 exercised input hashes matched before commit; raw working blob IDs matched
all staged entries. Whitespace and narrow credential/private-key pattern checks
passed. Documentation/evidence follows separately. Final launcher and B3 manifests
match exactly; the B2 compatibility demo run preceded three extra B3 reference
assertions, and the complete final suite was rerun by both later harnesses.

GitHub connector is the selected forge interface under the runtime preference for
the available service plugin. Scoped Git push publishes only the B3 branch.
PR targets the unchanged B2 branch; no earlier branch or main is written.

Documentation/evidence commit `79b9293` follows implementation `13f6f65`.
Remote readback matched that published head and preserved B2 c7567fd, B1 8f09b99
and main d1f13c2. [PR #3](https://github.com/jieuneun1999/Breadcrumbs/pull/3) was
created open/non-draft with the correct B2 base and verified head. This receipt
is a documentation-only follow-up. Final readback follows its push.
No merges, tags, releases, deployments or unrelated external messages occurred.
