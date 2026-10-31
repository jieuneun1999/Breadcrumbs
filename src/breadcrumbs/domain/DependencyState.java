package breadcrumbs.domain;

import breadcrumbs.dsa.graph.TaskDependencyGraph;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;

/** Workspace-owned canonical revision history. Rebuild before accepting any mutation. */
public final class DependencyState {
    private final WorkspaceState workspace;
    private GraphRevision[] revisions = new GraphRevision[0];
    public DependencyState(WorkspaceState workspace) { this.workspace = workspace; }
    public GraphRevision[] revisions() { return revisions.clone(); }
    public GraphRevision current() { return revisions.length == 0 ? null : revisions[revisions.length - 1]; }
    public String currentId() { return current() == null ? "" : current().id(); }
    public GraphRevision find(String id) {
        for (GraphRevision revision : revisions) { if (revision.id().equals(id)) { return revision; } }
        return null;
    }
    public WorkItem findItem(String id) {
        if (current() != null) { for (WorkItem item : current().items()) { if (item.id().equals(id)) { return item; } } }
        return null;
    }
    private String workspaceId() {
        if (workspace.activeBaseline() == null) { throw new IllegalArgumentException("Active workspace required."); }
        return workspace.activeBaseline().workspaceId();
    }
    public WorkItem create(String title, String description, WorkItem.Status status, String owner,
                           java.time.LocalDate due, String[] refs, boolean approved) {
        if (!approved) { throw new IllegalArgumentException("Work item cancelled."); }
        WorkItem[] items = current() == null ? new WorkItem[0] : current().items();
        int number = current() == null ? 1 : current().nextTaskNumber();
        Instant time = now();
        WorkItem item = new WorkItem(workspaceId(), String.format(Locale.ROOT, "T-%06d", number), title,
                description, status, owner, due, workspace.currentObservationId(), refs, time, time);
        WorkItem[] added = Arrays.copyOf(items, items.length + 1); added[items.length] = item;
        restore(new GraphRevision(nextId(), time, number + 1, added, current() == null ? new Dependency[0] : current().dependencies()));
        return item;
    }
    public void add(String prerequisite, String dependent, String evidenceId, String declaration, boolean approved) {
        if (!approved || current() == null) { throw new IllegalArgumentException("Dependency cancelled or no canonical tasks."); }
        Dependency edge = new Dependency(workspaceId(), prerequisite, dependent, workspace.currentObservationId(), evidenceId, declaration, now());
        TaskDependencyGraph projection = graph(); projection.addDependency(edge);
        Dependency[] old = current().dependencies(), added = Arrays.copyOf(old, old.length + 1); added[old.length] = edge;
        restore(new GraphRevision(nextId(), edge.confirmedAt(), current().nextTaskNumber(), current().items(), added));
    }
    public void remove(String prerequisite, String dependent, boolean approved) {
        if (!approved || current() == null) { throw new IllegalArgumentException("Removal cancelled."); }
        TaskDependencyGraph projection = graph();
        if (!projection.removeDependency(prerequisite, dependent)) { throw new IllegalArgumentException("Dependency not found."); }
        Dependency[] old = current().dependencies(), kept = new Dependency[old.length - 1]; int count = 0;
        for (Dependency edge : old) { if (!(edge.prerequisite().equals(prerequisite) && edge.dependent().equals(dependent))) { kept[count++] = edge; } }
        restore(new GraphRevision(nextId(), now(), current().nextTaskNumber(), current().items(), kept));
    }
    public TaskDependencyGraph graph() {
        return current() == null ? new TaskDependencyGraph(workspaceId()) : TaskDependencyGraph.rebuild(workspaceId(), current());
    }
    private String nextId() { return String.format(Locale.ROOT, "G-%06d", revisions.length + 1); }
    private Instant now() {
        Instant time = Instant.now();
        if (current() != null && time.isBefore(current().confirmedAt())) { time = current().confirmedAt(); }
        if (time.isBefore(workspace.activeBaseline().confirmedAt())) { time = workspace.activeBaseline().confirmedAt(); }
        if (time.isBefore(workspace.currentObservation().observedAt())) { time = workspace.currentObservation().observedAt(); }
        return time;
    }
    public void restore(GraphRevision revision) {
        if (revisions.length >= 100 || !revision.id().equals(nextId())
                || revision.confirmedAt().isBefore(workspace.activeBaseline().confirmedAt())
                || current() != null && revision.confirmedAt().isBefore(current().confirmedAt())) { throw new IllegalArgumentException("Invalid graph revision sequence/time or capacity."); }
        int number = 0;
        for (WorkItem item : revision.items()) {
            if (!item.workspaceId().equals(workspaceId()) || item.updatedAt().isAfter(revision.confirmedAt())
                    || item.createdAt().isBefore(workspace.activeBaseline().confirmedAt())) { throw new IllegalArgumentException("Foreign task or invalid time."); }
            int value = Integer.parseInt(item.id().substring(2));
            if (value <= number) { throw new IllegalArgumentException("Task IDs must be ordered and unique."); } number = value;
            references(item.observationId(), item.evidenceIds(), item.createdAt());
        }
        if (revision.nextTaskNumber() <= number) { throw new IllegalArgumentException("Invalid task high-water counter."); }
        for (Dependency edge : revision.dependencies()) {
            if (edge.confirmedAt().isAfter(revision.confirmedAt()) || edge.confirmedAt().isBefore(workspace.activeBaseline().confirmedAt())) { throw new IllegalArgumentException("Invalid edge confirmation time."); }
            references(edge.observationId(), edge.evidenceId().isEmpty() ? new String[0] : new String[] {edge.evidenceId()}, edge.confirmedAt());
        }
        TaskDependencyGraph.rebuild(workspaceId(), revision);
        if (current() != null) {
            if (revision.nextTaskNumber() < current().nextTaskNumber() || revision.items().length < current().items().length) { throw new IllegalArgumentException("Historical tasks/counters cannot disappear."); }
            for (int i = 0; i < current().items().length; i++) {
                WorkItem a = current().items()[i], b = revision.items()[i];
                if (!a.id().equals(b.id()) || !a.createdAt().equals(b.createdAt()) || !a.title().equals(b.title())
                        || a.status() != b.status() || !a.description().equals(b.description()) || !a.owner().equals(b.owner())
                        || !java.util.Objects.equals(a.dueDate(), b.dueDate()) || !a.observationId().equals(b.observationId())
                        || !Arrays.equals(a.evidenceIds(), b.evidenceIds()) || !a.updatedAt().equals(b.updatedAt())) {
                    throw new IllegalArgumentException("Existing work items are immutable in B5.");
                }
            }
        }
        revisions = Arrays.copyOf(revisions, revisions.length + 1); revisions[revisions.length - 1] = revision;
        workspace.enableVersionTwo();
    }
    private void references(String observation, String[] refs, Instant time) {
        var scan = workspace.observation(observation);
        if (scan == null || scan.observedAt().isAfter(time)) { throw new IllegalArgumentException("Unresolved task/dependency observation."); }
        for (String ref : refs) { if (workspace.evidence(observation, ref) == null) { throw new IllegalArgumentException("Unresolved task/dependency evidence."); } }
    }
    public void copyFrom(DependencyState source) { for (GraphRevision revision : source.revisions) { restore(revision); } }
}
