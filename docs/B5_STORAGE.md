# Format 3 and canonical dependency history

Version1/2 encodings and semantics are unchanged. Genuine original fixtures under
tests/fixtures/b3-v1 and b4-v2 exercise read/open/explicit-upgrade preservation.
B4 v2 fixture SHA256 is 0bffba4b79af24050df7a27c3495eb92a4e2fb52ae65c26244f702926d6245bd.
New state remains v1/v2 until its first confirmed task/dependency mutation. Loading
never scans the source root or writes/upgrades storage.

Format3 starts FORMAT_VERSION=3 and retains the v2 B0/observations/reviews prefix.
Before nextCheckpoint it adds graphRevisions count. Each revision is ordered:

- graphId, graphTime, nextTask, workItems count.
- Each task: taskWorkspace, taskId, taskTitle, description, taskStatus, taskOwner,
  due (empty or ISO date), taskObservation, taskRefs count, taskRef values,
  taskCreated, taskUpdated.
- dependencies count, then edgeWorkspace, prerequisite, dependent, edgeObservation,
  edgeEvidence (empty means direct human declaration), declaration, edgeConfirmed.

Version3 checkpoint grammar adds graphRevision after confirmed. Empty means
NOT RECORDED for an older checkpoint; no graph is invented for it. Other records
reference an existing revision no later than checkpoint confirmation time. Revisions
G-000001 onward append monotonically. Every accepted mutation adds an immutable
snapshot. Task IDs T-000001 onward use persisted high-water counters; no recycling.
Task edit/deactivation is not exposed. Historical snapshots retain original task
values and dependency declarations. Current changes cannot rewrite them.

WorkspaceState owns DependencyState and its immutable revision values. Custom Graph
rebuilds from the selected snapshot and stores only canonical IDs/declaration references.
Reconstruction rejects cycles, duplicate/self/foreign/dangling edges, invalid counters,
provenance references and missing historical revisions before activation. Rejected
mutations build no new revision. Max100 revisions,100 tasks,500 edges per revision.
Existing8MiB/200000-line/field bounds apply and may be reached first. Format3 still
uses exact UTF8 Base64 named fields and checksum, detached validation, atomic single
file replacement and readback. Indexes are disposable and never persisted.

Historical handoff selects the checkpoint's graph revision; baseline-context handoff
uses the current graph. Both show revision identity, declarations, confirmation time,
accepted source evidence IDs or direct-assertion provenance and potential reachability.
Missing historical dependencies remain UNKNOWN. A graph cannot infer definite task
blockage or independently verify a user's stated completion.
