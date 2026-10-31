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
        ui.heading("RECOVERY CANDIDATES"); ui.note("Inspection is read-only; no source scan occurs.");
        for (var saved : store.list()) { ui.plain(saved.id() + " | " + breadcrumbs.inspector.ContentSafety.terminal(saved.name(), 120) + (saved.problem().isEmpty() ? "" : " | needs recovery")); }
        String id = input.read("Workspace ID (0 = back): "); if (id.equals("0")) { return null; }
        WorkspaceStore.validateId(id);
        for (var candidate : store.recoveryCandidates(id)) {
            ui.plain(candidate.generation() + " | valid=" + candidate.valid() + " | recorded=" + candidate.recordedAt()
                    + " | checkpoints=" + candidate.checkpoints() + " | " + candidate.problem());
        }
        ui.plain("Older generations can be previewed; recovery only activates the newest intact history. No silent rollback.");
        String generation = input.read("Generation to preview (0 = back, prune = retain newest two): ");
        if (generation.equals("0")) { return null; }
        if (generation.equalsIgnoreCase("prune")) {
            ui.plain("This removes only validated older app-owned generations and retains the newest two. Invalid/interrupted evidence is retained.");
            store.prune(id, 2, input.read("Type PRUNE to confirm: ").equals("PRUNE")); ui.plain("Retention completed. Current and prior valid history retained."); return null;
        }
        WorkspaceState preview = store.readRecovery(id, generation);
        ui.plain("Read-only preview: " + id + " | checkpoint count=" + preview.checkpoints().size() + " | next ID=" + preview.nextCheckpointNumber());
        if (!leave.confirmed()) { return null; }
        boolean acknowledged = input.read("Type RECOVER " + generation + " to activate a newly saved recovery generation: ").equals("RECOVER " + generation);
        if (!acknowledged) { ui.plain("Recovery cancelled. Existing state retained."); return null; }
        WorkspaceState recovered = OperationFeedback.run(out, theme, width, OperationFeedback.Operation.RECOVER, () -> store.recover(id, generation, true));
        ui.plain("Recovered validated generation " + generation + ". Interrupted bytes retained; current pointer read back.");
        ui.plain("Atomic rename and file flush are used where supported. Power-loss/directory-flush durability is not guaranteed. One process per storage root.");
        return recovered;
    }
}
