package breadcrumbs.domain;

import breadcrumbs.dsa.dll.CheckpointList;

/** Sole owner of the active project's confirmed baseline and chronological checkpoint list. */
public final class WorkspaceState {
    private BaselineB0 activeBaseline;
    private CheckpointList checkpoints = new CheckpointList();

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
    }
}
