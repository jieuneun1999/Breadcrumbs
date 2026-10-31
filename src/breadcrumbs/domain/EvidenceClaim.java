package breadcrumbs.domain;

import java.time.Instant;

/** An immutable literal claim. Review never rewrites its source or historical verification. */
public record EvidenceClaim(String workspaceId, String evidenceId, Field field, String entityId,
                            String literalValue, String comparisonValue, String relativePath,
                            int line, String section, String fingerprint, ArtifactRole artifactRole,
                            Instant observedAt, String sourceStatedDate, SourceKind sourceKind, Provenance provenance,
                            Verification verification, ReviewDisposition review) {
    public enum Field {
        OBJECTIVE, DELIVERABLE, CURRENT_STATE, NEXT_ACTION, OWNER, BLOCKERS,
        PROGRESS, DECISION, RATIONALE, TEST_RESULTS, TASK_STATUS, TASK_MARKER, DOCUMENT_TEXT
    }
    public enum ArtifactRole { INSTRUCTIONS, CURRENT_STATE, HANDOFF, TASK_LIST, DECISION, UNKNOWN }
    public enum Provenance { OBSERVED, EXPLICIT, DERIVED }
    public enum SourceKind { SOURCE_REPORTED, USER_SUPPLIED }
    public enum Verification { SOURCE_OBSERVED, UNVERIFIED, USER_CONFIRMED, CONFLICTING, UNREADABLE }
    public enum ReviewDisposition { PENDING, CONFIRMED, REJECTED, LEFT_UNKNOWN }

    public EvidenceClaim withReview(ReviewDisposition disposition) {
        return new EvidenceClaim(workspaceId, evidenceId, field, entityId, literalValue,
                comparisonValue, relativePath, line, section, fingerprint, artifactRole,
                observedAt, sourceStatedDate, sourceKind, provenance, verification, disposition);
    }

    public EvidenceClaim withVerification(Verification state) {
        return new EvidenceClaim(workspaceId, evidenceId, field, entityId, literalValue,
                comparisonValue, relativePath, line, section, fingerprint, artifactRole,
                observedAt, sourceStatedDate, sourceKind, provenance, state, review);
    }

    public boolean userSupplied() {
        return sourceKind == SourceKind.USER_SUPPLIED;
    }
    public boolean paragraphLocated() { return relativePath.toLowerCase(java.util.Locale.ROOT).endsWith(".docx") && section.equals("DOCX main paragraph " + line); }
    public String locationUnit() { return paragraphLocated() ? "paragraph" : "line"; }
}
