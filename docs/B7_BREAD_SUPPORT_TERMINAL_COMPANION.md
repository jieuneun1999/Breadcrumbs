# Bread Support terminal companion

Bread Support adds a deterministic, visible-symbol character to the existing
Java console. The approved B3 loaf and wordmark remain the brand logo.

## Approved artwork

The owner approved the reference-based symbol version on 8 November 2026, then
requested smoother looping animation and better colors. That instruction freezes
the current character shape. Earlier half-block and ghost-like prototypes are
rejected and superseded. The companion now uses visible ASCII marks only, including
`@`, `O`, `#`, `o`, `*`, `+`, punctuation and slashes. There are no block glyphs,
background fills, image protocols, emoji or runtime image loading.

The full character is a 64-by-22 transcription of the supplied bread reference.
`ReferenceBreadArt` stores its marks, palette and source SHA-256. The offline
`scripts/generate-bread-companion.py` samples only the stated crop of the owner's
RGB PNG. It needs Python's standard library, not Pillow or an image service. The
runtime needs neither the original image nor Python. Color refinement changes the
render palette without regenerating the approved symbol geometry.

The compact character is a separate 28-by-10 design with its own rounded contour,
scoring, face and paw placement. It is not a truncated full frame. Both sizes use
fixed grids, immutable frames, validated row widths and the existing TerminalTheme
foreground colors. Every SGR run resets. Monochrome retains all symbols.
The classic Windows console uses distinct ANSI cream, gold, crust and rose: its
observed RGB rendering flattened the companion into yellow. The native helper identifies a visible classic-console window and selects ANSI,
even if inherited WT_SESSION/COLORTERM variables describe its parent host. A
pseudoconsole keeps RGB; without the helper, explicit truecolor environment
capability is a color hint only and never enables cursor animation by itself.
The original brand helper and accepted startup artwork retain their behavior.

The palette has toasted gold, honey highlights, deep warm brown, cream and muted
rose. The lighter cream makes the face readable on black. Lifted brown shadows
retain the right-side depth without disappearing into the host background.

## Motion and lifecycle

The frame library has nine moods: IDLE, WELCOME, WORKING, ENCOURAGING, SUCCESS,
NEEDS_REVIEW, ERROR, GOODBYE and CURIOUS. A 96-tick cycle holds the approved resting pose,
closes and reopens the eyes, changes a small paw highlight, lifts the existing
right paw and varies a few crumbs. It returns to the exact resting frame. There
is no separate floating third paw. The reserved viewport adds four blank columns:
the character glides up to two cells left or right, then returns to its baseline.
The adjacent box and input prompt stay still. Welcome adds a wave; working uses a
focused mouth and paw highlights; exploration/review use attentive eyes and a
quieter loop. Success adds crumbs, and errors use a calm concerned mouth.

Supported menu input supplies ticks at a configured interval of 125 milliseconds.
Unchanged poses emit no output. Stale queued ticks are discarded after operations;
old frames cannot play back as a rapid burst. Working feedback keeps its existing
bounded timer, advancing the same companion region rather than a second spinner.
No action waits for an animation or celebration to finish.

`BreadSupport` owns one active viewport. It draws only changed mascot lines and
restores the owned input cursor. Menu choices stay fixed. Completing, cancelling
or failing a read disposes its screen before dispatch. Free-form prompts remain
static. Resize stops the old region before input is repainted; the next menu uses
the new viewport. Very small/unknown viewports retain ordinary numbered menus.
If a resize leaves earlier content outside the viewport, `?` redisplays the menu.

If live input fails, the helper is closed and the same prompt asks for a fresh
answer through the ordinary Reader. Interrupted partial input is explicitly
rejected rather than submitted. No menu animation thread is created. Operation
feedback pauses before prompts and closes on every terminal result. Source labels with ambiguous-width Unicode choose
plain static navigation rather than invalidating the fixed animation grid.

## Windows input adapter and portability

Ordinary Reader input remains the portable JDK17 baseline. The optional Windows
adapter is `tools/bread-keys.cs`, built as `bread-keys.exe` beside the application
classes. It attaches only to its Java parent's console, reads CONIN$ key records
and queries CONOUT$ dimensions. Its bounded UTF-8 event pipe is separate from the
console screen. It does not change input modes or use PowerShell.

These documented Windows mechanisms are described in Microsoft's
[console handle guide](https://learn.microsoft.com/en-us/windows/console/console-handles)
and [low-level input guide](https://learn.microsoft.com/en-us/windows/console/low-level-console-input-functions).
The adapter is optional. Missing compiler/helper, redirected handles, missing VT
capability or a failed readiness handshake select static/portable input. No global
installation, credential, system configuration or mandatory native UI library is
introduced. Windows build.bat uses the already installed framework compiler when
available. The core still compiles and runs with only a JDK.

The live editor supports number-plus-Enter, backspace, Ctrl+U, Escape cancellation
and Ctrl+D/Ctrl+Z EOF. It stores bounded input and keeps its visible suffix on one
row. Ambiguous-width Unicode is preserved in the value but echoed as `?` in this
optional editor. Ordinary Reader input retains native Unicode line editing.
Arrow keys and mouse input are not required.

## Menus, truth and preferences

Home, project workspace, setup review, Project Map, progress confirmation, notes,
search, handoff and file-review menus use the shared presenter. Existing action
numbers retain their meanings. Advanced diagnostics remain available. The mascot
contains fixed UI copy, never confidential source text or inferred project facts.
Saving, failed saving, recovery, source freshness and unsaved-session meaning stay
with their existing services and written outcomes.

A full welcome needs at least 120 columns and 30 rows. Compact side-by-side menus
need at least 76 columns and 18 rows, with additional room for every option. If the
whole group cannot fit, the normal wrapped menu remains. Art never removes choices.

Every shared multi-option menu gets an ASCII frame with gold keys, a cream
context question and neutral action labels. Welcome screens place the bread left
of the card; ordinary screens place it right. Result screens use the matching
compact bread and pair actions when both labels fit without wrapping, leaving
space for the actual summary, warnings and next step above. Narrow cards keep
one column. All routes come from the existing canonical action strings.

`--no-animation` disables motion. `--no-mascot` removes the companion. `--no-color`,
NO_COLOR, `--ascii`, redirected input/output and `--color=always` remain supported.
Forced color does not force live cursor control. No preference schema is added.

## Preview and verification

The production entrypoint accepts `--bread-preview=all`, `full`, `compact` and
`idle`. The atlas contains both sizes, all moods, blink and paw poses. It never
opens WorkspaceStore or persists project data. Add `--no-color --no-animation`
for a monochrome atlas. No image file is required at runtime.

The current WSL developer runner is `python3 scripts/verify-bread-support.py`.
`--focused` runs the art/input and production-process groups. `--preview` opens an
isolated idle preview in black CMD; `--preview --app` opens the real application
with its original startup brand and a separate synthetic-demo storage directory. `--native-check` opens a black CMD window
and drives real console key records through the production application. Neither
testing route invokes PowerShell. The native driver in `tools/bread-preview.cs`
captures original console-buffer text. Automated checks launch minimized to avoid
stealing keyboard focus; visible previews also capture a window screenshot. Such a
screenshot can include overlapping windows and is not stronger than buffer evidence
for routing. All demo data is synthetic.

The runner compiles with `--release 17 -Xlint:all -Werror`, uses unique local
Windows temp directories, copies only existing synthetic fixtures and records
source/class hashes. The Linux worktree remains the source; the original Windows
checkout is not modified. No test expectation for persistence, provenance, recovery,
search, privacy, the Project Map or the four custom DSAs is removed.

Native key injection is automated terminal evidence, not human typing. Actual
JDK17, VS Code, Windows Terminal and other host acceptance are separate claims.
Results, source binding, owner feedback and remaining limits are recorded in the
existing B7_EXECPLAN and the current evidence directory. Earlier failed and
superseded attempts remain local and are not final acceptance evidence.
