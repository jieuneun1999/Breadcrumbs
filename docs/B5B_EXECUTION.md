# B5B execution record

B5A passed its independent gate before any Hash implementation. Parent commit
5b44fbb, PR5 https://github.com/jieuneun1999/Breadcrumbs/pull/5 targets B4.
Branch feat/b5b-custom-hash-indexes targets B5A. No merges or Discord changes.
Same STANDARD single-owner packet and manual/Pstack adaptations as B5A apply.

## Design and implementation

CustomHashTable uses a manual bucket-head array and custom singly linked HashEntry
nodes. Deterministic String.hashCode xor unsigned upper16 spreading and a power-of-two
mask handle negative hashes without Math.abs. Null keys/values rejected, duplicate
keys update their value, threshold0.75 triggers manual node rehash. Default max50000
entries, max131072 buckets; overflow fails before adding. No built-in map/queue.

WorkspaceIndexes is a derived projection bound to one WorkspaceState. Immutable
checkpoint values, canonical work-item values, observation-scoped evidence, relative
paths and all reliable equal-hash candidates are indexed. Evidence/path/hash compound
keys include workspace and length-delimited observation ID. Other tables are private
per-workspace instances; workspace switches increment the epoch. No node pointers
escape the DLL. Graph has a separate derived ID-to-slot table, with explicit linear
lookup mode. Edge nodes retain integer slots, keeping traversal independent of Hash.

Canonical mutations increment the workspace epoch. DLL pointer mutations have their
own counter, detecting even changes made through the existing DLL API. Query detects
mismatch and rebuilds before returning a value. Load starts with unbuilt indexes;
first query rebuilds. Disabling indexes uses canonical lookup. Index-capacity failure
falls back to canonical queries for that epoch, preserving functionality. Hash caches
are never serialized. Validation checks table structure, canonical counts/identities
and observation references. Graph/Hash demo data stays separate from actual records.

Actual callers use indexed checkpoint selection and referenced evidence/path lookup;
graph endpoint lookup uses its slot index. Drift deliberately retains its reference
algorithm so equal-hash ambiguity remains unchanged; candidate index is queryable and
parity-tested, not falsely presented as an optimized drift implementation.

## Algorithms and cost

Hash expected get/put/remove O(1), worst O(n); insertion expected amortized O(1),
single resize O(capacity+n). Memory O(capacity+n). Full pairwise duplicate validation
worst O(n²+capacity). Diagnostics O(capacity+n), bounded rendered text. Hash is not
cryptographic; SHA256 remains separate file fingerprinting.

Graph BFS/DFS/cycle validation O(V+E), O(V) visits/queue/recursion, plus initial vertex
resolution O(1) expected with Hash or O(V) linear. Sorted edge insert/remove includes
outdegree scan; cycle rejection traverses reachable edges. Rebuilding repeatedly
checks additions and costs more than a single traversal. Immutable revision copying
adds O(V+E) per mutation and retains O(revisions*(V+E)) history within explicit bounds.
Canonical reference validation may add observation/evidence scans. Never claim all
Graph mutation is O(1). Rebuilding workspace indexes scans all canonical arrays, with
expected linear table insertion plus duplicate-hash candidate-array copying (quadratic
within a large identical-hash group). No measured speedup claim.

## Verification

Initial strict compile/full suite pass: 1017 Hash assertions,20 index parity assertions,
31 Graph/domain/storage and9 Graph CLI assertions, all original B1–B4 tests intact.
Additional Hash CLI and corruption/bounds checks added before final verification.
Final native Windows run and source-bound evidence will be recorded after completion.

Final direct JDK verification PASS on Windows10/JDK25, Java17 target. The native
junction-enabled full suite passes 5540 assertions (4450 original/B4 +1090 B5).
Separate production Main processes pass10 B5 assertions and the original55 B4
process assertions. Actual Graph and Hash CLI transcripts and 96 exercised input
hashes are retained under docs/evidence/b5. Compilation uses --release17,
-encoding UTF-8, -Xlint:all, -Werror. No test bodies from B1–B4 were changed.

Actual commands executed directly through JDK executables:

```text
javac --release 17 -encoding UTF-8 -Xlint:all -Werror -d build/app -sourcepath src src/breadcrumbs/Main.java
javac --release 17 -encoding UTF-8 -Xlint:all -Werror -d build/test -sourcepath "src;tests" tests/breadcrumbs/AllTests.java tests/breadcrumbs/b5/ProcessScenarios.java tests/breadcrumbs/b4/B4Scenarios.java
java -cp build/test breadcrumbs.AllTests
java -cp build/test breadcrumbs.b5.ProcessScenarios
java -cp build/test breadcrumbs.b4.B4Scenarios
```

The final full-suite command also supplies breadcrumbs.junction.fixture,
breadcrumbs.b3Junction and breadcrumbs.b4ExportJunction to real disposable Windows
junction paths under build/b5-final. ACL and junction rejection pass. Symlink creation
remains skipped. The shell wrapper initially stalled or returned invalid syntax; direct JDK execution
provided independent proof. After the host recovered, scripts\test-b5.bat ran
through full regression and both process runners with exit0. The failed provisional
PowerShell wrapper was removed. Existing run.bat is unchanged; no fresh external-
directory launcher test is claimed.

Self-review specification verdict PASS for both staged B5 scopes; engineering review
PASS with initial host wrapper failures repaired by the recovered execution path. No independent review claimed.
B5 is DEMO_READY on direct Windows/JDK invocation. B6 can use the reusable custom
Hash API and stable observation/evidence identities; no B6 implementation has begun.

Task shape follows two independently gated units. Blocking B5A gate passed first;
single writer owns integration; no unapproved agents/worktrees/global changes.
Supporting source review caught repeated per-vertex allocation in traversal; direct
linked iteration and bounded DFS now preserve O(V+E) traversal. Pointer-corruption
tests validate cycle protection. Review also tightened DLL-index invalidation and
historical-vs-current handoff wording. Existing B1–B4 test bodies unchanged.

Remaining limits: plaintext single-process storage and basic atomic replacement,
no task deletion/edit/status-update UI, no B6 Trie/postings/tokenizer, advanced parsers,
AI/Git/network/source execution or B7 recovery. Instructor hardware, actual JDK17
runtime, Unix and symlink creation remain unverified. Native junctions/ACL are checked
by the retained Windows harness. No external service needed to run the application.
