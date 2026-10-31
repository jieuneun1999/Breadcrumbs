# B6 execution record

## Envelope and authority

The owner's supplied B6 task is the active STANDARD packet. Repository
jieuneun1999/Breadcrumbs, clean baseline 27b0ee8d908813418f22cc077cfcdfd97ae6acb7.
Live PRs 1 through 6 were read on 2026-11-01 Manila time, all open/unmerged
with the expected stack. B6 branches from B5B and targets B5B. Scoped local
implementation, synthetic fixtures, commits/push and one reviewable PR are
authorized by the supplied delivery brief. No earlier PR mutation, merge,
Discord, deployment, source-project writes/execution, real-document collection,
credentials, new dependencies or global changes. One owner/writer, no additional
worktrees or delegation. Six repairs per gate, two per unchanged hypothesis.
Recovery preserves the B5B branch and all frozen specifications/evidence.

Loaded relevant sections of Manual, Workflow, Routing and Craft. The installed
Pstack 1.16.0 poteto-mode and Feature playbook were read completely in dedicated
calls. Supporting how, architect, model-the-domain and verification-before-
completion instructions were read; TDD and writing-good-tests were loaded for
executable checks. Craft's single-owner adaptation replaces delegated design,
implementation and panels; direct comparison of two designs remains. No
independent review is claimed. No project-local verification skill is installed;
the existing native Windows/JDK test commands are the verification path.

## Playbook task list

1. `how` over the affected subsystem.
2. `architect` for parallel design exploration.
   Adaptation: direct alternatives under one owner, no panel/worktree.
3. Write the throughput checkpoint as four todo items.
4. Delegate code-writing through provider dispatch.
   Adaptation: primary owns writing under Craft's one-writer rule.
5. Verify on the matching surface.
6. Rebase into small, ordered commits. Stack follow-ups.
   Adaptation: ordered new commits without rewriting earlier branches.
7. If the design is contested, `interrogate` before shipping.
   Skip unless evidence contests the design; no panel authorized.
8. Run **Opening a PR**.

Throughput checkpoint:
- Blocking first steps: reconcile live stack, inspect canonical contracts, run B5 baseline.
- Independent workstreams: n/a, one search feature with coupled privacy/revision rules.
- Shared mutable state: WorkspaceState remains sole canonical owner; detached candidates publish atomically.
- Smallest safe decomposition: tokenizer/Trie, canonical posting projection, CLI, verification/documentation/PR.

## Ground, sketch, agree, implement, scrap

WorkspaceState owns observations, reviews, canonical task revisions and the DLL.
Its epoch plus the DLL modification counter already invalidate derived B5 indexes.
Claims preserve only recognized TXT/MD/CSV fields and field locators. Current
source bodies are never retained in full. WorkspaceIndexes resolves IDs scoped
to observation/workspace. WorkspaceCodec v1/v2/v3 already preserves everything
needed to reconstruct search; no storage change is needed.

Alternative A would keep a mutable search cache incrementally updated by every
canonical mutation. Rejected because missed update hooks could expose stale text.
Alternative B builds a detached dictionary/Trie/posting candidate from canonical
fields, validates it, then publishes a revision-bound projection. Selected because
existing epochs cover mutation and failed/cancelled builds cannot expose partial
results. No persistent second truth store or raw body cache.

Usage sketch: rebuild(state, policy), suggestTerms(prefix, limit),
searchExact(term, limit), resolve result through canonical ID, display coverage.
PrefixTrie owns sorted code-point child/sibling links. The existing B5
CustomHashTable maps exact terms to manual posting chains. Locations contain
scoped identifiers and fields; canonical resolution supplies display values.
Revision includes state identity, workspace, observation, epoch, DLL counter,
graph revision, tokenizer and full policy. Explicit states distinguish READY,
INDEX_OUTDATED, INDEX_UNAVAILABLE and DISABLED.

Default source policy requires current confirmed evidence; a session-only explicit
opt-in can index other safe, non-rejected captured source claims. Unreviewed
snippets remain withheld. Owner fields, instructions, secret markers, unsafe text
and unsupported bodies are excluded. All active DLL checkpoints are labeled
historical with time/observation, never current source evidence. Current work-item
title/description/status are user assessments. NFKC plus Locale.ROOT lowercasing
and bounded Unicode letter/digit/mark tokens is lexical only.

Verification and observed evidence will be appended after each gate. Full real-
document validation remains a future owner-authorized acceptance gate.

## Observed implementation and verification

Baseline `scripts\test-b5.bat` passed before implementation, including all original
groups, 10 B5 process assertions and 55 B4 process assertions. Optional junctions
were absent in that baseline run; ACL checks passed and symlink creation was denied.

Test-first stages observed missing behavior before implementation: normalized
tokenization, complete search rebuild and the CLI search entry each failed their
named assertions against minimal empty scaffolds. Boundary tests then reproduced
full-width sensitive-marker bypass and surrogate-pair snippet truncation before
the corresponding fixes. No empty scaffold remains. Test authoring corrections
fixed an exception declaration, a false collision fixture and input-flow expectations.
Production-path corrections also separated skipped excluded files from metadata-only
coverage. These corrections used evidence rather than weakening exclusion checks.

Final `scripts/verify-b6-windows.ps1` PASS at 2026-11-01 02:05:42 through 02:06:10
Asia/Manila. Windows 10 build 19045, JDK 25, `--release 17 -encoding UTF-8
-Xlint:all -Werror`. Production and test compilation exited 0. Native suite passed
5,765 assertions, comprising the unchanged 5,540 B1–B5 assertions and 225 B6.
Separate production Main processes passed 21 B6 A–H assertions, 10 B5 and 55 B4.
Native junction rejection, ACL denial and external-directory `run.bat` passed.
All source/test/script hashes remained unchanged through the run. Retained logs,
input manifests and actual console output are under `docs/evidence/b6`.

Actual B6 demonstrations import synthetic TXT/Markdown/CSV through existing services,
then exercise the production CLI. They show prefix order, source-linked exact hits,
repeated occurrences, evidence plus historical checkpoint matches, unsupported
Office/PDF coverage, consented rescan, stale-index rejection, replaced/deleted source
terms, rebuild, save/restart and workspace switching. Source hashes remain unchanged
by the application. Search-unit tests also exercise genuine old v1/v2 records,
v3 roundtrip, task/review/DLL changes, normalized Hash collisions, capacity failure,
cancelled/raced builds, corrupt links/references and Unicode/privacy boundaries.
Search has no production filesystem, network or process APIs. Tests use synthetic
fixtures and launch only this application's production Main.

Specification-compliance self-review PASS for the supplied B6 scope. Custom DSAs,
source truth, coverage, exclusions and disposable storage boundary match the brief.
Engineering-quality self-review PASS. Canonical resolution and eligibility share
one boundary; index candidate publication is atomic; resource exhaustion yields
an explicit unavailable/outdated state. No new canonical counters or persisted
pointer caches were added. This is self-review, not independent review. The loaded
no-comments workflow was adapted to direct scoped comment inspection under the
one-owner topology. No comment deletions or suppressions were needed. Deslop and
technical-writing/unslop were applied to the new diff and delivery text. The
Opening-a-PR worktree/rebase defaults are narrowed to the authorized stacked branch
and new commits, preserving every earlier branch. Model the Domain shaped the
explicit index status enum, scoped location record and immutable policy/revision.

No mandatory dependency, parser, storage schema, frozen spec or branding change.
Real-document validation, actual JDK 17 runtime, Unix/instructor hardware, symlink
creation and B7 recovery remain unverified/unimplemented. B6 is DEMO_READY for the
tested synthetic Windows/JDK 25 scope. Full application completion is not claimed.
The compatibility matrix documents all requested formats and the future actual-file
acceptance procedure. B7 can harden this reconstructable index and separately scope
optional adapters. Owner may CONTINUE with a B7 packet, CHANGE the reviewed behavior,
RELEASE only through separate exact authorization, or STOP at this checkpoint.

## Saved implementation checkpoint

Implementation/test commit `8aa5812` contains the exercised search source and
verification runners. A subsequent final `scripts\test-b6.bat` run exited 0,
including all original suites and 225 B6 assertions plus the 21/10/55 process
assertions. That batch run did not supply junction fixtures; the broader native
run above did. Both logs are retained separately. The 115-file native manifest
was read back after implementation and matched every actual input with zero drift.
Frozen specifications, accepted branding and existing B1–B5 test bodies have empty
diffs. Only the suite aggregator adds B6 calls. The connected GitHub service is
the PR transport; `gh repo view` independently verified the canonical repository.
Origin is absent. No forge or global configuration was changed.
