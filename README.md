# Breadcrumbs

> Leave context behind. Pick up where work stopped.

Breadcrumbs is an offline Java command-line app for recording project progress
and picking up where you left off. Keep what changed, what's blocking progress,
and what to do next in a local workspace you can reopen or hand off to a teammate.

[Quick start](#start-on-windows) · [Classroom demo](#try-the-doubly-linked-list-demo) · [Codebase guide](docs/CODEBASE_GUIDE.md)

- **Capture checkpoints.** Record updates as work happens and reopen a project at
  its latest saved state.
- **Review local files.** Import a project folder with your approval. Review
  supported fields in the Project Map and follow their source references.
- **Share a handoff.** Preview and export Markdown with the project's recorded
  progress, open blockers, next actions, and supporting references.

**Built for Java 17+ with the JDK standard library.** A custom doubly linked list
stores checkpoint history. A directed graph represents declared dependencies, a
custom hash table indexes records, and a custom trie powers prefix suggestions
over captured search terms.

## Start on Windows

1. Download or clone this repository. If you downloaded a ZIP, extract it first.
2. Open Command Prompt and run `java --version` and `javac --version`. Both tools
   need to be available from a **JDK 17 or newer**. If either command is missing,
   add the JDK's `bin` folder to PATH and open a new Command Prompt.
3. Open the extracted folder in File Explorer and double-click [**run.bat**](run.bat).
   It opens the colored Breadcrumbs Home with the large B3 wordmark and animated
   mascot. Navigate with the arrow keys and Enter or type a displayed number.

## Your first project

1. On Home, select **2 Create or import project**, then **1 Create an empty
   project**. Name it `Community food drive`. Press Enter for the General
   project type, then enter any starting details you know. Press Enter to skip
   optional details or the empty-folder prompt.
2. Review the starting overview. Select **4 Confirm starting project record**,
   type `yes` to confirm, then type `yes` at **Save this project on this
   computer?**. Saving lets you reopen this project after closing the app.
3. On Home, select **7 Open project workspace**, then **2 Record progress
   update**. For a practice update, enter what changed (`Booked the hall`),
   what's in the way (`Waiting for the delivery quote`), and what comes next
   (`Ask the delivery company for a quote`). Review the preview and type `yes`.
   Look for the saved confirmation. You can also use **12 Save project** in the
   workspace before closing.
4. To pick up where you left off, launch **run.bat** again. On Home select
   **1 Open saved project** and choose your project. Select **7 Open project
   workspace**, then **1 View project summary**. You'll see the next action and
   any blockers from your last saved update.

To bring in existing files, choose **2 Import an existing folder** during
setup and enter the path to a local folder. Approve the read-only scan and
review the starting overview before confirming. Later, use **13 Explore Project
Map** to review sources, **4 Review file changes** to request a fresh check, or
**11 Prepare project handoff** to preview and export a summary. Type `?` to
show the full menu. Use `0` to go back or exit.

## Try the doubly linked list demo

Double-click **scripts/DSA_DEMO.bat**. The five synthetic checkpoints are ready on
startup. Choose **9 Display structure** to see the links, **1 Append at tail**
to add a checkpoint, **6 Traverse forward** or **7 Traverse backward** to read
the list, and **0 Exit** to end the demo. Results show what happened and the
operation cost. These practice records are discarded on exit.

The Bread developed Breadcrumbs as *An Evidence-Driven Project Continuity and
Context Management System*.

## Command-line and test options

From the repository folder, run `run.bat` in Command Prompt or
`.\run.bat` in PowerShell. It compiles locally and enables terminal colors
automatically. The Windows keyboard helper builds with the compiler included
with .NET Framework 4.x. No package downloads or color settings are needed.

The full Home needs at least **160 columns × 52 rows**. The launcher tries to
fit the window automatically. If your terminal controls the window size,
maximize it or reduce the font size before retrying. Startup failures leave
the window open with an explanation.

For regression testing from Command Prompt, run `scripts\test-b7.bat`.

Native Windows verification with junction and access-denial fixtures uses
`pwsh -NoProfile -File scripts/verify-b7-stage.ps1 -Stage B7E -NativeWindows`.
See the [delivery report](docs/CLASSROOM_FINALIZATION_REPORT.md) for observed
results, runtime version and classroom hardware gates.

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

## Terminal and repository

The colored layout pairs the approved loaf and wordmark with a moving mascot,
written status labels, and keyboard navigation. Run the full app through
[`run.bat`](run.bat) in an interactive Windows console.

| Folder or file | Purpose |
| --- | --- |
| [src/breadcrumbs](src/breadcrumbs/) | Java application and custom structures |
| [tests](tests/) | Unit, integration, process tests and controlled fixtures |
| [scripts](scripts/) | Build and verification wrappers |
| [tools](tools/) | Windows keyboard helper and developer evidence tooling |
| [docs](docs/README.md) | Source guide, classroom checklist, verification and preserved history |

See [the classroom demo walkthrough](docs/B7_CLASSROOM_DELIVERY.md) for the
full numbered DLL menu and a ten-minute presentation. For available full-app
actions, follow the on-screen menu and type `?` to display it again. Supporting
implementation details and the current evidence are linked from the
[documentation index](docs/README.md).
