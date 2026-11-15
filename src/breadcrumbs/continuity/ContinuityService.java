package breadcrumbs.continuity;

import breadcrumbs.domain.*;
import breadcrumbs.audit.ContextAudit;
import breadcrumbs.inspector.ContentSafety;

public final class ContinuityService {
    public String brief(WorkspaceState state, boolean persisted) {
        Checkpoint last = state.checkpoints().getLatest();
        StringBuilder out = new StringBuilder("FOLLOW THE BREADCRUMBS\n");
        out.append("Project: ").append(clean(state.activeBaseline().displayName())).append(" | ").append(state.activeBaseline().workspaceId()).append('\n');
        out.append(persisted ? state.dirty() ? "UNSAVED changes. Retry Save before leaving.\n" : "Saved canonical context.\n" : "SESSION-ONLY. Unsaved context will not survive exit.\n");
        if (last == null) {
            out.append("B0 ONLY. No native checkpoint; not a historical resume.\nWHERE YOU LEFT OFF\nNo prior native history.\n");
            out.append("Objective: ").append(present(state, EvidenceClaim.Field.OBJECTIVE)).append("\nNEXT ACTION\n").append(present(state, EvidenceClaim.Field.NEXT_ACTION));
            out.append("\nBLOCKERS\n").append(present(state, EvidenceClaim.Field.BLOCKERS)).append('\n');
        } else {
            ProjectContext context = last.project();
            out.append("Latest checkpoint: ").append(last.checkpointId()).append(" at ").append(last.createdAt()).append("\nWHERE YOU LEFT OFF\n")
                    .append(clean(last.summary())).append("\nUSER_CONFIRMED historical present assessment; past outcomes are not independently verified.\n")
                    .append("NEXT ACTION\n").append(clean(context.nextActions())).append("\nBLOCKERS\n").append(context.blockerState()).append(" | ").append(clean(context.blockers()))
                    .append("\nDECISIONS / RATIONALE\n").append(clean(context.decisions())).append("\nEVIDENCE\n");
            int count = 0;
            for (String id : context.evidenceIds()) {
                if (count++ == 10) { out.append("Further references omitted; open the full handoff.\n"); break; }
                var claim = state.evidence(context.snapshotId(), id);
                out.append(id).append(" | ").append(clean(claim.relativePath())).append(' ').append(claim.locationUnit()).append(' ').append(claim.line()).append(" | ").append(claim.sourceKind()).append(" / ").append(claim.verification()).append('\n');
            }
            if (count == 0) { out.append("No source evidence linked. This remains a human assessment.\n"); }
            for (String id : context.manualEvidenceIds()) {
                var capture = state.manualContext().find(id);
                out.append(id).append(" | MANUAL ").append(capture.type()).append(capture.type() == ManualContext.Type.REPORTED_STATEMENT ? " | SOURCE_REPORTED, independently UNVERIFIED\n" : " | USER_DECLARED, not independently verified\n");
            }
        }
        out.append("Saved observation: ").append(state.currentObservationId()).append(" at ").append(state.currentObservation().observedAt()).append('\n');
        out.append("Live source freshness UNKNOWN until a consented rescan. Opening did not read the source folder.\n");
        out.append("Referenced saved-observation freshness: ").append(new ReferenceFreshness().assess(state, last).state()).append('\n');
        out.append("NEEDS ATTENTION\n").append(ContextAudit.priority(new ContextAudit().evaluate(state, last))).append('\n');
        out.append("Unsupported/unextracted document portions remain unknown. Source reports do not verify project progress.\n");
        return out.toString().replace("\nWHERE YOU LEFT OFF", "\n\nWHERE YOU LEFT OFF")
                .replace("\nNEXT ACTION", "\n\nNEXT ACTION").replace("\nBLOCKERS", "\n\nBLOCKERS")
                .replace("\nDECISIONS / RATIONALE", "\n\nDECISIONS / RATIONALE").replace("\nEVIDENCE", "\n\nEVIDENCE")
                .replace("\nNEEDS ATTENTION", "\n\nNEEDS ATTENTION");
    }
    private static String present(WorkspaceState state, EvidenceClaim.Field field) {
        ContextReview review = CurrentAssessment.statement(state, field);
        if (review != null) { return "USER_CONFIRMED current assessment: " + clean(review.value()); }
        for (var claim : CurrentAssessment.claims(state)) {
            if (claim.field() == field && claim.review() == EvidenceClaim.ReviewDisposition.CONFIRMED) {
                return clean(claim.literalValue()) + " | " + claim.sourceKind() + " / " + claim.verification();
            }
        }
        return "UNKNOWN. Review the captured evidence before adopting a value.";
    }
    public String health(WorkspaceState state) {
        var report = new ContextAudit().evaluate(state, state.checkpoints().getLatest());
        StringBuilder out = new StringBuilder("CONTINUITY HEALTH\n").append(report.policy()).append(" | ").append(report.readiness()).append("\nKnown:\n");
        for (var finding : report.findings()) {
            if (finding.state() == ContextAudit.State.EVIDENCED) { out.append(finding.id()).append(" | ").append(clean(finding.explanation())).append('\n'); }
        }
        out.append("Needs review:\n"); int count = 0;
        for (var finding : report.findings()) {
            if (finding.state() == ContextAudit.State.EVIDENCED || finding.state() == ContextAudit.State.NOT_APPLICABLE) { continue; }
            if (count++ == 2) { break; }
            out.append(finding.id()).append(" | ").append(finding.state()).append(" | ").append(clean(finding.explanation())).append("\nNext question: ").append(clean(finding.nextAction())).append('\n');
        }
        out.append("Best next review: ").append(ContextAudit.priority(report)).append('\n');
        out.append("Assessable context coverage: ").append(String.format(java.util.Locale.ROOT, "%.0f%%", report.coverage())).append(". UNKNOWN is not MISSING.\n").append(ContextAudit.DISCLAIMER).append('\n');
        return out.toString();
    }
    public String changes(WorkspaceState state) {
        var before = state.activeBaseline().snapshot(); var after = state.currentObservation();
        StringBuilder out = new StringBuilder("GUIDED CHANGE REVIEW\nSaved observation: ").append(state.currentObservationId()).append('\n');
        if (before == after) { return out.append("No later saved observation. Live freshness UNKNOWN; rescan requires explicit consent.\n").toString(); }
        int displayed = 0;
        for (var change : new DriftEngine().compare(before, after).changes()) {
            if (change.status() == DriftEngine.Status.UNCHANGED) { continue; }
            if (displayed++ == 20) { out.append("Further changes omitted. Use detailed B4 comparisons.\n"); break; }
            out.append(change.status()).append(" | ").append(clean(change.oldPath())).append(" -> ").append(clean(change.newPath())).append('\n');
            var old = DriftEngine.find(before.entries(), change.oldPath()); var now = DriftEngine.find(after.entries(), change.newPath());
            out.append("Original SHA256: ").append(old == null || old.sha256().isEmpty() ? "UNKNOWN" : old.sha256()).append("\nObserved SHA256: ")
                    .append(now == null || now.sha256().isEmpty() ? "UNKNOWN" : now.sha256()).append('\n');
            out.append(clean(change.reason())).append('\n');
        }
        out.append("Checkpoint-specific evidence impact, latest 20 checkpoints at most:\n"); int count = 0;
        for (var checkpoint : state.checkpoints().traverseBackward()) {
            if (count++ == 20) { out.append("Older checkpoints omitted; select their original handoff to inspect.\n"); break; }
            var freshness = new ReferenceFreshness().assess(state, checkpoint);
            out.append(checkpoint.checkpointId()).append(" | ").append(freshness.state()).append('\n');
            int refs = 0;
            for (var reference : freshness.references()) {
                if (refs++ == 10) { out.append("Further reference rows omitted.\n"); break; }
                out.append(reference.evidenceId()).append(" | ").append(clean(reference.path())).append(" | ").append(reference.state()).append('\n');
            }
        }
        out.append("Unrelated file changes do not automatically stale a checkpoint. Acknowledgement means reviewed, never verified completion.\n");
        return out.toString();
    }
    private static String clean(String value) { return ContentSafety.terminal(value, 600); }
}
