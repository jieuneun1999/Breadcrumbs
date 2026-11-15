package breadcrumbs.b5;

import breadcrumbs.b4.*;
import breadcrumbs.domain.*;
import breadcrumbs.continuity.*;
import breadcrumbs.inspector.*;
import breadcrumbs.storage.*;
import java.nio.file.*;
import static breadcrumbs.b4.Checks.*;

public final class IndexTest {
    private IndexTest() { }
    public static void run() throws Exception {
        int initial = Checks.count();
        Path source = Files.createTempDirectory(Files.createDirectories(Path.of("build/b5-fixtures")), "indexes-").toAbsolutePath();
        Files.writeString(source.resolve("README.md"), "# Purpose\nSynthetic project.\n# Next Steps\nReview.\n");
        Files.writeString(source.resolve("copy.md"), Files.readString(source.resolve("README.md")));
        var state = WorkflowTest.imported(source, "W503");
        String observation = state.currentObservationId(), evidenceId = state.activeBaseline().claims()[0].evidenceId();
        WorkflowTest.checkpoint(state, "First", new String[] {evidenceId});
        var indexes = state.indexes(); indexes.rebuild(); require(indexes.validate(), "canonical counts/references validate");
        for (boolean enabled : new boolean[] {true, false}) {
            indexes.setEnabled(enabled);
            require(indexes.checkpoint("CP-000001") == state.checkpoints().findById("CP-000001"), "checkpoint same canonical immutable value");
            require(indexes.evidence(observation, evidenceId).equals(state.evidence(observation, evidenceId)), "evidence same original observation");
            require(indexes.path(observation, "README.md").relativePath().equals("README.md"), "path scoped snapshot");
            String hash = indexes.path(observation, "README.md").sha256();
            require(indexes.hashCandidates(observation, hash).length == 2, "all equal-hash rename candidates retained");
        }
        indexes.setEnabled(true); indexes.rebuild();
        state.dependencies().create("A", "", WorkItem.Status.UNKNOWN, "", null, new String[0], true);
        require(indexes.stale() && indexes.workItem("T-000001") == state.dependencies().findItem("T-000001"), "task mutation invalidates and rebuilds");
        state.dependencies().create("B", "", WorkItem.Status.UNKNOWN, "", null, new String[0], true);
        state.dependencies().add("T-000001", "T-000002", "", "Explicit", true);
        var graph = state.dependencies().graph();
        String indexedTraversal = graph.displayAdjacencyList(); graph.setIndexed(false);
        require(graph.displayAdjacencyList().equals(indexedTraversal) && graph.breadthFirst("T-000001").length == 2, "graph same without Hash");
        WorkflowTest.checkpoint(state, "Second", new String[0]);
        require(indexes.stale() && indexes.checkpoint("CP-000002") != null, "checkpoint mutation rebuild");
        Files.move(source.resolve("README.md"), source.resolve("renamed.md")); Files.move(source.resolve("copy.md"), source.resolve("renamed-copy.md"));
        new ObservationService().scan(state, true, n -> true);
        require(indexes.stale() && indexes.path(state.currentObservationId(), "renamed.md") != null, "scan invalidation");
        var drift = new DriftEngine().compare(state.activeBaseline().snapshot(), state.currentObservation());
        for (var change : drift.changes()) { require(change.status() == DriftEngine.Status.AMBIGUOUS_RENAME, "rename ambiguity preserved with Hash"); }
        String order = "";
        for (boolean enabled : new boolean[] {true, false}) {
            indexes.setEnabled(enabled); String current = "";
            for (var rank : new CheckpointRetrieval().rank(state, new String[] {evidenceId})) { current += rank.checkpoint().checkpointId() + "=" + rank.score() + ";"; }
            if (order.isEmpty()) { order = current; } else { require(order.equals(current), "ranking/freshness identical disabled"); }
        }
        indexes.setEnabled(true); indexes.rebuild();
        state.addReview(new ContextReview(state.currentObservationId(), "", EvidenceClaim.Field.OBJECTIVE, "", "Current", EvidenceClaim.ReviewDisposition.CONFIRMED, java.time.Instant.now()));
        require(indexes.stale() && indexes.validate(), "review invalidation/rebuild");
        var store = new WorkspaceStore(source.resolveSibling(source.getFileName() + "-storage")); store.save(state);
        var loaded = store.load("W503"); require(loaded.indexes().stale() && loaded.indexes().validate(), "restart rebuild indexes");
        require(loaded.indexes().checkpoint("CP-000001").project().graphRevisionId().isEmpty(), "historical checkpoint never changed by indexes");
        Path foreign = Files.createDirectory(source.resolveSibling(source.getFileName() + "-foreign"));
        var other = WorkflowTest.imported(foreign, "W504");
        state.replaceWith(other);
        require(indexes.checkpoint("CP-000001") == null && indexes.evidence(observation, evidenceId) == null && indexes.workItem("T-000001") == null, "switch cannot return previous workspace values");
        System.out.println("PASS: B5B indexes and disabled parity (" + (Checks.count() - initial) + " assertions)");
    }
}
