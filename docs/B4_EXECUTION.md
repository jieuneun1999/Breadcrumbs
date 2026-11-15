# B4 execution record

## Packet, authority and baseline

Owner's B4 attachment authorizes local implementation, synthetic verification,
focused commits, publication and a stacked PR. No merge, deployment,
source-project execution/write or global configuration changes. STANDARD local
feature, one execution owner/writer, existing checkout; no agents or worktrees.
Six repair attempts per gate, two per hypothesis before re-grounding. Target
DEMO_READY, never release acceptance. Branding classes and frozen v1.2 documents
are outside the editing allowlist.

Read README, Batch Guide, relevant master architecture/domain/audit/DLL/CLI/
handoff/storage/security sections, complete First-Import Contract, B3 storage
grammar, relevant B3 execution record and actual affected source/tests. Canonical
Manual 273–335, Workflow 236–292, Routing 68–115, Craft 66–101, 142–183 and
311–346 loaded. Pstack 1.16.0 poteto-mode, figure-it-out, feature playbook,
show-me-your-work, how and architect were read completely. Craft's one-owner
adaptation replaces skill-mandated arena/delegation and transcript reviewers with
direct design alternatives and evidence review; no independent review claimed.
No project verification skill is installed: extend the JDK test path. Log evidence
in B4_DECISIONS.tsv, with observed results separate from planned gates.

Owner later explicitly authorized a brief B4 Discord progress summary through
Alagad ni Mae after passing verification and publishing the GitHub checkpoint/PR.
This supersedes the brief's earlier no-Discord boundary for that single summary
in the existing Breadcrumbs dev-updates channel. No pings/channel changes are authorized.

Live origin B3 head 5000974f2fb005b262ad5b9f125bc986050992f8 equals local
baseline. PR #3 is open/unmerged; main is d1f13c2. B4 branch
feat/b4-context-audit-drift-handoff starts there and will target B3.
Original B1–B3 tests passed before implementation (optional link/junction fixtures
not supplied); strict Java --release 17 compilation passed on Windows/JDK 25.

## Grounding and design

CLI owns interaction/consent; BootstrapService activates immutable B0. Original
ScanResult claims and baseline reviewed claims remain separate. WorkspaceState
owns DLL, high-water ID and dirty state. ProjectCheckpointService previews then
confirms native records. WorkspaceCodec reconstructs detached state and validates
all references before WorkspaceStore atomically saves/readbacks. Restore reads
only app files, never saved source roots.

Compared replacing B0 with latest data versus appending observations: replacement
would break old evidence references and conceal history. Choose immutable later
observations in WorkspaceState. Compared a separate current-context store versus
workspace-owned review amendments: choose amendments, with source claims unchanged.
Derived audit/drift/retrieval/reports never become a competing truth store.

Usage sketch: audit.evaluate(state, checkpoint); observations.scan(state, consent);
drift.compare(old, later); freshness.assess(state, checkpoint); retrieval.rank(state,
evidenceIds); handoff.preview(state, checkpoint); export(preview, destination,
explicitConsent). All operate offline. B3-only records retain the exact v1 codec;
B4 state explicitly uses v2 with later observations/reviews before DLL checkpoints.
No v1 bytes are reinterpreted or upgraded on load. First B4 mutation and explicit
Save upgrade the record; genuine v1 fixture is protected under tests/fixtures.

## Plan and throughput checkpoint

- [x] Ground baseline, authority, live stack, existing tests and genuine v1 fixture.
- [x] Immutable observations/reviews and strict compatible storage.
- [x] Drift/freshness and nine-rule audit, formula/uncertainty tests.
- [x] Retrieval, resume/handoff/export and guided CLI integration.
- [x] A–F runtime scenarios, full regression, safety/quality review.
- [x] Documentation, focused commits, remote readback and stacked PR.

Blocking work: reference schema and compatibility before reporting. Independent
workstreams: none delegated; coupled service units verified sequentially. Shared
state: WorkspaceState only. Smallest units: domain/storage, audit/drift,
retrieval/report, CLI, integration evidence/docs/publication. Existing suites
remain intact. Evidence fixtures are synthetic and app storage injected.

## Policy decisions

Audit policy B4-1 uses the nine frozen weights and 40/25/20/15 dimensions.
Coverage = assessable applicable rule weights / applicable rule weights overall;
dimension coverage uses its own weights. Overall score renormalizes dimension
weights over dimensions with assessable input. Readiness precedence: insufficient
coverage, critical referenced changes, context/conflict gaps, then ready. Profile
does not imply solo; explicit solo review alone makes owner N/A. No prior
comparison makes freshness N/A; unassessed references remain UNKNOWN.

Conservative drift: additions/deletions/renames require complete comparable scans;
same-path reliable hashes can still establish equality/change under partial scans.
Different roots/policies are UNKNOWN. Duplicate unmatched hashes are ambiguous.
Relevant ranking omits unsupported tags; shows evidence overlap, confirmed
freshness and DLL-derived recency. Export omits raw imported snippets by default,
lists safe provenance, refuses overwrite/links/import-root/app-state destinations.

## Evidence and final review

Final source verification: 2026-10-31T12:57:18+08:00 through 12:57:44+08:00,
Windows 10 / JDK 25. Java --release 17 strict production/test compilation and
actual Windows CMD launcher from outside the repository pass. Source/test/script
hashes remained unchanged during verification. See committed evidence under
`docs/evidence/b4`, with raw local fixtures/transcripts retained under
`build/b4-verification-bb3dc6dc70654decb2056b9d69c2e9bf` and
`build/b4-scenarios/run-5155024042723035543`.

Commands actually executed:

```text
javac --release 17 -encoding UTF-8 -Xlint:all -Werror -d build/app -sourcepath src src/breadcrumbs/Main.java
javac --release 17 -encoding UTF-8 -Xlint:all -Werror -d build/test -sourcepath "src;tests" tests/breadcrumbs/AllTests.java tests/breadcrumbs/b4/B4Scenarios.java
java -cp build/test breadcrumbs.b4.B4Tests
java -cp build/test breadcrumbs.b4.B4Scenarios
scripts\test-b4.bat
powershell.exe -NoLogo -NoProfile -ExecutionPolicy Bypass -File scripts/verify-b4-windows.ps1
git diff --check
```

The final harness supplies native junction fixtures to `breadcrumbs.AllTests`.
Observed results: B1 3,780; B2 192; B3 continuity/CLI 138 and presentation 153;
B4 187. Total 4,450 assertions, all passing. Separate Main processes pass 55
assertions covering A–F. Without optional junction fixtures, B4's fast suite passes
186 assertions. The original B1–B3 test files are byte-unchanged; only AllTests
adds the B4 groups. Existing symlink-creation skip remains visible; no claim of
symlink creation, JDK 17 runtime, Unix or instructor-device verification is made.

Behavior evidence: A baseline-only report/no invented CP; B same-ID conflict with
both locators and skip/cancel; C confirmed CP save/restart, no automatic scan,
unrelated FRESH versus referenced STALE after consented scans; D real duplicate-hash
rename ambiguity; E three native DLL nodes, explained stable ranking and exported
standalone handoff; F genuine October-30 imported v1 checkpoint read/open/explicit
upgrade/separate-process restore with original refs and counter. Fixture source
hashes before/after app calls prove the app did not write source projects.

Repairs: one missing static test import; one incorrect expected coverage denominator;
process harness initially pointed at test output without Main, then exposed Windows
default redirected output encoding. Fixed production classpath and explicit UTF-8
project output. All were repaired before final verification; no existing tests
were loosened. Final review also tightened decision/rationale credit, explicit
statement rejection, stale-preview detection, unknown freshness and scope validation.
The documented `scripts\test-b4.bat` also ran successfully: 186 fast assertions
and 55 process assertions, exit 0. An earlier wrapper invocation using forward
slashes for the CMD batch path was rejected; the documented backslash path passed.

Specification-compliance review: PASS for B4 M2/M4/M5 scope, fixed weights/states,
B0/native separation, provenance, immutable references, consent and safe export,
v1 compatibility, JDK-only/manual-DLL preservation and frozen branding/specs.
Engineering-quality self-review: PASS for bounded arrays/I/O, detached validation,
separate CLI/domain/derived services and readable academic comments. No independent
model review or full release acceptance is claimed. Static forbidden-collection
scan finds none in application sources; `git diff --check` passes. No generated
classes/build output are intended for the commit.

## Complexity and remaining limits

Let P = native checkpoints, F = compared inventory entries, C = claims per
observation, R = current reviews, E = references per checkpoint, Q = query IDs,
S = observations (at most 21 including B0). Bounds count declared arrays; string
comparison/hashing also costs the processed bytes.

| Operation | Worst-case time |
| --- | --- |
| DLL append/prepend | O(P), including duplicate-ID search; pointer relink O(1) |
| DLL insert/find/remove by ID | O(P); unlink after lookup O(1) |
| DLL latest/size/isEmpty | O(1) |
| DLL forward/backward/structure | O(P) |
| DLL full integrity | O(P²), including pairwise ID uniqueness |
| Drift comparison | O(F²) bounded path/hash matching and deterministic insertion sort |
| Referenced freshness | O(P + S + F² + E(C + F)) including canonical/reference lookup |
| Audit | O(C² + RC + P + S + F² + E(C + F)); nine-rule scoring itself O(1) |
| Retrieval | O(QSC + P² + P(S + F² + E(C + F + Q))); no Hash index claimed |
| Handoff/export | Linear rendering/encoding/output bytes plus audit, drift, reference lookup and canonical validation costs |

The rank projection uses O(P) extra entries; drift uses O(F) changes/masks; source
fingerprinting uses the existing bounded scanner. Application remains CLI-only,
Java 17 target, dependency-free and offline. Current records/exports are plaintext;
source roots cannot be rebound, snapshots cap at 20 later observations, no new
scan policy editor, tags/semantic search/Graph/Hash/Trie/advanced parsers/AI/Git/
watchers are implemented. No backup recovery, concurrent writer or power-loss
guarantee; hostile concurrent filesystem replacement is outside these path checks.
Rejected/unknown evidence remains visible for review. Old checkpoints are immutable;
reviewed drift requires a new confirmed checkpoint rather than retargeting history.

B4 acceptance is DEMO_READY on the tested platform. B5 can begin from this verified
head when separately instructed; its Graph and Hash units remain unstarted. Keep
the stacked PRs unmerged. Publication/session checkpoint follows below.

## Session checkpoint

Functional commit: `e981c5bc43f27fdb3b505b100350db22a2219e9a`.
Implementation is complete and verified. Evidence/session commit
`9b52a98999dacc81aa530a79529ecdab4b4dbf6b` preserves the exact exercised source
hashes and A–F transcripts. [PR #4](https://github.com/jieuneun1999/Breadcrumbs/pull/4)
is open, non-draft and unmerged, with GitHub reporting mergeable. It targets B3's
verified `5000974f2fb005b262ad5b9f125bc986050992f8`. Remote readback matches the
published checkpoint; main remains `d1f13c2bb84c9e0841e3b204e32cdbc403b78613`.
No merge is authorized. This final documentation-only follow-up records those side effects.

The owner's requested brief progress summary was posted through Alagad ni Mae to
the existing Breadcrumbs dev-updates channel at 2026-10-31T05:05:30.995Z, with no
mentions. Readback returned the exact message and link:
[Discord B4 update](https://discord.com/channels/1565343957708181586/1565415507421691934/1565954853023391815).
No channel, permission, milestone topic or unrelated message was changed.

Final state: **B4 DEMO_READY**, self-reviewed, no unresolved failing local gates.
Unverified platform/release boundaries above remain explicit. No B5 work began.
Owner choices are CONTINUE with the next scoped B5 prompt, CHANGE after review,
RELEASE through a separately authorized release packet, or STOP with this saved checkpoint.

For the next chat: read README, Batch Guide, this record and B4_POLICY_AND_STORAGE,
then verify live branch/PR heads before starting the owner's next batch. B5 Graph
and Hash are separate future units; do not infer task dependencies from B4 text
or turn derived retrieval arrays into a second history store. Preserve branding,
frozen v1.2 specs, original v1 fixtures and full original regression suites.
