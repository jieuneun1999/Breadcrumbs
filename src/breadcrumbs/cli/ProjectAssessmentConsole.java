package breadcrumbs.cli;

import breadcrumbs.domain.*;
import java.io.IOException;
import java.io.PrintStream;
import java.time.Instant;

final class ProjectAssessmentConsole {
    private ProjectAssessmentConsole() { }
    static ContextReview collect(ContextConsole.Input input, PrintStream out, TerminalTheme theme, int width,
                                 String observation, Instant observedAt, boolean proposed) throws IOException {
        var ui = new TerminalOutput(out, theme, width);
        EvidenceClaim.Field[] fields = {EvidenceClaim.Field.OBJECTIVE, EvidenceClaim.Field.CURRENT_STATE, EvidenceClaim.Field.NEXT_ACTION,
                EvidenceClaim.Field.BLOCKERS, EvidenceClaim.Field.DELIVERABLE, EvidenceClaim.Field.DECISION, EvidenceClaim.Field.RATIONALE, EvidenceClaim.Field.OWNER};
        ui.heading("\nClarify project context");
        ui.note("This records your present assessment, without changing source statements or certifying past work.");
        for (int i = 0; i < fields.length; i++) { ui.menu("[" + (i + 1) + "] " + ProjectPresentation.fieldLabel(fields[i])); }
        ui.menu("[0] Back"); String choice = input.read("Context field: "); if (choice.equals("0")) { return null; }
        int selected;
        try { selected = Integer.parseInt(choice) - 1; } catch (NumberFormatException bad) { throw new IllegalArgumentException("Select a displayed context field."); }
        if (selected < 0 || selected >= fields.length) { throw new IllegalArgumentException("Select a displayed context field."); }
        String value = ProjectContext.note(input.read("Your current assessment [Enter = unknown; NOT_DOCUMENTED = explicitly not documented]: "));
        ui.value("Proposed assessment", value);
        ui.note(proposed ? "This applies only if you confirm the starting project record. Cancelling setup discards it." : "This adds a current review; original files and past updates remain unchanged.");
        if (!input.read("Accept this assessment? Type yes: ").equalsIgnoreCase("yes")) { return null; }
        Instant time = Instant.now(); if (time.isBefore(observedAt)) { time = observedAt; }
        return new ContextReview(observation, "", fields[selected], "", value, EvidenceClaim.ReviewDisposition.CONFIRMED, time);
    }
}
