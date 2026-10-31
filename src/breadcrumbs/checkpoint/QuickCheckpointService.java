package breadcrumbs.checkpoint;

import breadcrumbs.domain.*;

public final class QuickCheckpointService {
    public record Options(String objective, String owner, String decisions, String[] evidenceIds) {
        public Options { evidenceIds = evidenceIds.clone(); }
        @Override public String[] evidenceIds() { return evidenceIds.clone(); }
    }
    public Options proposed(WorkspaceState state) {
        Checkpoint last = state.checkpoints().getLatest();
        if (last != null) { return new Options(last.project().objective(), last.project().owner(), "UNKNOWN", new String[0]); }
        return new Options(confirmed(state, EvidenceClaim.Field.OBJECTIVE), confirmed(state, EvidenceClaim.Field.OWNER), "UNKNOWN", new String[0]);
    }
    private String confirmed(WorkspaceState state, EvidenceClaim.Field field) {
        var statement = breadcrumbs.continuity.CurrentAssessment.statement(state, field);
        if (statement != null) { return statement.value(); }
        for (var claim : breadcrumbs.continuity.CurrentAssessment.claims(state)) {
            if (claim.field() == field && claim.review() == EvidenceClaim.ReviewDisposition.CONFIRMED) { return claim.literalValue(); }
        }
        return "UNKNOWN";
    }
    public Checkpoint preview(WorkspaceState state, String changed, String blocker, String next, Options options) {
        String answer = blocker == null ? "" : blocker.strip();
        ProjectContext.BlockerState status = answer.isEmpty() || answer.equalsIgnoreCase("unknown") ? ProjectContext.BlockerState.UNKNOWN
                : answer.equalsIgnoreCase("none") || answer.equalsIgnoreCase("none_reported") ? ProjectContext.BlockerState.NONE_REPORTED : ProjectContext.BlockerState.BLOCKED;
        String title = changed == null || changed.isBlank() ? "Quick update" : changed.substring(0, Math.min(90, changed.length()));
        if (!title.isEmpty() && Character.isHighSurrogate(title.charAt(title.length() - 1))) { title = title.substring(0, title.length() - 1); }
        return new ProjectCheckpointService().preview(state, title, changed, options.objective(), status,
                status == ProjectContext.BlockerState.BLOCKED ? answer : "UNKNOWN", next, options.decisions(), options.owner(), options.evidenceIds());
    }
}
