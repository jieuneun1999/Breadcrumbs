package breadcrumbs.domain;

import java.time.Instant;

/** Canonical bounded snapshot. Values are immutable; the graph is a disposable projection. */
public record GraphRevision(String id, Instant confirmedAt, int nextTaskNumber, WorkItem[] items, Dependency[] dependencies) {
    public GraphRevision {
        if (id == null || !id.matches("G-[0-9]{6}") || confirmedAt == null || nextTaskNumber < 1
                || nextTaskNumber > 1000000 || items == null || items.length > 100 || dependencies == null || dependencies.length > 500) {
            throw new IllegalArgumentException("Invalid graph revision or capacity.");
        }
        items = items.clone(); dependencies = dependencies.clone();
    }
    @Override public WorkItem[] items() { return items.clone(); }
    @Override public Dependency[] dependencies() { return dependencies.clone(); }
}
