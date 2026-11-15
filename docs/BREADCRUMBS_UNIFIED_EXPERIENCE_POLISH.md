# Breadcrumbs unified experience polish

Status: DEMO_READY for bounded Windows/JDK25 on `codex/breadcrumbs-session`, based on
`fcbef69`. This guide owns presentation decisions, not canonical project truth.
Owner visual approval remains pending.

Published to main through PR13 under the owner authorization. See the main
publication receipt at the end; earlier local-only statements are historical.

## Design and boundaries

The baseline separate live B3 introduction was erased before Home. Prompt text
also acted as screen identity, so ordinary fields replayed the clear/header scan.
The first prototype kept art in report flow, which let paging push it out of view.
The owner requests one product composition, meaningful navigation and real gaze.

Preserve the B3 reference arrays, original64x22 bread, 56x20/48x17/40x14 portraits,
existing palette, action tokens and every canonical feature. No domain, storage,
recovery, source interpretation, privacy, consent or custom DSA changes.

Use an ASCII application frame, stable identity/location header, a bounded content
area and a fixed input/footer region. Gold marks identity/actions, cream highlights
context, neutral values retain default foreground, and existing semantic colors
retain their meaning. Use one action group, avoiding repeated nested card borders.
Typography is cell hierarchy and spacing; the application does not change fonts.

Home composes brand/tagline, actual saved-project availability, current project
context when supplied by its owner, bread and every action together. The complete
unaltered B3 transcription is included only when both dimensions fit the entire
Home. Otherwise its established name/tagline form the compact header. There is no
separate splash phase, cropped logo or alternative artwork.

## Presentation shape

`ScreenId` names actual view families. Existing consoles call `ui.view(id)` at
navigation boundaries. `TerminalScreenManager` retains output capture, adds the
shared shell and compares content regions. A same-view field updates its footer
and relevant content without clearing or animating the application header. Changed
screen identity permits a short dim/replace/reveal of the content only. Urgent
errors and capability fallback replace immediately. No new scheduler or framework.

BreadSupport remains the single input/animation coordinator. The manager supplies
a dedicated portrait region outside the report text flow. Paging changes text
without moving the bread; each roomy detail view also supplies the approved portrait. Free-form input retains a static portrait, typed values stay
with the existing editor/field owner, and menu arrows dispatch original tokens.
Transient focus is remembered by view. Blank Enter never silently approves.

The frame uses bounded presets: Home32 rows, workspace34, setup/forms/Map30,
record detail26, other views32. The footer stays fixed while editing that view;
navigation may move to a shorter frame. Old frame rows are cleared on shrink.
A full-B3 Home fits the actual160x52 viewport: its one-cell side border leaves
all158 approved art cells, and a51-row frame leaves a terminal safety row beneath
it. Larger viewports cap this composition at52 rows. Other views use the usual
inset and shorter heights. On navigation, both old lower rows and the former
rightmost border column are cleared. Full-width writes always reposition before
another printable cell, and the bottom border stays above the physical last row.
This keeps whitespace around groups while removing the large empty middle of short
screens. The same56x20/48x17/40x14 art stays docked at the upper content edge.
A portrait must leave48 text cells; select the approved40x14 derivative if needed,
then omit optional art when the complete action group needs that width. At80x24,
the workspace keeps enhanced paging and every action rather than forcing classic
mode to retain a decorative column.
Report text gets its own word-wrapped column, with every line and action reachable.
Long instructions remain complete in the body when their footer label is bounded.

## Responsive content and accessibility

Test28/40/80/120/160 columns and realistic heights. Narrow screens favor actions
and context over art. Long reports remain paged; the action page must contain all
available keys. If the viewport cannot safely contain an action group or line,
release complete content to classic mode instead of clipping it. A report resize
or keyboard failure preserves all lines and continues through existing fallback.
No-color, no-animation, no-mascot, classic and redirected paths retain functions.

## Gaze and motion

Add forward/left/right and modest intermediate eye poses derived from the same
reference face. The sampled eye cells translate after scaling, preventing loss
of pupils between samples at40 columns. Both eyes shift together inside their face region; silhouette,
lighting and paw geometry are never mirrored. Closed eyes remain coherent.
Cache immutable directional variants lazily rather than multiplying all artwork
eagerly. Use three deterministic24-second cycles with different glance timing and
hold durations, then repeat the bounded72-second schedule. Glances return to center. Content position supplies attention direction. Keep125ms ticks and
gentle sway; arrow feedback may request a glance without restarting body motion.

## Delivery sequence and evidence

Prove unified Home composition and field behavior first, inspect actual Home,
then propagate view boundaries and finish gaze/integration. Add behavior tests for
shell fit, transition semantics, fixed input, gaze/identity, focus and fallbacks.
Run the complete B1-B7 suite and production-process scenarios. Exercise real native
console navigation, paused partial input, save/restart and terminal restoration.
Record a believable synthetic project at reading pace with an idle gaze interval.
Label scripted key input and inspect decoded footage. Keep the recorded window
at one size; test the actual80x24 restart separately without recording so the
capture rectangle cannot leave stale imagery outside a smaller window. Record exact source/classes,
state readback and host limits. Actual outcomes will be appended after execution.


## Requirement and review map

| Owner requirement | Implementation / verification |
| --- | --- |
| One Home, exact identity | Shared Home, unchanged B3 arrays, complete large logo only when the whole composition fits; UnifiedExperienceTest |
| Cohesive navigation | Explicit ScreenId, one shell, remembered numeric focus, blank Enter safety; TerminalScreenTest |
| Smaller empty areas | Preset view heights and removal of obsolete lower frame rows; independent viewport assertions |
| Bread stays visible | Dock independent of text paging; every-page and record-detail viewport assertions |
| Natural direction | Both sampled eyes move together, held left/right/intermediate poses and bounded timing variation; BreadGazeTest |
| Stable editing | Same-view fields retain footer and static portrait; full long instructions stay readable |
| Truthful operations | Immediate real status/counter, result only on completion, existing portrait reused, scheduler cleanup |
| Responsive fallbacks | Complete actions/report content, safe ambiguous-width release, narrow paging, numeric/arrows with no art |
| Canonical preservation | Only CLI presentation and its tests/tools/docs change; full B1-B7 and separate-process scenarios |
| Real demonstration | Synthetic campus food drive, actual Java/native key injection, report reading pauses, save/restart and read-only state reconciliation |

Specification compliance and engineering quality are separate single-owner
self-reviews. Automated viewport/gaze checks establish geometry and behavior;
they do not establish owner artistic preference or independent human usability.
Final executed receipts, media and unresolved host limits are recorded below.


## Final executed acceptance (15 November 2026)

DEMO_READY for bounded Windows/JDK25, targeting Java17. The implementation remains
local on codex/breadcrumbs-session above fcbef69. Owner visual approval is pending.
No new commit, push, PR12 update, merge, release or external publication occurred.

| Receipt | Fresh observed result |
| --- | --- |
| build/bread-cmd-da0354e4fcd849a5ae4c901bc153cd3c | PASS: complete B1-B7 groups, separate-process scenarios and exact isolated run.bat/build.bat launcher |
| build/bread-cmd-a2537eea1f21484db6e458226833a8c1 | PASS: native Home/partial input/static fields/arrows/Map/save/exit plus actual80x24 restart with reduced motion |
| build/bread-cmd-e358150f444440549ac087fa0dbd06b8 | PASS: final stable160x52 recording, same navigation/save/restart checks, restored terminal |
| Source reconciliation | All173 executable/test/tool inputs and190 Java classes match across the three runs and the final tree |
| Canonical readback | One actual progress update; exact next action Confirm volunteer shifts before Friday; all8 saved files unchanged by read-only loading |
| Approved identity | Every22-row native B3 output matches the unchanged approved renderer; no cropped lettering or Home scroll |
| Media | Actual83.083s1280x832 H264,1989frames/about23.94fps; full decode PASS; Home/gaze/detail/form/save/reopen frames inspected |

The Windows review copy is
C:/Users/Papa/Desktop/Knowledgebase/Projects/Breadcrumbs/build/unified-experience-2026-11-15-e358150f/.
It contains one named MP4, three decoded checkpoints and verification metadata.
The video uses believable synthetic context and scripted native keys. It does not
claim independent human usability validation. Earlier attempts and occluded screen
copies are intermediate records, not the final visual receipt.

Specification self-review: PASS within exercised scope. One opening composition,
exact original identity, complete actions, meaningful transitions, stable fields,
docked responsive bread, coherent eyes, professional existing routes and unchanged
canonical services are covered by source inspection, viewport/gaze assertions and
the native run. The owner-reported disappearance/empty-space and missing-font
issues are explicitly covered. Engineering-quality self-review: PASS within scope;
one render/input owner, bounded capture/cache, clean scheduler/cursor lifecycle,
complete fallbacks and source-bound regression/native evidence. These are separate
single-owner verdicts, not independent reviews. Final Impeccable detect exited0
with no output; Java coverage is not inferred from that result.

Live80-column resize while in the alternate buffer remains NOT_PROVEN because the
host retained160. This differs from the actual80x24 restarted process that passed.
Actual JDK17 runtime, Linux JVM, Windows Terminal, VS Code, independent human input,
Ctrl+C and owner artistic acceptance remain open. Junction fixtures unavailable to
the runner were skipped, not passed. No RELEASE_READY claim. The owner may CONTINUE,
CHANGE, RELEASE or STOP; any protected effect still needs its exact authorized scope.


## Main publication completed (15 November 2026)

Owner-authorized integration PR13 merged to main at
b000b2a3046b0551e1852a307fed23b424e48fe2, with server-enforced expected head
7c54c8938fb7e88297103437db38b6cf9c09ef1d. Immediate GitHub readback confirms base
main, merged state, both expected parents and tree
a989b656c6d7199741f65778e2052c3ffff25571 equal to the reviewed candidate.
GitHub also confirms PRs1 through12 closed with non-null merged_at timestamps.
Their heads were preserved, retargeted to main without rewriting, and incorporated
by the consolidation merge. No open item remains in the frozen13-PR queue.

Independent read-only Terra verdict: PASS+NOTES at C2 above. Fresh native evidence
build/bread-cmd-0a8441a1ddae455bb504bbfc81602ed0 passed actual160x52 B3 Home, numbered
and arrow routes, paused partial/freeform input, Map, save/restart, cleanup and
reduced motion. All173 current inputs match that fresh run. All190 Java class
hashes equal the full B1-B7/process/launcher receipt da0354e4. Full-suite application
evidence is reused by exact byte identity; only the C# native-driver assertion
changed after that full run, and the new native run compiles/exercises it.

The120x30 reviewer attempt exposed the unconditional full-logo test precondition,
not an application regression. One harness-only correction now requires the full
B3 art when it actually fits and otherwise verifies compact branding while marking
full-art acceptance NOT_PROVEN for that run. The earlier83-second recording still
shows the exact same application Java bytes. Its historical173-input equality
predates this single harness change; do not claim that old driver hash is current.

There are zero configured GitHub check runs/statuses. Local regression and the
fresh independent native run are the verification evidence, not a CI claim.
Specification and quality review found no material correctness, security, privacy,
storage or custom-DSA defect. Comment review recorded zero material MUST KILL flags.
No additional application edits or style churn were made during publication.

Code is now published to main under the owner's exact authorization. Application
readiness remains DEMO_READY for the exercised Windows/JDK25 scope targetingJava17.
Live80-column alternate-buffer resize, human usability/Ctrl+C, other terminals and
actual JDK17 runtime plus owner artistic acceptance remain open. No tag, release,
deployment, force push, rebase, branch deletion or external team message occurred.
The primary Windows launcher checkout remains untouched on its existing branch;
the Orca source worktree advances normally to the merged history. A documentation-
only receipt commit follows this merge; it leaves all173 executable inputs intact.
