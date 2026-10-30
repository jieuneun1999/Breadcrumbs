package breadcrumbs.domain;

public record ConflictGroup(EvidenceClaim.Field field, String entityId,
                            String[] evidenceIds, boolean resolvedByReview) {
    public ConflictGroup {
        evidenceIds = evidenceIds.clone();
    }

    @Override
    public String[] evidenceIds() {
        return evidenceIds.clone();
    }
}
