# B7 classroom delivery and defense

## Runnable source and ownership

`src/breadcrumbs/Main.java` and `run.bat` build/run with an existing JDK 17+.
No package manager, Office, database, GUI framework or parser library is required.
`WorkspaceState` owns confirmed context, observations, reviews, manual captures and
declared graph revisions. The custom DLL owns native chronology. Graph, Hash and
Trie are derived structures; generated Markdown and search indexes are not new
truth stores. The four graded DSAs remain manually implemented with linked nodes,
arrays and supporting JDK utilities; built-in collection classes do not implement
their core operations. Tests may use collection utilities for fixtures.

## DSA APIs and true costs

Let n=checkpoints, V=vertices, E=edges, d=out-degree, b=buckets, h=Hash entries,
L=key/token length, C=maximum Trie sibling scan, N=Trie nodes, K=visited suggestion
nodes, P=postings. String hashing/comparison adds key-length costs where relevant.
Fixed classroom capacity bounds do not justify claiming every operation O(1).

| DSA / APIs | Time and auxiliary space |
| --- | --- |
| DLL append/prepend | O(n) uniqueness check, then O(1) link change; O(1) extra |
| DLL insertAt, findById, removeById | O(n), including positional/ID lookup and duplicate checks; O(1) extra |
| DLL traverseForward/traverseBackward | O(n) time and returned array |
| DLL getLatest/getOldest, size/isEmpty/modification | O(1) |
| DLL displayStructure | O(n) time/output |
| DLL validateIntegrity | O(n²) uniqueness plus O(n) links/cycle/count; O(1) extra |
| Graph constructor/counts/setIndexed | O(1); bounded vertex/slot storage |
| Graph contains/addWorkItem | Expected indexed O(1) lookup excluding key length, collision worst O(V); unindexed O(V). Slot insertion may resize. |
| Graph addDependency | Endpoint lookup plus adjacency duplicate scan O(d) and cycle-prevention traversal with prevalidation. Worst O(V²·L + E·L + b), O(V) traversal arrays |
| Graph removeDependency | Endpoint lookup plus O(d); O(1) extra |
| Graph breadthFirst/depthFirst/validate | Worst O(V²·L + E·L + b) with colliding ID lookups. The BFS/DFS loop itself is O(V+E), with O(V) arrays. |
| Graph hasCycle | O(V+E) time and O(V) visited flags and stack. |
| Graph displayAdjacencyList | O(V+E) time/output |
| Graph rebuild | Repeated vertex/edge additions and validation; includes per-edge cycle checks, bounded by O(V²·L + (E+1)(V²·L + E·L + b)) as a conservative collision bound. Not one linear pass. |
| Graph clear | O(V+b) bounded arrays/slot reset |
| Hash get/containsKey/put/remove | Expected O(1) bucket work excluding O(L) hashing, worst O(h) collision chain; put includes possible O(h) rehash/resize; amortized expected O(1) insertion |
| Hash size/capacity | O(1) |
| Hash bucketIndex | O(L) hashing |
| Hash clear/displayBuckets | O(b), O(b+h) respectively; display has output space |
| Hash validate | Worst O(h²) duplicate/chain consistency plus bucket/key costs |
| Trie insert/contains | O(L*C), manually linked sorted siblings; O(L) code-point array. Insertion stores at most O(L) new nodes |
| Trie suggest | O(prefix length*C + K), visit/result capped; O(L + R) code points, recursion and R returned terms |
| Trie termCount/nodeCount | O(1) |
| Trie clear | O(1) root replacement; old nodes become eligible for GC |
| Trie validate | O(N*L) path construction, O(N + L²) visited flags and retained recursive path strings |

Search exact Hash lookup then scans the full term posting chain before bounded
results resolve canonical records: expected O(L+P+R*resolution), not merely O(R).
Source resolution can scan reviews or rebuild/fall back from B5 indexes. Full
search validation retokenizes fields and scans postings, including Hash worst-case
duplicate checks; no unmeasured speedup is claimed. Generation saves validate and
serialize the complete bounded canonical state. See B1/B5/B6 execution documents
for source-bound original tests and detailed costs.

## Ten-minute classroom plan

Double-click `scripts/DSA_DEMO.bat` in File Explorer. The title identifies The Bread.
Type the displayed number and press Enter. All positions are zero-based.

| Time | What to show |
| --- | --- |
| 0:00–1:00 | Explain that Breadcrumbs helps resume a project from confirmed checkpoints and references. Open the focused DLL demo. |
| 1:00–2:00 | Choose **9**. Start with the five seeded synthetic records. Point out HEAD, TAIL, previous/next links and the null endpoints. |
| 2:00–4:00 | Use **1** to append a title/summary and note the generated ID. Use **2** with a unique ID to prepend. Use **3** with position `2` and another unique ID to insert in the middle. |
| 4:00–6:00 | **4** searches for `DEMO-000003`. **6** and **7** show opposite orders. **8** reads the tail. **5** deletes by ID. **10** shows size and **11** verifies links. |
| 6:00–7:30 | Search for `MISSING-DEMO` with **4**. Use **3** with position `99`. Explain that an invalid position leaves the list as it was and brings back the menu. |
| 7:30–9:00 | Exit with **0**. If time permits, open `run.bat` on approved disposable data to show where the real checkpoint workflow lives. |
| 9:00–10:00 | Explain the DLL trade-off. Two links support both directions and O(1) relinking after a node is found. The extra pointer costs memory, and position and ID lookup remain linear. |

**12** reloads any missing seed records. Insertion, search and removal report DLL
costs. Automatic rendering after an operation adds linear output, and the
classroom diagnostic adds O(n²) uniqueness checking. For selected operations
that only read a cached value or relink an already located node, explain those
costs apart from rendering and validation.

## Further questions on the full app

1. Open/create a project, advanced 16: create two explicitly declared work items,
   add prerequisite T-000001 -> T-000002 after preview, show adjacency/BFS/DFS and
   potential downstream impact. Try the reverse edge; cycle prevention rejects it.
   Dependencies never auto-complete/block tasks or infer causality from prose.
2. Advanced 17: fixture Aa/BB proves collision chaining, more inserts demonstrate
   resizing, then show buckets/get/remove/validate. Compare indexed/canonical
   checkpoint lookup, disable and rebuild the disposable index.
3. Daily Search or advanced 18: rebuild eligible fields; prefix `meth` shows
   deterministic Trie suggestions. Exact selection resolves a manual posting
   chain and canonical field/locator. Show stale-index rejection after a review
   or rescan, rebuild and inspect coverage. Missing hits are not source absence.

Run `scripts/test-b7.bat` for reproducible assertion/process checks. The four-profile
Main runner creates GENERAL/ACADEMIC/OPERATIONS/SOFTWARE imports, explicit graph
edges, two checkpoints across restarts, DOCX search, source drift and portable
packages. Separate recovery and B6/B5/B4 runners cover interruption, isolation,
conflicts, privacy, unsupported-body negatives and original version compatibility.

## Source and license inventory

| Component | Origin / distribution |
| --- | --- |
| Application / custom DSAs / tests / scripts | Breadcrumbs repository source; no vendored third-party application code was added in B7. No new repository-wide license is invented. |
| java.base, java.xml, ZIP, secure StAX, SHA-256, NIO, concurrency | Existing owner's JDK standard libraries. Runtime vendor licensing applies to that installed JDK; no JDK binaries are redistributed. |
| PDF/XLSX/PPTX/OCR packages | None selected, installed, downloaded at startup or included in the core. |
| Python and FFmpeg | Existing developer tools used only for evidence capture; not application dependencies and not bundled. FFmpeg build license/options remain those of the installed binary. |
| Public project template | Government of Northwest Territories Finance, project_management_plan.docx, linked in B7 final acceptance. Exact downloaded bytes and provenance recorded locally. Not copied into application source or committed. |
| Public PDF fixture | W3C dummy.pdf, provenance in B7 final acceptance. Local-only metadata-negative sample, no body-extraction claim. |
| Public DOCX comparison fixture | python-openxml/python-docx known-paragraphs fixture, retrieved locally for comparison, not shipped or used to infer broad compatibility. |
| Original terminal art | Existing accepted repository artwork; unchanged in B7. |

No external document text is executed, no Office automation is used and no
untrusted dependency manifests/install scripts are run. The public sources are
test inputs, not an endorsement or general malware-free guarantee.

## Security and portability limits

Import and rescan require local-root consent. Root/ancestor/link checks, exclusions,
bounded reads, stable fingerprints, secure ZIP/XML parsing, secret-marker screens,
neutral terminal values and create-only exports enforce distinct boundaries.
Canonical checksums are integrity checks, not authentication/encryption. Manual
SENSITIVE capture cannot flow into exportable present-context reviews through the
Inbox convenience action. No universal PII classifier is promised.

See the [local delivery report](CLASSROOM_FINALIZATION_REPORT.md) for current
Windows coverage and gaps. The build targets Java 17.
Single process per storage root; quiescent local input. No hostile-race sandbox,
power-loss durability, directory-fsync or distributed/concurrent-writer claim.
Final source/expected-observed results and remaining gates are in B7_FINAL_ACCEPTANCE.
