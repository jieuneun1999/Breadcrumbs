# B7 terminal experience

## Theme and rendering

The existing TerminalTheme palette/API and all accepted loaf/wordmark artwork are
preserved. `colorEnabled` and `animationsEnabled` expose capability decisions;
the third constructor enables transient feedback only when explicitly selected by
the entrypoint. TerminalOutput centralizes fixed headings/menu numbers, trusted
labels, secondary explanations, warnings/errors/success, neutral sanitized values
and narrow wrapping. Styling never interprets a source/user ANSI sequence.

Screens using the shared theme include startup/project/daily navigation, quick
checkpoint, Follow, Inbox, Health, audit, rescan/change review, search/results,
baseline/history/diagnostics, work-item/Graph, Hash, DLL sandbox, recovery, document
previews and handoff/export. Default legacy constructors still produce plain text;
the production entrypoint passes one theme through all actual navigation routes,
including --dsa. No parallel theme, full-screen TUI or new artwork was introduced.

## Operation feedback

OperationFeedback implements STARTED, RUNNING, COMPLETED,
COMPLETED_WITH_WARNINGS, FAILED and CANCELLED. Real inspect/rescan counters come
from actual visited-entry callbacks. Save/load/recovery, index rebuild, audit,
comparison and handoff/document generation use explicit operation scopes.
Completion is emitted only after the operation returns and validates. Canonical
save and optional view generation report independent outcomes.

Capable interactive terminals get at most one temporary carriage-return/clear-line
status row, beginning only after 200ms. A per-operation daemon scheduler redraws
every120ms; output is synchronized, it pauses before input and shutdown/clear
occurs on success, warnings, cancellation, exceptions or scope close. Tests verify
real counters and no surviving scheduler/output after completion. Ordinary lookup
and menu navigation have no animation. No work is delayed to display a spinner.
The narrow Windows AccessDenied I/O backoff is separate filesystem recovery logic.

Redirected/no-color/NO_COLOR/unknown terminals use one start and one final result,
with bounded detail lines. No cursor controls or per-frame logging are emitted.
Forced --color=always can color redirected labels, but does not force animation.
ASCII spinner frames and written statuses preserve meaning without Unicode/color.
COLUMNS requests a minimum28-column layout; live terminal size is not detected.
Terminal palette/font/capability differences remain environmental limitations.

## Evidence and adaptation

OperationFeedbackTest covers states, tones, counter redraw, neutral injection,
NO_COLOR, exception cleanup and orphan-thread absence. TerminalConsistencyTest
covers shared advanced-screen coloring, no-color/narrow layout and sensitive-Inbox
isolation. Existing startup presentation tests prove original art/layout behavior;
all B1-B6 state/effect assertions are preserved. The later beginner-language
follow-up migrates presentation literals explicitly. Native/process results are in the final gate.

The owner's recording is a native screen capture of actual Java stdout with
scripted stdin. Java correctly detects its input as piped and uses the concise
nonanimated fallback; the recording does not pretend to prove interactive spinner
capability on all terminal hosts. The actual redraw/cleanup path is exercised by
the focused feedback tests. HTML is labeled a separate readable transcript.

HyperFrames entrypoint and Computer Use guidance were loaded for the recording.
The requested deliverable is raw real-terminal evidence, not an authored film.
No HyperFrames composition/install/update is needed or authorized. Native FFmpeg
window capture and a prompt-driven subprocess script preserve genuine output.
No terminal input is sent through Windows UI automation; that skill's terminal
automation prohibition is respected. No global terminal/system setting is changed.
Only the evidence process's console mode/title is set for readable native capture.

Impeccable Operate guidance was applied as a scoped CLI refinement: existing
identity and fixed labels, observable fallback, bounded output and genuine rendered
inspection. Browser/font/layout detectors were inapplicable; no install occurred.
The Manual/Craft single-owner adaptation kept design, implementation and review
with one writer; no independent review/model agreement claim is made.

## Owner branding clarification

The owner explicitly reaffirmed the polished B3 loaf/3D wordmark as the sole
production logo. Production now calls BrandRenderer.renderApproved, which uses
the exact unchanged RichBrandRenderer/ReferenceBrandArt at160+columns, in color or
monochrome. No explicit --rich-brand selection is needed. Unknown requested width
now defaults to160; COLUMNS can request the actual narrower width. Smaller layouts
show a text label and width hint, with no alternate loaf/wordmark. The old renderer
entrypoint remains only for the frozen original B3 fixture suite; no production
route calls it. New assertions compare approved output exactly to the unchanged
B3 transcription and reject alternate-logo output in narrow production mode.
The first120-column native recording is superseded; delivery uses the clarified
160-column brand. This owner direction governs over the older fallback examples.
## Owner pacing and text-density correction

The owner observed that the first recording was too fast, crowded and appeared to
animate each logo line. The recording driver had flushed each character, which
made the unchanged artwork look like an overlong typewriter sequence. This was
an evidence-tool artifact, compounded by genuine repeated menus/baseline reports.

Production import now presents one short coverage preview. It no longer dumps a
second full preview and a third established-baseline report. Full policy/inventory,
hashes and source detail remain available through the explicit B0 detail action.
Source review shows a bounded literal and defensible origin/locator. Confirmations
and canonical data/limits are unchanged. Home, daily, search and document menus
show in full once per entry, then use short action reminders; `?` redisplays the
full menu. Sections and value groups have breathing room. Follow no longer prints
a duplicated title. Diagnostic reports remain explicit advanced actions.

The approved artwork now renders as one buffered frame. In genuinely capable
interactive output, the startup intro reveals the loaf as one group and replaces
it with the complete approved loaf/wordmark after 120 ms. One cursor-up replacement,
no per-character/per-line delay, no repeated full-screen clearing and no footer
logo. It leaves one static header. This short, explicitly requested brand reveal
is separate from operation feedback and does not pace or delay save/scan/search.
Plain/redirected/uncertain terminals get the whole static approved mark immediately.
ReferenceBrandArt is unchanged; the completed color/monochrome frame retains the
same marks, caption and palette as B3. RichBrandRenderer now buffers that output.

CalmConsoleTest first failed on repeated navigation, then passed menu-density,
section-spacing and grouped-intro assertions. The readable Main subprocess replay
publishes complete prompt-bound responses, preserves newline decoding and pauses
at key reading points. A separate native capture harness runs the actual production
ProjectConsole in two Java processes with visibly scripted Reader input. Process-
local VT support is checked through the real console API before the Java child
inherits that screen buffer. That capture exercises the real grouped intro and
feedback path without pipe-driven character output or UI input injection.
Capture-only Reader pacing is 1 s per ordinary response, 2.2 s at confirmations and 6 s
at each process's initial prompt. No such delays exist in product input handling.
The MP4/manifest distinguishes this harness from ordinary Main-process regression.

## Beginner language and reassuring motion follow-up

The owner approved a clearer everyday workflow while retaining the functions.
`ProjectPresentation` is a read-only CLI projection. It groups the project name,
save state, last update, next step, waiting-on and one priority derived from the
existing B4 audit. It does not create a second stored project model or change
the audit score. Document statements, personal assessments, missing details and
explicitly undocumented details remain distinct. Original literal content is
never processed through a jargon-replacement filter.

| Detailed concept | Everyday label |
| --- | --- |
| Checkpoint | Progress update |
| Continuity brief | Project summary |
| Manual Evidence Inbox | Notes and updates |
| Rescan and drift comparison | Check files for changes |
| Canonical save | Save your project on this computer |
| Materialized artifacts and portable package | Handoff files and handoff folder |

The detailed forms, original numbered advanced tools, full reports, hashes,
IDs, DSA demonstrations and file coverage remain accessible. Note numbers and
progress-update numbers can be used alongside original IDs. File comparison's
`d` option shows the unchanged detailed report. Default search results explain
where a match came from and whether its reported work was independently checked.
Advanced search still shows exact locators, occurrences and index diagnostics.

Operation messages describe the action and its result. Successful project saves
and unsuccessful handoff publication remain separate outcomes. Failed saves say
that changes are still open and need saving before exit. An unsaved generation
preference is explicitly identified as temporary.

The existing one-row feedback now breathes through four ASCII dot frames. It
starts after the existing 200 ms threshold and redraws at 120 ms. Real callbacks
supply counters. Fast work is never delayed. Final written messages remain
understandable without color. The polished B3 art and its 120 ms grouped intro
are unchanged by this follow-up.

`BeginnerExperienceTest` exercises create, record, save, restart, technical access,
save failure and independent handoff failure. It checks source-versus-owner
meaning, unknown-versus-undocumented details, narrow layouts and read-only
canonical bytes. Feedback tests observe all four frames, actual counters,
long-counter width, cleanup and plain output. UI-copy tests are migrated to the
approved labels; persistence, fault, privacy, DSA and old-format assertions remain.

`record-b7-beginner.py` prepares a populated synthetic community food drive with
a 62-paragraph DOCX and coordination files. It first exercises ordinary Main
processes, then records production ProjectConsole in two native Java processes.
All people and reported events are invented. The input harness supplies reading
pauses from a separate per-answer file; the application has no demo delays.
The earlier public-template capture remains historical evidence, while the new
recording demonstrates a believable everyday project rather than a blank template.

## Professional navigation correction

The owner rejected overly casual menu language, inconsistent capitalization,
nonsequential display order and choices hidden by short reminders. This correction
supersedes those incomplete reminders. Home, workspace, search and handoff each
have one ordered action definition reused by full and compact displays. After a
result, every available action remains visible. Wide terminals use two aligned
columns in row order; narrow terminals use a single wrapped list. Historical
numerical routes are retained. Primary headings, labels and prompts use sentence
case with specific action verbs.

Progress confirmation explicitly lists YES/E/A/M/O/D/F/0/? before accepting input.
It no longer accepts useful actions that are mentioned only in hidden help. File
review decisions are separate menu rows. Shared word-aware wrapping preserves
ordinary words and separates prompts from results. The search-specific duplicate
hard-wrap renderer was removed in favor of TerminalOutput. This changes display
only, never the original stored statements.

Home option 8 and workspace option 12 expose the existing Save function directly. Persistence
remains owned by ProjectConsole. DailyConsole reads its current mode through a
BooleanSupplier so enabling saving in the workspace also governs later updates
and save-status messages. A regression exercises actual save/reopen data.

Search's return label works from either workspace or advanced tools. Search
coverage now includes the actual saved observation counts and eligibility/format
limits rather than generic help alone. Underlying search privacy, index and DSA
behavior are unchanged. The accepted B3 logo, colors and operation animation remain.

ProfessionalNavigationTest first failed on hidden post-result actions, then passed
complete action visibility, numeric order, explicit saving, truthful coverage,
confirmation keys, and wrapped choices/prompts at 28/40/80/120 columns in color and
plain output. The native demo preflight additionally asserts every selected menu
key is present in the immediately preceding displayed choices. Deterministic
checks are separate from native visual inspection and owner acceptance.
