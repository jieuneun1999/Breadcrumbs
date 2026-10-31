package breadcrumbs.cli;

import breadcrumbs.domain.WorkspaceState;
import breadcrumbs.storage.*;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;

final class DocumentsConsole {
    private final ContextConsole.Input input; private final PrintStream out; private final WorkspaceState state;
    private final WorkspaceStore store; private final DailyConsole.Action save; private final TerminalTheme theme;
    private final int width; private final TerminalOutput ui; private final ArtifactService artifacts = new ArtifactService();
    DocumentsConsole(ContextConsole.Input input, PrintStream out, WorkspaceState state, WorkspaceStore store, DailyConsole.Action save, TerminalTheme theme, int width) {
        this.input = input; this.out = out; this.state = state; this.store = store; this.save = save; this.theme = theme; this.width = width; ui = new TerminalOutput(out, theme, width);
    }
    private String[] allCheckpoints() {
        var checkpoints = state.checkpoints().traverseForward(); String[] ids = new String[checkpoints.length];
        for (int i = 0; i < ids.length; i++) { ids[i] = checkpoints[i].checkpointId(); } return ids;
    }
    void run() throws IOException {
        boolean showMenu = true;
        while (true) {
            if (showMenu) {
                ui.heading("\nSTATE AND HANDOFF DOCUMENTS");
                ui.note("Generated views are rebuildable. Source files are never copied or overwritten.");
                ui.menu("[1] View state.md\n[2] View HANDOFF.md\n[3] View historical checkpoint Markdown\n\n[4] Generate app-owned continuity documents\n[5] Configure checkpoint-triggered generation\n[6] Export portable Markdown package\n[0] Back"); showMenu = false;
            } else { ui.note("\n1=state  2=handoff  4=generate  6=export  ?=menu  0=back"); }
            String choice = input.read("documents > ");
            switch (choice) {
                case "0" -> { return; }
                case "?" -> showMenu = true;
                case "1", "2", "3" -> {
                    String id = choice.equals("1") ? "state" : choice.equals("2") ? "handoff" : input.read("Historical checkpoint ID: ");
                    String path = id.equalsIgnoreCase("state") ? "state.md" : id.equalsIgnoreCase("handoff") ? "HANDOFF.md" : "checkpoints/" + id + ".md";
                    var bundle = artifacts.preview(state, allCheckpoints()); boolean found = false;
                    for (var file : bundle.files()) { if (file.path().equals(path)) { ui.plain(file.text()); found = true; } }
                    if (!found) { ui.warning("Choose state, handoff or an existing checkpoint ID."); }
                    if (state.dirty()) { ui.warning("Preview only. This canonical revision is not yet durably saved."); }
                }
                case "4" -> {
                    var bundle = artifacts.preview(state, allCheckpoints()); preview(bundle);
                    if (input.read("Generate these app-owned views after matching the saved revision? Type yes: ").equalsIgnoreCase("yes")) {
                        Path path = OperationFeedback.run(out, theme, width, OperationFeedback.Operation.EXPORT, () -> artifacts.materialize(state, store, bundle));
                        ui.success("Continuity documents generated."); ui.value("App-owned bundle", path.toString());
                    }
                }
                case "5" -> {
                    ui.warning("Opt-in writes state.md, HANDOFF.md and immutable checkpoint summaries in app-owned storage after a successful canonical save. Raw source snippets and sensitive/unapproved manual bodies stay omitted. Preview views before approving.");
                    boolean enable = !state.manualContext().artifactsAllowed();
                    if (input.read(enable ? "Enable this preference? Type yes: " : "Disable this preference? Type yes: ").equalsIgnoreCase("yes")) {
                        state.manualContext().setArtifactsAllowed(enable); save.run();
                        ui.value("Checkpoint-triggered generation preference", Boolean.toString(enable));
                    }
                }
                case "6" -> {
                    String selected = input.read("Selected checkpoint IDs, comma-separated; blank exports no checkpoint archives: ");
                    String[] ids = selected.isBlank() ? new String[0] : selected.split(","); for (int i = 0; i < ids.length; i++) { ids[i] = ids[i].strip(); }
                    var bundle = artifacts.preview(state, ids); Path destination = Path.of(input.read("New absolute local package directory, outside source/app storage: "));
                    preview(bundle); ui.value("Destination", destination.toString());
                    if (input.read("I reviewed privacy/redaction and consent to this create-only package. Type yes: ").equalsIgnoreCase("yes")) {
                        Path path = OperationFeedback.run(out, theme, width, OperationFeedback.Operation.EXPORT, () -> artifacts.exportPackage(state, store, bundle, destination, true));
                        ui.success("Portable package exported."); ui.value("Readable Markdown folder", path.toString());
                    }
                }
                default -> ui.warning("Choose a listed document action.");
            }
        }
    }
    private void preview(ArtifactService.Bundle bundle) {
        ui.value("Canonical revision", bundle.revision());
        for (var file : bundle.files()) { ui.value("Generated member", file.path() + " | " + file.text().length() + " characters"); }
        ui.warning("INTERNAL context. Only individually approved manual bodies are included. No source documents, absolute source roots or credentials are copied.");
    }
}
