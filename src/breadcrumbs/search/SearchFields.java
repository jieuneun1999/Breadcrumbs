package breadcrumbs.search;

import breadcrumbs.domain.*;
import breadcrumbs.inspector.*;
import java.util.function.Consumer;

/** Shared eligibility and canonical-resolution boundary for builds and queries. */
final class SearchFields {
    record Field(SearchLocation location, String value) { }
    private SearchFields() { }
    static void visit(WorkspaceState state, SearchPolicy policy, Consumer<Field> accept) {
        String observation = state.currentObservationId();
        for (EvidenceClaim claim : state.claimsAt(observation)) { visitEvidence(state, policy, claim, accept); }
        if (policy.contentVersion().equals("CAPTURED-2")) {
            for (var capture : state.manualContext().captures()) { emit(manual(state, capture), accept); }
        }
        for (Checkpoint cp : state.checkpoints().traverseForward()) {
            for (String field : new String[] {"TITLE", "SUMMARY", "OBJECTIVE", "NEXT_ACTIONS", "DECISIONS", "BLOCKERS"}) {
                emit(checkpoint(state, cp, field), accept);
            }
        }
        if (state.dependencies().current() != null) {
            for (WorkItem item : state.dependencies().current().items()) {
                for (String field : new String[] {"TITLE", "DESCRIPTION", "STATUS"}) { emit(workItem(state, item, field), accept); }
            }
        }
    }
    private static void emit(Field field, Consumer<Field> accept) { if (field != null) { accept.accept(field); } }
    static void visitEvidence(WorkspaceState state, SearchPolicy policy, EvidenceClaim claim, Consumer<Field> accept) {
        emit(evidence(state, policy, claim), accept);
    }
    static Field resolve(WorkspaceState state, SearchPolicy policy, SearchLocation location) {
        if (state.activeBaseline() == null || !state.activeBaseline().workspaceId().equals(location.workspaceId())) { return null; }
        Field field = switch (location.kind()) {
            case MANUAL -> policy.contentVersion().equals("CAPTURED-2") ? manual(state, state.indexes().manual(location.recordId())) : null;
            case EVIDENCE -> {
                if (!location.observationId().equals(state.currentObservationId())) { yield null; }
                yield evidence(state, policy, state.indexes().evidence(location.observationId(), location.recordId()));
            }
            case CHECKPOINT -> checkpoint(state, state.indexes().checkpoint(location.recordId()), location.field());
            case WORK_ITEM -> workItem(state, state.indexes().workItem(location.recordId()), location.field());
        };
        return field != null && field.location().equals(location) ? field : null;
    }
    private static Field evidence(WorkspaceState state, SearchPolicy policy, EvidenceClaim claim) {
        if (claim == null || !claim.workspaceId().equals(state.activeBaseline().workspaceId())
                || claim.artifactRole() == EvidenceClaim.ArtifactRole.INSTRUCTIONS || claim.field() == EvidenceClaim.Field.OWNER
                || !safePath(claim.relativePath()) || claim.verification() == EvidenceClaim.Verification.UNREADABLE) { return null; }
        String observation = state.currentObservationId();
        var review = claim.review();
        for (ContextReview amendment : state.reviews()) {
            if (amendment.observationId().equals(observation) && amendment.evidenceId().equals(claim.evidenceId())) { review = amendment.disposition(); }
        }
        if (review == EvidenceClaim.ReviewDisposition.REJECTED
                || !policy.includeUnreviewed() && review != EvidenceClaim.ReviewDisposition.CONFIRMED) { return null; }
        if (!claim.userSupplied()) {
            SnapshotEntry entry = state.indexes().path(observation, claim.relativePath());
            if (entry == null || entry.status() != SnapshotEntry.Status.SUPPORTED
                    || entry.adapter() != SnapshotEntry.Adapter.TXT && entry.adapter() != SnapshotEntry.Adapter.MARKDOWN && entry.adapter() != SnapshotEntry.Adapter.CSV
                    || !entry.sha256().equals(claim.fingerprint()) || claim.line() < 1) { return null; }
        }
        String locator = claim.relativePath() + ", line " + claim.line() + (claim.section().isEmpty() ? "" : ", " + claim.section());
        return field(new SearchLocation(claim.workspaceId(), SearchLocation.Kind.EVIDENCE, claim.evidenceId(),
                claim.field().name(), observation, locator, claim.sourceKind() + " / " + claim.provenance() + " / " + claim.verification(),
                review.name(), "INTERNAL", claim.observedAt(), false), claim.literalValue());
    }
    private static Field manual(WorkspaceState state, ManualContext.Capture capture) {
        if (capture == null || !capture.workspaceId().equals(state.activeBaseline().workspaceId()) || !state.manualContext().indexEligible(capture.id())) { return null; }
        String truth = capture.type() == ManualContext.Type.REPORTED_STATEMENT ? "SOURCE_REPORTED / MANUAL_RECORDED / UNVERIFIED"
                : "USER_DECLARED / MANUAL_RECORDED / not independently verified";
        return field(new SearchLocation(capture.workspaceId(), SearchLocation.Kind.MANUAL, capture.id(), "STATEMENT", "MANUAL",
                "manual origin: " + capture.origin() + " | declared reference: " + capture.reference(), truth, "CONFIRMED", capture.sensitivity().name(), capture.recordedAt(), false), capture.statement());
    }
    private static Field checkpoint(WorkspaceState state, Checkpoint cp, String field) {
        if (cp == null || cp.origin() != Checkpoint.Origin.USER_CONFIRMED || cp.project() == null
                || !cp.project().workspaceId().equals(state.activeBaseline().workspaceId())
                || state.observation(cp.project().snapshotId()) == null) { return null; }
        String value = switch (field) {
            case "TITLE" -> cp.title(); case "SUMMARY" -> cp.summary(); case "OBJECTIVE" -> cp.project().objective();
            case "NEXT_ACTIONS" -> cp.project().nextActions(); case "DECISIONS" -> cp.project().decisions();
            case "BLOCKERS" -> cp.project().blockers(); default -> null;
        };
        return field(new SearchLocation(cp.project().workspaceId(), SearchLocation.Kind.CHECKPOINT, cp.checkpointId(), field,
                cp.project().snapshotId(), "checkpoint field " + field, "USER_CONFIRMED present assessment, past work not independently verified",
                "CONFIRMED", "INTERNAL", cp.createdAt(), true), value);
    }
    private static Field workItem(WorkspaceState state, WorkItem item, String field) {
        if (item == null || !item.workspaceId().equals(state.activeBaseline().workspaceId()) || state.observation(item.observationId()) == null) { return null; }
        String value = switch (field) {
            case "TITLE" -> item.title(); case "DESCRIPTION" -> item.description(); case "STATUS" -> item.status().name(); default -> null;
        };
        return field(new SearchLocation(item.workspaceId(), SearchLocation.Kind.WORK_ITEM, item.id(), field,
                item.observationId(), "current graph " + state.dependencies().currentId() + ", field " + field,
                "USER_CONFIRMED present assessment, not independently verified", "CONFIRMED", "INTERNAL", item.updatedAt(), false), value);
    }
    private static Field field(SearchLocation location, String value) {
        if (value == null || value.isBlank() || value.equals("UNKNOWN") || ContentSafety.hasSecretMarker(value)
                || ContentSafety.hasSecretMarker(location.locator())
                || ContentSafety.hasSecretMarker(java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFKC))) { return null; }
        try { Tokenizer.checkUnicode(value); Tokenizer.checkUnicode(location.locator()); }
        catch (IllegalArgumentException unsafe) { return null; }
        return new Field(location, value);
    }
    private static boolean safePath(String path) {
        if (path == null || path.length() > 4096 || path.startsWith("/") || path.contains(":")) { return false; }
        for (String part : path.replace('\\', '/').split("/", -1)) {
            if (part.equals("..") || ContentSafety.excludedName(part)
                    || part.equalsIgnoreCase("AGENTS.md") || part.equalsIgnoreCase("CLAUDE.md")) { return false; }
        }
        return true;
    }
}
