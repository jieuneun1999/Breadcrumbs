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
            ui.heading("EVIDENCE INBOX");
            ui.note("Manual origin is user-declared. No source path/hash/physical line is invented. Do not enter credentials.");
            for (var capture : state.manualContext().captures()) { ui.value(capture.id(), capture.type() + " | " + capture.recordedAt() + " | " + capture.sensitivity() + " | " + state.manualContext().disposition(capture.id())); }
            ui.menu("[1] Capture manual context\n[2] Review a capture\n[3] View a capture\n[4] Link a capture to next checkpoint\n[0] Back");
            String choice = input.read("inbox > ");
            if (choice.equals("0")) { return null; }
            if (choice.equals("1")) {
                String captured = capture(); if (captured != null) { return captured; }
            } else if (choice.equals("2") || choice.equals("3") || choice.equals("4")) {
                String id = input.read("Manual capture ID: "); var capture = state.manualContext().find(id);
                if (capture == null) { throw new IllegalArgumentException("Choose a capture from this workspace."); }
                if (choice.equals("4")) { return id; }
                if (choice.equals("2")) { review(capture); }
                else if (input.read("Display this manual statement locally? Type yes: ").equalsIgnoreCase("yes")) {
                    ui.value("Literal recorded statement", capture.statement()); ui.value("Declared origin", capture.origin());
                    ui.value("Declared reference, not an observed file locator", capture.reference());
                }
            } else { ui.warning("Choose a listed Inbox action."); }
        }
    }
    private String capture() throws IOException {
        ui.menu("[1] Personal observation\n[2] User-declared current state\n[3] Reported statement\n[4] Decision / agreement\n[5] Next-action candidate");
        int number;
        try { number = Integer.parseInt(input.read("Capture type: ")); }
        catch (NumberFormatException invalid) { throw new IllegalArgumentException("Choose capture type 1-5."); }
        if (number < 1 || number > 5) { throw new IllegalArgumentException("Choose capture type 1-5."); }
        var type = ManualContext.Type.values()[number - 1];
        String statement = input.read("Literal statement: "), origin = input.read("Declared origin: "), reference = input.read("Optional declared reference (no fabricated file location): ");
        String sensitivity = input.read("Sensitivity [Enter=INTERNAL, s=SENSITIVE]: ");
        if (!sensitivity.isEmpty() && !sensitivity.equalsIgnoreCase("s")) { throw new IllegalArgumentException("Choose INTERNAL or SENSITIVE."); }
        var classification = sensitivity.equalsIgnoreCase("s") ? ManualContext.Sensitivity.SENSITIVE : ManualContext.Sensitivity.INTERNAL;
        ui.value("Preview statement, recorded as user input", ProjectContext.note(statement)); ui.value("Declared origin", ProjectContext.note(origin));
        ui.warning(type == ManualContext.Type.REPORTED_STATEMENT ? "SOURCE_REPORTED. The report is not independently verified." : "A user declaration is not independent verification of an external event.");
        String action = input.read("c=record and confirm present review, p=leave unconfirmed, 0=cancel: ");
        if (action.equals("0")) { return null; }
        if (!action.equalsIgnoreCase("c") && !action.equalsIgnoreCase("p")) { throw new IllegalArgumentException("Choose c, p or 0."); }
        var captured = state.manualContext().capture(type, statement, origin, reference, classification);
        ui.success("Captured " + captured.id()); save.run();
        if (action.equalsIgnoreCase("c")) {
            boolean index = classification == ManualContext.Sensitivity.INTERNAL && type == ManualContext.Type.REPORTED_STATEMENT
                    && input.read("Specifically allow this unverified reported statement in lexical search? Type yes: ").equalsIgnoreCase("yes");
            boolean export = classification == ManualContext.Sensitivity.INTERNAL && input.read("Allow this literal manual body/origin in handoff export? Type yes: ").equalsIgnoreCase("yes");
            state.manualContext().review(captured.id(), EvidenceClaim.ReviewDisposition.CONFIRMED, index, export); save.run();
            if (classification == ManualContext.Sensitivity.INTERNAL && input.read("Also record an explicit present-context assessment from this note? Type yes: ").equalsIgnoreCase("yes")) {
                var field = type == ManualContext.Type.NEXT_ACTION_CANDIDATE ? EvidenceClaim.Field.NEXT_ACTION : type == ManualContext.Type.DECISION ? EvidenceClaim.Field.DECISION : EvidenceClaim.Field.CURRENT_STATE;
                String value = type == ManualContext.Type.REPORTED_STATEMENT ? "Reported statement, independently unverified: " + captured.statement() : captured.statement();
                state.addReview(new ContextReview(state.currentObservationId(), "", field, "", value, EvidenceClaim.ReviewDisposition.CONFIRMED, Instant.now())); save.run();
            }
        }
        return input.read("Leave a Breadcrumb linking this note now? Type yes: ").equalsIgnoreCase("yes") ? captured.id() : null;
    }
    private void review(ManualContext.Capture capture) throws IOException {
        String disposition = input.read("Review [confirm/reject/unknown], 0 cancels: "); if (disposition.equals("0")) { return; }
        var selected = switch (disposition.toLowerCase(java.util.Locale.ROOT)) {
            case "confirm" -> EvidenceClaim.ReviewDisposition.CONFIRMED; case "reject" -> EvidenceClaim.ReviewDisposition.REJECTED;
            case "unknown" -> EvidenceClaim.ReviewDisposition.LEFT_UNKNOWN; default -> throw new IllegalArgumentException("Choose confirm/reject/unknown.");
        };
        boolean index = selected == EvidenceClaim.ReviewDisposition.CONFIRMED && capture.sensitivity() == ManualContext.Sensitivity.INTERNAL
                && capture.type() == ManualContext.Type.REPORTED_STATEMENT && input.read("Explicitly allow reported terms in search? Type yes: ").equalsIgnoreCase("yes");
        boolean export = selected == EvidenceClaim.ReviewDisposition.CONFIRMED && capture.sensitivity() == ManualContext.Sensitivity.INTERNAL
                && input.read("Allow body/origin export? Type yes: ").equalsIgnoreCase("yes");
        state.manualContext().review(capture.id(), selected, index, export); save.run();
        ui.success("Manual review recorded. Original statement and origin retained.");
    }
}
