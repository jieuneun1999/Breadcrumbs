package breadcrumbs.domain;

import breadcrumbs.inspector.ScanResult;
import java.time.Instant;

/** First observation and reviewed present context, deliberately not a Checkpoint subtype. */
public record BaselineB0(String workspaceId, String displayName, ProjectProfile profile,
                         EntryMode entryMode, ScanResult snapshot, EvidenceClaim[] claims,
                         ConflictGroup[] conflicts, Instant confirmedAt) {
    public BaselineB0 {
        claims = claims.clone();
        conflicts = conflicts.clone();
    }

    @Override
    public EvidenceClaim[] claims() {
        return claims.clone();
    }

    @Override
    public ConflictGroup[] conflicts() {
        return conflicts.clone();
    }
}
