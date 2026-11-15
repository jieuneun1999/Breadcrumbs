# B3 storage format version 1

## Scope and ownership

One `workspaces/<workspaceId>/state.txt` contains the complete canonical workspace.
There is no manifest or recent-selection pointer. Directory discovery reads only
these app records. Loading never reads a saved source root. Missing files, invalid
records and unsupported versions are controlled errors; another project remains
available. Storage metadata is not a second checkpoint DSA.

`WorkspaceCodec` owns the field grammar and validation. `WorkspaceStore` owns
safe path checks, bounded file I/O and replacement. `WorkspaceState` owns the
active baseline, custom DLL, next-ID counter and dirty state. `ProjectContext`
is immutable data inside a `Checkpoint`, with a compatible B1 constructor.
No independent checkpoint metadata map exists.

## Encoding

Files start with exact ASCII `FORMAT_VERSION=1` and LF. Each following field is
`name=base64(UTF-8 value)` followed by LF. Names occur in a fixed order, including
repeated array fields. Empty string is an empty Base64 value. Numbers use canonical
base-10, booleans use `true`/`false`, timestamps use `Instant` parsing, and enums
must match a known constant. Noncanonical Base64, extra/reordered/missing fields,
invalid UTF-8 and incorrect counts fail. No Java object deserialization is used.

The final `CHECKSUM=<lowercase SHA-256>` covers all preceding exact UTF-8 bytes,
including the version line and final field LF. The checksum detects accidental
corruption; someone able to edit the file can recompute it. This is not signing,
encryption, or proof that stored statements are historically true.

## Ordered record groups

| Group | Contents |
| --- | --- |
| Workspace | Stable ID, name, profile, entry mode, B0 confirmation time |
| Snapshot | Source root reference, scan ID, observation time, policy version and all nine policy limits, completeness, bytes read |
| Diagnostics | Count then bounded diagnostic values |
| Inventory | Count then path, size, modified time, SHA-256, status, adapter, role, observation time and reason per entry |
| Original claims | Count then the 17 EvidenceClaim components per original source claim |
| Reviewed claims | Count then the same components with review dispositions; source information cannot change |
| Sequence | Persisted next native checkpoint number, then checkpoint count |
| Checkpoints | DLL order, each six B1 fields plus ProjectContext fields, counted evidence IDs, previous ID and confirmation time |

Conflict groups are reconstructed deterministically from explicit identity,
literal comparison values and review dispositions. They do not require a second
persisted truth copy. `B0-NO-SCAN` identifies a NEW_EMPTY baseline without an
observed directory. Otherwise checkpoints reference the exact B0 scan ID.
No later rescan snapshot is invented in B3.

Restoration builds a detached state, appends values through the manual DLL,
validates links/uniqueness, chronology, workspace/snapshot/evidence references,
source provenance and the high-water ID, then activates only the complete state.
IDs range CP-000001 through CP-999999; counter 1000000 means exhausted.
Workspace IDs are `W` plus 3-9 digits for B2 compatibility or `W-` plus 32 lowercase
hexadecimal UUID digits for new B3 workspaces. Demo records are rejected.
Project deletion, edits and corrections are deferred; no history is silently lost.

## Resource and privacy limits

| Limit | Value |
| --- | --- |
| Workspace entries in storage | 100 |
| File bytes | 8,388,608 |
| Field lines before splitting | 200,000 |
| Decoded characters per transport field | 8,192 |
| Context note characters | 4,000 |
| Title / progress characters | 120 / 600 |
| Native checkpoints per workspace | 1,000 |
| Claims per original/reviewed array | 2,000, also original scan policy limit |
| Snapshot entries | Stored scan policy maximum, at most 10,000 |
| Evidence references per checkpoint | 2,000 |

The total file bound can be reached before individual array maxima. Unicode,
delimiters, tabs and newlines round-trip in context notes. B1-compatible titles
and progress remain single-line. Unsafe controls, incomplete Unicode and known
sensitive text markers are rejected. Source extraction retains only existing
bounded B2 claims and metadata, not complete documents. This is local plaintext
metadata under the filesystem's permissions. Exclusions cannot recognize every
possible secret. Only use appropriate, quiescent local folders.

## Save lifecycle and limitations

Consent after B0 enables auto-save after each confirmed native checkpoint. Save
can opt in later. A failed save marks state dirty, retains memory and supports
retry. Dirty switching/exit offers save, explicit discard, or cancellation.
Saving before reopening the same workspace reloads the new file after save,
so the old pre-save object cannot replace newly confirmed history.

Before replacement, encode and decode-validate the exact proposed bytes. A corrupt
existing record is retained for manual recovery rather than overwritten. Write a
temporary sibling, close it, require ATOMIC_MOVE with replacement, then read back
the exact bytes. If atomic moves are unsupported, fail closed. No weaker fallback
is used. Filesystem/provider behavior still controls rename guarantees; there is
no fsync/power-loss guarantee, backup generation, lock or multi-file transaction.
The current implementation is for one application process per storage root.

NOFOLLOW_LINKS and ancestor/type checks reject observed symlinks/reparse paths.
They are not an OS sandbox against hostile concurrent directory replacement or
hard links. Storage permission and existence errors are disclosed without reading
source projects. Existing source-folder disappearance does not prevent retrieval.
Freshness remains UNKNOWN. Full drift/handoff is B4; recovery/locking is B7.
Future snapshot/model changes must use an explicitly supported format migration
or version rather than silently changing the version-1 grammar.
