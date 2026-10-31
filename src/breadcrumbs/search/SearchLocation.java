package breadcrumbs.search;

import java.time.Instant;

/** Scoped reference only. A posting never owns a canonical text value. */
public record SearchLocation(String workspaceId, Kind kind, String recordId, String field,
        String observationId, String locator, String truth, String review,
        String sensitivity, Instant recordedAt, boolean historical) {
    public enum Kind { EVIDENCE, CHECKPOINT, WORK_ITEM }
}
