# B4 policy and storage contract

## Current context and original evidence

`WorkspaceState` remains the canonical owner. B0 is immutable and never enters
the checkpoint DLL. Later scans append `OBS-<workspace>-0001` through `0020`,
with independent `EV-<workspace>-O0001-000001` evidence IDs. Old native checkpoints
retain their original observation/evidence references; new checkpoints select
evidence from the current observation. Source roots are read only after explicit
rescan consent. Startup, restore, audit, retrieval and report preview use saved
records; none silently rescans.

Immutable `ContextReview` amendments describe present assessments. A source review
must preserve the exact evidence ID, field, explicit entity and literal value.
Confirm/reject/skip changes the derived present assessment, not original source
provenance or historical checkpoint values. A new user statement has no evidence
ID and is explicitly a human assessment. Latest disposition wins for that
assessment only. A later scan requires fresh reviews for its new evidence IDs.
No independent current-context file or second checkpoint chronology exists.

Conflict identity is the existing B2 explicit entity ID plus field. All original
locators are retained. A present conflict resolves only after one value is
confirmed and incompatible alternatives are explicitly rejected. Similar text,
filenames and modification times never pick a winner. Instructions are inert.
Unchecked literal task markers and Next Steps are candidates; no inferred owners,
dependencies, urgency or verified outcomes are generated.

## Context Audit B4-1

| Rule | Weight | Dimension |
| --- | ---: | --- |
| CTX-PURPOSE | 12 | Continuability |
| CTX-DELIV | 10 | Continuability |
| CTX-STATE | 13 | Continuability |
| CTX-NEXT | 16 | Continuability |
| CTX-BLOCK | 11 | Coordination |
| CTX-DECISION | 10 | Traceability |
| CTX-OWNER | 10 | Coordination |
| CTX-EVIDENCE | 8 | Traceability |
| CTX-FRESH | 10 | Freshness |

The rule weights total 100. Dimension weights are 40/25/20/15 for
Continuability/Traceability/Coordination/Freshness. EVIDENCED = 1, PARTIAL = 0.5,
STALE = 0.25, MISSING = 0 and CONFLICTING = 0. UNKNOWN, UNSUPPORTED and
NOT_APPLICABLE are excluded from the assessable numerator. UNKNOWN and UNSUPPORTED
remain in the applicable coverage denominator; NOT_APPLICABLE does not.

Within a dimension, score = 100 × weighted earned factors / assessable rule
weights. Coverage = 100 × assessable / applicable rule weights. Zero assessable
weight yields UNASSESSED, not a zero score. Overall score renormalizes dimension
weights over dimensions that have assessable input. Overall coverage = 100 ×
all assessable rule weights / all applicable rule weights.

Literal unreviewed context is PARTIAL; explicit current confirmation can make it
EVIDENCED. Decision and rationale must both be documented and confirmed for full
CTX-DECISION credit. Source claims that tests passed never become independently
verified tests. `NOT_DOCUMENTED`, explicitly entered and confirmed in a guided
gap review, means confirmed absent documentation (MISSING). Blank/unrecognized
headings stay UNKNOWN; unparsed-only input yields UNSUPPORTED. Profiles do not
silently imply solo work. Explicit solo applicability makes owner N/A. Initial
B0 with no prior comparison makes freshness N/A; native records without a later
comparison remain UNKNOWN.

Readiness precedence, tested at the 60% and 80% boundaries:

1. Coverage below 60%: INSUFFICIENT_EVIDENCE.
2. Critical checkpoint-referenced changes: REVIEW_CHANGES.
3. Unresolved conflict, next-action gap, unassessed native freshness, or score
   below 80: NEEDS_CONTEXT.
4. Otherwise: READY_TO_RESUME under this proposed policy.

The priority gap appears before the numeric score. Every finding has a rule ID,
weight, state, explanation, evidence references and next review action. Evaluation
time, observation, checkpoint and policy are explicit. No clock-based age bonus
is used. **This does not measure project completion or quality.**

## Drift, freshness and retrieval

Same-path reliable SHA-256 equality/difference establishes UNCHANGED/MODIFIED.
Missing hashes, failed/skipped/unstable entries, foreign roots or incompatible
policies yield UNKNOWN. ADDED/DELETED and rename inference require complete,
comparable observations within the declared bounded scan scope. Even one partial
subtree conservatively suppresses all absence-based conclusions. Metadata-only
unsupported files can still have reliable hashes; no body semantics are inferred.

Unique one-to-one equal hashes among unmatched paths yield RENAMED_EXACT. Multiple
equal-hash candidates yield AMBIGUOUS_RENAME. A rename changes the locator, so a
checkpoint reference still needs review. Referenced modification/deletion/unique
rename is STALE; ambiguous/unassessed references are UNKNOWN. An unrelated changed
file does not make a checkpoint stale. Saved comparisons describe their observation
time; they are not continuous live monitoring.

Ranking linearly reads the authoritative DLL into a disposable array. Score is
+3 for any explicitly shared evidence ID, +2 for confirmed referenced freshness,
and DLL position / checkpoint count for recency in (0,1]. It reports overlap count,
each bonus and total. No tags exist in the current model; no tag factor is
fabricated. Ties use timestamp descending, then checkpoint ID descending. Sorting
never mutates DLL order. Foreign/demo records and invalid/duplicate queries fail.

## Reports and export

No selected native checkpoint produces **Baseline context report — not a historical
resume**. A selected/latest native record produces historical resume with immutable
recorded context, present assessment, original evidence references, later drift,
reference-specific freshness, audit, unknowns and limitations. Dependencies and
downstream impact stay UNKNOWN until B5. No code/test execution is claimed.

Reports are terminal-copyable and standalone Markdown-compatible plain text.
Imported snippets are INTERNAL and omitted by default. An explicit preview option
can include only CONFIRMED reviewed snippets. Sensitive markers and terminal/
active-markup characters are redacted/escaped. Relative source locator, hash,
observed time and source kind remain visible. No absolute imported root is placed
in the handoff. Human statements remain labeled as current assessments.

Export needs preview, explicit destination and final confirmation. It requires an
existing normalized absolute local directory outside imported root/app state,
safe simple .md/.txt filename and CREATE_NEW. Existing targets, traversal, reserved
Windows names, symlink/reparse ancestors and unsafe destinations are rejected.
Readback must match exact UTF-8 preview bytes. A canonical state fingerprint
rejects stale previews after intervening reviews, scans or checkpoints. No overwrite
option is offered. Maximum preview is 1,000,000 characters and output 4 MB.
Checks do not provide an OS sandbox against hostile concurrent path replacement.

## Version 2 ordered grammar and compatibility

[Version 1](B3_STORAGE_FORMAT.md) is preserved. Both versions use strict ordered
named Base64 UTF-8 fields, the same SHA-256 footer, bounded parsing and detached
validation. Version 2 starts `FORMAT_VERSION=2` and has these ordered groups:

1. Workspace identity/name/profile/mode/confirmed time, identical to v1.
2. Original B0 snapshot: root, scan ID, observed time, policy/limits, completeness,
   bytes, diagnostics, inventory and original claims, identical to v1.
3. Original B0 reviewed claims, identical to v1.
4. `observations` count, then each later snapshot using the same snapshot grammar.
5. `soloOwner` boolean, `reviews` count, then each amendment's `observation`,
   `evidence`, `field`, `entity`, `value`, `disposition`, `reviewed` fields.
6. `nextCheckpoint` and DLL checkpoints, identical field grammar to v1, with
   references resolving to their actual recorded observation.
7. Exact-byte checksum footer, identical algorithm to v1.

Limits: at most 20 later observations, 2,000 reviews and the existing 1,000 native
checkpoints, 8 MiB file, 200,000 pre-footer lines and per-policy inventory/claims
bounds. Large combinations can reach the total file bound first and fail safely.
Snapshot IDs are contiguous append-only observation IDs; evidence namespace and
workspace/root/time/source references are validated. Original v1 references still
mean B0; no later snapshot is invented for them.

Loading v1 performs no disk write or upgrade and ordinary B3-only saves remain
byte-compatible v1. A B4 mutation enables v2 in memory; explicit Save (or existing
consented checkpoint/EOF saving) writes it. No destructive migration is needed.
Two genuine pre-B4 v1 fixtures test empty-project and imported-reference upgrades;
their hashes and provenance are recorded in the fixture README. Unknown versions,
invalid counts, forged references, bad encoding/checksum and partial files fail
without replacing active state or silently overwriting the original record.

The B3 atomic single-file replacement/readback contract still applies. There is
no B7 backup rotation, writer locking, hostile-filesystem isolation or power-loss
durability guarantee. App records and exports are local plaintext, not encrypted
or authenticated. Basic redaction cannot recognize every possible secret.
