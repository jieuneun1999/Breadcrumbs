package breadcrumbs.inspector;

import breadcrumbs.domain.EvidenceClaim.ArtifactRole;
import java.time.Instant;

/** Empty sha256 means no trustworthy fingerprint, not the hash of an empty file. */
public record SnapshotEntry(String relativePath, long sizeBytes, Instant lastModified,
                            String sha256, Status status, Adapter adapter, ArtifactRole artifactRole,
                            Instant observedAt, String reason) {
    public enum Status { SUPPORTED, SKIPPED, UNSUPPORTED, FAILED, UNSTABLE }
    public enum Adapter { TXT, MARKDOWN, CSV, METADATA_ONLY, DIRECTORY, DOCX }
}
