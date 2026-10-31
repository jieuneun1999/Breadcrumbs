package breadcrumbs.cli;

import breadcrumbs.domain.*;
import breadcrumbs.handoff.HandoffService;
import java.io.IOException;
import java.io.PrintStream;

/** Explicit mutation previews. Nothing imported automatically becomes a task or edge. */
final class DependencyConsole {
    private final ContextConsole.Input input;
    private final PrintStream out;
    private final WorkspaceState state;
    private final TerminalOutput ui;
    DependencyConsole(ContextConsole.Input input, PrintStream out, WorkspaceState state) {
        this(input, out, state, new TerminalTheme(false), 80);
    }
    DependencyConsole(ContextConsole.Input input, PrintStream out, WorkspaceState state, TerminalTheme theme, int width) {
        this.input = input; this.out = out; this.state = state;
        ui = new TerminalOutput(out, theme, width);
    }
    void run() throws IOException {
        while (true) {
            ui.heading("PROJECT DEPENDENCIES"); ui.line("Revision: ", state.dependencies().currentId());
            ui.menu("[1] List work items\n[2] Create work item\n[3] Add dependency\n[4] Remove dependency\n[5] Adjacency\n[6] Potential downstream impact\n[7] BFS\n[8] DFS\n[9] Validate\n[0] Back");
            String option = input.read("dependencies > ");
            if (option.equals("0")) { return; }
            try {
                switch (option) {
                    case "1" -> {
                        if (state.dependencies().current() == null) { ui.plain("No canonical work items. Imported claims are candidates only."); }
                        else { for (WorkItem item : state.dependencies().current().items()) {
                            ui.plain(item.id() + " | " + HandoffService.clean(item.title()) + " | explicit " + item.status());
                        } }
                    }
                    case "2" -> {
                        String title = input.read("Title: "), description = input.read("Description: ");
                        WorkItem.Status status = WorkItem.Status.valueOf(input.read("Explicit status [UNKNOWN/NOT_STARTED/IN_PROGRESS/BLOCKED/COMPLETED]: "));
                        String owner = input.read("Owner (blank unknown): "), due = input.read("Due date YYYY-MM-DD (blank unknown): ");
                        String refs = input.read("Current observation evidence IDs comma-separated (blank none): ");
                        String[] ids = refs.isBlank() ? new String[0] : refs.split(",");
                        for (int i = 0; i < ids.length; i++) { ids[i] = ids[i].strip(); }
                        ui.plain("PREVIEW user present assessment | " + HandoffService.clean(title) + " | " + status + ". Past actions not independently verified.");
                        if (input.read("Confirm creation? yes: ").equalsIgnoreCase("yes")) {
                            ui.plain("Created " + state.dependencies().create(title, description, status, owner,
                                    due.isBlank() ? null : java.time.LocalDate.parse(due), ids, true).id());
                        } else { ui.plain("Cancelled. Canonical state unchanged."); }
                    }
                    case "3", "4" -> {
                        String from = input.read("Prerequisite ID: "), to = input.read("Dependent ID: ");
                        WorkItem a = state.dependencies().findItem(from), b = state.dependencies().findItem(to);
                        if (a == null || b == null) { throw new IllegalArgumentException("Existing canonical work items required."); }
                        ui.plain("PREVIEW " + from + " " + HandoffService.clean(a.title()) + " -> " + to + " " + HandoffService.clean(b.title()));
                        String evidence = "", declaration = "";
                        if (option.equals("3")) { evidence = input.read("Accepted source evidence ID (blank direct declaration): "); declaration = input.read("Explicit declaration / rationale: "); }
                        if (input.read("Confirm change? yes: ").equalsIgnoreCase("yes")) {
                            if (option.equals("3")) { state.dependencies().add(from, to, evidence, declaration, true); }
                            else { state.dependencies().remove(from, to, true); }
                            ui.plain("Confirmed revision " + state.dependencies().currentId() + ". Save project to persist.");
                        } else { ui.plain("Cancelled. Canonical state unchanged."); }
                    }
                    case "5" -> ui.plain(state.dependencies().graph().displayAdjacencyList());
                    case "6", "7", "8" -> {
                        String start = input.read("Starting work item ID: ");
                        var graph = state.dependencies().graph();
                        var visits = option.equals("8") ? graph.depthFirst(start) : graph.breadthFirst(start);
                        ui.plain("Potentially affected work items | revision " + state.dependencies().currentId());
                        for (var visit : visits) { ui.plain(visit.id() + " | level " + visit.level() + " | via " + visit.via()); }
                        ui.plain("Unique downstream: " + (visits.length - 1) + ". Statuses unchanged. BFS/DFS O(V+E) after O(V) start lookup; validation O(V+E).");
                    }
                    case "9" -> ui.plain("Graph valid: " + state.dependencies().graph().validate());
                    default -> ui.plain("Choose 0-9.");
                }
            } catch (IllegalArgumentException | java.time.DateTimeException invalid) { ui.plain("Rejected: " + HandoffService.clean(invalid.getMessage())); }
        }
    }
}
