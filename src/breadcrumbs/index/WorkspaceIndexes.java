package breadcrumbs.index;

import breadcrumbs.domain.*;
import breadcrumbs.inspector.*;
import breadcrumbs.dsa.hash.CustomHashTable;

/** Disposable workspace-bound indexes. Epoch mismatch rebuilds before returning a canonical value. */
public final class WorkspaceIndexes {
    private final WorkspaceState state;
    private boolean enabled = true;
    private long epoch = -1;
    private long dllModification = -1;
    private long unavailableEpoch = -1;
    private CustomHashTable<Checkpoint> checkpoints = new CustomHashTable<>();
    private CustomHashTable<WorkItem> tasks = new CustomHashTable<>();
    private CustomHashTable<EvidenceClaim> evidence = new CustomHashTable<>();
    private CustomHashTable<SnapshotEntry> paths = new CustomHashTable<>();
    private CustomHashTable<SnapshotEntry[]> hashes = new CustomHashTable<>();
    public WorkspaceIndexes(WorkspaceState state) { this.state = state; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean enabled() { return enabled; }
    public boolean stale() { return epoch != state.epoch() || dllModification != state.checkpoints().modification(); }
    private String key(String observation, String id) { return state.activeBaseline().workspaceId() + "|" + observation.length() + ":" + observation + id; }
    public void rebuild() {
        var cp = new CustomHashTable<Checkpoint>(); var wi = new CustomHashTable<WorkItem>();
        var ev = new CustomHashTable<EvidenceClaim>(); var pa = new CustomHashTable<SnapshotEntry>(); var ha = new CustomHashTable<SnapshotEntry[]>();
        for (Checkpoint checkpoint : state.checkpoints().traverseForward()) { cp.put(checkpoint.checkpointId(), checkpoint); }
        if (state.dependencies().current() != null) { for (WorkItem item : state.dependencies().current().items()) { wi.put(item.id(), item); } }
        for (ScanResult scan : observations()) {
            String id = scan.scanId().isEmpty() ? "B0-NO-SCAN" : scan.scanId();
            for (EvidenceClaim claim : state.claimsAt(id)) { ev.put(key(id, claim.evidenceId()), claim); }
            for (SnapshotEntry entry : scan.entries()) {
                pa.put(key(id, entry.relativePath()), entry);
                if (!breadcrumbs.continuity.DriftEngine.reliable(entry)) { continue; }
                String hashKey = key(id, entry.sha256()); SnapshotEntry[] old = ha.get(hashKey);
                SnapshotEntry[] all = old == null ? new SnapshotEntry[1] : java.util.Arrays.copyOf(old, old.length + 1);
                all[all.length - 1] = entry; ha.put(hashKey, all);
            }
        }
        checkpoints = cp; tasks = wi; evidence = ev; paths = pa; hashes = ha; epoch = state.epoch();
        dllModification = state.checkpoints().modification();
    }
    private boolean ready() {
        if (!enabled || unavailableEpoch == state.epoch()) { return false; }
        if (stale()) {
            try { rebuild(); }
            catch (IllegalArgumentException capacity) { unavailableEpoch = state.epoch(); return false; }
        }
        return true;
    }
    public Checkpoint checkpoint(String id) { return ready() ? checkpoints.get(id) : state.checkpoints().findById(id); }
    public WorkItem workItem(String id) { return ready() ? tasks.get(id) : state.dependencies().findItem(id); }
    public EvidenceClaim evidence(String observation, String id) {
        if (state.observation(observation) == null) { return null; }
        return ready() ? evidence.get(key(observation, id)) : state.evidence(observation, id);
    }
    public SnapshotEntry path(String observation, String path) {
        ScanResult scan = state.observation(observation); if (scan == null) { return null; }
        return ready() ? paths.get(key(observation, path)) : breadcrumbs.continuity.DriftEngine.find(scan.entries(), path);
    }
    public SnapshotEntry[] hashCandidates(String observation, String hash) {
        ScanResult scan = state.observation(observation); if (scan == null) { return new SnapshotEntry[0]; }
        if (ready()) { SnapshotEntry[] found = hashes.get(key(observation, hash)); return found == null ? new SnapshotEntry[0] : found.clone(); }
        SnapshotEntry[] found = new SnapshotEntry[scan.entries().length]; int count = 0;
        for (SnapshotEntry entry : scan.entries()) { if (breadcrumbs.continuity.DriftEngine.reliable(entry) && entry.sha256().equals(hash)) { found[count++] = entry; } }
        return java.util.Arrays.copyOf(found, count);
    }
    public boolean validate() {
        if (!ready()) { return true; }
        int taskCount = state.dependencies().current() == null ? 0 : state.dependencies().current().items().length;
        int evidenceCount = 0, pathCount = 0;
        for (Checkpoint cp : state.checkpoints().traverseForward()) { if (checkpoints.get(cp.checkpointId()) != cp) { return false; } }
        if (state.dependencies().current() != null) { for (WorkItem task : state.dependencies().current().items()) { if (tasks.get(task.id()) != task) { return false; } } }
        for (ScanResult scan : observations()) {
            String id = scan.scanId().isEmpty() ? "B0-NO-SCAN" : scan.scanId();
            evidenceCount += state.claimsAt(id).length; pathCount += scan.entries().length;
            for (EvidenceClaim claim : state.claimsAt(id)) { if (!claim.equals(evidence.get(key(id, claim.evidenceId())))) { return false; } }
            for (SnapshotEntry entry : scan.entries()) { if (paths.get(key(id, entry.relativePath())) != entry) { return false; } }
        }
        return checkpoints.size() == state.checkpoints().size() && tasks.size() == taskCount && evidence.size() == evidenceCount
                && paths.size() == pathCount && checkpoints.validate() && tasks.validate() && evidence.validate() && paths.validate() && hashes.validate();
    }
    private ScanResult[] observations() {
        var later = state.laterObservations(); var scans = new ScanResult[later.length + 1]; scans[0] = state.activeBaseline().snapshot();
        System.arraycopy(later, 0, scans, 1, later.length); return scans;
    }
}
