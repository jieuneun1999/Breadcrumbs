package breadcrumbs.continuity;

import breadcrumbs.domain.*;
import breadcrumbs.inspector.*;
import java.io.IOException;
import java.util.Locale;
import java.util.function.IntPredicate;

/** Reuses the B2 read-only inspector; consent is checked before any source access. */
public final class ObservationService {
    public ScanResult scan(WorkspaceState state, boolean approved, IntPredicate continueScanning) throws IOException {
        if (!approved) { throw new IllegalArgumentException("No rescan consent. Saved observations retained."); }
        if (state.activeBaseline() == null || state.currentObservation().root() == null
                || state.laterObservations().length >= 20) {
            throw new IllegalArgumentException("A saved local source root and observation capacity are required.");
        }
        String number = String.format(Locale.ROOT, "%04d", state.laterObservations().length + 1);
        String workspace = state.activeBaseline().workspaceId();
        ScanResult raw = new ProjectInspector().inspect(state.activeBaseline().snapshot().root(), workspace,
                state.currentObservation().policy(), continueScanning);
        EvidenceClaim[] claims = raw.claims();
        for (int i = 0; i < claims.length; i++) {
            EvidenceClaim c = claims[i];
            // A scan-scoped ID prevents a later parser result from retargeting an old checkpoint.
            claims[i] = new EvidenceClaim(c.workspaceId(), "EV-" + workspace + "-O" + number
                    + String.format(Locale.ROOT, "-%06d", i + 1), c.field(), c.entityId(), c.literalValue(),
                    c.comparisonValue(), c.relativePath(), c.line(), c.section(), c.fingerprint(),
                    c.artifactRole(), c.observedAt(), c.sourceStatedDate(), c.sourceKind(), c.provenance(),
                    c.verification(), c.review());
        }
        ScanResult later = new ScanResult(raw.root(), "OBS-" + workspace + "-" + number, raw.observedAt(),
                raw.policy(), raw.entries(), claims, raw.diagnostics(), raw.complete(), raw.bytesRead());
        state.addObservation(later);
        return later;
    }
}
