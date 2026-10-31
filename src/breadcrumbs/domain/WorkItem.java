package breadcrumbs.domain;

import java.time.Instant;
import java.time.LocalDate;

/** Immutable explicit present assessment. A source reference does not independently verify work. */
public record WorkItem(String workspaceId, String id, String title, String description, Status status,
                       String owner, LocalDate dueDate, String observationId, String[] evidenceIds,
                       Instant createdAt, Instant updatedAt) {
    public enum Status { UNKNOWN, NOT_STARTED, IN_PROGRESS, BLOCKED, COMPLETED }
    public WorkItem {
        if (workspaceId == null || !workspaceId.matches("W(?:[0-9]{3,9}|-[a-f0-9]{32})")
                || id == null || !id.matches("T-[0-9]{6}") || id.equals("T-000000")
                || title == null || title.isBlank() || title.length() > 120 || status == null
                || observationId == null || evidenceIds == null || evidenceIds.length > 2000
                || createdAt == null || updatedAt == null || updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("Invalid work-item identity, fields or confirmation time.");
        }
        title = ProjectContext.note(title); description = ProjectContext.note(description); owner = ProjectContext.note(owner);
        evidenceIds = evidenceIds.clone();
        for (int i = 0; i < evidenceIds.length; i++) {
            if (evidenceIds[i] == null || !evidenceIds[i].matches("EV-[A-Za-z0-9-]{1,80}")) { throw new IllegalArgumentException("Invalid evidence ID."); }
            for (int j = 0; j < i; j++) { if (evidenceIds[i].equals(evidenceIds[j])) { throw new IllegalArgumentException("Duplicate evidence."); } }
        }
    }
    @Override public String[] evidenceIds() { return evidenceIds.clone(); }
}
