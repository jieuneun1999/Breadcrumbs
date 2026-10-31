# B4 observed verification evidence

All projects/claims in these artifacts are synthetic fixtures. They are not evidence
that Breadcrumbs itself completed the fictional project work. Output comes from
the real Java CLI; no screenshot/mock interface was substituted. Accepted B3 artwork
remains unchanged; [prior startup visuals](../terminal-visual/README.md) remain the
visual checkpoint.

Final Windows/JDK 25 run: 2026-10-31 12:57:18–12:57:44 +08:00. Strict Java 17
target compilation and **4,450 suite assertions** pass: original B1 3,780, B2 192,
B3 291 including presentation, B4 187. Separate processes pass **55 A–F assertions**.

| Artifact | Observed evidence |
| --- | --- |
| [summary.txt](summary.txt) | Complete Windows harness outcome; native junction/ACL tests and external-directory CMD launcher |
| [tests.txt](tests.txt) | All original and B4 test-group pass counts and visible symbolic-link skip |
| [source-sha256.txt](source-sha256.txt) | Exact exercised source/test/script/run.bat bytes; read back against current files |
| [scenarios-summary.txt](scenarios-summary.txt) | A–F process results, times and retained local fixture directory |
| [a-baseline.txt](a-baseline.txt) | NEW_EMPTY B0, no native history, nine-rule audit and baseline-only report |
| [b-conflict.txt](b-conflict.txt) | Same-ID state/tasks conflict, original locators and skip/cancel review |
| [c-restart-unscanned.txt](c-restart-unscanned.txt) | Reopened native CP with unknown live freshness and no automatic rescan |
| [c-unrelated-scan.txt](c-unrelated-scan.txt) | Changed unrelated notes; checkpoint-referenced freshness remains FRESH |
| [c-referenced-scan.txt](c-referenced-scan.txt) | Changed referenced README; MODIFIED and STALE warnings |
| [d-ambiguous-rename.txt](d-ambiguous-rename.txt) | Actual two-to-two equal-hash rename ambiguity |
| [e-retrieval-handoff.txt](e-retrieval-handoff.txt) | Three DLL checkpoints, explicit rank factors, selected CP preview and export confirmation |
| [sample-handoff.md](sample-handoff.md) | Exact exported standalone handoff, original provenance, stale warning and uncertainty |
| [f-v1-read.txt](f-v1-read.txt) | Original imported B3 v1 CP restored without rewriting storage |
| [f-explicit-upgrade.txt](f-explicit-upgrade.txt) | Current applicability mutation then explicit Save upgrades v2 |
| [f-upgrade-restart.txt](f-upgrade-restart.txt) | Separate process restores original CP, evidence IDs and counter after upgrade |
| [launcher.txt](launcher.txt) | Actual run.bat invoked by its absolute path outside the repository |
| [fixtures](fixtures) | Synthetic source files after deliberate test edits/renames; initial bytes and transitions are defined in B4Scenarios.java |

Raw input scripts, app storage files and all process transcripts remain under the
ignored local `build/b4-scenarios/run-5155024042723035543`; verification metadata
remains under `build/b4-verification-bb3dc6dc70654decb2056b9d69c2e9bf`. This committed
selection is 116 KB before this index. Source hashes were stable throughout the run.
The fixtures' `.env` value is synthetic and excluded from output and this source pack.

Actual baseline audit excerpt:

```text
Readiness: INSUFFICIENT_EVIDENCE
Priority: CTX-STATE (UNKNOWN): Review CTX-STATE; confirm, reject, skip or leave unknown.
Context score: 100.00% | Assessable coverage: 42.22%
CTX-FRESH | NOT_APPLICABLE
This does not measure project completion or quality.
```

This demonstrates why a high score on sparse assessable inputs cannot imply
readiness or completion. The original nine findings and dimensions are retained
in the full output. Metadata timestamps naturally differ across evaluations;
finding states/scores/rank factors depend only on recorded inputs and policy.

Observed limitation: the PowerShell-captured scenario heading in summary.txt has a
console-encoding replacement glyph. The original UTF-8 scenarios-summary.txt and
application/handoff files are intact. The application project stream was fixed
to UTF-8 after the real-process test exposed the Windows default encoding.
The launcher capture is transcoded from PowerShell's UTF-16 redirection to UTF-8
for GitHub readability; its original remains in the retained verification directory.
Committed text transcripts normalize Windows CRLF and trailing whitespace for
review. Raw process captures remain untouched under build; sample-handoff.md is
the exact exported UTF-8 file.

Not verified: creating symbolic links (environment denied), actual JDK 17 runtime,
Unix, instructor hardware, hostile concurrent filesystem replacement or release
recovery/locking. Junction rejection and actual ACL denial were exercised. Source
root references are platform-native; cross-OS saved-root migration is unverified.
