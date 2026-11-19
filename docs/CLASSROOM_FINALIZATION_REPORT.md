# Classroom finalization and delivery

## Repository baseline

The worktree is `codex/breadcrumbs-session`, based on
`a3f727f91c753d4a77eba7a5aff804f714e4f7e9`. GitHub confirmed that this is
`main` and that [PR #13](https://github.com/jieuneun1999/Breadcrumbs/pull/13)
was merged as `b000b2a3046b0551e1852a307fed23b424e48fe2`. Verification started from that baseline plus the classroom finalization changes.

| Count | Starting | With local work |
| --- | ---: | ---: |
| Project files tracked or intended as source deliverables | 425 | 435 |
| Java application files | 97 | 97 |
| Java test files | 54 | 55 |
| Documentation and retained evidence files under `docs/` | 245 | 251 |

The reviewed changes modify 24 existing files and add 10 files.
Generated verification evidence lives in the ignored local `build/` directory.

## Code changes and review

- `Main` and `ConsoleMenu` use the real DLL for the focused classroom mode.
  Startup seeds five records. The classroom menu limits paths to the
  demonstrated structure and prevents manually assigned IDs from occupying the
  automatic tail-insert namespace. The original `--dsa` menu stays available.
- The classroom demo uses the same gold palette and approved startup brand in
  compatible terminals. The classroom action frame pairs options when there is
  room and leaves room for the latest result. Plain mode uses numbered input.
  The full-app mascot, motion and number/arrow navigation use the established
  production paths.
- `CheckpointList`, `TaskDependencyGraph`, `CustomHashTable`, `PrefixTrie`,
  `PostingList` and `ContextSearchIndex` explain invariants and costs beside
  the operations. The graph and trie references account for collision work,
  code-point arrays and retained recursion. No DSA algorithm was replaced.
- `BreadSupport`, `TerminalScreenManager`, `ContextConsole`, `ProjectMapConsole`,
  `ProjectMapService`, `ClaimExtractor`, `WorkspaceCodec` and `ArtifactService`
  document boundaries that matter during maintenance and defense. Review found
  a dangling double Javadoc, reserved-ID collision and stale DOCX coverage text.
  Those were corrected. The schema, file bytes and recovery order are preserved.
- `DEMO.bat`, `DSA_DEMO.bat`, `scripts/build.bat` and the Windows matrix clarify
  missing/incompatible Java, failed compilation and optional input fallback.
  `AllTests` runs the new `ClassroomConsoleTest`. The changes to the native
  preview and its driver collect actual classroom output.
- The [README](../README.md), [documentation index](README.md),
  [codebase guide](CODEBASE_GUIDE.md), [requirements checklist](CLASSROOM_REQUIREMENTS_CHECKLIST.md),
  [defense plan](B7_CLASSROOM_DELIVERY.md) and [Mae notes](MAE_DOCUMENTATION_HANDOFF.md)
  connect the runnable source to academic requirements. The audit of 97 Java
  files is in the ignored readability inventory.

## Import and search observations

| File or boundary | What the implementation and fixtures exercise |
| --- | --- |
| Project directory | Controlled roots with empty and initial imports, nested and space-containing paths, ignored locations, consent, bounded reads, cancellation, and test-driven file changes and disappearance. Initial scan is B0. Restarts use separate storage and tests reject source-contained storage. Source hash/byte checks, save/reopen, rescan, drift and search invalidation/rebuild test the lifecycle. |
| TXT | `normal.txt` contains a declared objective and next action. Controlled tests exercise recognized UTF-8 fields, empty text, malformed encoding, binary text and per-file/scan limits. |
| Markdown | `README.md` captures the objective “Build a planner.” at line 2. Known headings and task markers retain real locators. Fenced task `TASK-3` is excluded and inspected text remains inert. |
| CSV | `rows.csv` captures the objective “Plan, review” and preserves an escaped quote in the owner field. Tests verify multiline row starts, malformed rows and incompatible columns. |
| DOCX | `research.docx` captures “Research accessibility” at paragraph 2 from split runs and literal text at paragraph 3. Field instructions and cached field results are omitted. Existing rejection fixtures cover malformed, encrypted/old, bounded ZIP/XML and ambiguous revision cases. Restart preserves the paragraph locator and source bytes. |
| PDF | Metadata/fingerprint and unsupported body only. SearchTest's `unsupported.pdf` contains `bodyonlyword`, which has zero hits after an eligible-fields rebuild. The inspector fixture reports the PDF as unsupported for body text. The previous public W3C PDF is historical evidence. The exact file was unavailable in the audited project locations. |
| XLSX, PPTX, images, code/schema | Metadata-only coverage. Search tests check unsupported bodies stay out of results. Hash availability remains subject to the scan policy. |

The public DOCX gate uses the retained exact copy of
`project_management_plan.docx` with SHA-256
`437972d69ad2e86137e62f9551689ec7247a253dacff83b11ad54645d2ea92c0`.
The professor's Word files support the documentation notes and are outside the
application fixtures.

## Verification evidence

Observed on Windows 10 build 19045 in native CMD, with Oracle JDK 25+37 LTS.
Java compilation used `--release 17 -encoding UTF-8 -Xlint:all -Werror`.

| Gate | Result and evidence |
| --- | --- |
| Final exact-source B1–B7 and process regressions | PASS. Strict build and all eight runners (`build/bread-cmd-57149099608f487bab36d6239a764aaa/run.json`) via `python3 scripts/verify-bread-support.py` with the actual Windows junction fixture. Includes DLL, Graph, Hash, Trie/search, DOCX, storage/recovery, Project Map, terminal, separate Main processes and exact production launcher. |
| Native Windows wrapper and junction suite | `pwsh -NoProfile -File scripts/verify-b7-stage.ps1 -Stage B7E -NativeWindows` passed in a verified Windows copy. It included ACL read/save denial, three junction guards, Windows file-lock behavior, old-format migration, save/restart, complete four-profile processes and a space-path launcher from another working directory. Final source binding is recorded below. |
| Launcher and fallback matrix | 9 CMD scenarios passed (`build/classroom-launch-a0c218ea6ed846218e34ff713535f0be/run.json`). Exact root wrappers, space paths and unrelated working directory, missing Java, Java 8, stale classes after build failure, optional C# build failure, numbered input and clean exit. |
| Public DOCX | Six assertions passed (`build/classroom-finalization-20261119/public-docx-final.txt`). Captured “Define the purpose and scope of the project.” at paragraph 154, confirmed it, matched `scope` with its source locator, saved and reopened a checkpoint, and kept the original hash. The source has 453 main paragraphs and coverage was correctly capped at 200 claims. |
| Enhanced terminal | Native-key checks passed (`build/bread-cmd-e014688f82a242f5aebf04bf0b5a2204/checks.txt`). Visible unchanged B3 wordmark, mascot on Home/workspace, frame change during partial numeric input, arrows and Enter, stable free text, new view, saving/reopening and reduced motion. Home (`build/bread-cmd-e014688f82a242f5aebf04bf0b5a2204/screen-01-home.png`), workspace (`build/bread-cmd-e014688f82a242f5aebf04bf0b5a2204/screen-04-arrow-workspace.png`). |
| Classroom terminal | Native-key checks passed (`build/bread-cmd-16b232207a86410a9968128565689002/checks.txt`). Four normal and two error states captured in real CMD. Each shows The Bread in the title. |

`python3 scripts/verify-bread-support.py --classroom-check` took the classroom
captures below from the actual console. The program received native key events
from the harness. These screenshots are local evidence for the later report.

| Normal cases | Error cases |
| --- | --- |
| Startup with five records (`build/bread-cmd-16b232207a86410a9968128565689002/01-start.png`) | Missing ID on delete (`build/bread-cmd-16b232207a86410a9968128565689002/05-missing-id.png`) |
| Display both endpoints and links (`build/bread-cmd-16b232207a86410a9968128565689002/02-structure.png`) | Out-of-range insertion (`build/bread-cmd-16b232207a86410a9968128565689002/06-invalid-position.png`) |
| Append with generated ID (`build/bread-cmd-16b232207a86410a9968128565689002/03-tail-insert.png`) | |
| Search and show a record (`build/bread-cmd-16b232207a86410a9968128565689002/04-found-record.png`) | |

The initial Windows wrapper suite ran before the final classroom menu layout
adjustment. The final suite repeated its core regressions on the current source,
including its Windows junction tests. The public DOCX was also rerun with current
application classes.

**Still unverified.** Actual mouse double-click from Explorer and instructor
hardware. Explorer could enumerate the window, but the native control timed out
before it could open the verified copied folder. Build and launch tests above
are scripted CMD or native-key evidence. Actual Java 17 runtime is **NOT VERIFIED**.
Native live resize to 80 columns did not apply on the host. Simulated narrow-width
and reduced-motion tests passed. Windows junctions worked and ordinary symbolic
link creation was unavailable for the fixture account. The build, launch and tests
used local files and tools. Host-level network-disconnected execution was
**NOT VERIFIED**.

Final source convergence is recorded in
SHA-256 input reconciliation (`build/classroom-finalization-20261119/final-verification.json`).

## Cleanup and handoff

The [cleanup manifest](CLASSROOM_CLEANUP_MANIFEST.md) lists the two reproducible
cache files removed and why local recordings, tests and historical evidence were
kept. The release checklist remains local. See
[Mae's Markdown notes](MAE_DOCUMENTATION_HANDOFF.md) for the supported report
material and the section, group number, member details and contributions the
team needs to provide. The report length instructions give 5–8 pages while the
template uses nine pages. Classroom computer acceptance and instructor
approval remain separate owner checks.
