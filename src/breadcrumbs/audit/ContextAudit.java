package breadcrumbs.audit;

import breadcrumbs.bootstrap.ClaimReview;
import breadcrumbs.continuity.*;
import breadcrumbs.domain.*;
import breadcrumbs.inspector.SnapshotEntry;
import java.time.Instant;
import java.util.Arrays;

/** Policy B4-1 measures documented continuity, never implementation quality or project completion. */
public final class ContextAudit {
    public static final String POLICY = "B4-1";
    public static final String DISCLAIMER = "This does not measure project completion or quality.";
    public enum State {
        EVIDENCED, PARTIAL, STALE, MISSING, CONFLICTING, UNKNOWN, UNSUPPORTED, NOT_APPLICABLE;
        public boolean assessable() { return ordinal() <= CONFLICTING.ordinal(); }
        public double factor() { return this == EVIDENCED ? 1 : this == PARTIAL ? .5 : this == STALE ? .25 : 0; }
    }
    public enum Readiness { INSUFFICIENT_EVIDENCE, REVIEW_CHANGES, NEEDS_CONTEXT, READY_TO_RESUME }
    private static final String[] IDS = {"CTX-PURPOSE", "CTX-DELIV", "CTX-STATE", "CTX-NEXT", "CTX-BLOCK",
            "CTX-DECISION", "CTX-OWNER", "CTX-EVIDENCE", "CTX-FRESH"};
    private static final int[] WEIGHTS = {12, 10, 13, 16, 11, 10, 10, 8, 10};
    private static final int[] DIMENSIONS = {0, 0, 0, 0, 2, 1, 2, 1, 3};
    private static final int[] DIMENSION_WEIGHTS = {40, 25, 20, 15};
    public record Finding(String id, int weight, int dimension, State state, String[] evidenceIds,
                          String explanation, String nextAction) {
        public Finding { evidenceIds = evidenceIds.clone(); }
        @Override public String[] evidenceIds() { return evidenceIds.clone(); }
    }
    public record Dimension(String name, double score, double coverage) { }
    public record Report(String policy, Instant evaluatedAt, String observationId, String checkpointId,
                         Finding[] findings, Dimension[] dimensions, double score, double coverage,
                         Readiness readiness, boolean noNativeHistory) {
        public Report { findings = findings.clone(); dimensions = dimensions.clone(); }
        @Override public Finding[] findings() { return findings.clone(); }
        @Override public Dimension[] dimensions() { return dimensions.clone(); }
    }

    public Report evaluate(WorkspaceState state, Checkpoint checkpoint) {
        EvidenceClaim[] claims = CurrentAssessment.claims(state);
        ConflictGroup[] conflicts = ClaimReview.conflicts(claims);
        ReferenceFreshness.Result freshness = new ReferenceFreshness().assess(state, checkpoint);
        EvidenceClaim.Field[][] fields = {
            {EvidenceClaim.Field.OBJECTIVE}, {EvidenceClaim.Field.DELIVERABLE},
            {EvidenceClaim.Field.CURRENT_STATE, EvidenceClaim.Field.PROGRESS, EvidenceClaim.Field.TASK_STATUS, EvidenceClaim.Field.TASK_MARKER},
            {EvidenceClaim.Field.NEXT_ACTION, EvidenceClaim.Field.TASK_MARKER}, {EvidenceClaim.Field.BLOCKERS},
            {EvidenceClaim.Field.DECISION, EvidenceClaim.Field.RATIONALE}, {EvidenceClaim.Field.OWNER}
        };
        Finding[] findings = new Finding[9];
        for (int i = 0; i < fields.length; i++) {
            String[] refs = new String[claims.length]; int count = 0;
            State result = State.UNKNOWN;
            String explanation = "No assessable statement. Unrecognized or missing headings do not prove absence.";
            boolean conflict = false;
            for (ConflictGroup group : conflicts) {
                if (contains(fields[i], group.field()) && !group.resolvedByReview()) { conflict = true; }
            }
            for (EvidenceClaim claim : claims) {
                if (!contains(fields[i], claim.field()) || claim.review() == EvidenceClaim.ReviewDisposition.REJECTED) { continue; }
                if (i == 3 && claim.field() == EvidenceClaim.Field.TASK_MARKER && !claim.comparisonValue().equals("UNCHECKED")) { continue; }
                refs[count++] = claim.evidenceId();
                if (!claim.literalValue().equalsIgnoreCase("UNKNOWN") && !claim.literalValue().isBlank()) {
                    result = claim.review() == EvidenceClaim.ReviewDisposition.CONFIRMED ? State.EVIDENCED
                            : result == State.EVIDENCED ? result : State.PARTIAL;
                    explanation = "Literal context only; source-reported outcomes remain independently unverified.";
                }
            }
            for (EvidenceClaim.Field field : fields[i]) {
                ContextReview statement = CurrentAssessment.statement(state, field);
                if (statement != null) {
                    result = statement.value().equals("NOT_DOCUMENTED") ? State.MISSING
                            : statement.value().equals("UNKNOWN") ? State.UNKNOWN : State.EVIDENCED;
                    explanation = "Explicit current human assessment at " + statement.reviewedAt() + "; does not verify past work.";
                }
            }
            if (i == 5 && result == State.EVIDENCED
                    && (!hasConfirmed(state, claims, EvidenceClaim.Field.DECISION)
                        || !hasConfirmed(state, claims, EvidenceClaim.Field.RATIONALE))) {
                result = State.PARTIAL;
                explanation = "Decision and rationale are separate documented requirements; one is not yet confirmed.";
            }
            if (count == 0 && result == State.UNKNOWN && onlyUnsupported(state)) {
                result = State.UNSUPPORTED; explanation = "Available content adapters cannot assess this field.";
            }
            // Human statements can add current context but cannot conceal an unresolved source conflict.
            if (conflict) { result = State.CONFLICTING; explanation = "Same explicit entity and field disagree; review every original locator."; }
            if (i == 6 && state.soloOwner()) { result = State.NOT_APPLICABLE; explanation = "Owner explicitly selected solo coordination applicability."; }
            findings[i] = finding(i, result, Arrays.copyOf(refs, count), explanation,
                    "Review " + IDS[i] + "; confirm, reject, skip or leave unknown.");
        }
        String[] evidence = new String[claims.length]; int evidenceCount = 0;
        for (EvidenceClaim c : claims) {
            if (!c.userSupplied() && c.review() != EvidenceClaim.ReviewDisposition.REJECTED
                    && !c.fingerprint().isEmpty() && c.line() > 0) { evidence[evidenceCount++] = c.evidenceId(); }
        }
        findings[7] = finding(7, evidenceCount > 0 ? State.EVIDENCED : onlyUnsupported(state) ? State.UNSUPPORTED : State.UNKNOWN,
                Arrays.copyOf(evidence, evidenceCount), "Checks usable provenance, not whether reported tests actually passed.", "Attach source-backed context if available.");
        State freshState = switch (freshness.state()) {
            case FRESH -> State.EVIDENCED; case STALE -> State.STALE;
            case UNKNOWN -> State.UNKNOWN; case NOT_APPLICABLE -> State.NOT_APPLICABLE;
        };
        String[] freshnessRefs = new String[freshness.references().length];
        for (int i = 0; i < freshnessRefs.length; i++) { freshnessRefs[i] = freshness.references()[i].evidenceId(); }
        findings[8] = finding(8, freshState, freshnessRefs,
                "Checkpoint-referenced comparison only. No prior native history is not a completion penalty.",
                "Request a consented read-only rescan; review changed references.");
        Report report = score(findings, state.currentObservationId(), checkpoint == null ? "" : checkpoint.checkpointId(),
                state.checkpoints().isEmpty(), freshness.state() == ReferenceFreshness.State.STALE);
        if (freshness.state() == ReferenceFreshness.State.UNKNOWN && checkpoint != null
                && report.readiness() == Readiness.READY_TO_RESUME) {
            return new Report(report.policy(), report.evaluatedAt(), report.observationId(), report.checkpointId(),
                    report.findings(), report.dimensions(), report.score(), report.coverage(), Readiness.NEEDS_CONTEXT, report.noNativeHistory());
        }
        return report;
    }

    /** Show the most consequential actionable gap before a numerical score. */
    public static String priority(Report report) {
        if (report.readiness() == Readiness.REVIEW_CHANGES) { return "CTX-FRESH: review changed checkpoint references before relying on recorded context."; }
        for (Finding finding : report.findings()) {
            if (finding.state() == State.CONFLICTING) { return finding.id() + ": review competing explicit source claims and original locators."; }
        }
        for (int index : new int[] {3, 2, 4, 0, 1, 5, 6, 7, 8}) {
            Finding finding = report.findings()[index];
            if (finding.state() != State.EVIDENCED && finding.state() != State.NOT_APPLICABLE) {
                return finding.id() + " (" + finding.state() + "): " + finding.nextAction();
            }
        }
        return "No critical unresolved context finding under policy " + report.policy() + ".";
    }

    private static boolean hasConfirmed(WorkspaceState state, EvidenceClaim[] claims, EvidenceClaim.Field field) {
        ContextReview statement = CurrentAssessment.statement(state, field);
        if (statement != null && !statement.value().equals("UNKNOWN") && !statement.value().equals("NOT_DOCUMENTED")) { return true; }
        for (EvidenceClaim claim : claims) {
            if (claim.field() == field && claim.review() == EvidenceClaim.ReviewDisposition.CONFIRMED
                    && !claim.literalValue().equals("UNKNOWN")) { return true; }
        }
        return false;
    }

    /** Pure scoring seam: formulas and every uncertainty state can be independently verified. */
    public Report score(Finding[] findings, String observation, String checkpoint, boolean noHistory, boolean criticalDrift) {
        if (findings.length != 9) { throw new IllegalArgumentException("All nine policy rules are required."); }
        double[] applicable = new double[4], assessable = new double[4], earned = new double[4];
        double allApplicable = 0, allAssessable = 0;
        boolean conflict = false, nextGap = false;
        for (int i = 0; i < findings.length; i++) {
            Finding f = findings[i];
            if (!f.id().equals(IDS[i]) || f.weight() != WEIGHTS[i] || f.dimension() != DIMENSIONS[i] || f.state() == null) {
                throw new IllegalArgumentException("Rule identity/weights/order must match B4-1.");
            }
            if (f.state() != State.NOT_APPLICABLE) { applicable[f.dimension()] += f.weight(); allApplicable += f.weight(); }
            if (f.state().assessable()) {
                assessable[f.dimension()] += f.weight(); allAssessable += f.weight(); earned[f.dimension()] += f.weight() * f.state().factor();
            }
            conflict |= f.state() == State.CONFLICTING;
            if (i == 8 && f.state() == State.STALE) { criticalDrift = true; }
            if (i == 3) { nextGap = f.state() != State.EVIDENCED; }
        }
        String[] names = {"Continuability", "Traceability", "Coordination", "Freshness"};
        Dimension[] dimensions = new Dimension[4]; double weighted = 0, usedWeights = 0;
        for (int d = 0; d < 4; d++) {
            double score = assessable[d] == 0 ? Double.NaN : 100 * earned[d] / assessable[d];
            double coverage = applicable[d] == 0 ? Double.NaN : 100 * assessable[d] / applicable[d];
            dimensions[d] = new Dimension(names[d], score, coverage);
            if (assessable[d] > 0) { weighted += score * DIMENSION_WEIGHTS[d]; usedWeights += DIMENSION_WEIGHTS[d]; }
        }
        double score = usedWeights == 0 ? Double.NaN : weighted / usedWeights;
        double coverage = allApplicable == 0 ? 0 : 100 * allAssessable / allApplicable;
        Readiness readiness = coverage < 60 ? Readiness.INSUFFICIENT_EVIDENCE : criticalDrift ? Readiness.REVIEW_CHANGES
                : conflict || nextGap || Double.isNaN(score) || score < 80 ? Readiness.NEEDS_CONTEXT : Readiness.READY_TO_RESUME;
        return new Report(POLICY, Instant.now(), observation, checkpoint, findings, dimensions, score, coverage, readiness, noHistory);
    }
    public static Finding finding(int index, State state, String[] evidence, String explanation, String next) {
        return new Finding(IDS[index], WEIGHTS[index], DIMENSIONS[index], state, evidence, explanation, next);
    }
    private static boolean contains(EvidenceClaim.Field[] fields, EvidenceClaim.Field field) {
        for (EvidenceClaim.Field candidate : fields) { if (candidate == field) { return true; } }
        return false;
    }
    private static boolean onlyUnsupported(WorkspaceState state) {
        boolean unsupported = false;
        for (SnapshotEntry entry : state.currentObservation().entries()) {
            if (entry.status() == SnapshotEntry.Status.SUPPORTED) { return false; }
            unsupported |= entry.status() == SnapshotEntry.Status.UNSUPPORTED;
        }
        return unsupported;
    }
}
