package breadcrumbs.cli;

import breadcrumbs.domain.WorkspaceState;
import breadcrumbs.storage.WorkspaceStore;
import java.io.IOException;
import java.io.PrintStream;

final class RecoveryConsole {
    @FunctionalInterface interface Leave { boolean confirmed() throws IOException; }
    private final ContextConsole.Input input;
    private final PrintStream out;
    private final WorkspaceStore store;
    private final TerminalTheme theme;
    private final int width; private final TerminalOutput ui;
    RecoveryConsole(ContextConsole.Input input, PrintStream out, WorkspaceStore store, TerminalTheme theme, int width) {
        this.input = input; this.out = out; this.store = store; this.theme = theme; this.width = width; ui = new TerminalOutput(out, theme, width);
    }
    WorkspaceState run(Leave leave) throws IOException {
        ui.view(ScreenId.RECOVERY);
        ui.heading("\nRESTORE A SAVED PROJECT"); ui.note("First inspect the saved copies. Nothing is restored until you confirm, and your original project folder is not read.");
        var savedProjects = store.list(); int number = 0;
        for (var saved : savedProjects) { ui.line("[" + (++number) + "] ", saved.name() + (saved.problem().isEmpty() ? "" : " | needs recovery")); }
        String id = input.read("Project number or workspace ID (0 = back): "); if (id.equals("0")) { return null; }
        try { int index = Integer.parseInt(id) - 1; if (index < 0 || index >= savedProjects.length) { throw new IllegalArgumentException("Choose a project from the list."); } id = savedProjects[index].id(); }
        catch (NumberFormatException suppliedId) { /* Existing workspace IDs remain accepted for advanced use. */ }
        WorkspaceStore.validateId(id);
        var copies = store.recoveryCandidates(id); number = 0;
        for (var candidate : copies) {
            ui.line("Copy " + (++number) + ": ", candidate.generation() + " | " + (candidate.valid() ? "checked and readable" : "cannot restore") + " | " + candidate.recordedAt());
            ui.line("Progress updates: ", Integer.toString(candidate.checkpoints()));
            if (!candidate.problem().isEmpty()) { ui.value("Problem with this copy", candidate.problem()); }
        }
        ui.note("You can inspect older copies. Only the newest readable history can be restored, so a restore cannot silently roll back later updates.");
        String generation = input.read("Copy number or generation ID to inspect (0 = back, prune = advanced cleanup): ");
        if (generation.equals("0")) { return null; }
        if (generation.equalsIgnoreCase("prune")) {
            ui.plain("This removes only validated older app-owned generations and retains the newest two. Invalid/interrupted evidence is retained.");
            store.prune(id, 2, input.read("Type PRUNE to confirm: ").equals("PRUNE")); ui.plain("Retention completed. Current and prior valid history retained."); return null;
        }
        try { int index = Integer.parseInt(generation) - 1; if (index < 0 || index >= copies.length) { throw new IllegalArgumentException("Choose a copy from the list."); } generation = copies[index].generation(); }
        catch (NumberFormatException suppliedId) { /* Generation IDs remain available to the exact acknowledgement flow. */ }
        WorkspaceState preview = store.readRecovery(id, generation);
        ui.line("Progress updates in this copy: ", Integer.toString(preview.checkpoints().size()));
        if (!leave.confirmed()) { return null; }
        boolean acknowledged = input.read("Type RECOVER " + generation + " to restore this checked copy as a new saved copy: ").equals("RECOVER " + generation);
        if (!acknowledged) { ui.plain("Recovery cancelled. Existing state retained."); return null; }
        final String selectedId = id, selectedCopy = generation;
        WorkspaceState recovered = OperationFeedback.run(out, theme, width, OperationFeedback.Operation.RECOVER, () -> store.recover(selectedId, selectedCopy, true));
        ui.success("Your checked project copy has been restored. Interrupted copies are kept for inspection.");
        ui.note("Keep only one Breadcrumbs app open for the same save folder. Sudden power loss can still interrupt disk writes.");
        return recovered;
    }
}
