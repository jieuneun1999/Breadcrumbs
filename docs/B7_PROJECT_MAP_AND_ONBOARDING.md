# B7 Project Map and onboarding

The initial review now explains the actual captured project goal, documented
state, next action, important uncertainty and inspection coverage before a
starting record is confirmed. All four profiles use the same eight sections.
This is a read-only organization of recorded context, not project understanding
or independent verification of work.

## Using the flow

With no saved projects, the welcome explains how to import a folder or start
without files. Returning users keep the ordinary project navigation. Choose
**2 Create or import project**, select the entry mode and profile, and approve a
bounded read-only folder inspection when applicable. Files are never executed,
moved or edited. Unsupported formats, excluded paths and extraction limits remain
explicit.

The overview offers these complete choices:

- **1** Review detected context individually: confirm, reject or leave unknown.
- **2** Explore the Project Map and its original source links.
- **3** Add or clarify current context as your own proposed assessment.
- **4** Confirm the starting project record.
- **0** Cancel setup.

Proposed assessments stay local to the unconfirmed setup. The existing immutable
BootstrapDraft carries source review dispositions; a local array carries proposed
ContextReview values. Only explicit final confirmation activates B0 and applies
those reviews through WorkspaceState. Cancelling discards both proposals and
leaves the previous active/saved project intact. Import never creates the first
progress update. Saving remains a separate explicit choice.

| Experience | Before | With this refinement |
| --- | --- | --- |
| First import | Counts and individual source-review prompts before confirmation | Actual goal/state/next action, relevant uncertainty and coverage; deeper review remains available |
| No files | Supplied starting fields and later update tools | The same useful overview/map, proposed current details and separate progress history |
| Return later | Summary and separate advanced record tools | Summary plus ordinary workspace map, preserving original records and explicit file-recheck consent |

After setup, choose **7 Open project workspace**, then **13 Explore Project Map**.
The existing numeric routes remain stable. Use section numbers, select a displayed
item, and use **T** for original record, observation, checkpoint, locator, timestamp
and fingerprint. **R** reviews eligible source or note records. **A** adds an
existing-field current assessment. **N/P** page; **0** returns one level; **?** at
the map explains its meaning. Only currently displayed page item numbers work.
The sources section also offers **S**, grouping files by their existing role.

| Section | Recorded information |
| --- | --- |
| Purpose | Source goals/deliverables, supplied context and explicit current assessments |
| Current state | Documented state/owner/test reports and current assessments; no invented runtime status |
| Progress & history | Original baseline, later observations and historical progress updates |
| Tasks & next actions | Document task candidates and separately labeled canonical WorkItems |
| Decisions | Source decisions/rationale, explicitly recorded assessments and eligible notes |
| Blockers & dependencies | Reported obstacles and explicitly declared Graph relationships |
| Evidence & sources | Saved file metadata, neutral document text and eligible manual context |
| Unknowns & conflicts | Missing fields, incomplete coverage, unresolved alternatives and existing audit/freshness findings |

## Truth, provenance and privacy

Each map item retains typed origin, review disposition and structured source/time
links. Source reports, supplied starting details, current human assessments,
historical updates, manual notes, tasks, dependencies and derived findings stay
distinct. Source task mentions never become WorkItems automatically. File roles
are classification hints. Unlabeled paragraphs remain document text rather than
invented purpose/current-state claims.

The overview uses actual values and locators from that same projection. Multiple
candidates remain alternatives: no newest-file, first-file or majority winner.
An explicit human assessment is displayed as that assessment, while unresolved
source conflicts remain visible. Bounded excerpts disclose that additional
candidates may exist; absence from an excerpt does not prove missing context.
Overview cap warnings consult the fields' own categories, so a document-text
limit does not manufacture uncertainty about a fully displayed project goal.
Unknown and NOT_DOCUMENTED remain explicit. Confirmation does not verify reported
test results or completed work.

PDF/XLSX/PPTX bodies contribute no extracted claims. Eligible DOCX main paragraphs
retain their true paragraph ordinals; partial capture stays partial. Excluded or
skipped file names/bodies are omitted. Private manual notes expose no body or
origin. Other manual text is withheld until an explicit local **V**, followed by
`yes`; existing search/export permissions are unchanged. New display boundaries
sanitize controls and withhold recognized secret markers or instruction-like
payloads. This conservative display filter is not a replacement for the existing
parser/path/security boundaries or a general malicious-text detector.

Opening a saved workspace performs no source scan. The map says **Files have not
been checked for changes during this session.** ProjectConsole compares the
current observation ID with the ID captured at open. A consented new observation
changes the current projection; old B0 and checkpoint references stay original.
Saved context remains available when the folder is offline. Immediate map
projection has no spinner or artificial delay. Existing real-operation feedback,
theme, B3 loaf/wordmark and monochrome fallbacks remain in place.

## Ownership and bounds

ProjectMap is an immutable disposable record projection with defensive array
copies. ProjectMapService reads BootstrapDraft or WorkspaceState and never reads
or writes source files. ProjectMapConsole handles navigation; BootstrapReviewConsole
owns unconfirmed proposals; DailyConsole routes requests to existing canonical
review/note owners and persistence callbacks. No new storage schema, parser,
index, DSA or competing handoff generator is introduced. ArtifactService continues
to generate state/HANDOFF from the canonical records.

Retained display limits: 8 categories, 64 items/category, 4 items/page, 1200
characters/value, 160/title, 600/note, 240/overview attention and 220/overview main
value (at most two 100-character alternative examples, 200-character basis each).
Counts include omitted items. At over 1000 historical
updates, only the latest is projected with an explicit notice; existing history
tools retain access. Detailed reused audit work is deferred above 500 entries per
compared baseline/current observation, 1000 updates or 200 latest-update source
references. Existing scan and canonical capacities still apply: default 300
entries/200 claims; configurable ceilings 10000 entries/2000 claims; 20 later
observations, 2000 context reviews, 500 manual captures/2000 manual reviews and
current Graph bounds of 100 items/500 dependencies.

Construction is not O(1). Let E be observed entries, C captured claims, R context
reviews, F context fields, M manual captures, MR manual reviews, H projected history,
T tasks and D dependencies. Filtering resolves source matches in O(C*E); source
role grouping adds O(E*C). Existing conflict grouping and per-claim membership
checks are O(C²). Current review overlay is O(R*C), current statements O(F*R),
manual dispositions O(M*MR), and history/Graph traversal O(H+T+D). Strings also
cost their processed lengths. Audit repeats bounded claim/review work and invokes
existing reference-freshness comparison/index resolution. Its saved observation
comparison may be quadratic in compared entries, and lazy disposable index
rebuilds traverse canonical records. These costs are reused, not hidden under a
constant-time claim. Category retention is O(8*64); working arrays also scale with
the bounded input records/conflict groups and reused audit/index work.

## Verification and usability evidence

ProjectMapTest covers all four profiles, eight sections, literal provenance,
same-task contradictory alternatives, graph/manual/history separation, original
B0 retention, authorized drift, persistence reconstruction and canonical byte
immutability. It also covers unrecognized input, unsupported formats, DOCX
paragraphs/partial capture, category caps, foreign-observation proposals and
display safety. ProjectMapConsoleTest exercises setup/source review C/R/U,
uncommitted clarification/cancellation, offline restart, all eight sections,
paging/help/back, source groups, local note consent and color/plain output at
28/40/80/120/160 columns. Existing assertions retain their state/effect checks;
production input tapes add the explicit overview-confirm action.

ProjectMapProcessTest runs six actual Main scenarios with fresh storage: thesis
documents, software documentation/code metadata, operations plans/CSV, a project
without files, messy/conflicting sources and the full continuity loop. Synthetic
DOCX files have 80 paragraphs. It checks actual saved canonical records, source
byte identity, no implicit scan, explicit recheck, original checkpoint references
and standalone handoff generated through the existing service.

The current native demo uses the real ProjectConsole in two Java processes,
scripted input and a populated 62-paragraph synthetic project. Reading pauses
belong only to its input harness. The preflight rejects selected menu keys absent
from the immediately preceding displayed menu. This is developer inspection with
scripted task evidence, not a usability study or independent participant feedback.
Current execution receipts and retained artifacts are linked from
[B7 acceptance](B7_FINAL_ACCEPTANCE.md) and the [ExecPlan](B7_EXECPLAN.md).

Developer inspection checklist: can the first screen explain goal/current state/
next action without hashes; can unknowns and alternatives be distinguished; can
the user reach original locators before confirming; does cancellation preserve
state; do fileless projects remain useful; do saved records reopen without a scan;
can tasks/reports/history be distinguished; and can a standalone handoff be read
without Breadcrumbs? Deterministic checks cover these routes. Actual rendered
inspection and owner acceptance remain separately identified in the receipts.

Windows/JDK25 targeting Java17 is the exercised runtime. Actual JDK17, Unix,
instructor hardware, full representative-file acceptance and independent/owner
UX acceptance remain open. No merge, release or new Discord publication follows
from these tests.
