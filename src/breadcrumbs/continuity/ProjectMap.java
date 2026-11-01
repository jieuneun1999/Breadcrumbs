package breadcrumbs.continuity;

import breadcrumbs.domain.EvidenceClaim;
import breadcrumbs.domain.ProjectProfile;
import java.time.Instant;

public record ProjectMap(String workspaceId, String project, ProjectProfile profile, boolean draft,
                         boolean checkedThisSession, String coverage, Bucket[] buckets) {
    public enum Section {
        PURPOSE("Purpose", "Project goals and planned deliverables."),
        CURRENT_STATE("Current state", "Documented status and current human assessments."),
        HISTORY("Progress & history", "The starting observation and recorded updates, not inferred earlier work."),
        TASKS("Tasks & next actions", "Tasks mentioned in documents are separate from tasks tracked by Breadcrumbs."),
        DECISIONS("Decisions", "Reported decisions and explicitly recorded decisions retain their origins."),
        BLOCKERS("Blockers & dependencies", "Reported obstacles and explicitly declared prerequisite relationships."),
        SOURCES("Evidence & sources", "Saved source inventory and project notes; no files are moved or copied."),
        UNKNOWNS("Unknowns & conflicts", "Missing details, competing statements, coverage limits and stale references.");
        private final String title, explanation;
        Section(String title, String explanation) { this.title = title; this.explanation = explanation; }
        public String title() { return title; }
        public String explanation() { return explanation; }
    }
    public enum Kind { SOURCE, STARTING_CONTEXT, ASSESSMENT, PROPOSED_ASSESSMENT, HISTORY, MANUAL, TASK, DEPENDENCY, FILE, FINDING, CONFLICT }
    public enum SourceGroup { PURPOSE, CURRENT_STATE, TASK_LIST, DECISION, HANDOFF, INSTRUCTIONS, OTHER, LIMITED }
    public record Link(String workspaceId, String recordId, String observationId, String checkpointId,
                       String path, String locator, Instant time, String fingerprint) { }
    public record Item(Section section, String title, String value, Kind kind, EvidenceClaim.Field field,
                       EvidenceClaim.ReviewDisposition review, String note, boolean historical,
                       boolean withheld, SourceGroup sourceGroup, Link link) { }
    public record Bucket(Section section, int total, Item[] items) {
        public Bucket { items = items.clone(); }
        @Override public Item[] items() { return items.clone(); }
    }
    public ProjectMap { buckets = buckets.clone(); }
    @Override public Bucket[] buckets() { return buckets.clone(); }
    public Bucket bucket(Section section) { return buckets[section.ordinal()]; }
}
