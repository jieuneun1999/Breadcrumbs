# B6 observed execution evidence

The native verification record is `summary.txt`. It records the actual Windows 10,
JDK 25 run, Java 17 target compilation, 5,765 suite assertions, 225 B6 assertions,
21 B6 A–H production-process assertions, 10 B5 and 55 B4 process assertions.
Junction/ACL checks and the external-directory launcher passed. Symbolic-link
creation was unavailable. Actual JDK 17, Unix and instructor hardware are untested.

All documents/workspaces in these logs are disposable synthetic fixtures. No real
task completion or private-document support is asserted. Text files are actual
Java output, not mockups, screenshots or native-terminal visual evidence.
Review copies normalize Windows line endings and trailing prompt whitespace.
Raw production output remains at the build locations named by the retained logs.

| Artifact | Evidence |
| --- | --- |
| `tests.txt` | Full B1–B6 suite with native junction fixtures |
| `breadcrumbs.b6.ProcessScenarios.txt` | B6 A–H assertions and retained raw fixture location |
| `a-b-c-e-h.txt` | TXT/Markdown search, shared prefixes, source/checkpoint hits, unsupported-body coverage and privacy |
| `d-rescan.txt` | Consented scan, old index rejection, rebuild, removed-source term absent and new-source term linked |
| `f-restart.txt` | New production process rebuilds search from saved observation/history |
| `g-isolation.txt` | Switching resets source opt-in and prevents cross-workspace postings |
| `narrow.txt` | Actual plain/no-color 40-column search output, with every search line checked |
| `breadcrumbs.b5.ProcessScenarios.txt` | Original B5 production-process regression |
| `breadcrumbs.b4.B4Scenarios.txt` | Original B4 A–F production-process regression |
| `launcher.txt` | Actual absolute-path launcher from an external directory |
| `source-sha256.txt` | 115 source/test/script/fixture/launcher input hashes, unchanged during the native verification run |
| `batch-final.txt` | Subsequent passing CMD batch run, without optional junction fixtures |
| `*.input.txt` | Exact synthetic input streams for the B6 production-process demonstrations |

The compatibility matrix and actual-file acceptance gate remain in
[Document compatibility and real-file test plan](../../DOCUMENT_COMPATIBILITY_AND_REAL_FILE_TEST_PLAN.md).
Synthetic format files validate negative body-search boundaries. They do not prove
DOCX, PDF, XLSX, PPTX, structured-format or OCR parsing. Search fixtures exercise
captured fields, not full-document ingestion.

Reproduce from the repository with `scripts\test-b6.bat` or run
`scripts\verify-b6-windows.ps1` for the broader native Windows verification.
