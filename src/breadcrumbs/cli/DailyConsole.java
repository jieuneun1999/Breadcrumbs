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
    private final boolean persisted;
    private final Action save, full, advanced;
    private final TerminalTheme theme;
    private final int width;
    private final WorkspaceStore store;
    private final ContextSearchIndex search;
    private final TerminalOutput ui;
    DailyConsole(ContextConsole.Input input, PrintStream out, WorkspaceState state, boolean persisted,
            Action save, Action full, Action advanced, TerminalTheme theme, int width, WorkspaceStore store, ContextSearchIndex search) {
        this.input = input; this.out = out; this.state = state; this.persisted = persisted; this.save = save;
        this.full = full; this.advanced = advanced; this.theme = theme; this.width = width; this.store = store;
        this.search = search; ui = new TerminalOutput(out, theme, width);
    }
    void run() throws IOException {
        if (state.activeBaseline() == null) { ui.warning("Open or create a project first."); return; }
        boolean showMenu = true;
        while (true) {
            if (showMenu) {
                ui.heading("\nYOUR PROJECT");
                ui.menu("[1] View project summary\n[2] Record an update\n[10] Notes and updates\n\n[3] What needs attention?\n[4] Check files for changes\n[5] Search your project\n[11] Project handoff / export\n\n[7] View saved updates\n[8] Tasks and dependencies\n[6] Detailed update form\n[9] More project tools\n[0] Back");
                showMenu = false;
            } else { ui.note("\n1=summary  2=record an update  5=search  ?=menu  0=back"); }
            try {
                switch (input.read("daily > ")) {
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
                        ui.heading("\nYOUR SAVED UPDATES"); int number = 0;
                        for (var cp : state.checkpoints().traverseForward()) { ui.line("Update " + (++number) + ": ", cp.title()); ui.plain(cp.summary()); }
                        if (number == 0) { ui.note("No progress updates yet. Choose Record an update after you work on the project."); }
                        ui.note("These are the updates you recorded. Breadcrumbs did not check whether the reported work happened.");
                    }
                    case "8" -> new DependencyConsole(input, out, state, theme, width).run();
                    case "9" -> advanced.run();
                    case "10" -> { String manual = new InboxConsole(input, out, state, save, theme, width).run(); if (manual != null) { quick(new String[] {manual}); } }
                    case "11" -> new DocumentsConsole(input, out, state, store, save, theme, width).run();
                    default -> ui.warning("Choose a listed daily action.");
                }
            } catch (IllegalArgumentException invalid) { ui.error("Operation cancelled or rejected."); ui.plain(invalid.getMessage()); }
        }
    }
    void follow() { new ProjectPresentation(out, theme, width).showSummary(state, persisted); }
    private void quick() throws IOException { quick(new String[0]); }
    private void quick(String[] manualIds) throws IOException {
        ui.heading("\nRECORD AN UPDATE");
        ui.note(switch (state.activeBaseline().profile()) {
            case SOFTWARE -> "Leave a short note about your work, anything in the way and the next step. Only report tests you actually ran.";
            case ACADEMIC -> "Note what you revised, any feedback you received and your next study or research step.";
            case OPERATIONS -> "Note what moved forward, anything you are waiting for and what to do next.";
            case GENERAL -> "A Breadcrumb is a saved progress update that helps you continue later. Answer three short questions.";
        });
        var service = new QuickCheckpointService(); var options = service.proposed(state);
        options = new QuickCheckpointService.Options(options.objective(), options.owner(), options.decisions(), options.evidenceIds(), manualIds);
        String changed = input.read("What changed or what did you work on? ");
        var previous = state.checkpoints().getLatest();
        if (previous != null) { ui.value("Previously waiting on; check whether this still applies", ProjectPresentation.blocker(previous.project())); }
        String blocker = input.read("Anything getting in the way or waiting on someone? [details, none, or Enter if unsure]: ");
        String next = input.read("What is the next small step? ");
        while (true) {
            var preview = service.preview(state, changed, blocker, next, options);
            ui.heading("\nREVIEW YOUR UPDATE");
            ui.value("What you worked on", preview.summary()); ui.value("Waiting on", ProjectPresentation.blocker(preview.project()));
            ui.value("Next step", preview.project().nextActions());
            if (!options.objective().equals("UNKNOWN")) { ui.value("Project goal carried over; check that it still fits", options.objective()); }
            if (!options.owner().equals("UNKNOWN")) { ui.value("Person responsible carried over; check before saving", options.owner()); }
            if (!options.decisions().equals("UNKNOWN")) { ui.value("Decision and reason", options.decisions()); }
            if (options.evidenceIds().length + options.manualIds().length > 0) { ui.line("Attached context: ", options.evidenceIds().length + " document references and " + options.manualIds().length + " notes"); }
            ui.note("This records your account of the work. Breadcrumbs does not check whether the work happened.");
            String action = input.read("Save this update? yes=record it, e=edit, ?=more options, 0=cancel: ");
            switch (action.toLowerCase(java.util.Locale.ROOT)) {
                case "yes" -> {
                    new ProjectCheckpointService().confirm(state, preview, true); ui.success("Your update has been added to the project.");
                    if (persisted) { save.run(); } else { ui.warning("This update is only open here. Save the project before closing to keep it."); }
                    return;
                }
                case "0" -> { ui.note("Cancelled. No update was added."); return; }
                case "?" -> ui.note("a=attach document references  m=attach notes  o=goal/person/decision  d=tasks and dependencies  f=detailed form");
                case "e" -> { changed = input.read("What changed: "); blocker = input.read("Anything in the way? [details, none, or Enter if unsure]: "); next = input.read("Next small step: "); }
                case "a" -> {
                    for (var claim : state.claimsAt(state.currentObservationId())) { ui.value(claim.evidenceId(), claim.field() + " | " + claim.sourceKind() + " | " + claim.review()); }
                    String selected = input.read("Current source evidence IDs, comma-separated; blank clears: ");
                    String[] refs = selected.isBlank() ? new String[0] : selected.split(","); for (int i = 0; i < refs.length; i++) { refs[i] = refs[i].strip(); }
                    options = new QuickCheckpointService.Options(options.objective(), options.owner(), options.decisions(), refs, options.manualIds());
                }
                case "m" -> {
                    for (var capture : state.manualContext().captures()) { ui.value(capture.id(), capture.type() + " | " + state.manualContext().disposition(capture.id())); }
                    String selected = input.read("Manual IDs, comma-separated; blank clears: "); String[] refs = selected.isBlank() ? new String[0] : selected.split(",");
                    for (int i = 0; i < refs.length; i++) { refs[i] = refs[i].strip(); }
                    options = new QuickCheckpointService.Options(options.objective(), options.owner(), options.decisions(), options.evidenceIds(), refs);
                }
                case "o" -> options = new QuickCheckpointService.Options(input.read("Objective, blank UNKNOWN: "), input.read("Owner, blank UNKNOWN: "), input.read("Decision / rationale / additional context, blank UNKNOWN: "), options.evidenceIds(), options.manualIds());
                case "d" -> new DependencyConsole(input, out, state, theme, width).run();
                case "f" -> { full.run(); return; }
                default -> ui.warning("Choose one of the options above. No update has been added.");
            }
        }
    }
    private void review() throws IOException {
        ui.heading("\nCHECK FOR CHANGES");
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
        String choice = input.read("[1] Mark as reviewed [2] Add your next step [3] Record an update [d] Technical details [0] Back: ");
        if (choice.equalsIgnoreCase("d")) { ui.plain(new ContinuityService().changes(state)); return; }
        if (choice.equals("1")) {
            if (input.read("Mark these changes as reviewed? This does not confirm the work is finished. Type yes: ").equalsIgnoreCase("yes")) {
                state.addReview(new ContextReview(state.currentObservationId(), "", EvidenceClaim.Field.PROGRESS, "CHANGE_REVIEW",
                        "Reviewed saved observation " + state.currentObservationId() + "; source correctness and completion remain unverified.", EvidenceClaim.ReviewDisposition.CONFIRMED, Instant.now()));
                ui.success("Marked as reviewed. Your recorded project progress is unchanged."); if (persisted) { save.run(); }
            }
        } else if (choice.equals("2")) {
            String value = input.read("What should your next step be? [Enter if unsure]: ");
            if (input.read("Use this as your current next step? Type yes: ").equalsIgnoreCase("yes")) {
                state.addReview(new ContextReview(state.currentObservationId(), "", EvidenceClaim.Field.NEXT_ACTION, "", value, EvidenceClaim.ReviewDisposition.CONFIRMED, Instant.now()));
                if (persisted) { save.run(); }
            }
        } else if (choice.equals("3")) { quick(); }
    }
}
