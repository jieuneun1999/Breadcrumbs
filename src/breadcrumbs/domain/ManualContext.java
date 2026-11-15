package breadcrumbs.domain;

import java.time.Instant;
import java.util.Arrays;
import breadcrumbs.inspector.ContentSafety;

public final class ManualContext {
    public enum Type { PERSONAL_OBSERVATION, CURRENT_STATE, REPORTED_STATEMENT, DECISION, NEXT_ACTION_CANDIDATE }
    public enum Sensitivity { INTERNAL, SENSITIVE }
    public record Capture(String workspaceId, String id, Type type, String statement, String origin, String reference, Instant recordedAt, Sensitivity sensitivity) {
        public Capture {
            if (workspaceId == null || !workspaceId.matches("W(?:[0-9]{3,9}|-[a-f0-9]{32})") || id == null || !id.matches("IN-[0-9]{6}")
                    || id.equals("IN-000000") || type == null || recordedAt == null || sensitivity == null || statement == null || statement.isBlank()) {
                throw new IllegalArgumentException("Complete manual origin/identity and a literal statement are required.");
            }
            statement = safe(statement, 4000); origin = safe(origin, 512); reference = safe(reference, 512);
        }
    }
    public record Review(String captureId, EvidenceClaim.ReviewDisposition disposition, boolean indexApproved, boolean exportApproved, Instant reviewedAt) {
        public Review {
            if (captureId == null || disposition == null || disposition == EvidenceClaim.ReviewDisposition.PENDING || reviewedAt == null
                    || disposition != EvidenceClaim.ReviewDisposition.CONFIRMED && (indexApproved || exportApproved)) { throw new IllegalArgumentException("Invalid manual review or permission state."); }
        }
    }
    private final WorkspaceState state;
    private Capture[] captures = new Capture[0];
    private Review[] reviews = new Review[0];
    private int nextNumber = 1;
    private boolean artifactsAllowed;
    public ManualContext(WorkspaceState state) { this.state = state; }
    private static String safe(String text, int maximum) {
        String result = ProjectContext.note(text);
        if (result.length() > maximum || ContentSafety.hasSecretMarker(java.text.Normalizer.normalize(result, java.text.Normalizer.Form.NFKC))) {
            throw new IllegalArgumentException("Manual field exceeds its bound or resembles credentials. Do not capture secrets.");
        }
        return result;
    }
    private Instant now() {
        Instant time = Instant.now();
        if (time.isBefore(state.activeBaseline().confirmedAt())) { time = state.activeBaseline().confirmedAt(); }
        if (captures.length > 0 && time.isBefore(captures[captures.length - 1].recordedAt())) { time = captures[captures.length - 1].recordedAt(); }
        if (reviews.length > 0 && time.isBefore(reviews[reviews.length - 1].reviewedAt())) { time = reviews[reviews.length - 1].reviewedAt(); }
        return time;
    }
    public Capture capture(Type type, String statement, String origin, String reference, Sensitivity sensitivity) {
        if (state.activeBaseline() == null) { throw new IllegalArgumentException("Confirm B0 before capturing manual context."); }
        Capture capture = new Capture(state.activeBaseline().workspaceId(), String.format(java.util.Locale.ROOT, "IN-%06d", nextNumber), type, statement, origin, reference, now(), sensitivity);
        restore(capture); return capture;
    }
    public Capture[] captures() { return captures.clone(); }
    public Review[] reviews() { return reviews.clone(); }
    public Capture find(String id) { for (Capture capture : captures) { if (capture.id().equals(id)) { return capture; } } return null; }
    public Review effectiveReview(String id) { Review result = null; for (Review review : reviews) { if (review.captureId().equals(id)) { result = review; } } return result; }
    public EvidenceClaim.ReviewDisposition disposition(String id) {
        Review review = effectiveReview(id); return review == null ? EvidenceClaim.ReviewDisposition.PENDING : review.disposition();
    }
    public void review(String id, EvidenceClaim.ReviewDisposition disposition, boolean index, boolean export) { restore(new Review(id, disposition, index, export, now())); }
    public void restore(Capture capture) {
        if (captures.length >= 500 || !capture.workspaceId().equals(state.activeBaseline().workspaceId())
                || !capture.id().equals(String.format(java.util.Locale.ROOT, "IN-%06d", nextNumber))
                || capture.recordedAt().isBefore(state.activeBaseline().confirmedAt())
                || captures.length > 0 && capture.recordedAt().isBefore(captures[captures.length - 1].recordedAt())) { throw new IllegalArgumentException("Invalid manual capture scope, sequence, time or capacity."); }
        captures = Arrays.copyOf(captures, captures.length + 1); captures[captures.length - 1] = capture; nextNumber++; state.enableVersionFour();
    }
    public void restore(Review review) {
        Capture capture = find(review.captureId());
        if (reviews.length >= 2000 || capture == null || review.reviewedAt().isBefore(capture.recordedAt())
                || reviews.length > 0 && review.reviewedAt().isBefore(reviews[reviews.length - 1].reviewedAt())
                || capture.sensitivity() == Sensitivity.SENSITIVE && (review.indexApproved() || review.exportApproved())) { throw new IllegalArgumentException("Invalid manual review scope/time, sensitive permission or capacity."); }
        reviews = Arrays.copyOf(reviews, reviews.length + 1); reviews[reviews.length - 1] = review; state.enableVersionFour();
    }
    public boolean indexEligible(String id) {
        Capture capture = find(id); Review review = effectiveReview(id);
        return capture != null && capture.sensitivity() == Sensitivity.INTERNAL && review != null
                && review.disposition() == EvidenceClaim.ReviewDisposition.CONFIRMED
                && (capture.type() != Type.REPORTED_STATEMENT || review.indexApproved());
    }
    public boolean exportEligible(String id) {
        Capture capture = find(id); Review review = effectiveReview(id);
        return capture != null && capture.sensitivity() == Sensitivity.INTERNAL && review != null
                && review.disposition() == EvidenceClaim.ReviewDisposition.CONFIRMED && review.exportApproved();
    }
    public int nextNumber() { return nextNumber; }
    public void restoreNextNumber(int number) { if (number < nextNumber || number > 1000000) { throw new IllegalArgumentException("Invalid manual ID high-water counter."); } nextNumber = number; }
    public boolean artifactsAllowed() { return artifactsAllowed; }
    public void setArtifactsAllowed(boolean allowed) { if (artifactsAllowed != allowed) { artifactsAllowed = allowed; state.enableVersionFour(); } }
    public void copyFrom(ManualContext other) { captures = other.captures.clone(); reviews = other.reviews.clone(); nextNumber = other.nextNumber; artifactsAllowed = other.artifactsAllowed; }
    public void validateReferences(String[] ids, Instant time) {
        for (String id : ids) { Capture capture = find(id); if (capture == null || capture.recordedAt().isAfter(time)) { throw new IllegalArgumentException("Manual checkpoint reference is unresolved or later than the checkpoint."); } }
    }
}
