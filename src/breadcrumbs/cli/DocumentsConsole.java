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
    private static final String ACTIONS = "[1] View project summary file\n[2] View handoff document\n[3] View archived progress update\n[4] Refresh handoff documents\n[5] Configure automatic document updates\n[6] Export handoff package\n[D] View technical file details\n[0] Back to workspace\n[?] Show menu";
    void run() throws IOException {
        boolean showMenu = true;
        while (true) {
            if (showMenu) {
                ui.heading("\nProject handoff");
                ui.note("Preview or export project context for continuing work. Source documents are not copied or modified.");
                ui.screen(BreadFrames.Mood.IDLE, "Choose which project context to prepare for later.", ACTIONS, false); showMenu = false;
            } else { ui.heading("\nHandoff actions"); ui.resultScreen(BreadFrames.Mood.IDLE, "Your original documents stay in their folder.", ACTIONS); }
            String choice = input.read("Handoff > ");
            switch (choice.toLowerCase(java.util.Locale.ROOT)) {
                case "0" -> { return; }
                case "?" -> showMenu = true;
                case "1", "2", "3" -> {
                    String id = choice.equals("1") ? "state" : choice.equals("2") ? "handoff" : checkpointId(input.read("Earlier update number or ID: "));
                    String path = id.equalsIgnoreCase("state") ? "state.md" : id.equalsIgnoreCase("handoff") ? "HANDOFF.md" : "checkpoints/" + id + ".md";
                    var bundle = artifacts.preview(state, allCheckpoints()); boolean found = false;
                    for (var file : bundle.files()) { if (file.path().equals(path)) { ui.plain(file.text()); found = true; } }
                    if (!found) { ui.warning("Choose a summary, handoff or existing progress update."); }
                    if (state.dirty()) { ui.warning("Preview only. Save the project before using these files as a saved handoff."); }
                }
                case "4" -> {
                    var bundle = artifacts.preview(state, allCheckpoints()); preview(bundle);
                    if (input.read("Refresh these handoff files from your saved project? Type yes: ").equalsIgnoreCase("yes")) {
                        Path path = OperationFeedback.run(out, theme, width, OperationFeedback.Operation.EXPORT, () -> artifacts.materialize(state, store, bundle));
                        ui.success("Your handoff files are up to date."); ui.value("Handoff files on this computer", path.toString());
                    }
                }
                case "d" -> { var bundle = artifacts.preview(state, allCheckpoints()); ui.value("Saved project reference", bundle.revision()); for (var file : bundle.files()) { ui.line("File: ", file.path() + " | " + file.text().length() + " characters"); } }
                case "5" -> {
                    ui.warning("If enabled, Breadcrumbs updates the project summary, handoff and saved-update files after a successful save. Private notes and notes you did not approve for export stay out. Your original documents are not copied.");
                    boolean enable = !state.manualContext().artifactsAllowed();
                    if (input.read(enable ? "Update the handoff files whenever you save? Type yes: " : "Stop updating the handoff files after saving? Type yes: ").equalsIgnoreCase("yes")) {
                        state.manualContext().setArtifactsAllowed(enable); save.run();
                        ui.value("Update handoff files after saving", enable ? "On" : "Off");
                        if (state.dirty()) { ui.warning("This choice is only set for the open project. Save again to keep it after closing."); }
                    }
                }
                case "6" -> {
                    String selected = input.read("Progress update numbers or IDs to include, separated by commas; Enter includes no earlier updates: ");
                    String[] ids = selected.isBlank() ? new String[0] : selected.split(","); for (int i = 0; i < ids.length; i++) { ids[i] = checkpointId(ids[i].strip()); }
                    var bundle = artifacts.preview(state, ids); Path destination = Path.of(input.read("New full folder path for the handoff, outside your project and save folders: "));
                    preview(bundle); ui.value("Destination", destination.toString());
                    if (input.read("Create this new handoff folder with the files shown above? Type yes: ").equalsIgnoreCase("yes")) {
                        Path path = OperationFeedback.run(out, theme, width, OperationFeedback.Operation.EXPORT, () -> artifacts.exportPackage(state, store, bundle, destination, true));
                        ui.success("Handoff package exported successfully."); ui.value("Export folder", path.toString());
                    }
                }
                default -> ui.warning("Choose a listed document action.");
            }
        }
    }
    private static String checkpointId(String value) {
        try { int number = Integer.parseInt(value); if (number < 1 || number > 999999) { throw new IllegalArgumentException("Choose an existing progress update number."); } return String.format(java.util.Locale.ROOT, "CP-%06d", number); }
        catch (NumberFormatException notNumber) { return value; }
    }
    private void preview(ArtifactService.Bundle bundle) {
        for (var file : bundle.files()) { ui.value("File", file.path()); }
        ui.warning("Review this handoff before sharing. Only notes you approved for export are included. Original documents and the project folder location are not copied.");
    }
}
