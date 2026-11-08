package breadcrumbs.cli;

import breadcrumbs.bootstrap.BootstrapDraft;
import breadcrumbs.continuity.ProjectMapService;
import breadcrumbs.domain.*;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;

final class BootstrapReviewConsole {
    @FunctionalInterface interface Reviewer { BootstrapDraft review(BootstrapDraft draft) throws IOException; }
    record Result(BootstrapDraft draft, ContextReview[] proposals, boolean approved) {
        Result { proposals = proposals.clone(); }
        @Override public ContextReview[] proposals() { return proposals.clone(); }
    }
    private final ContextConsole.Input input;
    private final PrintStream out;
    private final TerminalTheme theme;
    private final int width;
    private final Reviewer reviewer;
    BootstrapReviewConsole(ContextConsole.Input input, PrintStream out, TerminalTheme theme, int width, Reviewer reviewer) {
        this.input = input; this.out = out; this.theme = theme; this.width = width; this.reviewer = reviewer;
    }
    Result run(BootstrapDraft draft) throws IOException {
        ContextReview[] proposals = new ContextReview[0]; var ui = new TerminalOutput(out, theme, width);
        while (true) {
            var map = new ProjectMapService().fromDraft(draft, proposals);
            var console = new ProjectMapConsole(input, out, theme, width); console.overview(map);
            ui.screen(BreadFrames.Mood.NEEDS_REVIEW, "Review what is recorded before confirming your starting context.",
                    "[1] Review detected context\n[2] Explore Project Map\n[3] Add or clarify current context\n[4] Confirm starting project record\n[0] Cancel setup", false);
            try {
                String choice = input.read("Overview action: ");
                if (choice.equals("0")) { return new Result(draft, proposals, false); }
                if (choice.equals("1")) { draft = reviewer.review(draft); }
                else if (choice.equals("4")) {
                    boolean approved = input.read("Confirm this starting record and proposed assessments? Type yes: ").equalsIgnoreCase("yes");
                    return new Result(draft, proposals, approved);
                } else if (choice.equals("2")) {
                    var request = console.run(map);
                    if (request.action() == ProjectMapConsole.Action.REVIEW_SOURCE) {
                        String selected = input.read("Review statement [C = confirm; R = reject; U = leave unknown; Enter = no change]: ");
                        if (!selected.isEmpty()) { draft = draft.review(request.item().link().recordId(), disposition(selected)); }
                    } else if (request.action() == ProjectMapConsole.Action.CLARIFY) {
                        var review = assessment(draft); if (review != null) { proposals = replace(proposals, review); }
                    }
                } else if (choice.equals("3")) {
                    var review = assessment(draft); if (review != null) { proposals = replace(proposals, review); }
                } else { ui.warning("Select an overview action from 0 to 4."); }
            } catch (IllegalArgumentException invalid) {
                if (invalid instanceof ProjectConsole.Cancelled) { throw invalid; }
                ui.warning(invalid.getMessage());
            }
        }
    }
    private ContextReview assessment(BootstrapDraft draft) throws IOException {
        String observation = draft.scan().scanId().isEmpty() ? "B0-NO-SCAN" : draft.scan().scanId();
        return ProjectAssessmentConsole.collect(input, out, theme, width, observation, draft.scan().observedAt(), true);
    }
    private static ContextReview[] replace(ContextReview[] proposals, ContextReview added) {
        ContextReview[] result = proposals.clone();
        for (int i = 0; i < result.length; i++) { if (result[i].field() == added.field()) { result[i] = added; return result; } }
        result = Arrays.copyOf(result, result.length + 1); result[result.length - 1] = added; return result;
    }
    static EvidenceClaim.ReviewDisposition disposition(String selected) {
        return switch (selected.toLowerCase(java.util.Locale.ROOT)) {
            case "c" -> EvidenceClaim.ReviewDisposition.CONFIRMED; case "r" -> EvidenceClaim.ReviewDisposition.REJECTED;
            case "u" -> EvidenceClaim.ReviewDisposition.LEFT_UNKNOWN; default -> throw new IllegalArgumentException("Select C, R or U.");
        };
    }
}
