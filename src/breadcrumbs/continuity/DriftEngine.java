package breadcrumbs.continuity;

import breadcrumbs.inspector.ScanResult;
import breadcrumbs.inspector.SnapshotEntry;
import java.util.Arrays;
import java.util.Objects;

/** Bounded quadratic array matching; timestamps never establish content identity. */
public final class DriftEngine {
    public enum Status { UNCHANGED, MODIFIED, ADDED, DELETED, RENAMED_EXACT, AMBIGUOUS_RENAME, UNKNOWN }
    public record Change(String oldPath, String newPath, Status status, String reason) { }
    public record Comparison(String oldObservation, String newObservation, Change[] changes) {
        public Comparison { changes = changes.clone(); }
        @Override public Change[] changes() { return changes.clone(); }
    }

    public Comparison compare(ScanResult before, ScanResult after) {
        SnapshotEntry[] oldEntries = before.entries(), newEntries = after.entries();
        boolean[] oldUnmatched = unmatched(oldEntries, newEntries), newUnmatched = unmatched(newEntries, oldEntries);
        Change[] result = new Change[oldEntries.length + newEntries.length];
        int count = 0;
        boolean compatible = before.root() != null && Objects.equals(before.root(), after.root())
                && before.policy().equals(after.policy());
        boolean covered = compatible && before.complete() && after.complete();
        for (SnapshotEntry old : oldEntries) {
            SnapshotEntry same = find(newEntries, old.relativePath());
            if (same != null) {
                Status status = !compatible || !reliable(old) || !reliable(same) ? Status.UNKNOWN
                        : old.sha256().equals(same.sha256()) ? Status.UNCHANGED : Status.MODIFIED;
                result[count++] = new Change(old.relativePath(), same.relativePath(), status,
                        status == Status.UNKNOWN ? "Unreliable fingerprint or incompatible root/policy." : "Compared reliable SHA-256 fingerprints.");
            } else {
                int oldMatches = unmatchedHashCount(oldEntries, oldUnmatched, old.sha256());
                int newMatches = unmatchedHashCount(newEntries, newUnmatched, old.sha256());
                Status status = !covered || !reliable(old) ? Status.UNKNOWN : newMatches == 0 ? Status.DELETED
                        : oldMatches == 1 && newMatches == 1 ? Status.RENAMED_EXACT : Status.AMBIGUOUS_RENAME;
                String target = "";
                if (status == Status.RENAMED_EXACT || status == Status.AMBIGUOUS_RENAME) {
                    for (int i = 0; i < newEntries.length; i++) {
                        SnapshotEntry entry = newEntries[i];
                        if (reliable(entry) && entry.sha256().equals(old.sha256()) && newUnmatched[i]) {
                            if (target.length() + entry.relativePath().length() > 4000) { target += " [further candidates omitted]"; break; }
                            target += (target.isEmpty() ? "" : ", ") + entry.relativePath();
                        }
                    }
                }
                result[count++] = new Change(old.relativePath(), target, status,
                        status == Status.UNKNOWN ? "Missing path is not a deletion under incomplete/unsafe coverage."
                        : status == Status.AMBIGUOUS_RENAME ? "Multiple unmatched equal-hash candidates; identity is unresolved."
                        : status == Status.RENAMED_EXACT ? "Unique one-to-one unmatched equal-hash move."
                        : "Absent from a complete comparable observation within the declared scan scope.");
            }
        }
        for (int i = 0; i < newEntries.length; i++) {
            SnapshotEntry entry = newEntries[i];
            if (!newUnmatched[i]) { continue; }
            // A paired or ambiguous rename is already represented by the old-side row(s).
            if (covered && reliable(entry) && unmatchedHashCount(oldEntries, oldUnmatched, entry.sha256()) > 0) { continue; }
            result[count++] = new Change("", entry.relativePath(), covered && reliable(entry) ? Status.ADDED : Status.UNKNOWN,
                    covered && reliable(entry) ? "New path in a complete comparable observation." : "Prior coverage or fingerprint is insufficient.");
        }
        Change[] changes = Arrays.copyOf(result, count);
        // Inventory enumeration order is filesystem-dependent. Reports have a deterministic path order.
        for (int i = 1; i < changes.length; i++) {
            Change item = changes[i]; int j = i - 1;
            while (j >= 0 && key(changes[j]).compareTo(key(item)) > 0) { changes[j + 1] = changes[j--]; }
            changes[j + 1] = item;
        }
        return new Comparison(before.scanId(), after.scanId(), changes);
    }
    private static String key(Change change) { return change.oldPath().isEmpty() ? change.newPath() : change.oldPath(); }
    private static boolean[] unmatched(SnapshotEntry[] entries, SnapshotEntry[] other) {
        boolean[] result = new boolean[entries.length];
        for (int i = 0; i < entries.length; i++) { result[i] = find(other, entries[i].relativePath()) == null; }
        return result;
    }
    private static int unmatchedHashCount(SnapshotEntry[] entries, boolean[] unmatched, String hash) {
        if (hash.isEmpty()) { return 0; }
        int count = 0;
        for (int i = 0; i < entries.length; i++) {
            SnapshotEntry entry = entries[i];
            if (unmatched[i] && reliable(entry) && entry.sha256().equals(hash)) { count++; }
        }
        return count;
    }
    public static SnapshotEntry find(SnapshotEntry[] entries, String path) {
        for (SnapshotEntry entry : entries) { if (entry.relativePath().equals(path)) { return entry; } }
        return null;
    }
    public static boolean reliable(SnapshotEntry entry) {
        return entry != null && entry.sha256().matches("[a-f0-9]{64}")
                && (entry.status() == SnapshotEntry.Status.SUPPORTED || entry.status() == SnapshotEntry.Status.UNSUPPORTED);
    }
}
