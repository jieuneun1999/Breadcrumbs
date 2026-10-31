package breadcrumbs.domain;

import java.time.Instant;

/** A present assessment. Source claims and previously saved checkpoints are never rewritten. */
public record ContextReview(String observationId, String evidenceId, EvidenceClaim.Field field,
                            String entityId, String value, EvidenceClaim.ReviewDisposition disposition,
                            Instant reviewedAt) {
    public ContextReview {
        if (observationId == null || evidenceId == null || field == null || entityId == null
                || disposition == null || disposition == EvidenceClaim.ReviewDisposition.PENDING
                || reviewedAt == null || entityId.length() > 1000) {
            throw new IllegalArgumentException("A complete present-context review is required.");
        }
        value = ProjectContext.note(value);
        entityId = entityId.isEmpty() ? "" : ProjectContext.note(entityId);
    }
}
