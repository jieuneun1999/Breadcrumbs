# Document compatibility and real-file test plan

## Current evidence boundary

B7 builds on B6 lexical search over canonical captured fields and adds bounded DOCX main-paragraph capture plus manual-origin Inbox context.
Its acceptance uses disposable synthetic classroom fixtures. No private working
documents were collected or uploaded. One owner-authorized public DOCX has partial bounded validation in B7; the full representative-format matrix remains **PENDING_OWNER_REVIEW**
and remains required before full-application completion. The frozen Master's
adapter matrix describes a wider target than the currently implemented inspector.
This document reports the actual implementation without modifying that frozen target.

Inventory, fingerprinting, extraction, reliable locations, index eligibility and
real-file acceptance are separate stages. A discovered extension is not parser
support. A fingerprint proves bytes, not their meaning or searchable body coverage.
A supported TXT file can still yield no configured field. An index can be READY
for captured fields while the scan is partial or documents remain unsupported.

## Actual format matrix

All current core paths use the JDK only. Metadata fingerprints require scan bounds,
stable reads and exclusion approval. Skipped files are not assumed fingerprinted.

| Format | Current body extraction and search | Locator and limits | Future adapter and dependencies | Safety and real-file acceptance | Current evidence |
| --- | --- | --- | --- | --- | --- |
| TXT | Bounded UTF-8 configured labels/recognized sections only. Eligible captured values searchable. | Physical line and configured field. Existing scan bytes/text/line/claim limits apply. | None for current contract. Full eligible-body search would need an explicit new capture policy. | Actual notes with known labeled phrases; unlabeled phrases must not appear. Include malformed UTF-8 and large lines. | Synthetic extraction/search/negative coverage demonstrated. Real files NOT_PROVEN. |
| Markdown | Configured headings, explicit task markers/statements and fields. No complete Markdown body capture. | Physical line and recognized section. Fenced code omitted. | Optional broader bounded extractor, separately scoped. | Real README/state/handoff/task notes. Preserve conflicting source claims. AGENTS/CLAUDE instructions remain inert and unindexed. | Synthetic captured fields, prefix/exact and instruction exclusion demonstrated. Real files NOT_PROVEN. |
| CSV | Existing bounded quoted CSV parser and configured header fields. Eligible captured cells searchable. | Row start physical line and actual CSV column. No invented word positions. | None for configured schema. Any additional schema requires an explicit mapping. | Real configured sheets exported as CSV; quoted newlines, duplicate headers, malformed rows and unknown columns. | Synthetic configured objective/owner policy checks demonstrated. Real files NOT_PROVEN. |
| DOCX | B7D JDK ZIP/XML main-document paragraphs and split runs; known labels or literal DOCUMENT_TEXT. Reviewed/opted-in captures searchable under CAPTURED-3. | Actual main-part paragraph ordinal including blank paragraphs; no page/layout claims. Archive/XML/text limits documented in B7D_DOCUMENT_ACCEPTANCE.md. | No libraries required. No Word rendering or relationship resolution. | Synthetic ZIP/XML/encryption/revision/sensitivity negatives; original hashes unchanged. No macro/object execution. | Synthetic safety/search/restart plus one authorized public 453-paragraph template, partially captured at the 200-claim bound. Exact paragraph/search/restart and unchanged hash demonstrated. Full representative-file gate pending. |
| PDF | Metadata only. No body text or OCR search. | File identity/hash only. | Optional vetted Java PDF parser. Image/scanned pages require a separately approved OCR adapter. | Actual text PDFs with expected page phrases; encrypted/corrupt PDFs, unusual fonts and extraction order. Image-only pages explicitly unextractable without OCR. | Synthetic PDF-only word absent from index. Page/text/OCR support NOT_PROVEN. |
| XLSX | Metadata only. No worksheet/cell extraction or formula evaluation. | File identity/hash only. | Optional bounded worksheet/cell adapter. No selected package/version. | Known sheet/cell addresses; shared strings, formulas versus cached values, hidden sheets, malformed/encrypted files and archive bounds. | Synthetic body-only term rejected. Cell support NOT_PROVEN. |
| PPTX | Metadata only. No slide/notes body extraction. | File identity/hash only. | Optional bounded slide/notes adapter. No selected package/version. | Known slide and notes phrases; grouped shapes, embedded objects, archive limits and password protection. | Synthetic body-only term rejected. Slide support NOT_PROVEN. |
| Java | Metadata only in the current inspector. No marker/AST search implemented. | File identity/hash only. | Future narrowly configured marker adapter, optional JDK Compiler Tree structure extraction. | Actual class/method examples with known structural locators; no compilation/execution or behavioral claims. | Synthetic no-body-search boundary demonstrated. Real source extraction NOT_PROVEN. |
| JSON | Metadata only. No arbitrary key/schema interpretation. | File identity/hash only. | Future explicit known-key adapter with depth/text bounds. | Known authorized schema and expected JSON path; malformed/deep input and sensitive fields. Never execute workflows. | Synthetic no-body-search boundary demonstrated. Real schema extraction NOT_PROVEN. |
| XML | Metadata only. No body search. | File identity/hash only. | Future configured secure XML adapter. | Known elements/attributes and stable locators; DTD/external entities prohibited, nesting/text bounds and malformed input. | Synthetic no-body-search boundary demonstrated. Real XML extraction NOT_PROVEN. |
| YAML/YML | Metadata only. No body search or general YAML interpretation. | File identity/hash only. | Future narrow known-marker adapter, with explicit schema limits. | Real allowed fields with line/path identity, aliases/tags/depth and sensitive content. No platform/runtime inference. | Synthetic no-body-search boundary demonstrated. Real extraction NOT_PROVEN. |
| Images/scanned documents | Metadata only. No OCR or visual understanding. | File identity/hash only. | Separate optional OCR/image adapter with explicit privacy approval. | Known text images/pages, orientation, low quality, mixed text/image pages and uncertain recognition. | Synthetic image boundary only. OCR and real-file tests NOT_PROVEN. |

POI, PDFBox and Tika are future candidates, not installed or selected B6
dependencies. Exact package/version selection requires a later compatibility,
transitive-dependency, license, security and packaging review. No claim about a
candidate's current version or suitability is made here. Optional adapters must
not prevent the JDK-only academic core from starting independently.

## Future real-document acceptance procedure

1. Obtain explicit owner authorization for exact representative actual documents
   and local fixture destinations. Do not discover unrelated private folders.
2. Keep originals read-only. Record local-only input identities and fingerprints.
   Never commit or upload private files, excerpts or credentials to GitHub.
3. Prepare an expected-results sheet before running extraction. Record exact known
   phrases/fields, positive and negative cases, consent/sensitivity, expected
   page/paragraph/sheet/cell/slide or line locators, and intentionally unsupported portions.
4. Record each stage independently for every file: discovered, fingerprinted,
   extracted body/fields, reliable locators, included search fields, and skipped,
   failed, unsupported or unextracted portions. A successful scan is not a full-text pass.
5. Exercise size, archive, text, token and elapsed-work limits; corrupt/malformed
   input; encryption/password protection; embedded objects/macros; excluded
   credentials; personal/sensitive text; Unicode and terminal-control injection.
   Collect no passwords. Disclose protected/unextractable content as such.
6. Search expected exact terms and prefixes through the actual CLI. Verify real
   canonical IDs, provenance, eligibility, occurrence counts and accurate locators.
   Unsupported-body-only terms must never produce fabricated body matches.
7. Save/restart, switch workspace, and perform an explicitly consented rescan
   after an authorized fixture change/deletion. Verify invalidation, rebuild,
   historical labels, read-only source hashes and unchanged history.
8. Produce a local redacted acceptance table with observed commands, environment,
   file-stage coverage, passing/failing cases and limitations. Publish only synthetic
   or explicitly approved redacted evidence. Keep unavailable gates open.

Real-document acceptance is complete only after these actual documents have been
tested and the owner has reviewed the coverage and failures. B6 synthetic results
do not satisfy that gate. B7 may harden the stable search projection and separately
scope optional adapters. B7 recovery is implemented and synthetically verified; full-application acceptance/release remain pending the owner gates.
