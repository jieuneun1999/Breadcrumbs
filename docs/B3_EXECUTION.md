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

## Terminal visual follow-up packet (2026-10-30)

Owner supplied the final terminal visual brief and requested implementation plus
a sample walkthrough or short recording. Baseline is clean B3 head 447e64e on
feat/b3-persistent-project-continuity. STANDARD local presentation refinement;
one owner/writer, no agents/worktrees, frozen specs and persistence/DSA algorithms
preserved. Scope is symbol-built lettering, bitten-loaf/crumb motif, calm startup
status, menu spacing, narrow/plain fallback, affected tests and a local demo.
This follow-up does not authorize commits, pushes, merges, releases or messages.
Six repair iterations maximum, two per hypothesis. Recover through scoped edits.

Reloaded installed Pstack 1.16 router completely in its dedicated call, then
Feature in its own call. Reused already loaded how, architect, model-the-domain,
prove-it-works, test-behavior-not-implementation and Codex mapping. Relevant Manual
01, Workflow 02, Routing 05 and Craft 06 sections read. Impeccable entrypoint and
context had run earlier in this session against this existing CLI; now read its
polish and craft-floor references. Owner's exact ASCII/amber brief is visual
authority. Adapt web-specific workflows to terminal columns and real output;
no raster art, new GUI, fonts, global setup, browser detector or new orchestrator.

Feature plan:

1. `how` over the affected subsystem. Main chooses color/width; BrandRenderer
   prints the header; ProjectConsole currently mixes startup status/menu strings
   with operations. TerminalTheme owns SGR. Existing behavior tests pin old art.
2. `architect` for parallel design exploration. Adapted to direct comparison of
   compact side-by-side and stacked symbol wordmarks under single-owner policy.
3. Write the throughput checkpoint as four todo items.
   - Blocking first steps. Brief, current clean baseline, authority and call sites.
   - Independent workstreams. n/a: one coupled terminal presentation change.
   - Shared mutable state. n/a: renderer owns no project or persistence state.
   - Smallest safe decomposition. Artwork/layout, status/menu, output invariants,
     real runtime walkthrough, one batched visual inspection and correction.
4. Delegate code-writing through provider dispatch. Adapted to direct owner work.
5. Verify on the matching surface. Java/Windows checks and actual-output replay
   completed below. Native terminal visual acceptance remains unverified.
6. Rebase into small, ordered commits. Skip: no new Git publication authorization.
7. If the design is contested, `interrogate` before shipping. Direct-owner
   adaptation: compare actual renders and owner feedback; no panel or shipping.
8. Run **Opening a PR**. Skip protected writes; leave reviewable local diff.

The requested sample will use captured real Java CLI states in a local interactive
walkthrough, clearly labeled as a replay rather than a desktop recording. It adds
no application GUI or runtime dependency. Hyperframes entrypoint was inspected;
film production, package installation and invented video content are unnecessary
for this user-authorized sample option. No video-framework execution is claimed.

### Reference refinement and rejected approach

The owner supplied a realistic gold loaf/wordmark reference and then rejected the
half-block refinement as pixelated, crowded and unnatural. That visual gate FAILED;
passing structure/runtime tests do not change the owner's visual verdict.
Loaded Pstack attack-the-premise completely in a dedicated read. Failed premise:
"More filled color cells and deeper letter shadows will make 80-column text look
more like the realistic reference." Actual output has large square tiles and
crowded counters, and the owner's feedback rejects those same properties.
Change the representation to fine contours/texture and lighter lettering, with
more separation at 100+ columns. Keep basic/narrow ASCII output and inspect a
new actual-output sample. Do not promote the rejected sample as accepted art.

The owner then clarified that the font and bread must retain the reference's
three-dimensional appearance. The thin-outline approach overcorrected and is
also not accepted. Current candidate restores shaded crust, scored crown, pale
cut face, missing bite, front-face highlights and down-right letter extrusion.
It uses fine ASCII texture in a 160-column rich layout instead of filled block
characters. The extra width is an explicit tradeoff for detail and spacing;
smaller terminals retain compact ASCII. It is character art, not a claim of
photorealism or an exact visual match. Visual state: **WIP / OWNER REVIEW PENDING**.

Final representation decision: the procedural candidate still invented a different
font/loaf. Transcribe the approved reference itself into fixed ASCII marks and
twelve warm lighting bands. This retains its silhouette, scored crust, pale bite
face, bevel, extrusion and original crumb curve. The optional Pillow maintenance
script reads pixels only to generate Java text data; no edited image or runtime
image asset is produced. It excludes browser chrome, the reference's sample
status/menu/version and raster caption. Real readable caption/status/menu remain
Java strings. Input is the supplied 1920x1050 reference (an initial 1080-height
assumption was rejected by the recipe's guard and corrected after inspecting its
actual dimensions). Reference SHA-256:
0ce72c26c9e912c8cc122081ad59242d86cd028e6cecbfc65e9b987e384bcc6b.
Foreground highlights are lifted to compensate for glyph coverage; this is a
terminal adaptation, not pixel-identical reproduction. No screenshot/browser
chrome/private account data is bundled with the application.

### Current implementation and observed verification

BrandRenderer chooses the width/mode. RichBrandRenderer prints fixed marks and
palette rows from ReferenceBrandArt; TerminalTheme owns SGR. StartupRenderer
owns truthful status and menu layout. Main accepts --rich-brand and explicitly
writes UTF-8 for that color mode. --no-color and NO_COLOR override it. There is
no image protocol, external font, fullscreen state, delay, subprocess, network
call or new application dependency. ProjectConsole only delegates presentation
and moves the long storage path to project information. Checkpoint, DLL, import,
codec and storage algorithms, original B1/B2 tests and frozen specs are unchanged.

Exact executed commands after the final application change:

```text
python scripts/capture-cli-walkthrough.py
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/verify-b3-windows.ps1
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/verify-windows.ps1
```

The helper was invoked using the installed Python313 executable. It recompiles
with `javac --release 17 -encoding UTF-8 -Xlint:all -Werror -d build/app
-sourcepath src src/breadcrumbs/Main.java`, then drives real Java processes.
The final B3 harness began 23:49:36+08 on Windows 10/JDK 25 and passed **4,263
assertions**: 3,780 B1 + 192 B2 + 138 B3 continuity/CLI + 153 presentation.
Both junction fixtures and Windows ACL denial passed. The ordinary suite passes
4,261 assertions, omitting the two optional junction assertions. Direct
`cmd.exe /d /c scripts\test.bat` also passed during implementation.

Fifteen separate Java processes cover prior B3 restart/import/switch/corruption
flows plus plain, gold, 160-column rich, 80-column rich fallback, redirected rich,
--no-color and NO_COLOR overrides. All exit 0. The real external-path run.bat
with quoted storage path also passes. The final launcher harness passes
root/external/spaced-path launches and copied fixture tests (23:51:22 to 23:51:58+08);
missing JDK and deliberate compiler failure return 1 without stale execution.
Both final harnesses have identical source/test/script manifests.

The final captured walkthrough is under ignored
build/launch-walkthrough/f6adcfe0a7a54644b203535d000e531a. It contains real first
launch, B0 confirmation/save, CP-000001 confirmation/save, process restart,
restoration of next action, DLL integrity, plain and narrow states. Captured app
and helper hashes match current files. Input echoes are explicitly labeled.
PNG is a rendering of captured text, not a native terminal screenshot.

An isolated Edge profile exercised the replay: **18 browser checks PASS** for
all eight frames, Previous/Next, step buttons, real Left-key navigation, stored
context and dependency-free offline markup. Screenshots were inspected.
Initial CDP navigation timed out despite the requested local page being present.
The replay verifier now waits for document readiness in the existing page rather than
starting a second navigation. The bounded readiness/action checks passed. CUA
initialization was unavailable; no global repair,
installation or user browser-profile access was attempted. These browser checks
verify the replay, not Windows Terminal's native font/ANSI appearance.

Retained final evidence (earlier B1-B3 evidence is preserved):

- [Windows assertions and process scenarios](evidence/terminal-visual-windows-summary.txt).
- [Launcher checks](evidence/terminal-visual-launcher-summary.txt).
- [Source/test/script manifest](evidence/terminal-visual-source-sha256.txt).
- [Replay navigation checks](evidence/terminal-visual-browser-summary.txt).

Specification self-review: presentation boundaries and runtime requirements PASS;
reference fidelity remains WIP pending owner judgment. Engineering self-review:
PASS for this local change; no independent review claimed. Actual JDK 17, Unix,
instructor machine, symbolic-link creation and native-terminal visual rendering
remain unverified. B3's prior functional DEMO_READY is preserved. This visual
follow-up is local and uncommitted/unpushed; no new PR update, message, merge or
release occurred. B4-B7 status is unchanged.

## Visual checkpoint publication packet (2026-10-31)

The owner accepted the current visual result "for now" and explicitly requested
saving progress, publishing the PR update without merging, and a brief Alagad ni
Mae dev update with the latest CLI state images. This authorizes scoped commits,
a normal push of feat/b3-persistent-project-continuity, PR #3 metadata updates,
and one image update in the existing Breadcrumbs dev-updates channel. It does
not authorize merging, changing main or earlier branches, release, deployment,
mentions or B4 implementation. This supersedes the earlier local-only packet
for these named actions. Current visual state is ACCEPTED_FOR_CHECKPOINT, with
further refinement possible. Native terminal fidelity remains unverified.

Baseline is 447e64e. GitHub readback confirms PR #3 is open, non-draft and unmerged
with the unchanged B2 base c7567fd. Main and B1 also retain their recorded heads.
Read relevant Manual 01 protection/completion, Workflow 02 publication, Routing
05 Pstack boundaries and Craft 06 source-binding/forge sections. Reloaded the
complete installed Pstack router and Opening a PR playbook in dedicated calls.
Read deslop, no-comments, technical-writing, unslop and prove-it-works.

Opening a PR checklist, adapted to this existing accepted checkpoint:

1. Worktree. Reuse the correct current branch; no new worktree, reset or rebase.
2. Commits. Separate source/test/helper changes from documentation and captures.
3. PRs. Review the scoped diff, preserve meaningful academic comments, and write
   the PR briefing. Direct-owner adaptation of no-comments; no subagent or panel.
4. Forge. Use the available GitHub connector for PR operations and ordinary Git
   for the local commit/push. This overrides the playbook's CLI default under the
   runtime connector preference. Canonical repository is jieuneun1999/Breadcrumbs.
5. Size and stacks. Update existing PR #3 on its current B2 base. No retarget.
6. Readiness. Preserve open/non-draft state. This is a checkpoint, not a release.
7. Babysit. Skip; no CI-repair or merge request was made.
8. Verify remote commit/tree, PR state and image-message readback.

Throughput checkpoint: blocking steps are source/evidence identity and exact
destination discovery. One writer owns Git and documentation. Source publication,
PR metadata and Discord posting run sequentially. No code behavior changes are
planned. All 53 final source/test/script SHA-256 entries match current files.
The 2026-10-30 runtime results retain their original timestamps; no new test run
is implied. Local diff inspection and documentation checks supplement them.

Preserved launch PNG, restored-checkpoint replay screenshot, narrow PNG and the
self-contained eight-state HTML replay under docs/evidence/terminal-visual.
PNG hashes match the original captured files. The standalone HTML normalizes
CRLF to LF and changes its footer's log-location sentence. All captured frames and navigation behavior stay
unchanged. No generated build trees or
synthetic state.txt files enter Git. Frozen specifications remain unchanged.

Alagad ni Mae discovery confirms the existing BREADCRUMBS category and its
dev-updates channel 1565415507421691934. The bot can attach images and read back
messages. The owner's requested summary covers this accepted visual checkpoint,
B3 persistence, 4,263 assertions, the unmerged PR and B4 awaiting its next prompt.
Screenshots are labeled as captured-output/replay views, not native terminal
captures. No pings, channel changes or unrelated messages are in scope.

### Publication checks and implementation commit

Fresh `cmd.exe /d /c scripts\test.bat` passed 4,261 assertions before commit.
The staged whitespace gate then detected CRLF in the generated art and mixed
line endings in two new files. Normalize only generate-reference-brand.py,
ReferenceBrandArt.java and RichBrandRenderer.java. The recipe now writes UTF-8
bytes instead of Windows-translated text. No global Git setting or attributes
changed. Academic comments remain; direct-owner comment/style review found no
behavioral changes to make in the accepted implementation.

Reran the full Windows B3 command after that change, starting
2026-10-31 00:13:33+08. It passes all 4,263 assertions, both junction fixtures,
Windows ACL checks, 15 separate Java-process scenarios and the real external
launcher. All 53 new source/test/script hashes are bound to this run. All 53
application class hashes match the pre-normalization build. Regenerating from
the supplied reference produces byte-identical LF artwork. These checks preserve
the validity of the accepted screenshots without relabeling them as new captures.

Implementation commit is 78edafff56e0bc82fc9df7f90d50b06f139e6398. Its 14 staged
paths matched their working Git blobs, passed whitespace and narrow credential
pattern checks, and exclude build data. No code comment deletions or independent
review are claimed. Source-bound verification remains distinct from owner visual
acceptance and from release approval.

Publication evidence supplements, rather than replaces, the historical records:

- [Fresh ordinary suite](evidence/terminal-visual-publication-tests.txt).
- [Fresh Windows verification](evidence/terminal-visual-publication-windows-summary.txt).
- [Current source hashes](evidence/terminal-visual-publication-source-sha256.txt).
- [Bytecode identity and recipe checks](evidence/terminal-visual-publication-bytecode.txt).
- [Launch, restored checkpoint, narrow fallback and replay](evidence/terminal-visual/README.md).

The reviewable state remains B3 DEMO_READY with the visual checkpoint accepted
for now. B4 awaits the owner's next prompt. PR #3 remains the publication target;
remote/PR/image-message receipts are checked after the push and kept in the local
build/publication-20261031 receipt. No automatic merge is permitted.
