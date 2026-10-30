package breadcrumbs.bootstrap;

import breadcrumbs.domain.BaselineB0;
import breadcrumbs.domain.EntryMode;
import breadcrumbs.domain.EvidenceClaim;
import breadcrumbs.domain.EvidenceClaim.ArtifactRole;
import breadcrumbs.domain.EvidenceClaim.Field;
import breadcrumbs.domain.EvidenceClaim.Provenance;
import breadcrumbs.domain.EvidenceClaim.ReviewDisposition;
import breadcrumbs.domain.EvidenceClaim.SourceKind;
import breadcrumbs.domain.EvidenceClaim.Verification;
import breadcrumbs.domain.ProjectProfile;
import breadcrumbs.domain.WorkspaceState;
import breadcrumbs.inspector.ContentSafety;
import breadcrumbs.inspector.ProjectInspector;
import breadcrumbs.inspector.ScanPolicy;
import breadcrumbs.inspector.ScanResult;
import breadcrumbs.inspector.SnapshotEntry;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Locale;
import java.util.function.IntPredicate;

/** One proposal and confirmation path for all entry modes. This service never appends checkpoints. */
public final class BootstrapService {
    private final ProjectInspector inspector;
    private final ScanPolicy policy;
    private int nextWorkspaceId = 1;

    public BootstrapService(ScanPolicy policy) {
        this.policy = policy;
        inspector = new ProjectInspector();
    }

    public BootstrapDraft prepare(EntryMode entryMode, String displayName, ProjectProfile profile,
                                  Path root, CurrentContext currentContext, boolean inspectionApproved,
                                  IntPredicate continueScanning) throws IOException {
        if (entryMode == null || profile == null || displayName == null || displayName.isBlank()
                || displayName.length() > 120 || !ContentSafety.terminal(displayName, 120).equals(displayName)
                || ContentSafety.hasSecretMarker(displayName)) {
            throw new IllegalArgumentException("Select an entry mode/profile and a safe project name of 1-120 characters.");
        }
        if (root != null && !inspectionApproved) {
            throw new IllegalArgumentException("Explicit read-only inspection approval is required.");
        }
        if (root == null && entryMode != EntryMode.NEW_EMPTY) {
            throw new IllegalArgumentException("An existing local directory is required for import.");
        }
        String workspaceId = String.format(Locale.ROOT, "W%03d", nextWorkspaceId++);
        ScanResult scan = root == null
                ? new ScanResult(null, "", Instant.now(), policy, new SnapshotEntry[0], new EvidenceClaim[0],
                        new String[] {"No directory was supplied; no file observation is claimed."}, true, 0)
                : inspector.inspect(root, workspaceId, policy, continueScanning);
        if (entryMode == EntryMode.NEW_EMPTY && root != null
                && (scan.entries().length != 0 || !scan.complete())) {
            throw new IllegalArgumentException("NEW_EMPTY requires an empty directory. Choose an import mode for existing files.");
        }
        EvidenceClaim[] claims = scan.claims();
        if (entryMode == EntryMode.NEW_EMPTY) {
            CurrentContext supplied = currentContext == null ? CurrentContext.unknown() : currentContext;
            Field[] fields = {Field.OBJECTIVE, Field.DELIVERABLE, Field.NEXT_ACTION, Field.OWNER, Field.BLOCKERS};
            String[] values = {supplied.objective(), supplied.deliverable(), supplied.nextAction(), supplied.owner(), supplied.blockers()};
            claims = new EvidenceClaim[fields.length];
            for (int index = 0; index < fields.length; index++) {
                claims[index] = new EvidenceClaim(workspaceId,
                        String.format(Locale.ROOT, "EV-%s-%06d", workspaceId, index + 1), fields[index], "",
                        values[index], values[index], "(user input)", 0, "Guided present assessment", "",
                        ArtifactRole.UNKNOWN, scan.observedAt(), "", SourceKind.USER_SUPPLIED, Provenance.EXPLICIT,
                        Verification.UNVERIFIED, ReviewDisposition.PENDING);
            }
        }
        return new BootstrapDraft(workspaceId, displayName.strip(), profile, entryMode, scan,
                ClaimReview.markConflicts(claims));
    }

    public boolean confirm(WorkspaceState state, BootstrapDraft draft, boolean approved) {
        if (!approved) {
            return false;
        }
        EvidenceClaim[] reviewed = draft.claims();
        for (int index = 0; index < reviewed.length; index++) {
            EvidenceClaim claim = reviewed[index];
            if (claim.userSupplied()) {
                reviewed[index] = claim.withReview(ReviewDisposition.CONFIRMED)
                        .withVerification(Verification.USER_CONFIRMED);
            } else if (claim.review() == ReviewDisposition.PENDING) {
                reviewed[index] = claim.withReview(ReviewDisposition.LEFT_UNKNOWN);
            }
        }
        state.activate(new BaselineB0(draft.workspaceId(), draft.displayName(), draft.profile(), draft.entryMode(),
                draft.scan(), reviewed, ClaimReview.conflicts(reviewed), Instant.now()));
        return true;
    }
}
