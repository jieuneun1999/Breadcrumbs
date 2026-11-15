# Bread Support: local review evidence

Evidence was captured before the owner-authorized checkpoint commit on branch
`codex/breadcrumbs-session`, starting HEAD
`ee01d6167718e7a2a71a170678b1fa00b145b53f`, with the verified local changes.
[PR12](https://github.com/jieuneun1999/Breadcrumbs/pull/12) was read back OPEN,
unmerged, head `feat/b7e-final-integration` at the same commit, base
`feat/b7d-document-compatibility`. At evidence capture no commit, push, PR edit or merge had been performed. The
later owner-authorized local checkpoint is recorded in
[the execution plan](../../../B7_EXECPLAN.md). Its source inputs match these gates;
no push or PR mutation is part of that checkpoint.

## Visual evidence

- [Full companion](full-stdout.svg) and [matching compact companion](compact-stdout.svg): exact production ANSI character/color output projected onto a fixed SVG cell grid. These are output previews, not native screenshots or live-input proof.
- [Actual visible black CMD preview](visible-cmd-preview.png): original B3 loaf/wordmark, companion and framed menu. It contains an owner's partially typed entry and overlapping window edges; it is not a controlled human acceptance result. Its [source/class binding](visible-preview-run.json) matches all 185 Java classes exercised by regression.
- [All nine expressions, both sizes and blink/paw/crumb poses](atlas-monochrome.txt), with [color output](atlas-color.ansi). These come from `Main --bread-preview=all`; preview opens no project storage.
- [Idle frame 1](native-console/01-full-main-menu.txt), [changed idle frame](native-console/02-idle-frame.txt) and [partial selection](native-console/03-partial-number.txt) are direct reads of the running application's native console buffer, not piped input recordings.
- [Project workspace](native-console/05-compact-workspace.txt), [Project Map](native-console/06-project-map.txt), [update confirmation](native-console/07-update-confirmation.txt), [saved update](native-console/08-saved-update.txt), [80-column navigation](native-console/10-narrower-workspace.txt), [exit](native-console/11-clean-exit.txt) and [returning user with actual next step](native-console/12-returning-user.txt) use synthetic data only.
- NEEDS_REVIEW/ERROR art is included in the atlas; state transitions and injected failure/recovery behavior are exercised by the tests. No native human error/recovery walkthrough is claimed.

## Verified commands

`python3 scripts/verify-bread-support.py` finished PASS. It compiled production and
tests with Windows JDK25, `--release 17 -encoding UTF-8 -Xlint:all -Werror`, and ran:

1. `breadcrumbs.AllTests` (existing B1-B7 groups plus BreadSupportTest).
2. `breadcrumbs.cli.BreadSupportProcessTest`.
3. `breadcrumbs.storage.B7RecoveryProcessTest`.
4. `breadcrumbs.b7.ProcessScenarios`.
5. `breadcrumbs.b7.ProjectMapProcessTest`.
6. `breadcrumbs.b6.ProcessScenarios`.
7. `breadcrumbs.b5.ProcessScenarios`.
8. `breadcrumbs.b4.B4Scenarios`.

[Regression logs and manifest](regression/run.json) bind all 165 source/tool/test
inputs and 185 application Java classes. Existing unavailable symlink/junction
fixtures are explicitly SKIP in AllTests; ACL tests ran. Those skips are not passes.
No existing assertions were weakened. The original B3 art files have an empty diff.

`python3 scripts/verify-bread-support.py --native-check` finished
PASS_NATIVE_AUTOMATED. Its [checks](native-console/checks.txt) prove changed live
frames without extra scrollback rows, preserved paused numeric input, static longer
input, one saved workspace with recovery generations, next-step visibility after
restart, and clean exits. The driver writes real console key records and reads the
actual console buffer. It launches minimized so checks do not take keyboard focus.
No PowerShell is invoked by these commands.

Source binding and unchanged inputs were checked again after both runs. The final
regression run is `215ab4e20bc746da846cb21d369fc0ff`; native check is
`825bb9a21421445396cbc08d404fb850`. Earlier failed/drifted runs are superseded.

## Reproduction and limits

Run `python3 scripts/verify-bread-support.py --preview --app` from the session
worktree to open the real application in black CMD with an isolated demo store.
Use `--preview` for the isolated living mascot, or production `--bread-preview=all`
for both sizes and all expressions. No project state is created by the art atlas.
The WSL runner uses the already installed Windows JDK25 and Framework compiler;
it does not install them. Normal Windows build/run scripts retain their JDK path.

| Environment or condition | Observed scope |
| --- | --- |
| Classic black CMD | Native event animation, partial numeric input, long form input, navigation, save/restart and exit passed under automated driving. Visible palette/menu inspected. |
| 80 x 24 resize | Old animation stopped; `?` repainted readable numbered navigation. Earlier scrollback can wrap. |
| Redirected/scripted, no color, no animation, no mascot | Production-process and deterministic fallback tests passed. |
| Windows Terminal / VS Code / other hosts | Not exercised for human input acceptance; RGB support is capability-gated. |
| PowerShell | Excluded by current owner direction. No current PowerShell test claimed. |
| Actual Java17 runtime | Not available; Java25 targeting release17 only. |
| Human typing protocol / all fonts and resize paths | Still open. Owner preview activity is not a documented acceptance pass. |

There is no video, external upload, independent participant result or claim of
universal live animation. Optional helper absence/failure selects portable input;
unknown/small viewports and ambiguous-width source labels use static navigation.
ASCII echo in the optional editor preserves Unicode values but displays uncertain
characters as `?`; ordinary Reader mode retains native line editing. The companion
never certifies work completion or changes canonical project semantics.

## Changed implementation files

The evidence directory is additional to the following source, tooling and design
changes. See the [design document](../../../B7_BREAD_SUPPORT_TERMINAL_COMPANION.md)
for architecture and preferences.

- `docs/B7_EXECPLAN.md`
- `scripts/build.bat`
- `src/breadcrumbs/Main.java`
- `src/breadcrumbs/cli/BootstrapConsole.java`
- `src/breadcrumbs/cli/BootstrapReviewConsole.java`
- `src/breadcrumbs/cli/ConsoleMenu.java`
- `src/breadcrumbs/cli/DailyConsole.java`
- `src/breadcrumbs/cli/DocumentsConsole.java`
- `src/breadcrumbs/cli/InboxConsole.java`
- `src/breadcrumbs/cli/OperationFeedback.java`
- `src/breadcrumbs/cli/ProjectConsole.java`
- `src/breadcrumbs/cli/ProjectMapConsole.java`
- `src/breadcrumbs/cli/SearchConsole.java`
- `src/breadcrumbs/cli/StartupRenderer.java`
- `src/breadcrumbs/cli/TerminalOutput.java`
- `src/breadcrumbs/cli/TerminalTheme.java`
- `tests/breadcrumbs/AllTests.java`
- `docs/B7_BREAD_SUPPORT_TERMINAL_COMPANION.md`
- `scripts/generate-bread-companion.py`
- `scripts/verify-bread-support.py`
- `src/breadcrumbs/cli/BreadFrames.java`
- `src/breadcrumbs/cli/BreadPortrait.java`
- `src/breadcrumbs/cli/BreadPreview.java`
- `src/breadcrumbs/cli/BreadSupport.java`
- `src/breadcrumbs/cli/MenuBox.java`
- `src/breadcrumbs/cli/ReferenceBreadArt.java`
- `src/breadcrumbs/cli/ReferenceCompactBreadArt.java`
- `src/breadcrumbs/cli/TerminalKeyboard.java`
- `tests/breadcrumbs/cli/BreadSupportProcessTest.java`
- `tests/breadcrumbs/cli/BreadSupportTest.java`
- `tools/bread-keys.cs`
- `tools/bread-preview.cs`
- `docs/B7_DECISIONS.tsv`

## Owner-authorized checkpoint verification

The later `python3 scripts/verify-bread-support.py --focused` run
`bdeab0e94de5449a8e8eec33ff3f51d0` passed BreadSupportTest and
BreadSupportProcessTest immediately before committing. All 165 inputs remained
unchanged. [Fresh checkpoint manifest](checkpoint-focused/run.json) and its logs
preserve that separate receipt; they do not relabel the earlier full/native runs.
The checkpoint adds the resume note to B7_EXECPLAN and remains local to the existing
session branch. The PR branch is checked out in the preserved Windows worktree.

Raw text/ANSI captures intentionally preserve CRLF and trailing cell padding.
The folder-local attributes retain their exact bytes and exempt only these
captures from whitespace checks. Source-code whitespace checks remain active.
