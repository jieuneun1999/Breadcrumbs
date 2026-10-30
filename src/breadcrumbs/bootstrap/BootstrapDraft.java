package breadcrumbs.bootstrap;

import breadcrumbs.domain.ConflictGroup;
import breadcrumbs.domain.EntryMode;
import breadcrumbs.domain.EvidenceClaim;
import breadcrumbs.domain.EvidenceClaim.ReviewDisposition;
import breadcrumbs.domain.ProjectProfile;
import breadcrumbs.inspector.ScanResult;

/** A proposal has no authority over WorkspaceState until explicit final confirmation. */
public record BootstrapDraft(String workspaceId, String displayName, ProjectProfile profile,
                             EntryMode entryMode, ScanResult scan, EvidenceClaim[] claims) {
    public BootstrapDraft {
        claims = claims.clone();
    }

    @Override
    public EvidenceClaim[] claims() {
        return claims.clone();
    }

    public BootstrapDraft review(String evidenceId, ReviewDisposition disposition) {
        if (disposition == null || disposition == ReviewDisposition.PENDING) {
            throw new IllegalArgumentException("Choose confirmed, rejected, or left unknown.");
        }
        EvidenceClaim[] reviewed = claims.clone();
        for (int index = 0; index < reviewed.length; index++) {
            if (reviewed[index].evidenceId().equals(evidenceId)) {
                reviewed[index] = reviewed[index].withReview(disposition);
                return new BootstrapDraft(workspaceId, displayName, profile, entryMode, scan, reviewed);
            }
        }
        throw new IllegalArgumentException("Unknown evidence ID.");
    }

    public ConflictGroup[] conflicts() {
        return ClaimReview.conflicts(claims);
    }
}
