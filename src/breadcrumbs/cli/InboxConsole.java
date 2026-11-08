package breadcrumbs.cli;

import breadcrumbs.domain.*;
import java.io.IOException;
import java.io.PrintStream;
import java.time.Instant;

final class InboxConsole {
    private final ContextConsole.Input input; private final WorkspaceState state;
    private final DailyConsole.Action save; private final TerminalOutput ui;
    InboxConsole(ContextConsole.Input input, PrintStream out, WorkspaceState state, DailyConsole.Action save, TerminalTheme theme, int width) {
        this.input = input; this.state = state; this.save = save; ui = new TerminalOutput(out, theme, width);
    }
    String run() throws IOException {
        while (true) {
            ui.heading("Project notes");
            ui.note("Keep a note about your project or something someone told you. Do not add passwords or access keys.");
            int number = 0;
            for (var capture : state.manualContext().captures()) { ui.line("Note " + (++number) + ": ", ProjectPresentation.noteType(capture.type()) + " | " + ProjectPresentation.review(state.manualContext().disposition(capture.id())) + (capture.sensitivity() == ManualContext.Sensitivity.SENSITIVE ? " | private" : "")); }
            ui.screen(BreadFrames.Mood.ENCOURAGING, "Would you like to add or review a project note?",
                    "[1] Add a note\n[2] Review a note\n[3] Read a note\n[4] Attach a note to a progress update\n[0] Back", false);
            String choice = input.read("Notes > ");
            if (choice.equals("0")) { return null; }
            if (choice.equals("1")) {
                String captured = capture(); if (captured != null) { return captured; }
            } else if (choice.equals("2") || choice.equals("3") || choice.equals("4")) {
                String id = noteId(input.read("Note number (or its ID): ")); var capture = state.manualContext().find(id);
                if (capture == null) { throw new IllegalArgumentException("Choose a note from this project."); }
                if (choice.equals("4")) { return id; }
                if (choice.equals("2")) { review(capture); }
                else if (input.read("Show this note on this computer? Type yes: ").equalsIgnoreCase("yes")) {
                    ui.value("Your note", capture.statement()); ui.value("Where this came from", capture.origin());
                    ui.value("Reference you provided", capture.reference());
                }
            } else { ui.warning("Choose one of the note actions above."); }
        }
    }
    private String noteId(String selected) {
        if (state.manualContext().find(selected) != null) { return selected; }
        try { int number = Integer.parseInt(selected); var captures = state.manualContext().captures(); if (number >= 1 && number <= captures.length) { return captures[number - 1].id(); } }
        catch (NumberFormatException invalid) { throw new IllegalArgumentException("Choose a note number or ID from the list above."); }
        throw new IllegalArgumentException("Choose a note number from the list above.");
    }
    private String capture() throws IOException {
        ui.menu("[1] Personal observation\n[2] Current project status\n[3] Reported statement\n[4] Decision or agreement\n[5] Proposed next action");
        int number;
        try { number = Integer.parseInt(input.read("Note category: ")); }
        catch (NumberFormatException invalid) { throw new IllegalArgumentException("Choose a note type from 1 to 5."); }
        if (number < 1 || number > 5) { throw new IllegalArgumentException("Choose a note type from 1 to 5."); }
        var type = ManualContext.Type.values()[number - 1];
        String statement = input.read("Note content: "), origin = input.read("Information source: "), reference = input.read("Optional reference, such as a meeting name or document title: ");
        String sensitivity = input.read("Privacy [S = private; Enter = standard project note]: ");
        if (!sensitivity.isEmpty() && !sensitivity.equalsIgnoreCase("s")) { throw new IllegalArgumentException("Enter s for a private note, or press Enter for an ordinary project note."); }
        var classification = sensitivity.equalsIgnoreCase("s") ? ManualContext.Sensitivity.SENSITIVE : ManualContext.Sensitivity.INTERNAL;
        ui.value("Review project note", ProjectContext.note(statement)); ui.value("Where this came from", ProjectContext.note(origin));
        ui.warning(type == ManualContext.Type.REPORTED_STATEMENT ? "This is what someone told you. Breadcrumbs has not checked whether it is true." : "This records your account. Breadcrumbs does not check whether outside events happened.");
        String action = input.read("Add note [C = confirm; P = pending review; 0 = cancel]: ");
        if (action.equals("0")) { return null; }
        if (!action.equalsIgnoreCase("c") && !action.equalsIgnoreCase("p")) { throw new IllegalArgumentException("Choose c, p or 0."); }
        var captured = state.manualContext().capture(type, statement, origin, reference, classification);
        ui.success("Your note has been added to this open project."); save.run();
        if (action.equalsIgnoreCase("c")) {
            boolean index = classification == ManualContext.Sensitivity.INTERNAL && type == ManualContext.Type.REPORTED_STATEMENT
                    && input.read("Allow words from this reported note to appear in search? This does not verify the note. Type yes: ").equalsIgnoreCase("yes");
            boolean export = classification == ManualContext.Sensitivity.INTERNAL && input.read("Allow this note and its origin to be included in handoff files? Type yes: ").equalsIgnoreCase("yes");
            state.manualContext().review(captured.id(), EvidenceClaim.ReviewDisposition.CONFIRMED, index, export); save.run();
            if (classification == ManualContext.Sensitivity.INTERNAL && input.read("Also use this note as your current description of the project? Type yes: ").equalsIgnoreCase("yes")) {
                var field = type == ManualContext.Type.NEXT_ACTION_CANDIDATE ? EvidenceClaim.Field.NEXT_ACTION : type == ManualContext.Type.DECISION ? EvidenceClaim.Field.DECISION : EvidenceClaim.Field.CURRENT_STATE;
                String value = type == ManualContext.Type.REPORTED_STATEMENT ? "Reported statement, independently unverified: " + captured.statement() : captured.statement();
                state.addReview(new ContextReview(state.currentObservationId(), "", field, "", value, EvidenceClaim.ReviewDisposition.CONFIRMED, Instant.now())); save.run();
            }
        }
        return input.read("Record a progress update with this note attached now? Type yes: ").equalsIgnoreCase("yes") ? captured.id() : null;
    }
    void review(ManualContext.Capture capture) throws IOException {
        String disposition = input.read("Use this note? confirm=reviewed by you, reject=do not use, unknown=decide later, 0=back: "); if (disposition.equals("0")) { return; }
        var selected = switch (disposition.toLowerCase(java.util.Locale.ROOT)) {
            case "confirm" -> EvidenceClaim.ReviewDisposition.CONFIRMED; case "reject" -> EvidenceClaim.ReviewDisposition.REJECTED;
            case "unknown" -> EvidenceClaim.ReviewDisposition.LEFT_UNKNOWN; default -> throw new IllegalArgumentException("Choose confirm/reject/unknown.");
        };
        boolean index = selected == EvidenceClaim.ReviewDisposition.CONFIRMED && capture.sensitivity() == ManualContext.Sensitivity.INTERNAL
                && capture.type() == ManualContext.Type.REPORTED_STATEMENT && input.read("Allow words from this reported note in search? Type yes: ").equalsIgnoreCase("yes");
        boolean export = selected == EvidenceClaim.ReviewDisposition.CONFIRMED && capture.sensitivity() == ManualContext.Sensitivity.INTERNAL
                && input.read("Include this note and its origin in handoff files? Type yes: ").equalsIgnoreCase("yes");
        state.manualContext().review(capture.id(), selected, index, export); save.run();
        ui.success("Your review is recorded. The original note and where it came from are kept.");
    }
}
