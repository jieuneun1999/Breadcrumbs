package breadcrumbs.cli;

import breadcrumbs.audit.ContextAudit;
import breadcrumbs.continuity.*;
import breadcrumbs.domain.*;
import java.io.PrintStream;

/** Read-only task language. Original statements and detailed reports retain their own meaning. */
final class ProjectPresentation {
    enum Basis { YOUR_UPDATE, DOCUMENT, NOT_PROVIDED }
    record Value(String text, Basis basis) { }
    record Summary(String project, String saved, String lastUpdate, Value nextStep, Value waitingFor, String attention, String fileCheck) { }
    private final TerminalOutput ui;
    ProjectPresentation(PrintStream out, TerminalTheme theme, int width) { ui = new TerminalOutput(out, theme, width); }

    static Summary summary(WorkspaceState state, boolean persisted) {
        var latest = state.checkpoints().getLatest();
        String saved = !persisted ? "This project is only open here. Save it before closing to keep your changes."
                : state.dirty() ? "You have changes that are not saved yet." : "Saved on this computer.";
        String last = latest == null ? "No progress updates yet. Record what you work on to make returning easier." : latest.summary();
        Value next = latest == null ? present(state, EvidenceClaim.Field.NEXT_ACTION) : value(latest.project().nextActions(), Basis.YOUR_UPDATE);
        Value waiting = latest == null ? present(state, EvidenceClaim.Field.BLOCKERS) : switch (latest.project().blockerState()) {
            case UNKNOWN -> new Value("Not provided yet", Basis.NOT_PROVIDED);
            case NONE_REPORTED -> new Value("Nothing reported when you saved this update", Basis.YOUR_UPDATE);
            case BLOCKED -> value(latest.project().blockers(), Basis.YOUR_UPDATE);
        };
        var report = new ContextAudit().evaluate(state, latest);
        String check = state.currentObservation().root() == null ? "No project folder is linked. You can still use notes and progress updates."
                : "Last file check: " + java.time.format.DateTimeFormatter.ofPattern("d MMM uuuu, h:mm a").withZone(java.time.ZoneId.systemDefault()).format(state.currentObservation().observedAt()) + ". Files may have changed since then.";
        return new Summary(state.activeBaseline().displayName(), saved, last, next, waiting, attention(report), check);
    }

    void showSummary(WorkspaceState state, boolean persisted) {
        Summary s = summary(state, persisted);
        ui.heading("\nPROJECT SUMMARY"); ui.line("Project: ", s.project()); ui.note(s.saved());
        ui.value("Where you left off", s.lastUpdate());
        ui.value("Next step", s.nextStep().text()); ui.value("Waiting on", s.waitingFor().text());
        if (s.nextStep().basis() == Basis.DOCUMENT || s.waitingFor().basis() == Basis.DOCUMENT) { ui.note("These details come from your documents. Breadcrumbs has not checked whether the work happened."); }
        else if (!state.checkpoints().isEmpty()) { ui.note("This is what you recorded earlier. Update it if your plans have changed."); }
        ui.value("Worth checking", s.attention()); ui.note("\n" + s.fileCheck());
    }

    void showHealth(ContextAudit.Report report) {
        ui.heading("\nWHAT NEEDS YOUR ATTENTION");
        ui.plain(switch (report.readiness()) {
            case INSUFFICIENT_EVIDENCE -> "A few project details still need to be added or reviewed.";
            case REVIEW_CHANGES -> "Some files linked to an earlier update have changed.";
            case NEEDS_CONTEXT -> "Review the items below before relying on your saved notes.";
            case READY_TO_RESUME -> "Your notes contain useful context for continuing. Check that the next step still fits.";
        });
        ui.value("A useful next action", attention(report));
        ui.note("This checks how useful your notes are for returning to work. It does not measure how much work is finished.\nDetailed findings are available in More project tools > Context audit.");
    }

    void showChanges(WorkspaceState state, DriftEngine.Comparison comparison) {
        ui.heading("\nCHANGES IN YOUR PROJECT FILES");
        if (state.currentObservation() == state.activeBaseline().snapshot()) {
            ui.note("There is no second file check to compare yet. Choose Check for changes when you want to read the folder again."); return;
        }
        int changed = 0;
        for (var change : comparison.changes()) {
            if (change.status() == DriftEngine.Status.UNCHANGED) { continue; }
            if (changed++ == 10) { ui.note("More changes are available in the detailed comparison."); break; }
            String label = switch (change.status()) {
                case MODIFIED -> "Updated file"; case ADDED -> "New file"; case DELETED -> "File no longer found";
                case RENAMED_EXACT -> "File moved or renamed"; case AMBIGUOUS_RENAME -> "Possible move; needs review";
                case UNKNOWN -> "Could not confirm this file's changes"; case UNCHANGED -> "Unchanged file";
            };
            ui.value(label, change.oldPath().equals(change.newPath()) ? change.newPath() : change.oldPath() + " -> " + change.newPath());
        }
        if (changed == 0) { ui.note("No changes were found among the files that could be checked."); }
        var latest = state.checkpoints().getLatest();
        if (latest != null && new ReferenceFreshness().assess(state, latest).state() == ReferenceFreshness.State.STALE) {
            ui.warning("A file linked to your latest progress update has changed. Review it before using that update as your current plan.");
        }
        ui.note("Your earlier updates and their original file references are kept. Files that could not be read may contain other changes.");
    }

    static String blocker(ProjectContext context) {
        return switch (context.blockerState()) {
            case UNKNOWN -> "Not provided yet"; case NONE_REPORTED -> "Nothing reported"; case BLOCKED -> context.blockers();
        };
    }

    static String noteType(ManualContext.Type type) {
        return switch (type) {
            case PERSONAL_OBSERVATION -> "Something you noticed"; case CURRENT_STATE -> "Where the project stands";
            case REPORTED_STATEMENT -> "Something someone told you"; case DECISION -> "A decision or agreement";
            case NEXT_ACTION_CANDIDATE -> "An idea for the next step";
        };
    }
    static String fieldLabel(EvidenceClaim.Field field) {
        return switch (field) {
            case OBJECTIVE -> "Project goal"; case DELIVERABLE -> "What you plan to deliver";
            case CURRENT_STATE, PROGRESS -> "Where the project stands"; case NEXT_ACTION -> "Suggested next step";
            case OWNER -> "Person responsible"; case BLOCKERS -> "What is getting in the way";
            case DECISION -> "Decision"; case RATIONALE -> "Reason"; case TEST_RESULTS -> "Reported test results";
            case TASK_STATUS, TASK_MARKER -> "Task update"; case DOCUMENT_TEXT -> "Text from the document";
        };
    }

    static String review(EvidenceClaim.ReviewDisposition disposition) {
        return switch (disposition) {
            case CONFIRMED -> "Reviewed by you"; case REJECTED -> "Not used";
            case PENDING -> "Not reviewed yet"; case LEFT_UNKNOWN -> "Left for later";
        };
    }

    private static Value present(WorkspaceState state, EvidenceClaim.Field field) {
        var review = CurrentAssessment.statement(state, field);
        if (review != null) { return value(review.value(), Basis.YOUR_UPDATE); }
        for (var claim : CurrentAssessment.claims(state)) {
            if (claim.field() == field && claim.review() == EvidenceClaim.ReviewDisposition.CONFIRMED) {
                return value(claim.literalValue(), claim.userSupplied() ? Basis.YOUR_UPDATE : Basis.DOCUMENT);
            }
        }
        return new Value("Not provided yet", Basis.NOT_PROVIDED);
    }

    private static Value value(String text, Basis basis) {
        if (text.equals("UNKNOWN") || text.isBlank()) { return new Value("Not provided yet", Basis.NOT_PROVIDED); }
        return new Value(text.equals("NOT_DOCUMENTED") ? "Not documented yet" : text, basis);
    }

    private static String attention(ContextAudit.Report report) {
        String priority = ContextAudit.priority(report);
        for (var finding : report.findings()) {
            if (!priority.startsWith(finding.id())) { continue; }
            if (finding.state() == ContextAudit.State.CONFLICTING) { return "Two notes give different answers. Review both before deciding which one to use."; }
            return switch (finding.id()) {
                case "CTX-NEXT" -> "Add or review the next small step you want to take.";
                case "CTX-STATE" -> "Describe where the project stands today.";
                case "CTX-BLOCK" -> "Note anything you are waiting for, or say that nothing is getting in the way.";
                case "CTX-PURPOSE" -> "Add or review what you want this project to achieve.";
                case "CTX-DELIV" -> "Describe what you plan to deliver.";
                case "CTX-DECISION" -> "Record an important decision and why you chose it.";
                case "CTX-OWNER" -> "Check who is responsible for the next step.";
                case "CTX-EVIDENCE" -> "Link a relevant note or document when you have one.";
                case "CTX-FRESH" -> finding.state() == ContextAudit.State.STALE
                        ? "A file linked to an earlier update has changed. Review the changes before continuing."
                        : "Check your files for changes when you want to confirm that the saved references are still current.";
                default -> "Review your project notes before continuing.";
            };
        }
        return "Check that your saved next step still matches what you want to do.";
    }
}
