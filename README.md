# Breadcrumbs

**Leave context behind. Pick up where work stopped.**

Breadcrumbs is a local, offline Java console application for project continuity.
Import a folder or start empty, review what is actually known, record a confirmed
checkpoint, and return later to its next action, blockers and original references.
No imported folder needs an existing Breadcrumbs session. First import establishes
B0, an observation; native history begins only when you confirm a checkpoint.

B7 implementation is under final acceptance on Windows/JDK 25, targeting Java 17.
Recovery, daily workflows, manual Evidence Inbox, generated Markdown, portable
packages and bounded DOCX extraction are implemented. See [B7 acceptance](docs/B7_FINAL_ACCEPTANCE.md)
for exact observed gates and remaining limits. Full representative-document/owner
acceptance, actual Java 17 runtime and instructor hardware remain pending. No
release or merge is implied. Frozen v1.2 specifications and prior evidence remain
available in [docs](docs/).

The new [Project Map and onboarding](docs/B7_PROJECT_MAP_AND_ONBOARDING.md) show
actual captured goals, documented state, next actions and uncertainty before you
confirm the starting record. Eight common sections work for every profile and
for projects without files. Source reports, your assessments and historical work
stay distinct; conflicting candidates remain unresolved until reviewed.

## Run and verify

Use an existing **JDK 17+** with `java` and `javac` on PATH. The application needs
no Maven, Gradle, Office, Python, database, network or third-party parser library.
From Windows CMD or PowerShell:

```text
run.bat
run.bat --no-color
run.bat "--storage-root=C:\Temp\Breadcrumbs Demo" --color=always
scripts\test-b7.bat
```

The launcher compiles with `--release 17 -Xlint:all -Werror`, resolves its own path
and stops on build failure. An unrelated working directory and paths with spaces
are covered by the native verification command:

```powershell
pwsh -NoProfile -File scripts/verify-b7-stage.ps1 -Stage B7E -NativeWindows
```

Native verification creates disposable junctions and a runnable copy under build,
exercises ACL/path guards, runs the complete regression/process scenarios and
records unchanged input hashes. Tests do not use your normal storage. Actual JDK
17 execution must be run separately when that runtime is available.

Equivalent JDK-only build:

```text
javac --release 17 -encoding UTF-8 -Xlint:all -Werror -d build/manual -sourcepath src src/breadcrumbs/Main.java
java -cp build/manual breadcrumbs.Main
```

Tests use `-sourcepath "src;tests"` on Windows (`"src:tests"` on Unix). Unix
execution is not verified. Source files are complete; no external runtime adapter
is downloaded or activated.

## A useful first session

1. Choose **2 Create or import project**. Choose a personal, study, team or
   software project. If you import a folder, approve reading it and review the
   initial overview. Explore its map, review individual source statements or add
   proposed current details, then choose **4 Confirm starting project record**.
   Cancel discards the proposal. This establishes starting information without
   inventing earlier progress.
2. Choose to save the project on this computer. Then choose **7 Open project workspace**
   and **1 View project summary** to see where you left off, your next step and
   anything you are waiting for. Use **13 Explore Project Map** for the eight
   sections, source details and review actions. Saved notes work even when the
   project folder is offline.
3. Choose **2 Record progress update**. Answer what changed, what is in the way and what
   to do next. Review the answers before typing `yes`. Use `e` to edit, `?` for
   references and other options, or `0` to cancel.
4. Use **10 Manage project notes** for an observation, meeting report or decision.
   A note about what someone said remains a report. You separately choose whether
   its words can appear in search and whether its body can be exported. Private
   notes stay out of search and handoff files.
5. Use **11 Prepare project handoff** to preview readable summary files or export
   a new folder outside the project and save folders. You can also choose to refresh
   these files after saving. Review the files before sharing them.
6. Exit and reopen the saved project. Its summary appears without rereading the
   original folder. Choose **4 Review file changes** when you want a new file
   check. **Technical details** shows the original hashes and reference impact.
   Marking changes as reviewed does not confirm that work is complete.

Menus use a complete compact action list after each result, with aligned columns
when the terminal is wide enough. Every accepted menu action stays visible.
Type `?` to show the full menu again. Options retain their established numbers
and appear in numeric order. Use workspace **12 Save project** or home **8 Save
active project** to save a session-only project; this asks before enabling local
storage. Import shows a source-linked overview; the map progressively reveals
bounded details, original record links and role-grouped sources. Full inventory
remains under View B0 / sources. Reports separate sections with whitespace.

The daily menu also offers attention checks, search, saved updates and dependencies.
Advanced project option **3** preserves the original numbered paths: **9** audit,
**10** current review, **11** rescan, **12** compare, **13** retrieval, **14** handoff,
**16** work items/Graph, **17** Hash and **18** search. Startup **4** is an isolated
DSA classroom sandbox, also available with `--dsa`. Demo data stays synthetic.

## Search and document coverage

Search is lexical exact terms and prefix suggestions over canonical captured
fields, with truthful IDs, origin/review, timestamps, occurrence counts and source
locations. It never rereads source files. Confirmed safe source fields are eligible
by default. Explicit session opt-in can include safe unreviewed terms, with their
snippets withheld. Switching workspaces resets that permission. Changes invalidate
the disposable index; rebuild before using it. No fuzzy/semantic/OCR search.

| Input | Actual body contract |
| --- | --- |
| TXT / Markdown | Bounded UTF-8 recognized labels/headings/task statements; **configured fields, not full text**. Physical line/section locators. Instruction artifacts stay inert and unindexed. |
| CSV | Bounded quoted CSV with known header mappings; row-start physical line and column. Arbitrary tables are not interpreted. |
| DOCX | Bounded main-document paragraphs/runs, known labels or literal DOCUMENT_TEXT; original paragraph ordinals. Field instructions/cached TOC results, comments, layout, headers, drawings and embedded content are omitted. Revisions/unsafe XML fail closed. |
| PDF / XLSX / PPTX / images | **Metadata/fingerprint only**; no body text, formulas, slides, PDF parsing or OCR. |
| Java / JSON / XML / YAML | Metadata only; no generic schema/code understanding or execution. |

The default inspector bounds are 300 entries, depth 5, 256 KiB compressed file,
2 MiB total input, 64 KiB text, 1,000-character paragraph/line, 200 claims and a
cooperative five-second scan budget. ZIP/XML have separate hard bounds. Large
files and partial capture stay visible as limits. A READY index is not complete
source coverage. Missing matches never prove information is absent.

[Compatibility and real-file procedure](docs/DOCUMENT_COMPATIBILITY_AND_REAL_FILE_TEST_PLAN.md),
[B7D contract and E extension](docs/B7D_DOCUMENT_ACCEPTANCE.md),
[search internals](docs/B6_SEARCH_POLICY.md).

## Persistence, privacy and recovery

Default storage is `${user.home}/.breadcrumbs/workspaces/<workspaceId>`. It contains
validated state.txt plus immutable recovery generations/manifests and an atomic
current pointer. Loading requires agreement; interrupted staging or disagreement
requires explicit recovery rather than silent rollback. Startup **5** inspects
read-only candidates; only the newest intact history can activate after exact
acknowledgement. Older candidates remain inspectable. Explicit retention keeps at
least two intact generations; invalid/interrupted bytes are retained.

Canonical schema 1/2/3 stays byte-compatible until a new capability requires schema
4. Checksums detect corruption; they are **not encryption or authentication**.
Use one process per storage root and a quiescent local source folder. Symlink/
junction/reparse paths, source-contained storage and network/drive roots are
rejected. Portable JDK checks are not an OS sandbox against hostile path races.
Atomic moves and file flushes do not establish multi-file atomicity, directory
fsync or power-loss durability. See [B7A recovery](docs/B7A_RECOVERY_EXECUTION.md).

A save failure keeps visible UNSAVED memory for retry. A later document-generation
failure reports **Checkpoint saved. Continuity export failed.** and retains saved
history/previous views. Generated Markdown is rebuildable and is never re-imported
as canonical truth. Portable output is create-only. Windows AccessDenied rename
contention gets four bounded attempts; persistent failures remain errors.

Imported roots are read-only; documents, instructions, macros and scripts never
execute. Known credential markers are rejected, not collected, but this is not a
universal personal-data/secret detector. Manual sensitive bodies remain local-only;
ordinary exported context is INTERNAL. Do not capture credentials. Original source
files and absolute source roots are not copied into portable packages.

## Terminal and classroom delivery

The accepted loaf, wordmark and bread-gold identity are preserved. Shared theme
labels and operation feedback now reach ordinary/advanced workflows. Untrusted
values stay sanitized in default foreground. Color never replaces written statuses.
`--no-color`, `NO_COLOR`, redirected input/output and uncertain terminals fall back
to concise start/final messages. Animation is limited to capable interactive
terminals; no fake percentage/count, per-frame history spam or operation delay.
`COLUMNS` requests width (minimum 28); it is not live size detection. The approved B3 polished logo is the only production artwork. Default requested
layout is160columns; set COLUMNS to the actual width. Smaller layouts show a text
label rather than substituting different artwork. --rich-brand remains accepted
for launcher compatibility; it is no longer needed to select the approved logo.

[Terminal enhancement](docs/B7_TERMINAL_EXPERIENCE.md),
[classroom API/complexity and security inventory](docs/B7_CLASSROOM_DELIVERY.md),
[final evidence and walkthrough](docs/B7_FINAL_ACCEPTANCE.md).
The approved intro uses two grouped frames (loaf, then complete wordmark) over
120ms on capable interactive terminals, then stays static. No per-line or
per-character logo animation. It appears once per application launch.

Python and FFmpeg are optional **developer evidence tools only** for the scripted
native recording. The recording uses real Main output and a downloaded public
project-plan template; scripted input, synthetic companion notes and omitted
content are labeled. The offline HTML transcript and MP4 are distinct artifacts.
