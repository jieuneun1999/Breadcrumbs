# B6 captured-context search reference

## Ownership and use

Project option **18** opens one lexical-search feature. Exact terms use the existing
B5 `CustomHashTable<PostingList>`. Prefix suggestions use `PrefixTrie`, whose
code-point child and sibling links are manually maintained in sorted order.
`PostingNode` chains reference slots in the candidate's scoped location array.
Each location contains workspace, record type and ID, field, observation, locator,
truth/review state, INTERNAL classification, timestamp and historical flag. Every
chain belongs to exactly one immutable index revision. No posting stores text.

`WorkspaceState` still owns B0, later observations, reviews, checkpoints and task
revisions. The DLL owns active chronology. The Graph owns declared relationships.
Search never creates facts, graph edges, checkpoints or reviews from word matches.
Only B5 `WorkspaceIndexes` resolves record identifiers. If B5 indexes are disabled
or over capacity, their existing canonical lookup path still resolves records.

The menu supports exact terms, numbered prefix suggestions and selection, coverage,
explicit rebuild, DSA health/demo, session source policy and enable/disable.
Each result shows the actual ID, field, source locator, original truth state,
effective review, observation, timestamp and occurrence count. Results have no
default snippet. Selecting a record and typing `yes` shows a bounded confirmed
field. Pending source fields remain withheld even with source-term opt-in.
Output wraps to the supplied terminal width, with a minimum of 28 code points.
The existing startup, loaf, wordmark and color theme are unchanged.

## Eligible fields and privacy

Default policy indexes safe current-observation evidence only after confirmation.
The latest matching B4 source review overrides the B0 disposition for eligibility,
without changing the original source verification. An explicit session-only opt-in
adds other safe captured, non-rejected evidence. Workspace creation/open/switch
resets this opt-in and creates a new disposable search object.

Evidence fields come from actual captured `EvidenceClaim.literalValue`, with their
exact configured field names. `OWNER` is always excluded. Source-reported claims
also require a matching SUPPORTED TXT, MARKDOWN or CSV snapshot entry, matching
fingerprint and a positive captured line. User-supplied evidence has its own origin.
Instructions, excluded path components, unreadable entries, secret markers,
malformed Unicode, control/format characters and unsafe locators are excluded.
Secret-marker screening runs on original text and its NFKC-normalized form.
These are conservative marker rules, not a general personal-data classifier.

Native active DLL checkpoint fields are TITLE, SUMMARY, OBJECTIVE, NEXT_ACTIONS,
DECISIONS and BLOCKERS. Each is explicitly **historical**, with its confirmation
time and recorded observation. Current Graph work-item fields are TITLE,
DESCRIPTION and STATUS. They remain user assessments. UNKNOWN placeholder fields
are omitted. Owner names, arbitrary raw bodies, filenames as terms and old source
observations are not indexed. Historical source search is not offered.

Search does not reread the source root, follow imported instructions, run processes,
make network calls or write imported files. Live source freshness remains UNKNOWN
until a separately consented scan. An on-disk change alone cannot be detected by
a saved-state query. After that scan, old source postings cannot pass revision checks.

## Normalization and bounds

Tokenizer `NFKC-ROOT-1` applies Java NFKC normalization and Locale.ROOT lowercase.
Tokens contain Unicode letters/digits with combining marks attached after a token
starts. Punctuation separates tokens. Queries require one complete normalized
lexical token or prefix. There is no stemming, fuzzy matching, phrase search,
synonym expansion, frequency ranking or language-aware segmentation. Trie edges
are Unicode code points. Prefix order is ascending code-point lexicographic order,
which is deterministic rather than locale-specific alphabetical collation.

`SEARCH-1` and `CAPTURED-1` select the current query/eligibility contract.

| Resource | Default | Hard constructor ceiling |
| --- | ---: | ---: |
| Unique terms | 10,000 | 50,000, B5 Hash limit |
| Term length in code points | 64 | 128 |
| Raw field plus normalized-token UTF-16 character budget | 1,000,000 | 4,000,000 |
| Tokens per field | 2,000 | 10,000 |
| Posting nodes | 50,000 | 100,000 |
| Posting nodes per term | 2,000 | Total posting cap |
| Suggestions | 10 | 50 |
| Trie query visits, including prefix sibling scans | 5,000 | 200,000 |
| Exact results | 20 | 100 |
| Snippet UTF-16 units before ellipsis | 160 | 400 |
| Indexed fields | 10,000 | 20,000 |
| Trie nodes including root | 200,000 | 500,000 |

Normalization is separately checked against the character budget before token
allocation. Repeated tokens in the same field increment one posting's occurrence
count. Field-level line/section or CSV-column locators are retained. Word positions
are never fabricated. Snippet truncation preserves surrogate-pair boundaries.
The scan/storage bounds can be reached before these search bounds.

## Revisions and atomic publication

A `ContextSearchIndex` is bound to one WorkspaceState object. Its revision contains
workspace ID, current observation, workspace epoch, DLL modification counter,
current graph revision and the entire immutable SearchPolicy, including versions
and every bound. No additional canonical mutation counter is introduced.

Builds create detached Trie, dictionary, posting chains and location arrays.
Cancellation is checked before building, per field/token and during validation.
Validation checks Trie structure/cycles, dictionary agreement, posting ordering,
counts, exact occurrence counts, eligibility and canonical references. Publication
also verifies that the requested revision still matches. SearchPolicy supports
version changes as invalidating input. Unknown tokenizer/content versions cannot
publish a candidate.

| Status | Meaning and available action |
| --- | --- |
| READY | Complete projection of eligible captured fields at the recorded revision |
| INDEX_OUTDATED | Canonical state/policy changed, explicit invalidation, or failed reference validation. Rebuild. |
| INDEX_UNAVAILABLE | No complete index exists. Rebuild with supported policy and bounds. |
| DISABLED | Search deliberately disabled. Enable and rebuild if needed. |

A failed/cancelled build keeps a prior candidate only through its existing revision
gate. A valid previous candidate remains usable. A stale candidate produces zero
hits and no suggestions. There is no separate linear search engine. The CLI offers
rebuild rather than claiming incomplete partial-index results are exhaustive.
Suggestion result/visit caps and exact result caps are explicitly reported.
No Trie nodes, caches or postings are serialized. Versions 1, 2 and 3 remain exact.

## Coverage and complexity

Coverage separates discovered entries, fingerprints, eligible captured fields,
policy-excluded fields, metadata-only files, skipped/sensitive files, unsupported
entries, failed/unstable extraction, scan completeness and index state. Metadata-only
counts exclude merely skipped excluded files. Supported TXT/MD/CSV extraction
captures configured fields, not every paragraph. PDF, DOCX, XLSX, PPTX, images,
Java, JSON, XML and YAML bodies are not searched in the current implementation.
An empty result proves only absence from eligible indexed captured fields.

Let L be token code-point length, C the maximum sibling scan, U unique terms,
N Trie nodes, P postings, F fields, K bounded visited query nodes and R returned hits.
Trie insertion/contains costs O(L*C). Suggestions cost O(prefix-length*C + K),
subject to a combined visit budget. Structural validation costs O(N*L) with path
construction, O(N) visited storage and O(L) bounded recursive depth. Exact Hash
lookup is expected O(L), including hashing the String, worst O(L*U) under collisions.
Chain validation scans the term's postings before bounded results are resolved.
Queries therefore cost that chain length plus R canonical resolutions, not O(R)
alone. A result's field is checked again before display.

Build tokenization is bounded by the character/token budgets. Each insertion adds
Trie sibling scans and expected Hash insertion, with resizing and collision costs.
Canonical source resolution includes review scans, B5 index construction when
needed, and canonical fallback scans when indexes are disabled. Full validation
retokenizes each field and scans its term chains to verify exact occurrences.
Worst-case costs include O(U²) B5 Hash duplicate validation, O(tokens*postings-per-
term) occurrence validation and repeated review/canonical lookups. It is not a
strictly linear build. Memory is O(N+U+P+F) plus bounded token/validation arrays;
an old and candidate index may coexist during rebuild. No speedup was measured.

The format matrix and future real-document gate are in
[Document compatibility and real-file test plan](DOCUMENT_COMPATIBILITY_AND_REAL_FILE_TEST_PLAN.md).
