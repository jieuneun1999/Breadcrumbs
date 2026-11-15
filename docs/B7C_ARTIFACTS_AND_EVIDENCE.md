# B7C Evidence Inbox and generated continuity artifacts

## ADR: canonical format 4

B7B passed its source-bound gate and is PR #9 at 915995f. B7C extends the
canonical model only for manual-origin captures, their review events, manual
checkpoint references and the opt-in artifact preference. Captures are not source
EvidenceClaims and never invent a source path, fingerprint or physical line.
WorkspaceState owns a bounded ManualContext module, matching its existing
DependencyState ownership. No additional graded DSA or independent truth store.

Format 4 preserves the complete version-3 grammar, allowing zero graph revisions
when there are none. Before nextCheckpoint it adds the artifact preference,
manual next-ID counter, immutable manual captures and append-only review events.
Each v4 checkpoint appends counted manual-reference IDs after graphRevision.
Versions 1/2/3 remain byte-exact until confirmed B7C data/preferences require v4.
All older fixtures and the B7A generation wrapper remain supported.

Manual captures contain workspace-scoped IN-000001 IDs, one of five capture types,
literal statement, declared origin/reference, recorded time and INTERNAL/SENSITIVE
classification. Review events retain disposition, time and explicit indexing/export
permissions. Reported third-party statements remain SOURCE_REPORTED and independently
unverified even after review. Sensitive content is never indexed/exported; known
credential markers are rejected entirely. Default search includes only confirmed
INTERNAL non-reported captures. Reported captures require an additional explicit
indexing approval. Exported manual bodies always require explicit per-record approval.
CAPTURED-2 adds manual fields; CAPTURED-1 retains its source/checkpoint-only meaning.

Generated Markdown is a materialized view. It is never reparsed as canonical input.
App-owned bundles use a canonical-state digest identity and atomic latest pointer.
They publish only after the exact canonical state is durably saved and validated.
Checkpoint summaries contain immutable recorded context/references, without later
freshness being rewritten into the historical summary. Handoff generation reuses
the existing HandoffService. A failed export must not mark a saved checkpoint lost.
Portable packages are previewed, explicitly consented, create-only Markdown folders
outside source/app storage, with selected checkpoint summaries and no source copies.

Implementation and observed gates are appended below.

## Observed implementation and validation

The NEW_EMPTY production-console workflow captures a reported meeting statement,
records separate indexing/export permissions, creates a linked native checkpoint,
enables app-owned document generation, exports a selected portable package and
finds the manual record with SOURCE_REPORTED provenance after restart. Manual
IDs, next-ID high-water marks, privacy preferences and reviews persist in format 4.
Additional tests reject cross-workspace captures, unresolved reviews and references
recorded after a historical checkpoint.

Artifact tests exercise canonical-save/export separation, exact saved-revision
checks, create-only export, immutable checkpoint summaries, selected archives,
privacy filtering and retry after a promoted bundle has a lagging latest pointer.
That retry initially failed its assertion; repairing the early-return path made
the regression pass. A separate first Windows directory-promotion attempt returned
AccessDenied; an unchanged repeat passed. Its environmental cause is NOT_PROVEN.
Publication fails closed and retains previous views and the saved canonical state.

The console exposes read-only document previews and explicit generation/export
consent. Canonical save success is reported independently of later view failure.
These tests use generated synthetic information only. Real-document acceptance
and independent review remain pending.

Full source-bound B7C gate passed: 135 unchanged input hashes; B7C 47 assertions, B7A/B7B and all preserved B1-B6/native process groups. See docs/evidence/b7/c. Logs are LF/trailing-whitespace normalized review copies; raw logs remain under build.
