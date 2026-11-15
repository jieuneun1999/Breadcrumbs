# B7D document compatibility and acceptance

## Design and scope

B7C is d640f53, PR #10. B7D adds a JDK-only, bounded in-memory DOCX
main-part reader behind the existing read-only inspector. No parser libraries,
Office automation, subprocesses, network lookups or runtime downloads are used.
The adapter sees bytes only; it receives no filesystem handle. Existing source
fingerprint, stability checks, exclusions, scan limits and consent still apply.

The adapter reads word/document.xml runs in document order. Each physical XML
paragraph increments the ordinal, including blank paragraphs. Recognized labels
map to existing configured fields; other paragraphs become literal DOCUMENT_TEXT.
This does not interpret unlabeled paragraphs as project progress. Main-paragraph
locators are explicit everywhere: baseline, search, handoff and historical views.
The legacy integer `line` slot stores this ordinal only when Adapter.DOCX and
`section=DOCX main paragraph N` agree. Format 4 declares the extension. Older
formats reject new adapter/field values and retain their original grammar/bytes.
CAPTURED-3 adds DOCX eligibility; CAPTURED-1/2 retain previous search semantics.

Archives have at most 128 entries, 256 KiB per inflated part, 1 MiB total inflated
bytes, ratio <=100, no duplicate/traversing member names, and the inspector's
compressed-byte limit (default 64 KiB). XML allows no DTD, external entities or
external resources; nesting <=64 and events <=100000. Paragraphs <=2000, with
existing per-paragraph/total-text/claim budgets. Credentials and unsafe controls
reject all captured text. ZIP encryption and legacy compound-file encryption fail
closed. No entries are extracted to disk. A scan duration is a cooperative budget,
not a hard wall-clock interrupt inside JDK ZIP/XML calls; byte/event bounds limit
work there.

Layout, comments, headers/footers, drawings, embedded content and page identity are
not extracted. Embedded objects/macros are never executed; non-main parts are
bounded but not interpreted. Tracked revisions, alternate content and field
instructions are rejected as unsupported rather than flattened. Strict OOXML
namespaces, exotic producer variants and visual reading order are not promised.
Discovered/fingerprinted/extracted/captured/eligible/searched/real-validated remain
separate stages. Review never independently verifies a source's historical claim.

## Optional format decision

PDF remains fingerprint/metadata only. A robust parser requires a separately
approved exact library/version, full transitive dependency, license/advisory,
runtime, offline-package and classroom-constraint review. No candidate is selected
or activated in this packet. No PDF syntax imitation or OCR is introduced.
XLSX and PPTX also remain metadata only: shared strings/cached formulas/hidden
sheets and grouped slide text/notes require distinct bounded contracts, reliable
locators and privacy tests. DOCX ZIP handling does not imply those semantics.
Java, JSON, XML, YAML and images retain their precise existing metadata boundaries.
The core starts without any optional adapter dependencies.

## Observed tests and remaining owner gate

DocxTest first failed at actual main-paragraph extraction on the unmodified
inspector. It now exercises split runs, blank paragraph ordinals, unlabeled literal
text, real approved-field search, canonical save/restart and unchanged source
bytes. Negatives cover traversal, ZIP inflation/entry bounds, encrypted ZIP and
compound documents, DTD/entities, malformed XML, tracked revisions, paragraph limits
and sensitive text. No partial claim is retained on a failed/rejected document.
All inputs are synthetic generated fixtures, not representative owner documents.

`REAL_DOCUMENT_ACCEPTANCE = PENDING_OWNER_FIXTURES`

Owner procedure: authorize exact local copies of TXT/MD/CSV/DOCX/PDF/XLSX/PPTX and
other formats present in the chosen project. Before running, record expected
phrases, configured fields, paragraph/line/column locators and excluded content.
Hash originals, import read-only, review claims, search approved terms and negative
unsupported-only terms, save/restart, then rehash originals. Record discovered,
fingerprinted, body-extracted, canonical-captured, locator-checked, index-eligible
and actually matched separately for each file. Keep originals, raw extracts and
private evidence local; publish only specifically approved redactions. Follow
docs/DOCUMENT_COMPATIBILITY_AND_REAL_FILE_TEST_PLAN.md for the complete checklist.
Synthetic results do not close that acceptance gate.

## Cross-stage Windows publication correction

Full regression exposed two coverage-classification mistakes for plain text named
.docx. Signature mismatch now remains unsupported metadata-only coverage, while a
real package uses the DOCX adapter and reports extraction failure distinctly. Prior
B6 assertions stayed unchanged. A later run hit AccessDenied at atomic portable
folder promotion, reproducing the earlier B7C environmental symptom. Diagnostic
logging now survives CLI assertion failures and reports export errors separately.

The assumption that a just-written directory is always immediately renamable was
tested directly: a native Windows NOSHARE_DELETE reader blocked promotion; release
of that handle allowed the same operation. The actual intermittent reader's owner
remains NOT_PROVEN. Artifact publication now retries only AccessDenied, at most four
attempts with 25/50/100ms I/O backoff, rechecking safe paths and absent destination
on every attempt. No operation delay is added when the first move succeeds. This
is filesystem contention handling, not animation pacing. Cancellation preserves
interrupt status. A real lock-release test proves convergence and a held-lock test
proves bounded failure with canonical/staged data retained. No silent overwrite,
non-atomic fallback, broad exception swallowing or whole-test retry was introduced.

## Final source-bound D gate

B7D | Started 2026-11-01T03:24:49.7804732+08:00 | Completed 2026-11-01T03:26:01.4910560+08:00
OS Microsoft Windows NT 10.0.19045.0
Exit 0 | 138 input hashes unchanged

All preserved B1-B6 groups, B7A-C, 45 B7D assertions and the recovery/B6/B5/B4 separate-process scenarios passed. Review logs under docs/evidence/b7/d are LF/trailing-whitespace normalized; raw logs remain under build. Specification compliance and engineering-quality self-review pass for bounded synthetic scope. No independent review, Java 17 runtime or real-file acceptance is claimed.

## B7E public-template extension (supersedes the initial D bounds)

The owner explicitly authorized downloading a substantial public sample on
2026-11-01. The Government of Northwest Territories Finance project-plan template
is90,770bytes, has453 main-part paragraphs and302,540bytes of main XML. Its ordinary
paragraphs fit the existing text/paragraph budgets; the previous tiny defaults
would reject a useful everyday document. The E extension raises default file bytes
to256KiB and per-inflated ZIP part to512KiB, preserving1MiB total inflation,128entries,
ratio100,200claims and all existing XML/resource/security bounds. The previous D
record remains an accurate statement of that tested stage, not today's bounds.

Complex/simple field instructions and their cached results are now omitted,
including generated table-of-contents text, instead of rejecting all ordinary
paragraphs in a file containing fields. Field nesting/balance is bounded and
validated; instructions never execute. Revision/alternate-content rejection is
unchanged. A new fixture proves instructions/cache are absent and ordinary body
paragraph2 retains its original ordinal. Capture saturation explicitly reports
partial coverage. No claim of full453-paragraph indexing is made.

PublicDocumentAcceptance independently checks exact paragraph154, confirms its
review, searches `scope`, saves a linked checkpoint, restarts and verifies the
original document SHA256 remains437972d69ad2e86137e62f9551689ec7247a253dacff83b11ad54645d2ea92c0.
Six assertions passed against the downloaded original. Public DOCX acceptance is
PARTIAL within the bounded contract. The complete representative-format matrix,
owner review, instructor/runtime acceptance and PDF extraction remain pending.