package breadcrumbs.cli;

import breadcrumbs.audit.ContextAudit;
import breadcrumbs.bootstrap.ClaimReview;
import breadcrumbs.continuity.*;
import breadcrumbs.domain.*;
import breadcrumbs.handoff.*;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.time.Instant;

/** Functional B4 menu only; accepted startup artwork and theme remain untouched. */
final class ContextConsole {
    @FunctionalInterface interface Input { String read(String prompt) throws IOException; }
    private final Input input;
    private final PrintStream output;
    private final WorkspaceState state;
    private final Path storage;
    private final TerminalOutput ui; private final TerminalTheme theme; private final int width;
    ContextConsole(Input input, PrintStream output, WorkspaceState state, Path storage) {
        this(input, output, state, storage, new TerminalTheme(false), 80);
    }
    ContextConsole(Input input, PrintStream output, WorkspaceState state, Path storage, TerminalTheme theme, int width) {
        this.input = input; this.output = output; this.state = state; this.storage = storage;
        this.theme = theme; this.width = width; ui = new TerminalOutput(output, theme, width);
    }
    void perform(String choice) throws IOException {
        switch (choice) {
            case "9" -> audit(); case "10" -> review(); case "11" -> scan();
            case "12" -> differences(); case "13" -> retrieve(); case "14" -> handoff();
            case "15" -> {
                String setting = input.read("Coordination [solo/team/skip]: ");
                if (setting.equalsIgnoreCase("solo") || setting.equalsIgnoreCase("team")) {
                    state.setSoloOwner(setting.equalsIgnoreCase("solo"));
                    ui.plain("Current applicability recorded. Save explicitly to persist B4 changes.");
                } else { ui.plain("Applicability unchanged."); }
            }
            default -> throw new IllegalArgumentException("Unknown context option.");
        }
    }
    private void audit() throws IOException {
        var report = OperationFeedback.run(output, theme, width, OperationFeedback.Operation.AUDIT, () -> new ContextAudit().evaluate(state, state.checkpoints().getLatest()));
        ui.heading("CONTEXT AUDIT"); ui.line("Policy / observation: ", report.policy() + " | " + report.evaluatedAt() + " | " + report.observationId());
        ui.line("Readiness: ", report.readiness().toString());
        ui.line("Priority: ", ContextAudit.priority(report));
        ui.plain("Context score: " + HandoffService.percent(report.score())
                + " | Assessable coverage: " + HandoffService.percent(report.coverage()));
        for (var dimension : report.dimensions()) {
            ui.plain(dimension.name() + " | score " + HandoffService.percent(dimension.score())
                    + " | coverage " + HandoffService.percent(dimension.coverage()));
        }
        for (var finding : report.findings()) {
            ui.plain(finding.id() + " | " + finding.state() + " | weight " + finding.weight()
                    + " | " + HandoffService.clean(finding.explanation()));
            for (String id : finding.evidenceIds()) {
                EvidenceClaim c = state.evidence(state.currentObservationId(), id);
                if (c == null && state.checkpoints().getLatest() != null) {
                    c = state.evidence(state.checkpoints().getLatest().project().snapshotId(), id);
                }
                if (c != null) { source(c); }
            }
            ui.plain("Next: " + finding.nextAction());
        }
        ui.plain("No native history: " + report.noNativeHistory() + " | " + ContextAudit.DISCLAIMER);
    }
    private void source(EvidenceClaim c) {
        ui.plain(HandoffService.clean(c.evidenceId()) + " | " + c.field() + " | " + HandoffService.clean(c.entityId())
                + " | " + HandoffService.clean(c.relativePath()) + (c.paragraphLocated() ? " paragraph " : ":") + c.line() + " | " + HandoffService.clean(c.section())
                + " | SHA-256 " + HandoffService.clean(c.fingerprint()) + " | observed " + c.observedAt());
        ui.plain("Literal source statement / unverified: " + HandoffService.clean(c.literalValue()));
    }
    private void review() throws IOException {
        EvidenceClaim[] claims = CurrentAssessment.claims(state);
        var conflicts = ClaimReview.conflicts(claims);
        ui.plain("Present review only. Original source claims and historical checkpoints stay intact. At most two questions.");
        // Show every member/locator before asking about any competing statement.
        for (ConflictGroup group : conflicts) {
            ui.plain("CONFLICT | " + group.field() + " | " + HandoffService.clean(group.entityId())
                    + " | resolved by present review: " + group.resolvedByReview());
            for (String id : group.evidenceIds()) { source(state.evidence(state.currentObservationId(), id)); }
        }
        int asked = 0;
        // Explicit conflicts first; then literal next-action candidates. No keyword similarity grouping.
        for (int pass = 0; pass < 2 && asked < 2; pass++) {
            for (EvidenceClaim c : claims) {
                if (asked == 2) { break; }
                boolean conflicting = false;
                for (ConflictGroup g : conflicts) {
                    if (!g.resolvedByReview()) { for (String id : g.evidenceIds()) { conflicting |= id.equals(c.evidenceId()); } }
                }
                boolean nextCandidate = c.field() == EvidenceClaim.Field.NEXT_ACTION
                        || c.field() == EvidenceClaim.Field.TASK_MARKER && c.comparisonValue().equals("UNCHECKED");
                if ((pass == 0 && !conflicting) || (pass == 1 && (conflicting || !nextCandidate))
                        || c.review() == EvidenceClaim.ReviewDisposition.REJECTED) { continue; }
                source(c); asked++;
                String answer = input.read("Use this as present context? [confirm/reject/skip/cancel]: ");
                if (answer.equalsIgnoreCase("cancel")) { ui.plain("Review stopped; earlier explicitly confirmed reviews retained."); return; }
                var disposition = switch (answer.toLowerCase(java.util.Locale.ROOT)) {
                    case "confirm" -> EvidenceClaim.ReviewDisposition.CONFIRMED;
                    case "reject" -> EvidenceClaim.ReviewDisposition.REJECTED;
                    case "skip" -> EvidenceClaim.ReviewDisposition.LEFT_UNKNOWN;
                    default -> throw new IllegalArgumentException("Use confirm, reject, skip or cancel.");
                };
                state.addReview(new ContextReview(state.currentObservationId(), c.evidenceId(), c.field(), c.entityId(),
                        c.literalValue(), disposition, Instant.now()));
            }
        }
        if (asked < 2) {
            var report = new ContextAudit().evaluate(state, state.checkpoints().getLatest());
            EvidenceClaim.Field[] fields = {EvidenceClaim.Field.OBJECTIVE, EvidenceClaim.Field.DELIVERABLE,
                    EvidenceClaim.Field.CURRENT_STATE, EvidenceClaim.Field.NEXT_ACTION, EvidenceClaim.Field.BLOCKERS,
                    EvidenceClaim.Field.DECISION, EvidenceClaim.Field.OWNER};
            for (int i : new int[] {2, 3, 4, 0, 1, 5, 6}) {
                if (asked == 2) { break; }
                var finding = report.findings()[i];
                if (finding.state() == ContextAudit.State.EVIDENCED || finding.state() == ContextAudit.State.NOT_APPLICABLE
                        || finding.state() == ContextAudit.State.CONFLICTING) { continue; }
                String value = input.read("Current " + fields[i] + " (blank=skip, cancel=stop, NOT_DOCUMENTED=confirmed documentation gap): ");
                asked++;
                if (value.equalsIgnoreCase("cancel")) { break; }
                if (value.isBlank()) { continue; }
                ui.plain("Present human statement: " + HandoffService.clean(value));
                if (input.read("Record? [confirm/reject/skip/cancel]: ").equalsIgnoreCase("confirm")) {
                    state.addReview(new ContextReview(state.currentObservationId(), "", fields[i], "", value,
                            EvidenceClaim.ReviewDisposition.CONFIRMED, Instant.now()));
                }
            }
        }
        ui.plain("Review finished. Save explicitly to retain B4 changes. This does not retrospectively verify old work.");
        audit();
    }
    private void scan() throws IOException {
        ui.plain("Read-only scan of saved local root. No commands or scripts run; same B2 exclusions/limits.");
        ui.plain("Root: " + HandoffService.clean(String.valueOf(state.activeBaseline().snapshot().root()))
                + " | Policy: " + state.currentObservation().policy());
        if (!input.read("Scan now? Type yes; anything else cancels: ").equalsIgnoreCase("yes")) {
            ui.plain("Rescan cancelled. No source access."); return;
        }
        breadcrumbs.inspector.ScanResult scan;
        try (var feedback = new OperationFeedback(output, theme, width, OperationFeedback.Operation.RESCAN)) {
            try { scan = new ObservationService().scan(state, true, visited -> { feedback.running(visited); return true; }); feedback.complete(!scan.complete(), scan.entries().length + " entries inventoried. Review coverage."); }
            catch (IOException | RuntimeException failure) { feedback.failed("Rescan failed; prior observations retained."); throw failure; }
        }
        ui.plain("Saved in memory: " + scan.scanId() + " | complete within policy: " + scan.complete()
                + " | entries: " + scan.entries().length + " | claims: " + scan.claims().length);
        ui.plain("B0 and prior checkpoint references retained. Save explicitly to persist this observation.");
        differences();
    }
    private void differences() throws IOException {
        ui.plain("Available comparisons: B0 or a native checkpoint ID. Only saved observations are used; no scan occurs.");
        String id = input.read("Compare from (blank=B0): ");
        var before = state.activeBaseline().snapshot();
        if (!id.isBlank() && !id.equalsIgnoreCase("B0")) {
            Checkpoint checkpoint = state.indexes().checkpoint(id);
            if (checkpoint == null) { throw new IllegalArgumentException("Checkpoint not found."); }
            before = state.observation(checkpoint.project().snapshotId());
        }
        if (before == state.currentObservation()) { ui.plain("No later observation. Freshness UNKNOWN; a consented scan is required."); return; }
        ui.plain("Workspace drift (separate from checkpoint-referenced freshness):");
        final var selectedBefore = before;
        for (DriftEngine.Change change : OperationFeedback.run(output, theme, width, OperationFeedback.Operation.COMPARE, () -> new DriftEngine().compare(selectedBefore, state.currentObservation())).changes()) {
            ui.plain(change.status() + " | " + HandoffService.clean(change.oldPath()) + " -> "
                    + HandoffService.clean(change.newPath()) + " | " + change.reason());
        }
        Checkpoint latest = state.checkpoints().getLatest();
        if (latest != null) {
            ui.plain("Latest checkpoint referenced freshness: " + new ReferenceFreshness().assess(state, latest).state());
        }
    }
    private void retrieve() throws IOException {
        String query = input.read("Query evidence IDs, comma separated (blank=recency/freshness only): ");
        String[] ids = query.isBlank() ? new String[0] : query.split(",", -1);
        for (int i = 0; i < ids.length; i++) { ids[i] = ids[i].strip(); }
        var ranks = new CheckpointRetrieval().rank(state, ids);
        ui.plain("Deterministic native DLL retrieval. Tags are not modeled; no tag score is invented.");
        if (ranks.length == 0) { ui.plain("No native checkpoints. Use baseline context report."); }
        for (var rank : ranks) {
            output.printf(java.util.Locale.ROOT, "%s | %.3f = overlap %d (+%d) + fresh %.0f + recency %.3f%n",
                    rank.checkpoint().checkpointId(), rank.score(), rank.sharedEvidence(), rank.sharedEvidence() > 0 ? 3 : 0,
                    rank.freshnessBonus(), rank.recency());
        }
    }
    private void handoff() throws IOException {
        String selected = input.read("Checkpoint ID (blank=latest; B0=baseline context): ");
        Checkpoint checkpoint = selected.equalsIgnoreCase("B0") ? null : selected.isBlank()
                ? state.checkpoints().getLatest() : state.indexes().checkpoint(selected);
        if (!selected.isBlank() && !selected.equalsIgnoreCase("B0") && checkpoint == null) {
            throw new IllegalArgumentException("Checkpoint not found.");
        }
        ui.plain("Imported snippets are INTERNAL and omitted by default. Only explicitly CONFIRMED source claims can be included.");
        boolean snippets = input.read("Include reviewed snippets in this preview? Type yes: ").equalsIgnoreCase("yes");
        var preview = OperationFeedback.run(output, theme, width, OperationFeedback.Operation.EXPORT, () -> new HandoffService().preview(state, checkpoint, snippets));
        ui.heading("HANDOFF PREVIEW"); ui.plain(preview.text());
        if (!input.read("Export this exact preview? Type yes; Enter keeps terminal copy: ").equalsIgnoreCase("yes")) { return; }
        Path directory = Path.of(input.read("Existing absolute export directory outside imported root/app state: "));
        String file = input.read("New filename (.md or .txt; no overwrite): ");
        ui.plain("Destination: " + HandoffService.clean(directory.resolve(file).toString()));
        if (!input.read("Confirm writing the reviewed handoff here? Type yes: ").equalsIgnoreCase("yes")) {
            ui.plain("Export cancelled."); return;
        }
        Path result = OperationFeedback.run(output, theme, width, OperationFeedback.Operation.EXPORT, () -> new HandoffExporter().export(state, preview, directory, file, storage, true));
        ui.plain("Exported and read back: " + HandoffService.clean(result.toString()));
    }
}
