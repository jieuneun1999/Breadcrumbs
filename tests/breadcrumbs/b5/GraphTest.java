package breadcrumbs.b5;

import breadcrumbs.b4.Checks;
import breadcrumbs.b4.WorkflowTest;
import breadcrumbs.domain.*;
import breadcrumbs.dsa.graph.TaskDependencyGraph;
import breadcrumbs.storage.*;
import breadcrumbs.handoff.HandoffService;
import java.nio.file.*;
import java.util.Arrays;
import static breadcrumbs.b4.Checks.*;

public final class GraphTest {
    private GraphTest() { }
    public static void main(String[] args) throws Exception { run(); }
    public static void run() throws Exception {
        int before = Checks.count();
        Path root = Files.createTempDirectory(Files.createDirectories(Path.of("build/b5-fixtures")), "graph-").toAbsolutePath();
        Files.writeString(root.resolve("README.md"), "# Next Steps\n- [ ] TASK-7 Imported candidate only\n");
        WorkspaceState state = WorkflowTest.imported(root, "W501");
        var domain = state.dependencies();
        require(domain.graph().vertexCount() == 0 && domain.graph().validate(), "empty graph and no automatic import");
        for (int i = 0; i < 5; i++) { domain.create("Task " + i, "Synthetic", WorkItem.Status.UNKNOWN, "", null, new String[0], true); }
        require(domain.findItem("T-000001").status() == WorkItem.Status.UNKNOWN && domain.graph().vertexCount() == 5, "canonical explicit status/vertices");
        var g = domain.graph();
        reject(() -> g.addWorkItem(domain.findItem("T-000001")), "duplicate vertex");
        reject(() -> domain.add("T-000001", "T-000001", "", "Self", true), "self edge");
        reject(() -> domain.add("T-000001", "T-999999", "", "Missing", true), "missing endpoint");
        reject(() -> domain.add("T-000001", "T-000002", "EV-FOREIGN", "Foreign evidence", true), "foreign evidence");
        String original = domain.currentId();
        reject(() -> domain.add("T-000001", "T-000002", "", "Cancelled", false), "explicit cancellation");
        require(original.equals(domain.currentId()), "cancel leaves revision unchanged");
        domain.add("T-000001", "T-000002", "", "Direct user declaration", true);
        String evidence = state.activeBaseline().claims()[0].evidenceId();
        domain.add("T-000001", "T-000003", evidence, "Explicit accepted source declaration", true);
        domain.add("T-000002", "T-000004", "", "Direct", true);
        domain.add("T-000003", "T-000004", "", "Direct", true);
        var visits = domain.graph().breadthFirst("T-000001");
        require(visits.length == 4 && visits[1].id().equals("T-000002") && visits[2].id().equals("T-000003") && visits[3].level() == 2, "diamond BFS unique count/order/level");
        var dfs = domain.graph().depthFirst("T-000001");
        require(dfs.length == 4 && dfs[1].id().equals("T-000002") && dfs[2].id().equals("T-000004"), "deterministic DFS");
        require(domain.graph().breadthFirst("T-000005").length == 1, "isolated component");
        reject(() -> domain.add("T-000004", "T-000001", "", "Cycle", true), "cycle rejection");
        reject(() -> domain.add("T-000001", "T-000002", "", "Duplicate", true), "duplicate edge rejection");
        require(domain.graph().validate() && !domain.graph().hasCycle() && domain.graph().edgeCount() == 4, "correct graph counts/invariants");
        require(domain.findItem("T-000004").status() == WorkItem.Status.UNKNOWN, "impact never changes explicit status");
        var cp1 = WorkflowTest.checkpoint(state, "Graph checkpoint", new String[0]);
        String historical = cp1.project().graphRevisionId();
        domain.remove("T-000003", "T-000004", true);
        var cp2 = WorkflowTest.checkpoint(state, "Later graph", new String[0]);
        require(!historical.equals(cp2.project().graphRevisionId()) && domain.find(historical).dependencies().length == 4, "historical dependency revision retained");
        var handoff = new HandoffService().preview(state, cp1, false).text();
        require(handoff.contains("Reported graph revision: " + historical) && handoff.contains("Potentially affected") && handoff.contains("Direct user declaration"), "historical handoff provenance and impact");
        var store = new WorkspaceStore(root.resolveSibling(root.getFileName() + "-app")); store.save(state);
        var restored = store.load("W501");
        require(restored.dependencies().graph().edgeCount() == 3 && restored.checkpoints().size() == 2, "v3 graph/checkpoint roundtrip");
        require(Arrays.equals(WorkspaceCodec.encode(state), WorkspaceCodec.encode(restored)), "exact v3 roundtrip");
        require(restored.dependencies().find(historical).dependencies().length == 4, "historical graph retained after restart");
        require(restored.dependencies().create("Next", "", WorkItem.Status.NOT_STARTED, "", null, new String[0], true).id().equals("T-000006"), "task counter after restart");
        var revision = domain.current();
        var corrupt = new Dependency("W501", "T-000004", "T-000001", state.currentObservationId(), "", "Corrupt cycle", java.time.Instant.now());
        Dependency[] edges = Arrays.copyOf(domain.find(historical).dependencies(), 5); edges[4] = corrupt;
        reject(() -> TaskDependencyGraph.rebuild("W501", new GraphRevision("G-000099", corrupt.confirmedAt(), 6, revision.items(), edges)), "preexisting cycle rejected during restore");
        reject(() -> TaskDependencyGraph.rebuild("W999", revision), "cross workspace vertex rejected");
        byte[] valid = WorkspaceCodec.encode(state);
        String body = new String(valid, java.nio.charset.StandardCharsets.UTF_8).replaceFirst("graphRevision=[^\n]*", "graphRevision=Ry05OTk5OTk=");
        body = body.substring(0, body.lastIndexOf("CHECKSUM="));
        byte[] invalid = (body + "CHECKSUM=" + WorkspaceCodec.digest(body.getBytes(java.nio.charset.StandardCharsets.UTF_8)) + "\n").getBytes(java.nio.charset.StandardCharsets.UTF_8);
        reject(() -> WorkspaceCodec.decode(invalid), "checksum-correct missing historical revision rejected");
        for (String file : new String[] {"tests/fixtures/b3-v1/imported-workspace.txt", "tests/fixtures/b4-v2/workspace.txt"}) {
            byte[] old = Files.readAllBytes(Path.of(file)); var loaded = WorkspaceCodec.decode(old);
            require(Arrays.equals(old, WorkspaceCodec.encode(loaded)), "genuine old format exact preservation");
            require(loaded.checkpoints().getLatest().project().graphRevisionId().isEmpty(), "old CP graph NOT RECORDED");
            loaded.dependencies().create("Upgrade", "", WorkItem.Status.UNKNOWN, "", null, new String[0], true);
            var upgraded = WorkspaceCodec.decode(WorkspaceCodec.encode(loaded));
            require(upgraded.checkpoints().size() == loaded.checkpoints().size() && upgraded.dependencies().graph().vertexCount() == 1, "old format upgrade preserves full state");
        }
        var projected = domain.graph(); projected.clear(); require(projected.vertexCount() == 0 && domain.graph().vertexCount() == 5, "clear/rebuild projection doesn't erase canonical records");
        System.out.println("PASS: B5A Graph/domain/storage (" + (Checks.count() - before) + " assertions)");
    }
}
