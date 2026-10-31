package breadcrumbs.cli;

import breadcrumbs.domain.*;
import breadcrumbs.checkpoint.*;
import breadcrumbs.continuity.*;
import breadcrumbs.search.ContextSearchIndex;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.time.Instant;

/** Daily workflows delegate to the existing canonical services and advanced controls. */
final class DailyConsole {
    @FunctionalInterface interface Action { void run() throws IOException; }
    private final ContextConsole.Input input;
    private final PrintStream out;
    private final WorkspaceState state;
    private final boolean persisted;
    private final Action save, full, advanced;
    private final TerminalTheme theme;
    private final int width;
    private final Path storage;
    private final ContextSearchIndex search;
    private final TerminalOutput ui;
    DailyConsole(ContextConsole.Input input, PrintStream out, WorkspaceState state, boolean persisted,
            Action save, Action full, Action advanced, TerminalTheme theme, int width, Path storage, ContextSearchIndex search) {
        this.input = input; this.out = out; this.state = state; this.persisted = persisted; this.save = save;
        this.full = full; this.advanced = advanced; this.theme = theme; this.width = width; this.storage = storage;
        this.search = search; ui = new TerminalOutput(out, theme, width);
    }
    void run() throws IOException {
        if (state.activeBaseline() == null) { ui.warning("Open or create a workspace first."); return; }
        while (true) {
            ui.heading("BREADCRUMBS / DAILY WORKSPACE");
            ui.menu("[1] Follow the Breadcrumbs\n[2] Leave a Breadcrumb\n[3] Continuity Health\n[4] Review changes\n[5] Search context\n[6] Full checkpoint form\n[7] Explore checkpoint history\n[8] Work items / dependencies\n[9] Advanced tools\n[0] Back");
            try {
                switch (input.read("daily > ")) {
                    case "0" -> { return; }
                    case "1" -> follow();
                    case "2" -> quick();
                    case "3" -> {
                        ui.heading("CONTINUITY HEALTH");
                        ui.plain(OperationFeedback.run(out, theme, width, OperationFeedback.Operation.AUDIT, () -> new ContinuityService().health(state)));
                        ui.note("Detailed findings remain available in Advanced tools, Context audit.");
                    }
                    case "4" -> review();
                    case "5" -> new SearchConsole(input, out, search, width, theme).run();
                    case "6" -> full.run();
                    case "7" -> {
                        ui.heading("RECORDED CHECKPOINT HISTORY");
                        for (var cp : state.checkpoints().traverseForward()) { ui.value(cp.checkpointId(), cp.createdAt() + " | " + cp.title()); }
                        ui.note("These are historical human assessments, not independently verified outcomes.");
                    }
                    case "8" -> new DependencyConsole(input, out, state).run();
                    case "9" -> advanced.run();
                    default -> ui.warning("Choose a listed daily action.");
                }
            } catch (IllegalArgumentException invalid) { ui.error("Operation cancelled or rejected."); ui.plain(invalid.getMessage()); }
        }
    }
    void follow() { ui.heading("FOLLOW THE BREADCRUMBS"); ui.plain(new ContinuityService().brief(state, persisted)); }
    private void quick() throws IOException {
        ui.heading("LEAVE A BREADCRUMB");
        ui.note(switch (state.activeBaseline().profile()) {
            case SOFTWARE -> "Record implementation, explicit test claims, decisions and the next technical step. Do not imply tests ran.";
            case ACADEMIC -> "Record a revision, any reported adviser feedback and the next research step.";
            case OPERATIONS -> "Record an update, explicitly reported approvals or supplier needs, and the next action.";
            case GENERAL -> "Record your current context, uncertainties and next action. No codebase is needed.";
        });
        var service = new QuickCheckpointService(); var options = service.proposed(state);
        String changed = input.read("What changed or what did you work on? ");
        var previous = state.checkpoints().getLatest();
        if (previous != null) { ui.value("Previously recorded blocker, review afresh", previous.project().blockerState() + " | " + previous.project().blockers()); }
        String blocker = input.read("Anything blocked or awaiting confirmation? [unknown/none/details]: ");
        String next = input.read("Next concrete action: ");
        while (true) {
            var preview = service.preview(state, changed, blocker, next, options);
            ui.heading("BREADCRUMB PREVIEW. No history change yet.");
            ui.value("What changed", preview.summary()); ui.value("Blocker assessment", preview.project().blockerState() + " | " + preview.project().blockers());
            ui.value("Next action", preview.project().nextActions()); ui.value("Proposed carried objective, review before confirming", options.objective());
            ui.value("Proposed carried owner, review before confirming", options.owner());
            ui.value("Decision / rationale", options.decisions()); ui.value("Source evidence IDs", String.join(", ", options.evidenceIds()));
            ui.note("Confirming records your present assessment. Source reports and historical tests stay independently unverified.");
            String action = input.read("yes=confirm, e=edit, a=evidence, o=options, d=dependencies, f=full form, 0=cancel: ");
            switch (action.toLowerCase(java.util.Locale.ROOT)) {
                case "yes" -> {
                    new ProjectCheckpointService().confirm(state, preview, true); ui.success("Confirmed " + preview.checkpointId());
                    if (persisted) { save.run(); } else { ui.warning("SESSION-ONLY checkpoint. Save explicitly before leaving to keep it across restart."); }
                    return;
                }
                case "0" -> { ui.note("Cancelled. No checkpoint added."); return; }
                case "e" -> { changed = input.read("What changed: "); blocker = input.read("Blocker assessment [unknown/none/details]: "); next = input.read("Next action: "); }
                case "a" -> {
                    for (var claim : state.claimsAt(state.currentObservationId())) { ui.value(claim.evidenceId(), claim.field() + " | " + claim.sourceKind() + " | " + claim.review()); }
                    String selected = input.read("Current source evidence IDs, comma-separated; blank clears: ");
                    String[] refs = selected.isBlank() ? new String[0] : selected.split(","); for (int i = 0; i < refs.length; i++) { refs[i] = refs[i].strip(); }
                    options = new QuickCheckpointService.Options(options.objective(), options.owner(), options.decisions(), refs);
                }
                case "o" -> options = new QuickCheckpointService.Options(input.read("Objective, blank UNKNOWN: "), input.read("Owner, blank UNKNOWN: "), input.read("Decision / rationale / additional context, blank UNKNOWN: "), options.evidenceIds());
                case "d" -> new DependencyConsole(input, out, state).run();
                case "f" -> { full.run(); return; }
                default -> ui.warning("Choose a preview action. No checkpoint has been created.");
            }
        }
    }
    private void review() throws IOException {
        ui.heading("GUIDED CHANGE REVIEW");
        if (input.read("Rescan source files now? Type yes; Enter reviews saved observations only: ").equalsIgnoreCase("yes")) {
            try (var progress = new OperationFeedback(out, theme, width, OperationFeedback.Operation.RESCAN)) {
                try {
                    var scan = new ObservationService().scan(state, true, count -> { progress.running(count); return true; });
                    progress.complete(!scan.complete(), scan.entries().length + " entries observed; " + scan.claims().length + " captured claims. Review coverage.");
                } catch (IOException | RuntimeException failure) { progress.failed("Rescan failed; existing observations retained."); throw failure; }
            }
        }
        ui.plain(OperationFeedback.run(out, theme, width, OperationFeedback.Operation.COMPARE, () -> new ContinuityService().changes(state)));
        String choice = input.read("[1] Acknowledge review [2] Present assessment [3] Leave a Breadcrumb [0] Return unresolved: ");
        if (choice.equals("1")) {
            if (input.read("Review acknowledgement does not verify completion/tests. Type yes: ").equalsIgnoreCase("yes")) {
                state.addReview(new ContextReview(state.currentObservationId(), "", EvidenceClaim.Field.PROGRESS, "CHANGE_REVIEW",
                        "Reviewed saved observation " + state.currentObservationId() + "; source correctness and completion remain unverified.", EvidenceClaim.ReviewDisposition.CONFIRMED, Instant.now()));
                ui.success("Review acknowledgement recorded separately from project progress."); if (persisted) { save.run(); }
            }
        } else if (choice.equals("2")) {
            String value = input.read("Your present assessment / next action, blank leaves UNKNOWN: ");
            if (input.read("Record this user assessment? Type yes: ").equalsIgnoreCase("yes")) {
                state.addReview(new ContextReview(state.currentObservationId(), "", EvidenceClaim.Field.NEXT_ACTION, "", value, EvidenceClaim.ReviewDisposition.CONFIRMED, Instant.now()));
                if (persisted) { save.run(); }
            }
        } else if (choice.equals("3")) { quick(); }
    }
}
