package breadcrumbs.dsa.graph;

import breadcrumbs.domain.*;
import java.util.Arrays;

/** Manually linked directed adjacency lists with bounded array queue/stack and visit states. */
public final class TaskDependencyGraph {
    private final String workspaceId;
    private final GraphVertex[] vertices = new GraphVertex[100];
    private int size;
    private int edges;
    public record Visit(String id, int level, String via, Dependency provenance) { }
    public TaskDependencyGraph(String workspaceId) { this.workspaceId = workspaceId; }
    public int vertexCount() { return size; }
    public int edgeCount() { return edges; }
    public void clear() { Arrays.fill(vertices, null); size = 0; edges = 0; }
    public boolean contains(String id) { return locate(id) >= 0; }
    private int locate(String id) {
        for (int i = 0; i < size; i++) { if (vertices[i].id.equals(id)) { return i; } }
        return -1;
    }
    public void addWorkItem(WorkItem item) {
        if (!workspaceId.equals(item.workspaceId()) || contains(item.id()) || size == vertices.length) {
            throw new IllegalArgumentException("Duplicate/foreign vertex or graph full.");
        }
        if (size > 0 && vertices[size - 1].id.compareTo(item.id()) >= 0) { throw new IllegalArgumentException("Canonical IDs must be in ascending order."); }
        vertices[size++] = new GraphVertex(item.id());
    }
    public void addDependency(Dependency dependency) {
        int from = locate(dependency.prerequisite()), to = locate(dependency.dependent());
        if (!workspaceId.equals(dependency.workspaceId()) || from < 0 || to < 0 || from == to || edges == 500) {
            throw new IllegalArgumentException("Missing/foreign/self dependency or edge capacity reached.");
        }
        EdgeNode previous = null, current = vertices[from].firstOutgoing;
        while (current != null && current.target < to) { previous = current; current = current.next; }
        if (current != null && current.target == to) { throw new IllegalArgumentException("Duplicate dependency."); }
        for (Visit visit : breadthFirst(dependency.dependent())) {
            if (visit.id().equals(dependency.prerequisite())) { throw new IllegalArgumentException("Dependency would create a cycle."); }
        }
        EdgeNode added = new EdgeNode(to, dependency, current);
        if (previous == null) { vertices[from].firstOutgoing = added; } else { previous.next = added; }
        edges++;
    }
    public boolean removeDependency(String prerequisite, String dependent) {
        int from = locate(prerequisite), to = locate(dependent);
        if (from < 0 || to < 0) { throw new IllegalArgumentException("Missing vertex."); }
        EdgeNode previous = null, current = vertices[from].firstOutgoing;
        while (current != null) {
            if (current.target == to) {
                if (previous == null) { vertices[from].firstOutgoing = current.next; } else { previous.next = current.next; }
                edges--; return true;
            }
            previous = current; current = current.next;
        }
        return false;
    }
    public Visit[] breadthFirst(String id) { return traverse(id, false); }
    public Visit[] depthFirst(String id) { return traverse(id, true); }
    private Visit[] traverse(String id, boolean depthFirst) {
        int start = locate(id);
        if (start < 0 || !validate()) { throw new IllegalArgumentException("Missing start or invalid graph."); }
        boolean[] visited = new boolean[size]; int[] queue = new int[size]; Visit[] result = new Visit[size];
        int[] levels = new int[size], parents = new int[size]; Dependency[] provenance = new Dependency[size];
        Arrays.fill(parents, -1);
        if (depthFirst) {
            int[] count = {0};
            depth(start, 0, "", null, visited, result, count);
            return Arrays.copyOf(result, count[0]);
        }
        int head = 0, count = 0, end = 1; queue[0] = start; visited[start] = true;
        while (depthFirst ? end > 0 : head < end) {
            int vertex = depthFirst ? queue[--end] : queue[head++];
            result[count++] = new Visit(vertices[vertex].id, levels[vertex], parents[vertex] < 0 ? "" : vertices[parents[vertex]].id, provenance[vertex]);
            for (EdgeNode edge = vertices[vertex].firstOutgoing; edge != null; edge = edge.next) {
                int target = edge.target;
                if (!visited[target]) {
                    visited[target] = true; levels[target] = levels[vertex] + 1; parents[target] = vertex;
                    provenance[target] = edge.declaration; queue[end++] = target;
                }
            }
        }
        return Arrays.copyOf(result, count);
    }
    private void depth(int vertex, int level, String via, Dependency source, boolean[] visited, Visit[] result, int[] count) {
        visited[vertex] = true;
        result[count[0]++] = new Visit(vertices[vertex].id, level, via, source);
        for (EdgeNode edge = vertices[vertex].firstOutgoing; edge != null; edge = edge.next) {
            if (!visited[edge.target]) { depth(edge.target, level + 1, vertices[vertex].id, edge.declaration, visited, result, count); }
        }
    }
    public boolean hasCycle() {
        int[] colors = new int[size];
        for (int i = 0; i < size; i++) { if (colors[i] == 0 && cycle(i, colors)) { return true; } }
        return false;
    }
    private boolean cycle(int vertex, int[] colors) {
        colors[vertex] = 1;
        for (EdgeNode edge = vertices[vertex].firstOutgoing; edge != null; edge = edge.next) {
            if (colors[edge.target] == 1 || colors[edge.target] == 0 && cycle(edge.target, colors)) { return true; }
        }
        colors[vertex] = 2; return false;
    }
    public boolean validate() {
        int counted = 0;
        for (int i = 0; i < size; i++) {
            int last = -1, chain = 0;
            for (EdgeNode edge = vertices[i].firstOutgoing; edge != null; edge = edge.next) {
                if (++chain > 500 || edge.target <= last || edge.target >= size || edge.target == i
                        || !workspaceId.equals(edge.declaration.workspaceId())
                        || !vertices[i].id.equals(edge.declaration.prerequisite())
                        || !vertices[edge.target].id.equals(edge.declaration.dependent())) { return false; }
                last = edge.target; counted++;
            }
        }
        return counted == edges && !hasCycle();
    }
    public String displayAdjacencyList() {
        if (!validate()) { throw new IllegalArgumentException("Invalid graph."); }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < size; i++) {
            out.append(vertices[i].id).append(" ->");
            for (EdgeNode edge = vertices[i].firstOutgoing; edge != null; edge = edge.next) { out.append(' ').append(vertices[edge.target].id); }
            out.append('\n');
        }
        return out.toString();
    }
    public static TaskDependencyGraph rebuild(String workspaceId, GraphRevision revision) {
        TaskDependencyGraph graph = new TaskDependencyGraph(workspaceId);
        for (WorkItem item : revision.items()) { graph.addWorkItem(item); }
        for (Dependency dependency : revision.dependencies()) { graph.addDependency(dependency); }
        return graph;
    }
}
