package breadcrumbs.continuity;

import breadcrumbs.domain.*;
import breadcrumbs.inspector.ScanResult;
import breadcrumbs.inspector.SnapshotEntry;
import java.util.Arrays;

/** Freshness belongs to a checkpoint's original evidence, not every file in its workspace. */
public final class ReferenceFreshness {
    public enum State { FRESH, STALE, UNKNOWN, NOT_APPLICABLE }
    public record Reference(String evidenceId, String path, State state, String explanation) { }
    public record Result(State state, Reference[] references) {
        public Result { references = references.clone(); }
        @Override public Reference[] references() { return references.clone(); }
    }
    public Result assess(WorkspaceState state, Checkpoint checkpoint) {
        if (checkpoint == null && state.laterObservations().length == 0) { return new Result(State.NOT_APPLICABLE, new Reference[0]); }
        if (checkpoint != null && (checkpoint.project() == null || !checkpoint.project().workspaceId().equals(state.activeBaseline().workspaceId()))) {
            throw new IllegalArgumentException("Select a native checkpoint from this workspace.");
        }
        if (checkpoint != null && state.checkpoints().findById(checkpoint.checkpointId()) != checkpoint) {
            throw new IllegalArgumentException("Checkpoint must be the canonical DLL value.");
        }
        ProjectContext context = checkpoint == null ? null : checkpoint.project();
        String observationId = context == null ? state.activeBaseline().snapshot().scanId() : context.snapshotId();
        if (observationId.isEmpty()) { observationId = "B0-NO-SCAN"; }
        ScanResult original = state.observation(observationId), later = state.currentObservation();
        if (original == null) { throw new IllegalArgumentException("Checkpoint observation is unresolved."); }
        String[] ids;
        if (context == null) {
            EvidenceClaim[] claims = state.claimsAt(observationId); ids = new String[claims.length];
            for (int i = 0; i < ids.length; i++) { ids[i] = claims[i].evidenceId(); }
        } else { ids = context.evidenceIds(); }
        Reference[] refs = new Reference[ids.length]; int count = 0;
        boolean stale = false, unknown = false;
        DriftEngine.Change[] changes = new DriftEngine().compare(original, later).changes();
        for (String id : ids) {
            EvidenceClaim claim = state.evidence(observationId, id);
            if (claim == null) { throw new IllegalArgumentException("Checkpoint evidence is unresolved."); }
            State freshness = State.UNKNOWN;
            String reason = "No later comparable observation; stored observation is not current verification.";
            if (claim.userSupplied()) {
                freshness = State.NOT_APPLICABLE; reason = "Human assessment has no file fingerprint.";
            } else if (original != later) {
                SnapshotEntry observed = DriftEngine.find(original.entries(), claim.relativePath());
                if (observed == null || !observed.sha256().equals(claim.fingerprint())) {
                    reason = "Original fingerprint is unresolved.";
                } else {
                    for (DriftEngine.Change change : changes) {
                        if (!change.oldPath().equals(claim.relativePath())) { continue; }
                        freshness = switch (change.status()) {
                            case UNCHANGED -> State.FRESH;
                            case MODIFIED, DELETED -> State.STALE;
                            // A rename changes the locator. Even equal bytes require explicit reference review.
                            case RENAMED_EXACT -> State.STALE;
                            default -> State.UNKNOWN;
                        };
                        reason = change.status() + ": " + change.reason(); break;
                    }
                }
            }
            stale |= freshness == State.STALE; unknown |= freshness == State.UNKNOWN;
            refs[count++] = new Reference(id, claim.relativePath(), freshness, reason);
        }
        State overall = stale ? State.STALE : unknown ? State.UNKNOWN : count == 0 ? State.UNKNOWN : State.NOT_APPLICABLE;
        for (Reference ref : refs) {
            if (ref.state() == State.FRESH && overall == State.NOT_APPLICABLE) { overall = State.FRESH; }
        }
        return new Result(overall, Arrays.copyOf(refs, count));
    }
}
