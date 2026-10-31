package breadcrumbs.continuity;

import breadcrumbs.domain.*;

/** Derived view of the latest observation plus its present reviews; no historical values mutate. */
public final class CurrentAssessment {
    private CurrentAssessment() { }
    public static EvidenceClaim[] claims(WorkspaceState state) {
        EvidenceClaim[] claims = state.claimsAt(state.currentObservationId());
        for (ContextReview review : state.reviews()) {
            if (!review.observationId().equals(state.currentObservationId()) || review.evidenceId().isEmpty()) { continue; }
            for (int i = 0; i < claims.length; i++) {
                if (claims[i].evidenceId().equals(review.evidenceId())) { claims[i] = claims[i].withReview(review.disposition()); }
            }
        }
        return claims;
    }
    public static ContextReview statement(WorkspaceState state, EvidenceClaim.Field field) {
        ContextReview selected = null;
        for (ContextReview review : state.reviews()) {
            if (review.observationId().equals(state.currentObservationId()) && review.evidenceId().isEmpty()
                    && review.field() == field && review.entityId().isEmpty()) { selected = review; }
        }
        return selected != null && selected.disposition() == EvidenceClaim.ReviewDisposition.CONFIRMED ? selected : null;
    }
}
