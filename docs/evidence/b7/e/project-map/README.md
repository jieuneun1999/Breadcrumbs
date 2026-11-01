# Project Map evidence — 1 November 2026

DEMO_READY for the exercised Windows/JDK25 scope, targeting Java17. Source commits
9fe697b, 4ef73fd and e121d75 implement the projection, CLI integration and final
field-limit correction. This bundle supersedes earlier local Project Map captures.
It is scripted developer inspection, not independent user feedback or owner
acceptance. PR12 remains the delivery target; no merge/release is implied.

## Observed checks

`pwsh -NoProfile -File scripts/verify-b7-stage.ps1 -Stage B7E -NativeWindows`
passed at 22:18:00–22:21:40 Asia/Manila. All 157 source/test/script inputs remained
unchanged. See [summary](verification-summary.txt), [full checks](verification-tests.txt),
[input hashes](verification-source-sha256.txt) and [space-path launcher](verification-launcher.txt).
The gate runs B1–B7, recovery processes, four-profile continuity, six Project Map
Main scenarios, native junction/ACL guards and an unrelated-cwd launcher.

| Production Main scenario | Initial overview/map | Return / continuity |
| --- | --- | --- |
| Academic thesis with documents | [Import](thesis-01-overview-map.txt) | [Reopen](thesis-02-reopen.txt) |
| Software documentation and code metadata | [Import](software-01-overview-map.txt) | [Reopen](software-02-reopen.txt) |
| Operations plan and CSV | [Import](operations-01-overview-map.txt) | [Reopen](operations-02-reopen.txt) |
| General project without files | [Setup](without-files-01-overview-map.txt) | [Saved update after restart](without-files-04-no-files-resume.txt) |
| Incomplete/conflicting project | [Import](conflicting-01-overview-map.txt) | [Reopen](conflicting-02-reopen.txt) |
| Complete continuity loop | [Import](continuity-01-overview-map.txt) | [Work, notes, review, tasks, handoff](continuity-03-work.txt); [authorized recheck and new handoff](continuity-04-recheck.txt) |

Each folder scenario includes an 80-paragraph synthetic DOCX. Fixtures, sources,
canonical stores and exported projects remain local. Source byte-identity checks
allow only the explicit harness edit in the continuity scenario. Initial setup
creates B0 only; no-file progress is a separately confirmed update. State/effect
assertions were retained when production input tapes gained overview confirmation.

## Actual native console

The final 332.667-second, 1280×800, 24fps video records the production ProjectConsole
in two Java processes. It uses a populated synthetic project with a 62-paragraph
DOCX, project overview and CSV. People/events/amounts are invented. Reading pauses
are scripted input timing, not fake application work. The preflight checks that
selected primary/menu keys were displayed immediately before selection.

Local MP4, not committed:

`build/b7-project-map-delivery-4a0023a07e87410db3562c1e99c84117/walkthrough.mp4`

SHA256: `f41a9901663a1ef01c84707194525d372c5fe3e0d24f4285ad21e54d310f1754`.

All 169 captured application class hashes match the current build; all 157 final
gate input hashes also match. Full MP4 decode passed without errors. Canonical
readback confirms one progress update, one reported note, one later observation
and one scoped change acknowledgement. All 12 saved file paths/bytes remain
unchanged by inspection. See [reconciliation](native-reconciliation.json),
[class/source manifest](native-class-and-source-manifest.json),
[decode](native-decode.txt), [readback](native-readback.txt), and native transcripts
for [setup](01-start-native.txt) and [returning work](02-return-native.txt).

Unaltered native frames, extracted at the stated video seconds:

| Seconds | Frame | Observed behavior |
| --- | --- | --- |
| 5 | [Approved B3 brand](00-approved-brand.png) | Existing loaf/wordmark and first-launch welcome |
| 74 | [Initial overview](01-initial-overview.png) | Actual goal, state, next action, source locators, uncertainty, coverage and complete confirmation choices |
| 52 | [Source details](02-source-record.png) | Literal value plus original record, observation, locator, timestamp and fingerprint; uncommitted status |
| 122 | [Recorded history](03-recorded-history.png) | Starting record separate from update; readable summary/next-action/blocker lines |
| 188 | [Returning map](04-returning-map.png) | Saved records reopen with the exact no-file-recheck statement |

The inspection also covered persisted progress, reported-note search, an explicit
file comparison and a standalone exported handoff. No new source-folder scan is
triggered by opening the map. The separate Main conflict scenario demonstrates
alternatives; the normal native example uses correctly labeled CSV categories.

## Developer usability checklist

| Question | Evidence and observed result |
| --- | --- |
| Can a newcomer identify the goal, state and next action before confirming? | Initial native frame/transcript: actual source-linked values, no hashes on the overview |
| Can they see unknowns and competing reports? | Conflict Main transcript and projection tests; no newest-file winner |
| Can they inspect where a statement came from? | Source detail frame and C/R/U CLI tests |
| Can they cancel proposed details safely? | CLI test: 0 and /cancel discard setup and preserve saved canonical bytes |
| Does it work without a project folder? | No-file Main setup/update/restart transcript |
| Can they distinguish history, source reports and tracked tasks? | Native history and full continuity Main scenario; explicit origin labels |
| Can they reopen context without rereading files? | Returning frame, offline CLI test, explicit recheck scenario |
| Can they retain a readable handoff? | Real native export and standalone Main package readback |

These are developer judgments and deterministic task results. Independent
participants: **0**. Owner UX acceptance, actual JDK17, Unix/instructor hardware
and the full representative-file matrix remain open. PDF/XLSX/PPTX bodies stay
unsupported; DOCX capture remains bounded. No benchmark/performance claim is made.

## Evidence handling and reproduction

Published text removes ANSI sequences, substitutes `<repo>`, `<demo>` or
`<scenario-root>` for local absolute roots, and trims trailing whitespace. It does
not rewrite application wording or results. Screenshots are unaltered pixels;
their selected views contain no private file paths. Raw logs, MP4, generated source
projects and canonical stores remain under build. Older passed/failed receipts
are retained locally and identified in the ExecPlan, rather than overwritten.

The application remains JDK-only. Developer capture additionally uses the existing
Python and FFmpeg installations. With current production classes built, compile
`tests/breadcrumbs/cli/NativeWalkthrough.java` against build/app into build/native-demo,
then preflight with `python scripts/record-b7-beginner.py --output build/NEW-UNIQUE-DIRECTORY`.
Record that directory with `--record` in a native Windows console configured to
160 columns/50 rows. Capture is create-only; use fresh storage/output paths.

See [implementation and domain mapping](../../../../B7_PROJECT_MAP_AND_ONBOARDING.md)
and [acceptance record](../../../../B7_FINAL_ACCEPTANCE.md).
