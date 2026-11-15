# B7B daily continuity workflows

## Current design and authority

B7A passed and is [PR #8](https://github.com/jieuneun1999/Breadcrumbs/pull/8),
head 3f6130e, stacked on B6. B7B starts from that exact head. The active
[ExecPlan](B7_EXECPLAN.md) and supplied additive terminal-experience steer govern.
No later stage began while B7A had a failing correctness/regression test.

Startup now offers a daily workspace alongside the preserved original numeric
project tools. Opening an existing project immediately prints a saved-state
Continuity Brief, without an extra prompt or source rescan. The daily menu groups
Follow, Leave, Health, Change review, Search, full capture, history, declared work
items and advanced controls. Original B1-B6 numeric paths remain available.

## Leave and follow

Quick capture asks what changed, current blocker state and the next action. The
preview labels carried objective/owner values as proposals. An old blocker must
be assessed again; blank/unknown never silently copies it. Optional actions edit
the capture, choose current source references, add objective/owner/decision context,
open declared dependencies, switch to the full form or cancel. Only explicit
confirmation calls the existing ProjectCheckpointService and appends to its DLL.
No quick-history store exists. No-evidence capture remains a human assessment.

Follow reports the latest historical checkpoint and its next action/blocker/decision
context, source identity and state, saved observation, reference-specific freshness,
and B4's highest-value next review. B0-only and session-only modes are explicit.
The source root can be offline. Live freshness stays UNKNOWN until a consented scan.
The four profiles supply small optional prompt examples, never inferred facts.

Health translates the same B4 nine-rule results. It retains rule IDs, readiness,
coverage and unknown/missing distinctions, shows at most two review rows and the
existing priority question. It does not create a new score or project-completion
percentage. Detailed B4 audit remains available under advanced tools.

Guided review performs only an explicitly requested rescan, then displays saved
drift classifications, original/observed hashes and bounded historical reference
impact. Acknowledgement uses an existing observation-scoped ContextReview with
entity CHANGE_REVIEW. CurrentAssessment excludes nonempty entity IDs, so an
acknowledgement cannot certify project progress. Present next-action assessments
use the existing canonical human-review path. Old hashes/observations never change.

## Additive terminal experience

Impeccable 4.6 context ran against ProjectConsole. It found no PRODUCT/DESIGN files
and explicitly allowed scoped refinement from existing code. Colorize, animate
and craft-floor guidance was loaded. This is an Operate surface. TerminalTheme
and the approved bread-gold palette remain the visual authority. Web font/layout
and detector requirements do not apply to this Java terminal. No branding asset,
loaf, startup renderer or wordmark is redesigned.

TerminalOutput styles only trusted headings/menu labels/statuses and sanitizes
values into the default foreground. OperationFeedback models STARTED, RUNNING,
COMPLETED, COMPLETED_WITH_WARNINGS, FAILED and CANCELLED. It uses one transient
ASCII spinner line only when Main's existing capability detection identifies a
capable interactive color terminal. Explicit no-color, NO_COLOR, redirected output
and unknown capability use concise start/final lines. Fast operations finish before
the 200 ms first redraw; no artificial delay exists. Running counters come from
real scan callbacks. The status line preserves counters on narrow terminals.

A per-operation daemon scheduler redraws under the stream's shared monitor. Finish
sets a terminal state, shuts down its scheduler and clears the line before final
output or new input. Inspection pauses redraw around cancellation prompts. Exceptions
report FAILED and propagate, while uncompleted explicit close is CANCELLED. No
canonical storage/index/schema change is made for styling. Current integrations
cover import, save, load, recovery, rebuild, daily rescan, audit and comparison.
Remaining advanced-screen styling and final captures belong to B7E, before recording.

## Verification

Tests first failed for missing B0-only brief and warning-state feedback, then passed
after implementation. Targeted tests exercise three-question capture/restart,
cancellation, fresh blocker assessment, real changed hashes, preserved provenance,
review acknowledgement, unavailable source folder, all tones, plain output,
warnings/failure/cancellation, real-count redraw and scheduler cleanup.
Full source-bound regression results are appended after the stage gate.

## Observed B7B gate

scripts/verify-b7-stage.ps1 -Stage B7B exited 0 on Windows 10/JDK 25,
Java 17 target, 2026-11-01 02:49:18 through 02:50:58 Asia/Manila. All 128
source/test/script/launcher inputs were hashed before/after with no changes.
All original B1-B6 test bodies passed; B6's dynamic narrow-output assertions now
count 229 because the new feedback adds bounded lines. B7A passed 69 assertions,
B7B continuity/feedback passed 43, and the recovery plus original B6/B5/B4 process
runners passed. Native ACL checks passed; optional junction fixtures were absent.
Actual Java 17, external terminal visual acceptance and real documents remain open.

The animated-feedback test waits on a real redraw event, verifies the supplied
counter and then verifies scheduler termination and unchanged output after completion.
It does not delay normal product operations. A regression caught truncated counters
on narrow status lines; the rendering now reserves suffix space for the counter.
Specification and engineering self-review PASS for this stage. No independent
review claimed. Shared theme/feedback and daily workflows are ready for B7C; complete
advanced-screen styling and the user's recording remain mandatory B7E deliverables.
