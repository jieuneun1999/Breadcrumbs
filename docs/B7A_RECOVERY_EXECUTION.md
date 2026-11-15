# B7A recovery execution

Authority and staged tasks are in [B7 ExecPlan](B7_EXECPLAN.md). B7A branches from
verified B6 at 671cbc2. No real user storage or imported source files are modified
by this engineering run. All recovery operations exercised here use disposable fixtures.

## Recovery container contract and ADR

This extends Master section 15 without rewriting its frozen illustrative paths or
canonical format versions. Each generation contains one complete existing-codec
state.txt plus a strict generation-format-1 manifest. The manifest contains the
generation ID, actual save timestamp, exact state SHA256 and a checksum of its
own body. Current.txt is an atomically replaced copy of that manifest. The existing
state.txt remains a compatibility snapshot; load requires exact agreement with
the selected immutable generation. These are serialized copies of one canonical
WorkspaceState, not independent history models. V1/v2/v3 state bytes stay exact.

Save encodes and validates before staging, force-writes a new stage, validates its
actual bytes/references, atomically promotes it to G-000001 onward, atomically
replaces the compatibility snapshot, then atomically replaces current.txt. The
pointer replacement is the final commit step. Neither this ordering nor individual
atomic renames claims multi-file atomicity. Any incomplete stage, newer uncommitted
generation or pointer/snapshot disagreement requires recovery rather than silently
returning older context. The prior immutable generation is never overwritten.

Recovery candidate inspection is read-only. Invalid candidates remain disclosed;
only decoded/schema/reference/checksum-valid candidates may be previewed. Recovery
requires exact CLI acknowledgement and activates only the newest intact candidate,
as a new generation. Older valid candidates are inspectable but cannot roll back
past newer intact history and reuse monotonic IDs. A valid compatibility snapshot
is offered only if no intact generation remains. Its timestamp is explicitly a
recorded-context time rather than an invented generation-save time.

Interrupted stages are renamed to retained evidence after acknowledged recovery,
not deleted. Pruning requires explicit consent, retains at least two newest intact
generations and the selected current generation, refuses unexpected members, and
deletes only exact old app-owned files. Invalid/interrupted evidence is retained.
Limits are 100 generations, 250 recovery entries and the existing 8 MiB canonical
state limit. Capacity exhaustion is explicit. No automatic destructive pruning.

Files use FileChannel.force(true); atomic move support is required. JDK/filesystem
directory flush and actual power-loss durability are not proven. One process per
storage root remains the operating contract. Hostile concurrent directory/hardlink
replacement is outside this path's isolation guarantee. Ancestor NOFOLLOW/type
checks preserve the original symlink/junction boundary.

## Observed development gates

The pre-change scripts/test-b6.bat baseline exited 0. Recovery tests first failed
because no immutable generations were retained. The implementation then passed
normal saves, read-only prior previews, all six write failure points, explicit
recovery, read faults, pruning, exact legacy v1/v2 wrapping and high-water counter
checks. A separate Java process was terminated after promotion; restart detected
the orphan generation and the actual CLI recovered CP-000002 only after exact
acknowledgement. The production application has no crash-injection command option.

The existing numeric menus remain available. Startup adds recovery inspection and
explicit in-memory operation without redesigning the accepted artwork. Recovery
activates validated state only after the current unsaved project is handled.
Further full-suite/stage-gate evidence is appended after execution.

## B7A gate

scripts/test-b7.bat exited 0 after the existing invalid-menu hint was kept accurate
for its grouped project/recovery/memory choices. No previous test was weakened.
All original B1-B6 bodies passed, plus 69 B7A assertions and four separate-process/
CLI recovery assertions. Original B6/B5/B4 process runners passed 21/10/55 assertions.
This run used Windows 10/JDK 25 and strict Java 17-target compilation. ACL checks
passed; optional junction fixtures were not supplied. Genuine Java 17 and instructor
hardware are still unverified. Raw output is build/b7a-final.txt; review copies
normalize line endings and trailing prompt whitespace. A post-run input manifest
records exercised source/test/script bytes. No source was edited during the passing
run. Native junction verification remains scheduled for the final integrated gate.

Specification self-review PASS for recovery, compatibility, explicit consent and
preserved history. Engineering self-review PASS for path bounds, detached validation,
failure propagation and retained prior generations. No independent review claimed.
B7A is DEMO_READY in the tested synthetic environment and unblocks B7B.
