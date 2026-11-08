package breadcrumbs.cli;

import breadcrumbs.domain.*;
import breadcrumbs.checkpoint.*;
import breadcrumbs.continuity.*;
import breadcrumbs.search.ContextSearchIndex;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.time.Instant;
import breadcrumbs.storage.WorkspaceStore;

/** Daily workflows delegate to the existing canonical services and advanced controls. */
final class DailyConsole {
    @FunctionalInterface interface Action { void run() throws IOException; }
    private final ContextConsole.Input input;
    private final PrintStream out;
    private final WorkspaceState state;
    private final java.util.function.BooleanSupplier persisted, checkedThisSession;
    private final Action save, saveExplicit, full, advanced;
    private final TerminalTheme theme;
    private final int width;
    private final WorkspaceStore store;
    private final ContextSearchIndex search;
    private final TerminalOutput ui;
    DailyConsole(ContextConsole.Input input, PrintStream out, WorkspaceState state, java.util.function.BooleanSupplier persisted, java.util.function.BooleanSupplier checkedThisSession,
            Action save, Action saveExplicit, Action full, Action advanced, TerminalTheme theme, int width, WorkspaceStore store, ContextSearchIndex search) {
        this.input = input; this.out = out; this.state = state; this.persisted = persisted; this.checkedThisSession = checkedThisSession; this.save = save; this.saveExplicit = saveExplicit;
        this.full = full; this.advanced = advanced; this.theme = theme; this.width = width; this.store = store;
        this.search = search; ui = new TerminalOutput(out, theme, width);
    }
    private static final String ACTIONS = "[1] View project summary\n[2] Record progress update\n[3] Assess project context\n[4] Review file changes\n[5] Search project records\n[6] Create detailed progress update\n[7] View update history\n[8] Manage tasks and dependencies\n[9] Open advanced project tools\n[10] Manage project notes\n[11] Prepare project handoff\n[12] Save project\n[13] Explore Project Map\n[0] Back to project navigation\n[?] Show menu";
    private static final String UPDATE_ACTIONS = "[YES] Confirm update\n[E] Edit progress details\n[A] Attach document references\n[M] Attach project notes\n[O] Edit goal, owner and decisions\n[D] Manage tasks and dependencies\n[F] Open detailed update form\n[0] Cancel update\n[?] Show update options";
    void run() throws IOException {
        if (state.activeBaseline() == null) { ui.warning("Open or create a project first."); return; }
        boolean showMenu = true;
        while (true) {
            if (showMenu) {
                ui.heading("\nProject workspace");
                ui.screen(BreadFrames.Mood.WELCOME, "Ready to pick up where you left off?", ACTIONS, false);
                showMenu = false;
            } else { ui.heading("\nWorkspace actions"); ui.resultScreen(BreadFrames.Mood.IDLE, "What would you like to review or record?", ACTIONS); }
            try {
                switch (input.read("Workspace > ")) {
                    case "0" -> { return; }
                    case "?" -> showMenu = true;
                    case "1" -> follow();
                    case "2" -> quick();
                    case "3" -> {
                        var report = OperationFeedback.run(out, theme, width, OperationFeedback.Operation.AUDIT, () -> new breadcrumbs.audit.ContextAudit().evaluate(state, state.checkpoints().getLatest()));
                        new ProjectPresentation(out, theme, width).showHealth(report);
                    }
                    case "4" -> review();
                    case "5" -> new SearchConsole(input, out, search, width, theme, false).run();
                    case "6" -> full.run();
                    case "7" -> {
                        ui.heading("\nProgress update history"); int number = 0;
                        for (var cp : state.checkpoints().traverseForward()) { ui.line("Update " + (++number) + ": ", cp.title()); ui.plain(cp.summary()); }
                        if (number == 0) { ui.note("No progress updates yet. Choose Record an update after you work on the project."); }
                        ui.note("These are the updates you recorded. Breadcrumbs did not check whether the reported work happened.");
                    }
                    case "8" -> new DependencyConsole(input, out, state, theme, width).run();
                    case "9" -> advanced.run();
                    case "10" -> { String manual = new InboxConsole(input, out, state, save, theme, width).run(); if (manual != null) { quick(new String[] {manual}); } }
                    case "11" -> new DocumentsConsole(input, out, state, store, save, theme, width).run();
                    case "12" -> saveExplicit.run();
                    case "13" -> map();
                    default -> ui.warning("Choose a listed daily action.");
                }
            } catch (IllegalArgumentException invalid) { ui.error("Operation cancelled or rejected."); ui.plain(invalid.getMessage()); }
        }
    }
    private void map() throws IOException {
        while (true) {
            var request = new ProjectMapConsole(input, out, theme, width).run(new ProjectMapService().fromState(state, checkedThisSession.getAsBoolean()));
            if (request.action() == ProjectMapConsole.Action.BACK) { return; }
            if (request.action() == ProjectMapConsole.Action.REVIEW_SOURCE) {
                String choice = input.read("Review statement [C = confirm; R = reject; U = leave unknown; Enter = no change]: ");
                if (!choice.isEmpty()) {
                    var claim = state.evidence(state.currentObservationId(), request.item().link().recordId());
                    state.addReview(new ContextReview(state.currentObservationId(), claim.evidenceId(), claim.field(), claim.entityId(), claim.literalValue(), BootstrapReviewConsole.disposition(choice), Instant.now()));
                    if (persisted.getAsBoolean()) { save.run(); }
                }
            } else if (request.action() == ProjectMapConsole.Action.REVIEW_NOTE) {
                new InboxConsole(input, out, state, save, theme, width).review(state.manualContext().find(request.item().link().recordId()));
            } else if (request.action() == ProjectMapConsole.Action.CLARIFY) {
                var review = ProjectAssessmentConsole.collect(input, out, theme, width, state.currentObservationId(), state.currentObservation().observedAt(), false);
                if (review != null) { state.addReview(review); if (persisted.getAsBoolean()) { save.run(); } }
            }
        }
    }
    void follow() { new ProjectPresentation(out, theme, width).showSummary(state, persisted.getAsBoolean()); }
    private void quick() throws IOException { quick(new String[0]); }
    private void quick(String[] manualIds) throws IOException {
        ui.heading("\nRecord progress update");
        ui.note(switch (state.activeBaseline().profile()) {
            case SOFTWARE -> "Summarize changes, blockers and next actions. Include only test results you observed.";
            case ACADEMIC -> "Record revisions, received feedback and the next research or study action.";
            case OPERATIONS -> "Record operational progress, pending dependencies and the next action.";
            case GENERAL -> "Record progress, blockers and the next action. Review the update before confirming it.";
        });
        var service = new QuickCheckpointService(); var options = service.proposed(state);
        options = new QuickCheckpointService.Options(options.objective(), options.owner(), options.decisions(), options.evidenceIds(), manualIds);
        String changed = input.read("Progress summary: ");
        var previous = state.checkpoints().getLatest();
        if (previous != null) { ui.value("Previous blockers or pending items; review before carrying forward", ProjectPresentation.blocker(previous.project())); }
        String blocker = input.read("Blockers or pending items [none, or Enter if unknown]: ");
        String next = input.read("Next action: ");
        while (true) {
            var preview = service.preview(state, changed, blocker, next, options);
            ui.heading("\nReview progress update");
            ui.value("Progress summary", preview.summary()); ui.value("Blockers or pending items", ProjectPresentation.blocker(preview.project()));
            ui.value("Next action", preview.project().nextActions());
            if (!options.objective().equals("UNKNOWN")) { ui.value("Proposed project goal; review before confirming", options.objective()); }
            if (!options.owner().equals("UNKNOWN")) { ui.value("Proposed responsible person; review before confirming", options.owner()); }
            if (!options.decisions().equals("UNKNOWN")) { ui.value("Decision and reason", options.decisions()); }
            if (options.evidenceIds().length + options.manualIds().length > 0) { ui.line("Attached context: ", options.evidenceIds().length + " document references and " + options.manualIds().length + " notes"); }
            ui.note("This records your account of the work. Breadcrumbs does not check whether the work happened.");
            ui.note("\nUpdate actions"); ui.screen(BreadFrames.Mood.ENCOURAGING,
                    "Review your update. Only Confirm adds it to your history.", UPDATE_ACTIONS, false);
            String action = input.read("Update action: ");
            switch (action.toLowerCase(java.util.Locale.ROOT)) {
                case "yes" -> {
                    new ProjectCheckpointService().confirm(state, preview, true); ui.success("Your update has been added to the project.");
                    if (persisted.getAsBoolean()) { save.run(); } else { ui.warning("This update is only open here. Save the project before closing to keep it."); }
                    return;
                }
                case "0" -> { ui.note("Cancelled. No update was added."); return; }
                case "?" -> ui.note("Select an option above. References and notes are optional. Only YES confirms the update; 0 cancels without adding it.");
                case "e" -> { changed = input.read("What changed: "); blocker = input.read("Anything in the way? [details, none, or Enter if unsure]: "); next = input.read("Next small step: "); }
                case "a" -> {
                    for (var claim : state.claimsAt(state.currentObservationId())) { ui.value(claim.evidenceId(), ProjectPresentation.fieldLabel(claim.field()) + " | " + ProjectPresentation.review(claim.review())); }
                    String selected = input.read("Document reference IDs, comma-separated; Enter clears: ");
                    String[] refs = selected.isBlank() ? new String[0] : selected.split(","); for (int i = 0; i < refs.length; i++) { refs[i] = refs[i].strip(); }
                    options = new QuickCheckpointService.Options(options.objective(), options.owner(), options.decisions(), refs, options.manualIds());
                }
                case "m" -> {
                    for (var capture : state.manualContext().captures()) { ui.value(capture.id(), ProjectPresentation.noteType(capture.type()) + " | " + ProjectPresentation.review(state.manualContext().disposition(capture.id()))); }
                    String selected = input.read("Project note IDs, comma-separated; Enter clears: "); String[] refs = selected.isBlank() ? new String[0] : selected.split(",");
                    for (int i = 0; i < refs.length; i++) { refs[i] = refs[i].strip(); }
                    options = new QuickCheckpointService.Options(options.objective(), options.owner(), options.decisions(), options.evidenceIds(), refs);
                }
                case "o" -> options = new QuickCheckpointService.Options(input.read("Project goal [Enter if unknown]: "), input.read("Responsible person [Enter if unknown]: "), input.read("Decision and reason [Enter if unknown]: "), options.evidenceIds(), options.manualIds());
                case "d" -> new DependencyConsole(input, out, state, theme, width).run();
                case "f" -> { full.run(); return; }
                default -> ui.warning("Choose one of the options above. No update has been added.");
            }
        }
    }
    private void review() throws IOException {
        ui.heading("\nReview file changes");
        if (input.read("Read your project folder again to check for changes? Type yes; Enter uses earlier file checks: ").equalsIgnoreCase("yes")) {
            try (var progress = new OperationFeedback(out, theme, width, OperationFeedback.Operation.RESCAN)) {
                try {
                    var scan = new ObservationService().scan(state, true, count -> { progress.running(count); return true; });
                    progress.complete(!scan.complete(), scan.entries().length + " items checked. " + (scan.complete() ? "Review the changes below." : "Some content could not be read within the file limits. Open file details for the full list."));
                } catch (IOException | RuntimeException failure) { progress.failed("Your earlier file checks are kept. Check that the project folder is available and try again."); throw failure; }
            }
        }
        var comparison = OperationFeedback.run(out, theme, width, OperationFeedback.Operation.COMPARE, () -> new DriftEngine().compare(state.activeBaseline().snapshot(), state.currentObservation()));
        new ProjectPresentation(out, theme, width).showChanges(state, comparison);
        ui.screen(BreadFrames.Mood.NEEDS_REVIEW, "Review the file comparison before updating your notes.",
                "[1] Mark changes as reviewed\n[2] Update next action\n[3] Record progress update\n[D] View technical comparison\n[0] Back to workspace", false);
        String choice = input.read("Review action: ");
        if (choice.equalsIgnoreCase("d")) { ui.plain(new ContinuityService().changes(state)); return; }
        if (choice.equals("1")) {
            if (input.read("Mark these changes as reviewed? This does not confirm the work is finished. Type yes: ").equalsIgnoreCase("yes")) {
                state.addReview(new ContextReview(state.currentObservationId(), "", EvidenceClaim.Field.PROGRESS, "CHANGE_REVIEW",
                        "Reviewed saved observation " + state.currentObservationId() + "; source correctness and completion remain unverified.", EvidenceClaim.ReviewDisposition.CONFIRMED, Instant.now()));
                ui.success("Marked as reviewed. Your recorded project progress is unchanged."); if (persisted.getAsBoolean()) { save.run(); }
            }
        } else if (choice.equals("2")) {
            String value = input.read("Current next action [Enter if unknown]: ");
            if (input.read("Use this as your current next step? Type yes: ").equalsIgnoreCase("yes")) {
                state.addReview(new ContextReview(state.currentObservationId(), "", EvidenceClaim.Field.NEXT_ACTION, "", value, EvidenceClaim.ReviewDisposition.CONFIRMED, Instant.now()));
                if (persisted.getAsBoolean()) { save.run(); }
            }
        } else if (choice.equals("3")) { quick(); }
    }
}
