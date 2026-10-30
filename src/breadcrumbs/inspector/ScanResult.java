package breadcrumbs.inspector;

import breadcrumbs.domain.EvidenceClaim;
import java.nio.file.Path;
import java.time.Instant;

public record ScanResult(Path root, String scanId, Instant observedAt, ScanPolicy policy,
                         SnapshotEntry[] entries, EvidenceClaim[] claims, String[] diagnostics,
                         boolean complete, long bytesRead) {
    public ScanResult {
        entries = entries.clone();
        claims = claims.clone();
        diagnostics = diagnostics.clone();
    }

    @Override
    public SnapshotEntry[] entries() { return entries.clone(); }

    @Override
    public EvidenceClaim[] claims() { return claims.clone(); }

    @Override
    public String[] diagnostics() { return diagnostics.clone(); }

    public int count(SnapshotEntry.Status status) {
        int count = 0;
        for (SnapshotEntry entry : entries) {
            if (entry.status() == status) {
                count++;
            }
        }
        return count;
    }
}
