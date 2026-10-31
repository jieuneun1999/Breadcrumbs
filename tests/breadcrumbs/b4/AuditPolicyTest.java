package breadcrumbs.b4;

import breadcrumbs.audit.ContextAudit;
import breadcrumbs.audit.ContextAudit.*;
import static breadcrumbs.audit.ContextAudit.finding;
import breadcrumbs.bootstrap.*;
import breadcrumbs.domain.*;
import breadcrumbs.inspector.ScanPolicy;
import static breadcrumbs.b4.Checks.*;
import java.time.Instant;

public final class AuditPolicyTest {
    private AuditPolicyTest() { }
    public static void run() throws Exception {
        ContextAudit audit = new ContextAudit();
        Finding[] findings = all(State.EVIDENCED);
        Report report = audit.score(findings, "S", "CP", false, false);
        close(report.score(), 100, "all evidenced score"); close(report.coverage(), 100, "full coverage");
        require(report.readiness() == Readiness.READY_TO_RESUME, "ready threshold");
        for (State state : State.values()) {
            Report value = audit.score(all(state), "S", "", true, false);
            if (state.assessable()) {
                close(value.coverage(), 100, "assessable coverage " + state);
                close(value.score(), state.factor() * 100, "exact factor " + state);
            } else {
                require(Double.isNaN(value.score()), "unassessed score is not zero " + state);
                close(value.coverage(), 0, "unassessed coverage " + state);
            }
            require(value.readiness() != Readiness.READY_TO_RESUME || state == State.EVIDENCED, "uncertainty cannot become ready");
        }
        findings[8] = finding(8, State.NOT_APPLICABLE, new String[0], "", "");
        Report initial = audit.score(findings, "B0", "", true, false);
        close(initial.score(), 100, "N/A dimension reweighted"); close(initial.coverage(), 100, "N/A excluded");
        require(initial.noNativeHistory(), "history fact distinct");
        findings[0] = finding(0, State.PARTIAL, new String[0], "", "");
        Report weighted = audit.score(findings, "B0", "", true, false);
        close(weighted.dimensions()[0].score(), 100.0 * (51 - 6) / 51, "rule weight within dimension");
        close(weighted.score(), (weighted.dimensions()[0].score() * 40 + 100 * 45) / 85, "dimension renormalization");
        findings[6] = finding(6, State.UNKNOWN, new String[0], "", "");
        Report unknownOwner = audit.score(findings, "B0", "", true, false);
        close(unknownOwner.coverage(), 100.0 * 80 / 90, "UNKNOWN excluded from numerator, retained denominator");
        findings[6] = finding(6, State.NOT_APPLICABLE, new String[0], "", "");
        close(audit.score(findings, "B0", "", true, false).coverage(), 100, "N/A owner excluded denominator");
        findings = all(State.UNKNOWN); findings[0] = finding(0, State.EVIDENCED, new String[0], "", "");
        Report sparse = audit.score(findings, "B0", "", true, true);
        close(sparse.coverage(), 12, "overall coverage uses applicable rule weights");
        require(sparse.readiness() == Readiness.INSUFFICIENT_EVIDENCE, "coverage warning has precedence over drift");
        require(audit.score(all(State.EVIDENCED), "S", "CP", false, true).readiness() == Readiness.REVIEW_CHANGES, "critical drift blocks ready");
        findings = all(State.UNKNOWN);
        for (int i : new int[] {0, 2, 3, 4, 7}) { findings[i] = finding(i, State.EVIDENCED, new String[0], "", ""); }
        Report sixty = audit.score(findings, "S", "", true, false);
        close(sixty.coverage(), 60, "exact sixty percent coverage");
        require(sixty.readiness() != Readiness.INSUFFICIENT_EVIDENCE, "coverage threshold inclusive");
        findings = all(State.EVIDENCED);
        findings[4] = finding(4, State.MISSING, new String[0], "", "");
        findings[6] = finding(6, State.MISSING, new String[0], "", "");
        Report eighty = audit.score(findings, "S", "", true, false);
        close(eighty.score(), 80, "exact eighty score"); require(eighty.readiness() == Readiness.READY_TO_RESUME, "eighty threshold inclusive");
        findings[7] = finding(7, State.PARTIAL, new String[0], "", "");
        require(audit.score(findings, "S", "", true, false).readiness() == Readiness.NEEDS_CONTEXT, "below eighty requires context");
        findings = all(State.EVIDENCED); findings[3] = finding(3, State.UNKNOWN, new String[0], "", "");
        require(audit.score(findings, "S", "", false, false).readiness() == Readiness.NEEDS_CONTEXT, "next action gap despite high score");
        findings = all(State.EVIDENCED); findings[4] = finding(4, State.CONFLICTING, new String[0], "", "");
        require(audit.score(findings, "S", "", false, false).readiness() == Readiness.NEEDS_CONTEXT, "conflict despite high score");
        Finding[] invalid = all(State.EVIDENCED); invalid[0] = new Finding("WRONG", 12, 0, State.EVIDENCED, new String[0], "", "");
        reject(() -> audit.score(invalid, "S", "", true, false), "policy weights/identities are not caller-configurable");
        String[] refs = {"EV-test"}; Finding immutable = finding(0, State.EVIDENCED, refs, "", ""); refs[0] = "changed";
        require(immutable.evidenceIds()[0].equals("EV-test"), "finding inputs copied");
        var state = empty(); Report baseline = audit.evaluate(state, null);
        require(baseline.findings().length == 9 && baseline.findings()[8].state() == State.NOT_APPLICABLE, "nine rules; initial freshness N/A");
        require(baseline.findings()[6].state() == State.UNKNOWN, "academic profile doesn't imply solo");
        state.setSoloOwner(true); require(audit.evaluate(state, null).findings()[6].state() == State.NOT_APPLICABLE, "explicit solo owner N/A");
        state.setSoloOwner(false);
        state.addReview(new ContextReview(state.currentObservationId(), "", EvidenceClaim.Field.CURRENT_STATE, "", "NOT_DOCUMENTED",
                EvidenceClaim.ReviewDisposition.CONFIRMED, Instant.now()));
        require(audit.evaluate(state, null).findings()[2].state() == State.MISSING, "explicit confirmed documentation gap");
        require(audit.evaluate(state, null).findings()[4].state() == State.UNKNOWN, "unknown blocker is not none");
        state.addReview(new ContextReview(state.currentObservationId(), "", EvidenceClaim.Field.BLOCKERS, "", "None reported",
                EvidenceClaim.ReviewDisposition.CONFIRMED, Instant.now()));
        require(audit.evaluate(state, null).findings()[4].state() == State.EVIDENCED, "explicit none remains a documented statement");
        state.addReview(new ContextReview(state.currentObservationId(), "", EvidenceClaim.Field.DECISION, "", "Use a small scope",
                EvidenceClaim.ReviewDisposition.CONFIRMED, Instant.now()));
        require(audit.evaluate(state, null).findings()[5].state() == State.PARTIAL, "decision without rationale only partial");
        state.addReview(new ContextReview(state.currentObservationId(), "", EvidenceClaim.Field.RATIONALE, "", "Fit available study time",
                EvidenceClaim.ReviewDisposition.CONFIRMED, Instant.now()));
        require(audit.evaluate(state, null).findings()[5].state() == State.EVIDENCED, "decision and rationale evidenced");
        state.addReview(new ContextReview(state.currentObservationId(), "", EvidenceClaim.Field.RATIONALE, "", "Fit available study time",
                EvidenceClaim.ReviewDisposition.REJECTED, Instant.now()));
        require(audit.evaluate(state, null).findings()[5].state() == State.PARTIAL, "later rejection revokes present statement without erasing history");
        System.out.println("PASS: B4 audit policy and scoring");
    }
    static WorkspaceState empty() throws Exception {
        var bootstrap = new BootstrapService(ScanPolicy.defaults(), () -> "W401");
        var draft = bootstrap.prepare(EntryMode.NEW_EMPTY, "Synthetic audit", ProjectProfile.ACADEMIC, null,
                new CurrentContext("Study", "Report", "Review literature", "", ""), false, count -> true);
        WorkspaceState state = new WorkspaceState(); bootstrap.confirm(state, draft, true); return state;
    }
    private static Finding[] all(State state) {
        Finding[] result = new Finding[9];
        for (int i = 0; i < result.length; i++) { result[i] = finding(i, state, new String[0], "Test", "Review"); }
        return result;
    }
}
