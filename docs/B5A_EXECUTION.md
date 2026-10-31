# B5A execution packet

Owner's B5 brief authorizes B5A then B5B, scoped commits, pushes and two stacked
PRs. No merge, Discord, deployment, source execution/writes or global changes.
STANDARD local synthetic work, one owner/writer, no worktrees/delegation. Six
repairs per gate, two per hypothesis. Preserve frozen specs and accepted branding.

Baseline clean B4 99719843e547430de6605bc4bf48f53d8eb83afc; live PRs 1–4 open
with expected stack, main d1f13c2. Read relevant Manual273–335, Workflow236–292,
Routing68–115, Craft66–101, project README/guide/master domain/Graph/Hash,
first-import/storage contracts and B4 checkpoint. Pstack 1.16.0 poteto-mode and
feature playbook read in dedicated calls; previously loaded how/architect applied
directly under Craft's single-owner adaptation. No independent review claimed.

Throughput checkpoint: blocking baseline/regressions; independent workstreams n/a
because persistence and canonical ownership are coupled; shared mutable state only
WorkspaceState; smallest units Graph/domain/schema/CLI/tests then independent Hash
stage after a recorded passing gate. No installed project verification skill.

Design alternatives: reject a mutable graph-owned task store and companion history
maps. Choose bounded immutable revision snapshots containing canonical work-item
values and declared edge values, owned by WorkspaceState. Graph vertices contain
only IDs and linked adjacency nodes with integer target slots. Checkpoints add an
optional graphRevisionId; old constructors mean NOT RECORDED. Max 100 tasks, 500
edges, 100 revisions. No task deletion/status-update UI in this batch.

Format 3 is documented before implementation: retain the complete v2 prefix,
then a graph revision count before nextCheckpoint. Each revision stores its ID,
confirmation time, next-task counter, counted WorkItems and counted Dependencies.
Each v3 checkpoint appends graphRevisionId after its confirmed field. Existing
v1/v2 grammar stays exact. All revisions reconstruct/validate before activation.
No upgrade on read; the first confirmed graph mutation opts into v3 on Save.

Plan: baseline -> manual graph -> canonical revision mutations -> v3 codec ->
CLI/handoff -> graph tests/full regression/runtime -> B5A commit/PR -> B5B.
Acceptance and observed commands/results will be appended after execution.

## B5A acceptance gate

PASS on Windows/JDK25, Java17 target. Ran strict javac AllTests followed by
`java -cp build/test breadcrumbs.AllTests`. Original suites pass unchanged;
B5A 31 domain/Graph/storage assertions plus 9 actual CLI/restart assertions pass.
CLI transcript build/b5-fixtures/cli-14841507436210444796/graph-cli.txt.
Genuine v2 protected before edits, SHA256
0bffba4b79af24050df7a27c3495eb92a4e2fb52ae65c26244f702926d6245bd.
Existing genuine v1 unchanged. No automatic task import, fabricated dependencies,
status propagation or source writes. Format3 roundtrip/historical revisions pass.
Optional junction fixtures not supplied in this gate; symlink creation still denied.

Graph uses custom vertices and sorted linked EdgeNodes. BFS uses a manually
managed array queue; DFS/cycle detection use bounded recursion/colors. Traversal
O(V+E) plus O(V) starting lookup; validation O(V+E); add includes linear endpoint
lookup, outgoing scan and cycle reachability; rebuild includes repeated insertion
checks. Canonical mutations copy bounded revision arrays; max100 revisions retains
historical task/edge values. Graph owns no statuses. Self-review PASS; no independent
review claimed. B5B may now begin from this passing B5A checkpoint.
