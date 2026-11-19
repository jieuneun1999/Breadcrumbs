package breadcrumbs.continuity;

import breadcrumbs.audit.ContextAudit;
import breadcrumbs.bootstrap.BootstrapDraft;
import breadcrumbs.bootstrap.ClaimReview;
import breadcrumbs.domain.*;
import breadcrumbs.inspector.*;
import java.time.Instant;
import java.util.Arrays;
import static breadcrumbs.continuity.ProjectMap.*;

/** Disposable bounded projection. This class has no filesystem or persistence access. */
public final class ProjectMapService {
    public static final int ITEMS_PER_SECTION = 64;
    public static final int DETAIL_CHARACTERS = 1200;
    public static final int PAGE_SIZE = 4;
    public static final int HISTORY_WORK_LIMIT = 1000;

    public ProjectMap fromDraft(BootstrapDraft draft) { return fromDraft(draft, new ContextReview[0]); }
    public ProjectMap fromDraft(BootstrapDraft draft, ContextReview[] proposals) {
        Builder b = new Builder(draft.workspaceId(), draft.scan());
        b.sources(draft.claims()); b.assessments(proposals, true); b.claims(draft.claims());
        b.history("Starting record proposal", "Not confirmed. No progress updates or prior history are created.", "", draft.scan().observedAt(), b.observation);
        b.missing();
        return b.finish(draft.displayName(), draft.profile(), true, draft.scan().root() != null);
    }
    /** Projects recorded state only. Freshness labels distinguish a saved observation from a new source scan. */
    public ProjectMap fromState(WorkspaceState state) { return fromState(state, false); }
    public ProjectMap fromState(WorkspaceState state, boolean checkedThisSession) {
        if (state.activeBaseline() == null) { throw new IllegalArgumentException("Open or confirm a project first."); }
        Builder b = new Builder(state.activeBaseline().workspaceId(), state.currentObservation());
        EvidenceClaim[] claims = CurrentAssessment.claims(state);
        b.sources(claims);
        ContextReview[] current = new ContextReview[EvidenceClaim.Field.values().length]; int count = 0;
        for (var field : EvidenceClaim.Field.values()) {
            var review = CurrentAssessment.statement(state, field); if (review != null) { current[count++] = review; }
        }
        b.assessments(Arrays.copyOf(current, count), false);
        var first = state.activeBaseline();
        b.history("Starting project record", "Original baseline retained; this is not a progress update.", "B0", first.confirmedAt(), id(first.snapshot()));
        for (var scan : state.laterObservations()) { b.history("Later file observation", "Saved file check; no project completion is inferred.", "", scan.observedAt(), id(scan)); }
        Checkpoint[] history = state.checkpoints().size() <= HISTORY_WORK_LIMIT ? state.checkpoints().traverseBackward()
                : new Checkpoint[] {state.checkpoints().getLatest()};
        for (var cp : history) {
            b.history(cp.title(), cp.summary() + "\nRecorded next action: " + cp.project().nextActions()
                    + "\nRecorded blockers: " + cp.project().blockerState() + " | " + cp.project().blockers()
                    + "\nRecorded decisions: " + cp.project().decisions() + "\nGraph reference: " + cp.project().graphRevisionId(),
                    cp.checkpointId(), cp.createdAt(), cp.project().snapshotId());
        }
        if (state.checkpoints().size() > HISTORY_WORK_LIMIT) { b.finding("History display limited", "Only the latest update is projected. Use the existing history tools for the remaining " + (state.checkpoints().size() - 1) + " updates.", "", null); }
        var graph = state.dependencies().current();
        if (graph != null) {
            for (var task : graph.items()) {
                b.add(Section.TASKS, "Tracked task " + task.id() + ": " + task.title(), task.description() + " | Reported status: " + task.status() + " | Owner: " + task.owner(),
                        Kind.TASK, EvidenceClaim.Field.TASK_STATUS, EvidenceClaim.ReviewDisposition.CONFIRMED, "Explicitly created task; status is a human assessment, not verified completion.", false, false, SourceGroup.OTHER,
                        b.link(task.id(), task.observationId(), "", "", "Declared work item", task.updatedAt(), ""));
            }
            for (var edge : graph.dependencies()) {
                b.add(Section.BLOCKERS, "Declared dependency " + edge.prerequisite() + " -> " + edge.dependent(), edge.declaration(), Kind.DEPENDENCY,
                        null, EvidenceClaim.ReviewDisposition.CONFIRMED, "Explicit Graph relationship; never inferred from a document.", false, false, SourceGroup.OTHER,
                        b.link(edge.evidenceId(), edge.observationId(), "", "", "Graph " + graph.id(), edge.confirmedAt(), ""));
            }
        }
        int privateNotes = 0;
        for (var capture : state.manualContext().captures()) {
            if (capture.sensitivity() == ManualContext.Sensitivity.SENSITIVE) { privateNotes++; continue; }
            var review = state.manualContext().disposition(capture.id());
            Section section = switch (capture.type()) {
                case CURRENT_STATE -> Section.CURRENT_STATE; case DECISION -> Section.DECISIONS;
                case NEXT_ACTION_CANDIDATE -> Section.TASKS; default -> Section.SOURCES;
            };
            b.add(section, "Project note " + capture.id() + " | " + capture.type(), capture.statement(), Kind.MANUAL, null, review,
                    "Text is withheld until you explicitly display it locally. Declared origin: " + capture.origin() + "; reference: " + capture.reference()
                            + (capture.type() == ManualContext.Type.REPORTED_STATEMENT ? "; third-party report, independently unverified." : "; personal account, not independent verification."), false, true, SourceGroup.OTHER,
                    b.link(capture.id(), "MANUAL", "", "", "User-declared origin; no file locator", capture.recordedAt(), ""));
        }
        if (privateNotes > 0) { b.finding("Private notes omitted", privateNotes + " private notes are not displayed in this map.", "", null); }
        b.claims(claims);
        ScanResult auditOriginal = state.checkpoints().getLatest() == null ? first.snapshot()
                : state.observation(state.checkpoints().getLatest().project().snapshotId());
        if (state.currentObservation().entries().length <= 500 && auditOriginal.entries().length <= 500
                && state.checkpoints().size() <= HISTORY_WORK_LIMIT && (state.checkpoints().getLatest() == null || state.checkpoints().getLatest().project().evidenceIds().length <= 200)) {
            var audit = new ContextAudit().evaluate(state, state.checkpoints().getLatest());
            for (var finding : audit.findings()) {
                if (finding.state() != ContextAudit.State.EVIDENCED && finding.state() != ContextAudit.State.NOT_APPLICABLE) {
                    b.finding(finding.id() + " | " + finding.state(), finding.explanation() + " " + finding.nextAction(), String.join(", ", finding.evidenceIds()), null);
                }
            }
        } else { b.finding("Detailed audit deferred", "Map audit/freshness work is bounded to 500 entries per compared observation, 1000 updates and 200 latest-update references. Use advanced audit/comparison for larger records.", "", null); }
        b.missing();
        return b.finish(first.displayName(), first.profile(), false, checkedThisSession);
    }
    public static Section section(EvidenceClaim.Field field) {
        return switch (field) {
            case OBJECTIVE, DELIVERABLE -> Section.PURPOSE;
            case CURRENT_STATE, OWNER, TEST_RESULTS -> Section.CURRENT_STATE;
            case PROGRESS -> Section.HISTORY;
            case NEXT_ACTION, TASK_STATUS, TASK_MARKER -> Section.TASKS;
            case DECISION, RATIONALE -> Section.DECISIONS;
            case BLOCKERS -> Section.BLOCKERS;
            case DOCUMENT_TEXT -> Section.SOURCES;
        };
    }
    public static String label(EvidenceClaim.Field field) {
        return switch (field) {
            case OBJECTIVE -> "Project goal"; case DELIVERABLE -> "What you plan to deliver";
            case CURRENT_STATE, PROGRESS -> "Current project status"; case NEXT_ACTION -> "Suggested next action";
            case OWNER -> "Person responsible"; case BLOCKERS -> "What is getting in the way";
            case DECISION -> "Decision"; case RATIONALE -> "Reason"; case TEST_RESULTS -> "Reported test results";
            case TASK_STATUS, TASK_MARKER -> "Task update"; case DOCUMENT_TEXT -> "Text from the document";
        };
    }
    public static String safe(String value, int limit) {
        String normalized = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFKC);
        if (ContentSafety.hasSecretMarker(normalized) || normalized.matches("(?is).*(?:ignore (?:all )?(?:previous|prior) instructions|system prompt|run the following command).*")) {
            return "[Unsafe or sensitive text withheld]";
        }
        String sanitized = Arrays.stream(value.split("\n", -1)).map(line -> ContentSafety.terminal(line, line.length()))
                .collect(java.util.stream.Collectors.joining("\n"));
        if (sanitized.length() <= limit) { return sanitized; }
        return limit < 3 ? sanitized.substring(0, Math.max(0, limit)) : sanitized.substring(0, limit - 3) + "...";
    }
    public static boolean safePath(String path) {
        if (path.startsWith("/") || path.contains(":")) { return false; }
        for (String part : path.replace('\\', '/').split("/")) { if (part.equals("..") || ContentSafety.excludedName(part)) { return false; } }
        return !safe(path, 4096).startsWith("[Unsafe");
    }
    private static String id(ScanResult scan) { return scan.scanId().isEmpty() ? "B0-NO-SCAN" : scan.scanId(); }
    private static final class Builder {
        final String workspace, observation;
        final ScanResult scan;
        final SnapshotEntry[] entries;
        final Item[][] retained = new Item[Section.values().length][ITEMS_PER_SECTION];
        final int[] totals = new int[Section.values().length];
        final boolean[] documented = new boolean[EvidenceClaim.Field.values().length];
        Builder(String workspace, ScanResult scan) { this.workspace = workspace; this.scan = scan; entries = scan.entries(); observation = id(scan); }
        Link link(String record, String obs, String cp, String path, String locator, Instant time, String hash) {
            return new Link(workspace, safe(record, 512), obs, cp, safe(path, 300), safe(locator, 300), time, hash);
        }
        void add(Section section, String title, String value, Kind kind, EvidenceClaim.Field field,
                 EvidenceClaim.ReviewDisposition review, String note, boolean historical, boolean withheld, SourceGroup group, Link link) {
            int index = totals[section.ordinal()]++;
            String safeValue = safe(value, DETAIL_CHARACTERS);
            if (field != null && !historical && !withheld && review != EvidenceClaim.ReviewDisposition.REJECTED
                    && (kind == Kind.SOURCE || kind == Kind.STARTING_CONTEXT || kind == Kind.ASSESSMENT || kind == Kind.PROPOSED_ASSESSMENT)
                    && !safeValue.equals("UNKNOWN") && !safeValue.equals("NOT_DOCUMENTED") && !safeValue.startsWith("[Unsafe")) { documented[field.ordinal()] = true; }
            Item item = new Item(section, safe(title, 160), safeValue, kind, field, review, safe(note, 600), historical, withheld, group, link);
            if (index < ITEMS_PER_SECTION) { retained[section.ordinal()][index] = item; }
            else if (kind == Kind.CONFLICT) {
                for (int i = ITEMS_PER_SECTION - 1; i >= 0; i--) { if (retained[section.ordinal()][i].kind() != Kind.CONFLICT) { retained[section.ordinal()][i] = item; break; } }
            }
        }
        void finding(String title, String text, String record, EvidenceClaim.Field field) {
            add(Section.UNKNOWNS, title, text, Kind.FINDING, field, EvidenceClaim.ReviewDisposition.LEFT_UNKNOWN,
                    "Derived from saved records; no live verification.", false, false, SourceGroup.OTHER, link(record, observation, "", "", "Saved context", scan.observedAt(), ""));
        }
        void claims(EvidenceClaim[] claims) {
            claims = Arrays.stream(claims).filter(this::allowed).toArray(EvidenceClaim[]::new);
            var conflicts = ClaimReview.conflicts(claims);
            for (var group : conflicts) {
                if (!group.resolvedByReview()) {
                    add(Section.UNKNOWNS, "Conflicting " + label(group.field()).toLowerCase(java.util.Locale.ROOT) + " | " + group.entityId(), "Alternatives remain unresolved; no winner has been selected.", Kind.CONFLICT,
                            group.field(), EvidenceClaim.ReviewDisposition.LEFT_UNKNOWN, "Review each original source locator.", false, false, SourceGroup.OTHER,
                            link(String.join(", ", group.evidenceIds()), observation, "", "", "Conflicting source references", scan.observedAt(), ""));
                }
            }
            int pending = 0;
            for (var claim : claims) {
                if (!allowed(claim)) { continue; }
                if (!claim.userSupplied() && (claim.review() == EvidenceClaim.ReviewDisposition.PENDING || claim.review() == EvidenceClaim.ReviewDisposition.LEFT_UNKNOWN)) { pending++; }
                boolean conflict = false;
                for (var group : conflicts) { if (!group.resolvedByReview() && group.field() == claim.field() && group.entityId().equals(claim.entityId())) { conflict = true; } }
                String note = claim.userSupplied() ? "Supplied starting context; not independent verification." : "Source-reported; not independently verified. Original flag: " + claim.verification();
                if (conflict) { note += " Unresolved conflict: review all alternatives."; }
                if (claim.review() == EvidenceClaim.ReviewDisposition.REJECTED) { note += " Rejected for the present assessment; original statement retained."; }
                if (claim.literalValue().isBlank() || claim.literalValue().equals("UNKNOWN") || claim.literalValue().equals("NOT_DOCUMENTED")) {
                    finding(claim.field() + " not established", claim.literalValue().equals("NOT_DOCUMENTED") ? "Explicitly not documented." : "Not provided / Unknown.", claim.evidenceId(), claim.field()); continue;
                }
                add(section(claim.field()), label(claim.field()) + (claim.entityId().isEmpty() ? "" : " | " + claim.entityId())
                                + (claim.field() == EvidenceClaim.Field.TASK_STATUS || claim.field() == EvidenceClaim.Field.TASK_MARKER ? " (mentioned in document; not a tracked task)" : ""),
                        claim.literalValue(), claim.userSupplied() ? Kind.STARTING_CONTEXT : Kind.SOURCE, claim.field(), claim.review(), note, false, false, SourceGroup.OTHER,
                        link(claim.evidenceId(), observation, "", claim.userSupplied() ? "" : claim.relativePath(), claim.userSupplied() ? "User input; no file locator" : claim.locationUnit() + " " + claim.line() + " | " + claim.section(), claim.observedAt(), claim.fingerprint()));
            }
            if (pending > 0) { finding("Source statements need review", pending + " source statements remain unreviewed or uncertain. Confirm, reject or leave them unknown; their reports are not independently verified.", "", null); }
        }
        boolean allowed(EvidenceClaim claim) {
            if (!claim.workspaceId().equals(workspace) || claim.artifactRole() == EvidenceClaim.ArtifactRole.INSTRUCTIONS
                    || claim.verification() == EvidenceClaim.Verification.UNREADABLE) { return false; }
            if (claim.userSupplied()) { return true; }
            if (!safePath(claim.relativePath()) || claim.line() < 1) { return false; }
            for (var entry : entries) {
                if (entry.relativePath().equals(claim.relativePath()) && entry.status() == SnapshotEntry.Status.SUPPORTED
                        && entry.adapter() != SnapshotEntry.Adapter.METADATA_ONLY && entry.adapter() != SnapshotEntry.Adapter.DIRECTORY
                        && !entry.sha256().isEmpty() && entry.sha256().equals(claim.fingerprint())) { return true; }
            }
            return false;
        }
        void assessments(ContextReview[] reviews, boolean proposed) {
            for (var review : reviews) {
                if (!review.observationId().equals(observation) || !review.evidenceId().isEmpty() || !review.entityId().isEmpty()) { throw new IllegalArgumentException("A matching present-context assessment is required."); }
                add(section(review.field()), label(review.field()) + (proposed ? " (proposed)" : " (current assessment)"), review.value(), proposed ? Kind.PROPOSED_ASSESSMENT : Kind.ASSESSMENT,
                        review.field(), review.disposition(), proposed ? "Not committed. Applies only if the starting record is confirmed." : "Explicit present assessment; does not rewrite source statements or history.", false, false, SourceGroup.OTHER,
                        link("", review.observationId(), "", "", "Present-context review", review.reviewedAt(), ""));
            }
        }
        void history(String title, String value, String cp, Instant time, String obs) {
            add(Section.HISTORY, title, value, Kind.HISTORY, EvidenceClaim.Field.PROGRESS, EvidenceClaim.ReviewDisposition.CONFIRMED,
                    "Historical record, not current verification. Original observation retained.", true, false, SourceGroup.OTHER, link(cp, obs, cp.startsWith("CP-") ? cp : "", "", "Recorded history", time, ""));
        }
        void sources(EvidenceClaim[] claims) {
            int omitted = 0;
            for (var entry : entries) {
                if (entry.status() == SnapshotEntry.Status.SKIPPED || !safePath(entry.relativePath())) { omitted++; continue; }
                if (entry.adapter() == SnapshotEntry.Adapter.DIRECTORY) { continue; }
                SourceGroup group = switch (entry.artifactRole()) {
                    case INSTRUCTIONS -> SourceGroup.INSTRUCTIONS; case CURRENT_STATE -> SourceGroup.CURRENT_STATE;
                    case HANDOFF -> SourceGroup.HANDOFF; case TASK_LIST -> SourceGroup.TASK_LIST; case DECISION -> SourceGroup.DECISION; case UNKNOWN -> SourceGroup.OTHER;
                };
                if (group == SourceGroup.OTHER) { for (var claim : claims) { if (claim.workspaceId().equals(workspace) && claim.relativePath().equals(entry.relativePath()) && claim.fingerprint().equals(entry.sha256()) && (claim.field() == EvidenceClaim.Field.OBJECTIVE || claim.field() == EvidenceClaim.Field.DELIVERABLE)) { group = SourceGroup.PURPOSE; break; } } }
                if (entry.status() != SnapshotEntry.Status.SUPPORTED) { group = SourceGroup.LIMITED; }
                add(Section.SOURCES, entry.relativePath(), entry.status() + " | " + entry.adapter() + " | " + entry.reason(), Kind.FILE, null,
                        EvidenceClaim.ReviewDisposition.LEFT_UNKNOWN, "Observed file metadata. Role is a classification hint, not proof of its contents.", false, false, group,
                        link("", observation, "", entry.relativePath(), "File metadata only", entry.observedAt(), entry.sha256()));
                if (entry.status() != SnapshotEntry.Status.SUPPORTED) { finding("Content unavailable: " + entry.relativePath(), entry.reason() + " No content-level conclusions are drawn.", "", null); }
            }
            if (omitted > 0) { finding("Excluded or skipped material", omitted + " entries omitted from map details; names and bodies are not displayed.", "", null); }
            if (!scan.complete()) { finding("Incomplete inspection", "Scan or extraction limits were reached. Additional context may exist in unread portions.", "", null); }
            if (scan.root() == null) {
                add(Section.SOURCES, "No folder linked", "Use supplied starting details, progress updates and project notes without importing files.", Kind.FINDING, null,
                        EvidenceClaim.ReviewDisposition.LEFT_UNKNOWN, "No file inspection is claimed.", false, false, SourceGroup.OTHER, link("", observation, "", "", "User context", scan.observedAt(), ""));
            }
        }
        void missing() {
            for (var field : new EvidenceClaim.Field[] {EvidenceClaim.Field.OBJECTIVE, EvidenceClaim.Field.CURRENT_STATE, EvidenceClaim.Field.NEXT_ACTION}) {
                if (!documented[field.ordinal()]) { finding(label(field) + " remains unknown", "Not documented / Unknown. You can add a current assessment without inventing source evidence.", "", field); }
            }
        }
        ProjectMap finish(String project, ProjectProfile profile, boolean draft, boolean checked) {
            Bucket[] buckets = new Bucket[Section.values().length];
            for (Section section : Section.values()) { int i = section.ordinal(); buckets[i] = new Bucket(section, totals[i], Arrays.copyOf(retained[i], Math.min(totals[i], ITEMS_PER_SECTION))); }
            String coverage = scan.root() == null ? "No folder linked; starting details and project notes are supported."
                    : scan.entries().length + " entries inspected; " + scan.count(SnapshotEntry.Status.SUPPORTED) + " supported entries; " + scan.count(SnapshotEntry.Status.UNSUPPORTED) + " unsupported entries; "
                            + scan.count(SnapshotEntry.Status.SKIPPED) + " skipped; " + (scan.count(SnapshotEntry.Status.FAILED) + scan.count(SnapshotEntry.Status.UNSTABLE)) + " reading problems; " + (scan.complete() ? "within scan bounds." : "partial coverage.");
            return new ProjectMap(workspace, safe(project, 120), profile, draft, checked, coverage, buckets);
        }
    }
}
