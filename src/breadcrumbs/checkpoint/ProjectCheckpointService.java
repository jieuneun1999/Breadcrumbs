package breadcrumbs.checkpoint;

import breadcrumbs.domain.Checkpoint;
import breadcrumbs.domain.ProjectContext;
import breadcrumbs.domain.WorkspaceState;
import java.time.Instant;
import java.util.Locale;

/** Creates preview values without reserving an ID or mutating the canonical DLL. */
public final class ProjectCheckpointService {
    public Checkpoint preview(WorkspaceState state, String title, String progress, String objective,
                              ProjectContext.BlockerState blockerState, String blockers, String next,
                              String decisions, String owner, String[] evidenceIds) {
        if (state.activeBaseline() == null || state.nextCheckpointNumber() > 999999
                || state.checkpoints().size() >= 1000) {
            throw new IllegalArgumentException("A B0 and available checkpoint capacity/ID are required.");
        }
        for (String id : evidenceIds) {
            boolean found = false;
            for (var claim : state.claimsAt(state.currentObservationId())) {
                found |= claim.evidenceId().equals(id);
            }
            if (!found) { throw new IllegalArgumentException("Evidence reference is outside this workspace."); }
        }
        Instant time = Instant.now();
        Checkpoint latest = state.checkpoints().getLatest();
        if (time.isBefore(state.activeBaseline().confirmedAt())) { time = state.activeBaseline().confirmedAt(); }
        if (latest != null && time.isBefore(latest.createdAt())) { time = latest.createdAt(); }
        if (time.isBefore(state.currentObservation().observedAt())) { time = state.currentObservation().observedAt(); }
        String snapshot = state.currentObservationId();
        ProjectContext context = new ProjectContext(state.activeBaseline().workspaceId(),
                snapshot.isEmpty() ? "B0-NO-SCAN" : snapshot, objective, blockerState, blockers,
                next, decisions, owner, evidenceIds, latest == null ? "" : latest.checkpointId(), time, state.dependencies().currentId());
        ProjectContext.note(title);
        ProjectContext.note(progress);
        return new Checkpoint(String.format(Locale.ROOT, "CP-%06d", state.nextCheckpointNumber()),
                title, progress, Checkpoint.Status.UNKNOWN, time, Checkpoint.Origin.USER_CONFIRMED, context);
    }

    public boolean confirm(WorkspaceState state, Checkpoint preview, boolean approved) {
        if (!approved) { return false; }
        Checkpoint latest = state.checkpoints().getLatest();
        if (preview.project() == null || !preview.project().previousCheckpointId()
                .equals(latest == null ? "" : latest.checkpointId())
                || !preview.project().snapshotId().equals(state.currentObservationId())
                || !preview.project().graphRevisionId().equals(state.dependencies().currentId())) {
            throw new IllegalArgumentException("Preview is stale. Review a fresh checkpoint.");
        }
        state.appendConfirmed(preview);
        return true;
    }
}
