package breadcrumbs.domain;

import breadcrumbs.dsa.dll.CheckpointList;

/** Sole owner of the active project's confirmed baseline and chronological checkpoint list. */
public final class WorkspaceState {
    private BaselineB0 activeBaseline;
    private CheckpointList checkpoints = new CheckpointList();
    private int nextCheckpointNumber = 1;
    private boolean dirty;

    public BaselineB0 activeBaseline() {
        return activeBaseline;
    }

    public CheckpointList checkpoints() {
        return checkpoints;
    }

    /** Called only after the bootstrap service receives explicit baseline confirmation. */
    public void activate(BaselineB0 confirmedBaseline) {
        if (confirmedBaseline == null || confirmedBaseline.confirmedAt() == null) {
            throw new IllegalArgumentException("A confirmed baseline is required.");
        }
        activeBaseline = confirmedBaseline;
        // B2 never migrates the separate B1 sandbox or imported chronology into a new project.
        checkpoints = new CheckpointList();
        nextCheckpointNumber = 1;
        dirty = true;
    }

    public int nextCheckpointNumber() { return nextCheckpointNumber; }
    public boolean dirty() { return dirty; }
    public void markUnsaved() { dirty = true; }
    public void markSaved() { dirty = false; }

    /** Only a completely validated detached state may replace the active workspace. */
    public void replaceWith(WorkspaceState validated) {
        activeBaseline = validated.activeBaseline;
        checkpoints = validated.checkpoints;
        nextCheckpointNumber = validated.nextCheckpointNumber;
        dirty = validated.dirty;
    }

    public void restoreNextCheckpointNumber(int number) {
        if (number < 1 || number > 1000000) {
            throw new IllegalArgumentException("Invalid next checkpoint number.");
        }
        nextCheckpointNumber = number;
    }

    public void appendConfirmed(Checkpoint checkpoint) {
        if (activeBaseline == null || checkpoint.project() == null
                || !checkpoint.project().workspaceId().equals(activeBaseline.workspaceId())
                || !checkpoint.checkpointId().equals(String.format(java.util.Locale.ROOT,
                        "CP-%06d", nextCheckpointNumber)) || nextCheckpointNumber > 999999) {
            throw new IllegalArgumentException("Checkpoint does not belong to the active sequence.");
        }
        checkpoints.append(checkpoint);
        nextCheckpointNumber++;
        dirty = true;
    }
}
