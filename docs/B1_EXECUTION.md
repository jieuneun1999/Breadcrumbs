# B1 execution record

## Run packet

Owner request dated 2026-10-30 authorizes B1 implementation, focused commits,
publication of `feat/m0-dll-foundation`, and a reviewable PR to
`jieuneun1999/Breadcrumbs:main`. No merge, deployment, release, unrelated
communication, credential inspection, or global configuration changes.

STANDARD feature. One execution owner and writer. No agents or worktrees.
Baseline `d1f13c2bb84c9e0841e3b204e32cdbc403b78613` was documentation-only.
The empty launcher directory was cloned from the specified repository.
The parent Knowledgebase checkout and its unrelated dirty files are outside scope.
No existing state, packet, ExecPlan, project verification skill, or local AGENTS
file was present. This record is the B1 packet, plan, and evidence location.
The owner's supplied AGENTS instructions apply.

Scope includes Java 17-compatible source, custom DLL, immutable checkpoint values,
CLI, five synthetic fixtures, tests, JDK-only launchers, README, and B1 status.
Frozen Master v1.2 and First-Import Contract remain unchanged. B2-B7 are excluded.
Acceptance follows the owner's B1 checklist and the Batch Guide.
No runtime credentials, network, external dependencies, or mock services.
Six repair attempts maximum, at most two under the same hypothesis.
Stop on scope/authority conflict or an unavailable acceptance boundary.
Recovery preserves existing files and main; local generated outputs are ignored.
Target result is DEMO_READY, subject to observed verification limits.

## Authority and instruction loading

Loaded supplied AGENTS, relevant governing sections of Manual 01, Workflow 02,
Routing Guide 05, and Craft Guide 06 from the canonical AI directory.
Read repository README, Batch Guide, Master v1.2, and First-Import Contract in
that order. Master sections 4, 5, 9, 13, 17-19 and ADR-004/013/019 govern B1.

Resolved installed Pstack 1.16.0 from the current catalog at
`C:/Users/Papa/.codex/plugins/cache/open-pstack/pstack/1.16.0/skills/`.
Read complete `poteto-mode/SKILL.md` in a dedicated call, Feature playbook,
Opening a PR playbook, `how`, `principle-model-the-domain`,
`principle-prove-it-works`, `technical-writing`, `unslop`, `deslop`,
`no-comments`, and the Codex tool mapping.
These reads prove loading, not correctness.

Craft Guide bounded adaptation applies. The primary owner performs grounding,
design, writing, testing, and two explicitly self-reviewed verdicts directly.
Mandatory delegated writing, panels, fresh worktrees, comment-agent review, and
history rewrites are not adopted. No independent review is claimed.
Architecture is already specified, so no parallel architecture exercise is needed.
CLI rendering uses the supplied menu and ASCII output; web visual design skills
are not relevant. Existing GitHub connector is used for PR operations.

## Plan and throughput checkpoint

1. `how` over the affected subsystem.
   Done. No executable subsystem exists. Intended flow is Main -> ConsoleMenu ->
   CheckpointList -> package-private CheckpointNode -> immutable Checkpoint.
2. `architect` for parallel design exploration.
   Adapted. Frozen DLL shape and one writer; direct boundary sketch above.
3. Write the throughput checkpoint as four todo items.
   - Blocking first steps. Verify repository, load authority, freeze semantics.
   - Independent workstreams. n/a: one coupled domain, DLL, and CLI feature.
   - Shared mutable state. Only CheckpointList owns nodes and pointer changes.
   - Smallest safe decomposition. One writer; tests, engine, CLI, evidence.
4. Delegate code-writing through provider dispatch.
   Adapted under Craft Guide. Owner implements directly with behavior tests.
5. Verify on the matching surface.
   Done on Windows/JDK 25 with Java 17 target. Actual JDK 17 runtime remains open.
6. Rebase into small, ordered commits. Stack follow-ups.
   Adapted. Focused new commits; no existing history needs rewriting.
7. If the design is contested, `interrogate` before shipping.
   n/a: no contested design or independent-review acceptance requirement.
8. Run **Opening a PR**.
   Done. [PR #1](https://github.com/jieuneun1999/Breadcrumbs/pull/1) is open and unmerged.

## Decisions

- Model the Domain leads to immutable Checkpoint with enum status and origin.
  B0 cannot be a checkpoint ID. Native IDs are monotonic and never recycled
  within a session, even after deletion. B1 has no persisted workspace or baseline.
- Native creation is append-only with explicit preview/confirmation. Classroom
  prepend/insert use synthetic DEMO records and demonstrate list order, not
  verified historical time. Latest means tail, not maximum timestamp.
- Linear duplicate checks precede public insertion. Append/prepend are O(n)
  overall with O(1) pointer changes. Full integrity checks use O(n^2) pairwise
  duplicate detection, O(n) link/cycle checks, and O(1) auxiliary space.
  This clarifies the frozen API's illustrative O(1)/O(n) comments without
  changing its architecture or introducing the deferred Hash Table.
- Prove It Works requires both assertion tests and the actual CMD launch path.
  JDK 25 is installed. `--release 17` will prove Java 17 language/API targeting;
  execution on an actual JDK 17 runtime remains unverified unless found locally.

## Verification and result

Executed on Windows 10.0.19045, Java/Javac 25. All final Java builds use
`--release 17 -encoding UTF-8 -Xlint:all -Werror`. Main.class reports major version
61 using `C:/Program Files/Java/jdk-25/bin/javap.exe -verbose
build/app/breadcrumbs/Main.class`. The PATH alias did not expose javap; the
installed JDK executable was used directly. No tool was installed or reconfigured.

Initial behavior tests compiled before implementation and exited 1 because domain,
DLL, and CLI classes did not yet exist. The first implemented suite passed 3,719
DLL/domain assertions but failed the terminal-control input case. The cause was
late form validation. Input now rejects controls at the read boundary. One bounded
repair and follow-up review also preserved cancellation at the status prompt.
Final tests add interior-cycle fixtures and cancellation/line-ending cases.

Actual successful commands:

```text
cmd.exe /d /c scripts\test.bat
cmd.exe /d /c "run.bat < tests\cli-demo-input.txt > build\b1-windows-demo.txt"
powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/verify-windows.ps1
javac --release 17 -encoding UTF-8 -Xlint:all -Werror -d build/manual -sourcepath src src/breadcrumbs/Main.java
javac --release 17 -encoding UTF-8 -Xlint:all -Werror -d build/manual-tests -sourcepath "src;tests" tests/breadcrumbs/AllTests.java
java -cp build/manual-tests breadcrumbs.AllTests
cmd.exe /d /c "java -cp build/manual breadcrumbs.Main < tests\cli-demo-input.txt > build\manual-demo.txt"
```

Observed Java output:

```text
PASS: DLL/domain (3721 assertions)
PASS: CLI (59 assertions)
PASS: all B1 test groups
```

The optional Windows verification harness also exercised root/external-directory
launches and copied only B1 source/scripts/tests into an ignored fixture path with
spaces. Both that launch and test suite passed. It temporarily scoped PATH to
Windows System32 and observed the missing-JDK exit 1, restoring PATH in finally.
It broke only the generated fixture Main.java and observed compilation exit 1
without stale application execution. No tracked source was changed by these tests.
Source hashes matched before/after the harness.

An additional actual PTY run of `cmd.exe /d /c run.bat` accepted `12`, `7`, `11`,
and `0`. It loaded five synthetic values, printed their reverse order, reported
valid integrity, and exited 0. This was observed interactively, separately from
the scripted Reader tests and redirected CMD demonstration.

Retained evidence:

- [Windows commands, environment, exits and timestamps](evidence/b1-windows-summary.txt).
- [Full actual CMD demonstration output](evidence/b1-windows-demo.txt).
- [SHA-256 of all exercised source, tests and launcher scripts](evidence/b1-source-sha256.txt).
- [Deterministic menu input](../tests/cli-demo-input.txt).

Retained text evidence normalizes CRLF to LF for Git whitespace checks. No output
content or source-hash values changed. Original captures remain in ignored build
output. The documentation commit initially retained CRLF; final review caught
and corrected that in a follow-up without rewriting published history.

The manifest binds verification to file bytes, including initially untracked
implementation inputs. Generated outputs are excluded. Documentation updates do
not alter those exercised inputs. The report records local time with UTC offset.

Specification-compliance self-review passes B1 executable functionality, custom
DSA ownership, synthetic/native separation, validation, test coverage, JDK-only
launch and scope exclusions. The Batch Guide's literal actual-JDK-17 Windows
runtime acceptance remains NOT_VERIFIED. Target compilation is not that runtime
test. No JDK 17 was present in the inspected Java installation directory.

Engineering-quality self-review passes explicit reciprocal pointer updates,
cycle-safe integrity checks, immutable values, hidden nodes, typed status/origin,
bounded terminal input, no mutation on rejected forms, and safe launcher failure
handling. No independent review is claimed. Deslop/no-comments checks found no
application comments, extra dependencies, collection wrappers, or future modules
to remove. Static forbidden-collection scan has no matches under src/tests.

Result is DEMO_READY for the demonstrated Windows/JDK 25 environment. No failing
B1 automated tests remain. Actual JDK 17 runtime, Unix, instructor-machine and
owner acceptance are unverified. Full B1 closure and B2 execution should wait
for the literal JDK 17 check or an explicit owner acceptance of the JDK 25 /
Java 17-target evidence. No B2 code has begun. No product RELEASE_READY claim.

## Publication

Implementation commit `44fca8a` contains only the 15 explicitly staged core,
test, fixture, and launcher paths. Retained SHA-256 evidence was matched to the
working files, and raw working Git blob IDs matched the staged bytes before
commit. `git diff --cached --check` passed. Narrow key/token/private-key pattern
scan of the staged diff found no matches. Build outputs are ignored.
The two frozen v1.2 Markdown specifications match main exactly.

Documentation/evidence commit `1616dc2` and implementation commit were pushed to
`origin/feat/m0-dll-foundation`. Remote readback matched `1616dc2` and main remained
`d1f13c2`. Commit `093410d` normalizes retained evidence line endings.
[PR #1](https://github.com/jieuneun1999/Breadcrumbs/pull/1) was created open and
ready for review against main, with those three commits and 21 changed files.
The PR records the actual JDK 17 runtime gap, decisions, complexities, commands,
and demo output. This publication receipt updates only the two B1 documentation
files after PR creation. Final tip readback follows its push. No merge, release,
deployment, tag, force-push, or unrelated external message was performed.
