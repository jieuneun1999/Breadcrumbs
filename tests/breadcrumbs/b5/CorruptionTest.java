package breadcrumbs.b5;

import breadcrumbs.domain.*;
import breadcrumbs.dsa.graph.TaskDependencyGraph;
import breadcrumbs.dsa.hash.CustomHashTable;
import static breadcrumbs.b4.Checks.*;
import java.time.Instant;

/** Deliberate private-pointer corruption proves validators do not just count healthy fixtures. */
public final class CorruptionTest {
    private CorruptionTest() { }
    public static void run() throws Exception {
        var graph = new TaskDependencyGraph("W599"); Instant time = Instant.now();
        for (int i = 1; i <= 100; i++) {
            graph.addWorkItem(new WorkItem("W599", String.format(java.util.Locale.ROOT, "T-%06d", i), "Synthetic task", "", WorkItem.Status.UNKNOWN,
                    "", null, "B0-NO-SCAN", new String[0], time, time));
        }
        reject(() -> graph.addWorkItem(new WorkItem("W599", "T-000101", "Overflow", "", WorkItem.Status.UNKNOWN, "", null, "B0-NO-SCAN", new String[0], time, time)), "vertex bound");
        graph.addDependency(new Dependency("W599", "T-000001", "T-000002", "B0-NO-SCAN", "", "Explicit", time));
        var verticesField = TaskDependencyGraph.class.getDeclaredField("vertices"); verticesField.setAccessible(true);
        Object[] vertices = (Object[]) verticesField.get(graph);
        var first = vertices[0].getClass().getDeclaredField("firstOutgoing"); first.setAccessible(true);
        Object edge = first.get(vertices[0]); var next = edge.getClass().getDeclaredField("next"); next.setAccessible(true); next.set(edge, edge);
        require(!graph.validate(), "adjacency pointer cycle rejected");
        reject(() -> graph.breadthFirst("T-000001"), "invalid graph cannot produce impact");
        var hash = new CustomHashTable<String>(); hash.put("Aa", "one"); hash.put("BB", "two");
        var bucketsField = CustomHashTable.class.getDeclaredField("buckets"); bucketsField.setAccessible(true);
        Object[] buckets = (Object[]) bucketsField.get(hash); Object head = buckets[hash.bucketIndex("Aa")];
        var hashNext = head.getClass().getDeclaredField("next"); hashNext.setAccessible(true); hashNext.set(head, head);
        require(!hash.validate(), "Hash chain cycle detected");
        System.out.println("PASS: B5 bounds and private-pointer corruption checks");
    }
}
