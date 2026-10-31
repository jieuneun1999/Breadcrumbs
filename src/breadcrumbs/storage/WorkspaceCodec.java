package breadcrumbs.storage;

import breadcrumbs.bootstrap.ClaimReview;
import breadcrumbs.domain.*;
import breadcrumbs.inspector.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/** Version 1 remains byte-compatible. Version 2 adds immutable observations and current reviews.
 * Both use ordered Base64 UTF-8 fields and exact-byte checksums. Validation precedes activation.
 */
public final class WorkspaceCodec {
    public static final int MAX_BYTES = 8 * 1024 * 1024;
    private static final String HEADER = "FORMAT_VERSION=1\n";
    private final StringBuilder output = new StringBuilder();
    private boolean versionTwo;
    private String[] lines;
    private int cursor;
    private int fieldCount;

    public static byte[] encode(WorkspaceState state) throws IOException {
        try {
            validate(state);
            WorkspaceCodec codec = new WorkspaceCodec();
            codec.versionTwo = state.versionTwo();
            codec.output.append(codec.versionTwo ? "FORMAT_VERSION=2\n" : HEADER);
            codec.write(state);
            byte[] body = codec.output.toString().getBytes(StandardCharsets.UTF_8);
            byte[] bytes = (codec.output + "CHECKSUM=" + digest(body) + "\n").getBytes(StandardCharsets.UTF_8);
            if (bytes.length > MAX_BYTES) { throw new IllegalArgumentException(); }
            return bytes;
        } catch (RuntimeException invalid) { throw new IOException("Workspace data is invalid or exceeds storage limits."); }
    }

    public static WorkspaceState decode(byte[] bytes) throws IOException {
        try {
            if (bytes.length == 0 || bytes.length > MAX_BYTES) { throw new IllegalArgumentException(); }
            String text = utf8(bytes);
            boolean versionTwo = text.startsWith("FORMAT_VERSION=2\n");
            if (!text.startsWith(HEADER) && !versionTwo) { throw new IOException("Unsupported storage version; expected 1 or 2."); }
            int footer = text.lastIndexOf("CHECKSUM=");
            if (footer < HEADER.length() || !text.endsWith("\n")
                    || !text.substring(footer).equals("CHECKSUM="
                    + digest(text.substring(0, footer).getBytes(StandardCharsets.UTF_8)) + "\n")) {
                throw new IOException("Workspace checksum/length mismatch. Keep the file for manual recovery.");
            }
            WorkspaceCodec codec = new WorkspaceCodec();
            codec.versionTwo = versionTwo;
            int lineCount = 0;
            for (int i = 0; i < footer; i++) {
                if (text.charAt(i) == '\n' && ++lineCount > 200000) { throw new IllegalArgumentException(); }
            }
            codec.lines = text.substring(HEADER.length(), footer).split("\n", -1);
            WorkspaceState result = codec.read();
            if (codec.cursor != codec.lines.length - 1 || !codec.lines[codec.cursor].isEmpty()) {
                throw new IllegalArgumentException();
            }
            validate(result);
            result.markSaved();
            return result;
        } catch (RuntimeException invalid) { throw new IOException("Invalid workspace fields, counts or references."); }
    }

    public static String digest(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    private static String utf8(byte[] bytes) throws IOException {
        return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
    }

    private void put(String key, Object value) {
        String text = value.toString();
        if (text.length() > 8192 || output.length() > MAX_BYTES || ++fieldCount > 199999) { throw new IllegalArgumentException(); }
        output.append(key).append('=').append(Base64.getEncoder()
                .encodeToString(text.getBytes(StandardCharsets.UTF_8))).append('\n');
    }

    private String get(String key) throws IOException {
        if (cursor >= lines.length || !lines[cursor].startsWith(key + "=")) { throw new IllegalArgumentException(); }
        String encoded = lines[cursor++].substring(key.length() + 1);
        if (encoded.length() > 44000) { throw new IllegalArgumentException(); }
        byte[] decoded = Base64.getDecoder().decode(encoded);
        if (!Base64.getEncoder().encodeToString(decoded).equals(encoded)) { throw new IllegalArgumentException(); }
        String value = utf8(decoded);
        if (value.length() > 8192) { throw new IllegalArgumentException(); }
        return value;
    }

    private long number(String key, long min, long max) throws IOException {
        String value = get(key);
        long result = Long.parseLong(value);
        if (!Long.toString(result).equals(value) || result < min || result > max) { throw new IllegalArgumentException(); }
        return result;
    }
    private int count(String key, int max) throws IOException { return (int) number(key, 0, max); }
    private Instant time(String key) throws IOException { return Instant.parse(get(key)); }
    private boolean flag(String key) throws IOException {
        return switch (get(key)) { case "true" -> true; case "false" -> false; default -> throw new IllegalArgumentException(); };
    }
    private <E extends Enum<E>> E choice(String key, Class<E> type) throws IOException { return Enum.valueOf(type, get(key)); }

    private void claims(EvidenceClaim[] claims) {
        put("claims", claims.length);
        for (EvidenceClaim claim : claims) {
            put("workspace", claim.workspaceId());
            put("evidence", claim.evidenceId());
            put("field", claim.field());
            put("entity", claim.entityId());
            put("literal", claim.literalValue());
            put("comparison", claim.comparisonValue());
            put("path", claim.relativePath());
            put("line", claim.line());
            put("section", claim.section());
            put("fingerprint", claim.fingerprint());
            put("role", claim.artifactRole());
            put("observed", claim.observedAt());
            put("assertedDate", claim.sourceStatedDate());
            put("kind", claim.sourceKind());
            put("provenance", claim.provenance());
            put("verification", claim.verification());
            put("review", claim.review());
        }
    }

    private EvidenceClaim[] claims() throws IOException {
        EvidenceClaim[] claims = new EvidenceClaim[count("claims", 2000)];
        for (int i = 0; i < claims.length; i++) {
            claims[i] = new EvidenceClaim(get("workspace"), get("evidence"), choice("field", EvidenceClaim.Field.class),
                    get("entity"), get("literal"), get("comparison"), get("path"), count("line", 1000000),
                    get("section"), get("fingerprint"), choice("role", EvidenceClaim.ArtifactRole.class), time("observed"),
                    get("assertedDate"), choice("kind", EvidenceClaim.SourceKind.class),
                    choice("provenance", EvidenceClaim.Provenance.class), choice("verification", EvidenceClaim.Verification.class),
                    choice("review", EvidenceClaim.ReviewDisposition.class));
        }
        return claims;
    }

    private void write(WorkspaceState state) {
        BaselineB0 baseline = state.activeBaseline();
        ScanResult snapshot = baseline.snapshot();
        put("workspace", baseline.workspaceId());
        put("name", baseline.displayName());
        put("profile", baseline.profile());
        put("mode", baseline.entryMode());
        put("confirmed", baseline.confirmedAt());
        snapshot(snapshot); claims(baseline.claims());
        if (versionTwo) {
            put("observations", state.laterObservations().length);
            for (ScanResult later : state.laterObservations()) { snapshot(later); }
            put("soloOwner", state.soloOwner());
            put("reviews", state.reviews().length);
            for (ContextReview review : state.reviews()) {
                put("observation", review.observationId()); put("evidence", review.evidenceId());
                put("field", review.field()); put("entity", review.entityId()); put("value", review.value());
                put("disposition", review.disposition()); put("reviewed", review.reviewedAt());
            }
        }
        put("nextCheckpoint", state.nextCheckpointNumber());
        put("checkpoints", state.checkpoints().size());
        for (Checkpoint checkpoint : state.checkpoints().traverseForward()) {
            put("id", checkpoint.checkpointId());
            put("title", checkpoint.title());
            put("summary", checkpoint.summary());
            put("status", checkpoint.status());
            put("created", checkpoint.createdAt());
            put("origin", checkpoint.origin());
            ProjectContext context = checkpoint.project();
            put("workspace", context.workspaceId());
            put("snapshot", context.snapshotId());
            put("objective", context.objective());
            put("blockerState", context.blockerState());
            put("blockers", context.blockers());
            put("next", context.nextActions());
            put("decisions", context.decisions());
            put("owner", context.owner());
            put("references", context.evidenceIds().length);
            for (String id : context.evidenceIds()) { put("reference", id); }
            put("previous", context.previousCheckpointId());
            put("confirmed", context.confirmedAt());
        }
    }

    private WorkspaceState read() throws IOException {
        String id = get("workspace"), name = get("name");
        ProjectProfile profile = choice("profile", ProjectProfile.class);
        EntryMode mode = choice("mode", EntryMode.class);
        Instant confirmed = time("confirmed");
        ScanResult snapshot = snapshot();
        EvidenceClaim[] reviewed = claims();
        WorkspaceState state = new WorkspaceState();
        state.activate(new BaselineB0(id, name, profile, mode, snapshot, reviewed, ClaimReview.conflicts(reviewed), confirmed));
        if (versionTwo) {
            int observationCount = count("observations", 20);
            for (int i = 0; i < observationCount; i++) { state.addObservation(snapshot()); }
            state.setSoloOwner(flag("soloOwner"));
            int reviewCount = count("reviews", 2000);
            for (int i = 0; i < reviewCount; i++) {
                state.addReview(new ContextReview(get("observation"), get("evidence"),
                    choice("field", EvidenceClaim.Field.class), get("entity"), get("value"),
                    choice("disposition", EvidenceClaim.ReviewDisposition.class), time("reviewed")));
            }
        }
        int next = (int) number("nextCheckpoint", 1, 1000000);
        int count = count("checkpoints", 1000);
        for (int i = 0; i < count; i++) {
            String cpId = get("id"), title = get("title"), summary = get("summary");
            Checkpoint.Status status = choice("status", Checkpoint.Status.class);
            Instant created = time("created");
            Checkpoint.Origin origin = choice("origin", Checkpoint.Origin.class);
            String workspace = get("workspace"), snapshotId = get("snapshot"), objective = get("objective");
            ProjectContext.BlockerState blocker = choice("blockerState", ProjectContext.BlockerState.class);
            String blockers = get("blockers"), actions = get("next"), decisions = get("decisions"), owner = get("owner");
            String[] refs = new String[count("references", 2000)];
            for (int j = 0; j < refs.length; j++) { refs[j] = get("reference"); }
            ProjectContext context = new ProjectContext(workspace, snapshotId, objective, blocker, blockers,
                    actions, decisions, owner, refs, get("previous"), time("confirmed"));
            // Storage is temporary transport. The custom pointer algorithm reconstructs chronology.
            state.checkpoints().append(new Checkpoint(cpId, title, summary, status, created, origin, context));
        }
        state.restoreNextCheckpointNumber(next);
        return state;
    }

    private void snapshot(ScanResult snapshot) {
        put("root", snapshot.root() == null ? "" : snapshot.root());
        put("scan", snapshot.scanId());
        put("observed", snapshot.observedAt());
        put("policyVersion", snapshot.policy().version());
        ScanPolicy policy = snapshot.policy();
        put("depth", policy.maxDepth());
        put("entriesLimit", policy.maxEntries());
        put("fileBytes", policy.maxFileBytes());
        put("totalBytes", policy.maxTotalBytes());
        put("textLimit", policy.maxTextCharacters());
        put("lineLimit", policy.maxLineLength());
        put("claimLimit", policy.maxClaims());
        put("columns", policy.maxCsvColumns());
        put("duration", policy.maxDurationMillis());
        put("complete", snapshot.complete());
        put("bytesRead", snapshot.bytesRead());
        put("diagnostics", snapshot.diagnostics().length);
        for (String d : snapshot.diagnostics()) { put("diagnostic", d); }
        put("entries", snapshot.entries().length);
        for (SnapshotEntry entry : snapshot.entries()) {
            put("path", entry.relativePath());
            put("size", entry.sizeBytes());
            put("modified", entry.lastModified() == null ? "" : entry.lastModified());
            put("sha256", entry.sha256());
            put("status", entry.status());
            put("adapter", entry.adapter());
            put("role", entry.artifactRole());
            put("observed", entry.observedAt());
            put("reason", entry.reason());
        }
        claims(snapshot.claims());
    }

    private ScanResult snapshot() throws IOException {
        String rootText = get("root");
        Path root = rootText.isEmpty() ? null : Path.of(rootText);
        if (root != null && (!root.isAbsolute() || !root.equals(root.normalize()) || rootText.startsWith("\\\\"))) {
            throw new IllegalArgumentException();
        }
        String scan = get("scan");
        Instant observed = time("observed");
        if (!get("policyVersion").equals("B2-1")) { throw new IllegalArgumentException(); }
        ScanPolicy p = new ScanPolicy(count("depth", 32), count("entriesLimit", 10000),
                number("fileBytes", 1, 1048576), number("totalBytes", 1, 16777216),
                count("textLimit", 262144), count("lineLimit", 4096), count("claimLimit", 2000),
                count("columns", 64), number("duration", 1, 60000));
        boolean complete = flag("complete");
        long bytes = number("bytesRead", 0, 16777216);
        String[] diagnostics = new String[count("diagnostics", 10000)];
        for (int i = 0; i < diagnostics.length; i++) { diagnostics[i] = get("diagnostic"); }
        SnapshotEntry[] entries = new SnapshotEntry[count("entries", p.maxEntries())];
        for (int i = 0; i < entries.length; i++) {
            String path = get("path"); long size = number("size", -1, Long.MAX_VALUE);
            String modified = get("modified");
            entries[i] = new SnapshotEntry(path, size, modified.isEmpty() ? null : Instant.parse(modified),
                    get("sha256"), choice("status", SnapshotEntry.Status.class), choice("adapter", SnapshotEntry.Adapter.class),
                    choice("role", EvidenceClaim.ArtifactRole.class), time("observed"), get("reason"));
        }
        return new ScanResult(root, scan, observed, p, entries, claims(), diagnostics, complete, bytes);
    }

    private static void safe(String text, int max) {
        if (text == null || text.length() > max || ContentSafety.hasSecretMarker(text)
                || !ProjectContext.note(text).equals(text.isBlank() ? "UNKNOWN" : text)) {
            throw new IllegalArgumentException();
        }
    }
    private static void relative(String path) {
        safe(path, 2048);
        if (path.isEmpty() || path.startsWith("/") || path.contains("\\") || path.contains(":")
                || path.equals("..") || path.startsWith("../") || path.contains("/../") || path.endsWith("/..")
                || path.contains("//") || path.startsWith("./") || path.contains("/./")) { throw new IllegalArgumentException(); }
    }
    private static void hash(String hash) {
        if (!hash.isEmpty() && !hash.matches("[a-f0-9]{64}")) { throw new IllegalArgumentException(); }
    }

    /** Untrusted storage must not introduce foreign references or upgrade imported provenance. */
    private static void validate(WorkspaceState state) {
        BaselineB0 baseline = state.activeBaseline();
        if (baseline == null || !baseline.workspaceId().matches("W(?:[0-9]{3,9}|-[a-f0-9]{32})")
                || baseline.profile() == null || baseline.entryMode() == null || baseline.confirmedAt() == null
                || !state.checkpoints().validateIntegrity() || state.checkpoints().size() > 1000) { throw new IllegalArgumentException(); }
        safe(baseline.displayName(), 120);
        if (baseline.displayName().isBlank()) { throw new IllegalArgumentException(); }
        ScanResult scan = baseline.snapshot();
        if (scan.root() == null ? !scan.scanId().isEmpty() || scan.entries().length != 0
                : !scan.scanId().equals("SCAN-" + baseline.workspaceId())) { throw new IllegalArgumentException(); }
        if (baseline.entryMode() != EntryMode.NEW_EMPTY && scan.root() == null) { throw new IllegalArgumentException(); }
        validateObservation(baseline);
        for (ScanResult later : state.laterObservations()) {
            if (!later.scanId().matches("OBS-" + baseline.workspaceId() + "-[0-9]{4}")) { throw new IllegalArgumentException(); }
            validateObservation(new BaselineB0(baseline.workspaceId(), baseline.displayName(), baseline.profile(),
                    baseline.entryMode(), later, later.claims(), ClaimReview.conflicts(later.claims()), later.observedAt()));
        }
        String previous = "";
        Instant previousTime = baseline.confirmedAt();
        int previousNumber = 0;
        for (Checkpoint checkpoint : state.checkpoints().traverseForward()) {
            ProjectContext context = checkpoint.project();
            int number = Integer.parseInt(checkpoint.checkpointId().substring(3));
            if (context == null || checkpoint.origin() != Checkpoint.Origin.USER_CONFIRMED || !context.workspaceId().equals(baseline.workspaceId())
                    || state.observation(context.snapshotId()) == null
                    || !context.previousCheckpointId().equals(previous) || number <= previousNumber
                    || checkpoint.createdAt().isBefore(previousTime) || !checkpoint.createdAt().equals(context.confirmedAt())) { throw new IllegalArgumentException(); }
            safe(checkpoint.title(), 120); safe(checkpoint.summary(), 600);
            ScanResult referenced = state.observation(context.snapshotId());
            if (referenced.observedAt().isAfter(context.confirmedAt())) { throw new IllegalArgumentException(); }
            for (String ref : context.evidenceIds()) { if (state.evidence(context.snapshotId(), ref) == null) { throw new IllegalArgumentException(); } }
            previous = checkpoint.checkpointId(); previousTime = checkpoint.createdAt(); previousNumber = number;
        }
        if (state.nextCheckpointNumber() <= previousNumber || state.nextCheckpointNumber() > 1000000) { throw new IllegalArgumentException(); }
    }

    private static void validateObservation(BaselineB0 baseline) {
        ScanResult scan = baseline.snapshot();
        if (scan.observedAt().isAfter(baseline.confirmedAt()) || scan.bytesRead() < 0
                || scan.bytesRead() > scan.policy().maxTotalBytes() || scan.entries().length > scan.policy().maxEntries()
                || scan.claims().length > scan.policy().maxClaims() || baseline.claims().length > 2000
                || !scan.scanId().matches("[A-Za-z0-9-]{0,100}")) { throw new IllegalArgumentException(); }
        if (scan.root() != null && (!scan.root().isAbsolute() || !scan.root().equals(scan.root().normalize())
                || scan.root().toString().startsWith("\\\\"))) { throw new IllegalArgumentException(); }
        for (String d : scan.diagnostics()) { safe(d, 4000); }
        SnapshotEntry[] entries = scan.entries();
        for (int i = 0; i < entries.length; i++) {
            SnapshotEntry entry = entries[i]; relative(entry.relativePath()); hash(entry.sha256()); safe(entry.reason(), 4000);
            if (entry.status() == null || entry.adapter() == null || entry.artifactRole() == null || entry.observedAt() == null
                    || entry.sizeBytes() < -1) { throw new IllegalArgumentException(); }
            for (int j = 0; j < i; j++) {
                if (entries[j].relativePath().equals(entry.relativePath())) { throw new IllegalArgumentException(); }
            }
        }
        validateClaims(scan.claims(), baseline, entries);
        validateClaims(baseline.claims(), baseline, entries);
        EvidenceClaim[] reviewedClaims = baseline.claims();
        EvidenceClaim[] originalClaims = scan.claims();
        ConflictGroup[] conflicts = ClaimReview.conflicts(reviewedClaims);
        // Every original source claim has exactly one reviewed counterpart with unchanged source data.
        for (EvidenceClaim original : originalClaims) {
            EvidenceClaim reviewed = find(reviewedClaims, original.evidenceId());
            boolean conflict = false;
            for (ConflictGroup group : conflicts) {
                for (String ref : group.evidenceIds()) { conflict |= ref.equals(original.evidenceId()); }
            }
            EvidenceClaim expected = conflict && !scan.scanId().startsWith("OBS-") ? original.withVerification(EvidenceClaim.Verification.CONFLICTING) : original;
            if (reviewed == null || !expected.withReview(reviewed.review()).equals(reviewed)) { throw new IllegalArgumentException(); }
        }
        for (EvidenceClaim claim : reviewedClaims) {
            if (!claim.userSupplied() && find(originalClaims, claim.evidenceId()) == null) { throw new IllegalArgumentException(); }
        }
    }

    private static EvidenceClaim find(EvidenceClaim[] claims, String id) {
        for (EvidenceClaim claim : claims) { if (claim.evidenceId().equals(id)) { return claim; } }
        return null;
    }

    private static void validateClaims(EvidenceClaim[] claims, BaselineB0 baseline, SnapshotEntry[] entries) {
        for (int i = 0; i < claims.length; i++) {
            EvidenceClaim claim = claims[i];
            if (!baseline.workspaceId().equals(claim.workspaceId()) || !claim.evidenceId().matches("EV-" + baseline.workspaceId()
                        + (baseline.snapshot().scanId().startsWith("OBS-") ? "-O" + baseline.snapshot().scanId().substring(baseline.snapshot().scanId().length() - 4) : "") + "-[0-9]{6}")
                    || claim.field() == null || claim.artifactRole() == null || claim.sourceKind() == null || claim.provenance() == null
                    || claim.verification() == null || claim.review() == null || claim.observedAt() == null) { throw new IllegalArgumentException(); }
            safe(claim.entityId(), 1000); safe(claim.literalValue(), 4000); safe(claim.comparisonValue(), 4000);
            safe(claim.section(), 4000); safe(claim.sourceStatedDate(), 1000); hash(claim.fingerprint());
            for (int j = 0; j < i; j++) { if (claims[j].evidenceId().equals(claim.evidenceId())) { throw new IllegalArgumentException(); } }
            if (claim.userSupplied()) {
                if (!claim.relativePath().equals("(user input)") || claim.line() != 0 || !claim.fingerprint().isEmpty()
                        || baseline.entryMode() != EntryMode.NEW_EMPTY
                        || claim.provenance() != EvidenceClaim.Provenance.EXPLICIT) { throw new IllegalArgumentException(); }
            } else {
                relative(claim.relativePath());
                boolean found = false;
                for (SnapshotEntry entry : entries) {
                    found |= entry.relativePath().equals(claim.relativePath()) && !entry.sha256().isEmpty()
                            && entry.sha256().equals(claim.fingerprint()) && entry.status() == SnapshotEntry.Status.SUPPORTED;
                }
                if (!found || claim.line() < 1 || claim.verification() == EvidenceClaim.Verification.USER_CONFIRMED
                        || claim.provenance() != EvidenceClaim.Provenance.EXPLICIT) { throw new IllegalArgumentException(); }
            }
        }
    }
}
