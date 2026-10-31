package breadcrumbs.domain;

import breadcrumbs.inspector.ContentSafety;
import java.time.Instant;

/** Human statements stay separate from referenced source claims; neither certifies past tests. */
public record ProjectContext(String workspaceId, String snapshotId, String objective,
                             BlockerState blockerState, String blockers, String nextActions,
                             String decisions, String owner, String[] evidenceIds,
                             String previousCheckpointId, Instant confirmedAt, String graphRevisionId) {
    public ProjectContext(String workspaceId, String snapshotId, String objective, BlockerState blockerState,
            String blockers, String nextActions, String decisions, String owner, String[] evidenceIds,
            String previousCheckpointId, Instant confirmedAt) {
        this(workspaceId, snapshotId, objective, blockerState, blockers, nextActions, decisions, owner,
                evidenceIds, previousCheckpointId, confirmedAt, "");
    }
    public enum BlockerState { UNKNOWN, NONE_REPORTED, BLOCKED }

    public ProjectContext {
        if (graphRevisionId == null || !graphRevisionId.isEmpty() && !graphRevisionId.matches("G-[0-9]{6}")) {
            throw new IllegalArgumentException("Invalid graph revision reference.");
        }
        if (workspaceId == null || !workspaceId.matches("W(?:[0-9]{3,9}|-[a-f0-9]{32})")
                || snapshotId == null || snapshotId.isBlank() || blockerState == null || confirmedAt == null) {
            throw new IllegalArgumentException("Workspace, snapshot and explicit blocker state are required.");
        }
        objective = note(objective);
        blockers = note(blockers);
        nextActions = note(nextActions);
        decisions = note(decisions);
        owner = note(owner);
        if (blockerState == BlockerState.BLOCKED && blockers.equals("UNKNOWN")) {
            throw new IllegalArgumentException("Describe the reported blocker or select UNKNOWN.");
        }
        if (evidenceIds == null || evidenceIds.length > 2000 || previousCheckpointId == null) {
            throw new IllegalArgumentException("Invalid checkpoint references.");
        }
        evidenceIds = evidenceIds.clone();
        for (int i = 0; i < evidenceIds.length; i++) {
            if (evidenceIds[i] == null || !evidenceIds[i].matches("EV-[A-Za-z0-9-]{1,80}")) {
                throw new IllegalArgumentException("Invalid evidence ID.");
            }
            for (int j = 0; j < i; j++) {
                if (evidenceIds[i].equals(evidenceIds[j])) {
                    throw new IllegalArgumentException("Duplicate evidence reference.");
                }
            }
        }
    }

    @Override
    public String[] evidenceIds() { return evidenceIds.clone(); }

    /** Multiline notes are storage-safe; terminal rendering still escapes controls at display time. */
    public static String note(String value) {
        if (value == null || value.isBlank()) { return "UNKNOWN"; }
        if (value.length() > 4000 || ContentSafety.hasSecretMarker(value)) {
            throw new IllegalArgumentException("Context exceeds 4000 characters or contains a sensitive marker.");
        }
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if ((Character.isISOControl(c) && c != '\n' && c != '\t')
                    || Character.getType(c) == Character.FORMAT) {
                throw new IllegalArgumentException("Context contains unsafe control or Unicode characters.");
            }
            if (Character.isHighSurrogate(c)) {
                if (i + 1 >= value.length() || !Character.isLowSurrogate(value.charAt(i + 1))) {
                    throw new IllegalArgumentException("Context contains an incomplete Unicode character.");
                }
                i++;
            } else if (Character.isLowSurrogate(c)) {
                throw new IllegalArgumentException("Context contains an incomplete Unicode character.");
            }
        }
        return value;
    }
}
