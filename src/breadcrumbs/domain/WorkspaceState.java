package breadcrumbs.domain;

import breadcrumbs.dsa.dll.CheckpointList;
import breadcrumbs.inspector.ScanResult;
import java.util.Arrays;

/** Sole owner of the active project's confirmed baseline and chronological checkpoint list. */
public final class WorkspaceState {
    private BaselineB0 activeBaseline;
    private CheckpointList checkpoints = new CheckpointList();
    private int nextCheckpointNumber = 1;
    private boolean dirty;
    private ScanResult[] laterObservations = new ScanResult[0];
    private ContextReview[] reviews = new ContextReview[0];
    private boolean versionTwo;
    private boolean soloOwner;
    private long epoch;
    private final breadcrumbs.index.WorkspaceIndexes indexes = new breadcrumbs.index.WorkspaceIndexes(this);
    public breadcrumbs.index.WorkspaceIndexes indexes() { return indexes; }
    public long epoch() { return epoch; }
    private ManualContext manualContext = new ManualContext(this);
    public ManualContext manualContext() { return manualContext; }
    private boolean versionFour;
    public boolean versionFour() { return versionFour; }
    public void enableVersionFour() { versionFour = true; versionTwo = true; markUnsaved(); }
    private DependencyState dependencies = new DependencyState(this);
    public DependencyState dependencies() { return dependencies; }

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
        laterObservations = new ScanResult[0];
        reviews = new ContextReview[0];
        versionTwo = false;
        soloOwner = false;
        dependencies = new DependencyState(this);
        manualContext = new ManualContext(this); versionFour = false;
        documentVersion(confirmedBaseline.snapshot());
        epoch++;
    }

    public int nextCheckpointNumber() { return nextCheckpointNumber; }
    public boolean dirty() { return dirty; }
    public void markUnsaved() { dirty = true; epoch++; }
    public void markSaved() { dirty = false; }

    /** Only a completely validated detached state may replace the active workspace. */
    public void replaceWith(WorkspaceState validated) {
        activeBaseline = validated.activeBaseline;
        checkpoints = validated.checkpoints;
        nextCheckpointNumber = validated.nextCheckpointNumber;
        dirty = validated.dirty;
        epoch++;
        laterObservations = validated.laterObservations.clone();
        reviews = validated.reviews.clone();
        versionTwo = validated.versionTwo;
        soloOwner = validated.soloOwner;
        dependencies = new DependencyState(this);
        dependencies.copyFrom(validated.dependencies);
        manualContext = new ManualContext(this); manualContext.copyFrom(validated.manualContext); versionFour = validated.versionFour;
        dirty = validated.dirty;
    }

    public ScanResult[] laterObservations() { return laterObservations.clone(); }
    public ContextReview[] reviews() { return reviews.clone(); }
    public boolean versionTwo() { return versionTwo; }
    public boolean soloOwner() { return soloOwner; }

    /** Explicit setting; no project profile silently implies that coordination is unnecessary. */
    public void setSoloOwner(boolean solo) { soloOwner = solo; enableVersionTwo(); }
    public void enableVersionTwo() { versionTwo = true; markUnsaved(); }

    public String currentObservationId() {
        String id = currentObservation().scanId();
        return id.isEmpty() ? "B0-NO-SCAN" : id;
    }
    public ScanResult currentObservation() {
        return laterObservations.length == 0 ? activeBaseline.snapshot() : laterObservations[laterObservations.length - 1];
    }
    public ScanResult observation(String id) {
        if (activeBaseline == null) { return null; }
        ScanResult first = activeBaseline.snapshot();
        if (id.equals(first.scanId().isEmpty() ? "B0-NO-SCAN" : first.scanId())) { return first; }
        for (ScanResult scan : laterObservations) { if (scan.scanId().equals(id)) { return scan; } }
        return null;
    }
    public EvidenceClaim[] claimsAt(String observationId) {
        ScanResult scan = observation(observationId);
        if (scan == null) { throw new IllegalArgumentException("Unknown observation."); }
        return scan == activeBaseline.snapshot() ? activeBaseline.claims() : scan.claims();
    }
    public EvidenceClaim evidence(String observationId, String evidenceId) {
        for (EvidenceClaim claim : claimsAt(observationId)) {
            if (claim.evidenceId().equals(evidenceId)) { return claim; }
        }
        return null;
    }
    public void addObservation(ScanResult scan) {
        if (activeBaseline == null || scan == null || scan.root() == null || laterObservations.length >= 20
                || !scan.scanId().equals("OBS-" + activeBaseline.workspaceId() + "-"
                    + String.format(java.util.Locale.ROOT, "%04d", laterObservations.length + 1))
                || !java.util.Objects.equals(scan.root(), activeBaseline.snapshot().root())
                || scan.observedAt().isBefore(currentObservation().observedAt())) {
            throw new IllegalArgumentException("Invalid later observation or capacity reached (20). B0 retained.");
        }
        laterObservations = Arrays.copyOf(laterObservations, laterObservations.length + 1);
        laterObservations[laterObservations.length - 1] = scan;
        documentVersion(scan);
        enableVersionTwo();
    }
    private void documentVersion(ScanResult scan) {
        for (var entry : scan.entries()) { if (entry.adapter() == breadcrumbs.inspector.SnapshotEntry.Adapter.DOCX) { enableVersionFour(); return; } }
    }
    public void addReview(ContextReview review) {
        if (reviews.length >= 2000 || observation(review.observationId()) == null
                || review.reviewedAt().isBefore(observation(review.observationId()).observedAt())) {
            throw new IllegalArgumentException("Invalid review observation/time or review capacity reached.");
        }
        if (!review.evidenceId().isEmpty()) {
            EvidenceClaim source = evidence(review.observationId(), review.evidenceId());
            if (source == null || source.field() != review.field() || !source.entityId().equals(review.entityId())
                    || !source.literalValue().equals(review.value())) {
                throw new IllegalArgumentException("Review must preserve the exact source statement.");
            }
        }
        reviews = Arrays.copyOf(reviews, reviews.length + 1);
        reviews[reviews.length - 1] = review;
        enableVersionTwo();
    }

    public void restoreNextCheckpointNumber(int number) {
        if (number < 1 || number > 1000000) {
            throw new IllegalArgumentException("Invalid next checkpoint number.");
        }
        nextCheckpointNumber = number;
    }

    public void appendConfirmed(Checkpoint checkpoint) {
        if (checkpoint.project() != null) { manualContext.validateReferences(checkpoint.project().manualEvidenceIds(), checkpoint.createdAt()); }
        if (activeBaseline == null || checkpoint.project() == null
                || !checkpoint.project().workspaceId().equals(activeBaseline.workspaceId())
                || !checkpoint.checkpointId().equals(String.format(java.util.Locale.ROOT,
                        "CP-%06d", nextCheckpointNumber)) || nextCheckpointNumber > 999999) {
            throw new IllegalArgumentException("Checkpoint does not belong to the active sequence.");
        }
        if (!checkpoint.project().snapshotId().equals(currentObservationId())) {
            throw new IllegalArgumentException("Checkpoint must reference the reviewed current observation.");
        }
        for (String id : checkpoint.project().evidenceIds()) {
            if (evidence(currentObservationId(), id) == null) { throw new IllegalArgumentException("Unresolved checkpoint evidence."); }
        }
        checkpoints.append(checkpoint);
        epoch++;
        nextCheckpointNumber++;
        dirty = true;
    }
}
