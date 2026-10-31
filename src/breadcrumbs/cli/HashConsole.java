package breadcrumbs.cli;

import breadcrumbs.domain.WorkspaceState;
import breadcrumbs.dsa.hash.CustomHashTable;
import java.io.IOException;
import java.io.PrintStream;

/** Synthetic hash sandbox is independent of actual project records. */
final class HashConsole {
    private final ContextConsole.Input input;
    private final PrintStream out;
    private final WorkspaceState state;
    private final TerminalOutput ui;
    private final TerminalTheme theme; private final int width;
    HashConsole(ContextConsole.Input input, PrintStream out, WorkspaceState state) { this(input, out, state, new TerminalTheme(false), 80); }
    HashConsole(ContextConsole.Input input, PrintStream out, WorkspaceState state, TerminalTheme theme, int width) { this.input = input; this.out = out; this.state = state; this.theme = theme; this.width = width; ui = new TerminalOutput(out, theme, width); }
    void run() throws IOException {
        CustomHashTable<String> table = new CustomHashTable<>(4, 100);
        while (true) {
            ui.heading("SYNTHETIC HASH DEMO"); ui.menu("[1] Insert/update\n[2] Lookup\n[3] Remove\n[4] Collision + resize fixture\n[5] Buckets\n[6] Validate\n[7] Compare canonical/index checkpoint lookup\n[8] Toggle canonical indexing\n[9] Rebuild/validate canonical indexes\n[0] Back");
            String choice = input.read("hash > "); if (choice.equals("0")) { return; }
            try {
                switch (choice) {
                    case "1" -> table.put(input.read("Synthetic key: "), input.read("Synthetic value: "));
                    case "2" -> ui.plain("Found: " + breadcrumbs.handoff.HandoffService.clean(String.valueOf(table.get(input.read("Key: ")))));
                    case "3" -> ui.plain("Removed: " + breadcrumbs.handoff.HandoffService.clean(String.valueOf(table.remove(input.read("Key: ")))));
                    case "4" -> {
                        table.put("Aa", "DEMO-A"); table.put("BB", "DEMO-B");
                        ui.plain("Aa and BB collide: " + (table.bucketIndex("Aa") == table.bucketIndex("BB")));
                        ui.plain(table.displayBuckets());
                        for (int i = 0; i < 8; i++) { table.put("DEMO-" + i, "Synthetic " + i); }
                        ui.plain("Resized buckets: " + table.capacity());
                    }
                    case "5" -> ui.plain(table.displayBuckets());
                    case "6" -> ui.plain("Valid: " + table.validate());
                    case "7" -> {
                        String id = input.read("Actual checkpoint ID: ");
                        var canonical = state.checkpoints().findById(id); var indexed = state.indexes().checkpoint(id);
                        ui.plain("Same canonical value: " + (canonical == indexed) + " | mode enabled: " + state.indexes().enabled());
                    }
                    case "8" -> { state.indexes().setEnabled(!state.indexes().enabled()); ui.plain("Index enabled: " + state.indexes().enabled()); }
                    case "9" -> { OperationFeedback.run(out, theme, width, OperationFeedback.Operation.REBUILD, () -> { state.indexes().rebuild(); return true; }); ui.line("Canonical indexes valid: ", Boolean.toString(state.indexes().validate())); }
                    default -> ui.plain("Choose 0-9.");
                }
                ui.plain("Hash expected get/put/remove O(1), collisions worst O(n), resize O(n), insertion amortized expected O(1). Diagnostics O(capacity+n), full validate worst O(n^2). Canonical rebuild includes scanning stored observations.");
                ui.plain(table.displayBuckets());
            } catch (IllegalArgumentException bad) { ui.plain("Rejected: " + breadcrumbs.inspector.ContentSafety.terminal(bad.getMessage(), 500)); }
        }
    }
}
