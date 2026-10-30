package breadcrumbs.bootstrap;

import breadcrumbs.domain.ConflictGroup;
import breadcrumbs.domain.EvidenceClaim;
import breadcrumbs.domain.EvidenceClaim.ReviewDisposition;
import breadcrumbs.domain.EvidenceClaim.Verification;

/** Bounded explicit-identity comparison, not the future weighted context-audit engine. */
public final class ClaimReview {
    private ClaimReview() { }

    public static ConflictGroup[] conflicts(EvidenceClaim[] claims) {
        ConflictGroup[] groups = new ConflictGroup[claims.length];
        boolean[] grouped = new boolean[claims.length];
        int count = 0;
        for (int index = 0; index < claims.length; index++) {
            EvidenceClaim first = claims[index];
            if (grouped[index] || first.entityId().isEmpty() || first.userSupplied()) {
                continue;
            }
            int matches = 0;
            boolean differentValue = false;
            for (EvidenceClaim candidate : claims) {
                if (sameIdentity(first, candidate)) {
                    matches++;
                    differentValue |= !first.comparisonValue().equals(candidate.comparisonValue());
                }
            }
            if (!differentValue) {
                continue;
            }
            String[] ids = new String[matches];
            int idCount = 0;
            String acceptedValue = null;
            boolean unresolved = false;
            for (int other = index; other < claims.length; other++) {
                EvidenceClaim candidate = claims[other];
                if (!sameIdentity(first, candidate)) {
                    continue;
                }
                grouped[other] = true;
                ids[idCount++] = candidate.evidenceId();
                if (candidate.review() == ReviewDisposition.CONFIRMED) {
                    if (acceptedValue != null && !acceptedValue.equals(candidate.comparisonValue())) {
                        unresolved = true;
                    }
                    acceptedValue = candidate.comparisonValue();
                } else if (candidate.review() != ReviewDisposition.REJECTED) {
                    unresolved = true;
                }
            }
            groups[count++] = new ConflictGroup(first.field(), first.entityId(), ids,
                    acceptedValue != null && !unresolved);
        }
        ConflictGroup[] result = new ConflictGroup[count];
        System.arraycopy(groups, 0, result, 0, count);
        return result;
    }

    static EvidenceClaim[] markConflicts(EvidenceClaim[] original) {
        EvidenceClaim[] result = original.clone();
        for (ConflictGroup group : conflicts(original)) {
            for (String evidenceId : group.evidenceIds()) {
                for (int index = 0; index < result.length; index++) {
                    if (result[index].evidenceId().equals(evidenceId)) {
                        result[index] = result[index].withVerification(Verification.CONFLICTING);
                    }
                }
            }
        }
        return result;
    }

    private static boolean sameIdentity(EvidenceClaim left, EvidenceClaim right) {
        return !right.userSupplied() && left.workspaceId().equals(right.workspaceId())
                && left.field() == right.field() && left.entityId().equals(right.entityId());
    }
}
