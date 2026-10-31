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
    HashConsole(ContextConsole.Input input, PrintStream out, WorkspaceState state) { this.input = input; this.out = out; this.state = state; }
    void run() throws IOException {
        CustomHashTable<String> table = new CustomHashTable<>(4, 100);
        while (true) {
            out.println("SYNTHETIC HASH DEMO\n[1] Insert/update\n[2] Lookup\n[3] Remove\n[4] Collision + resize fixture\n[5] Buckets\n[6] Validate\n[7] Compare canonical/index checkpoint lookup\n[8] Toggle canonical indexing\n[9] Rebuild/validate canonical indexes\n[0] Back");
            String choice = input.read("hash > "); if (choice.equals("0")) { return; }
            try {
                switch (choice) {
                    case "1" -> table.put(input.read("Synthetic key: "), input.read("Synthetic value: "));
                    case "2" -> out.println("Found: " + breadcrumbs.handoff.HandoffService.clean(String.valueOf(table.get(input.read("Key: ")))));
                    case "3" -> out.println("Removed: " + breadcrumbs.handoff.HandoffService.clean(String.valueOf(table.remove(input.read("Key: ")))));
                    case "4" -> {
                        table.put("Aa", "DEMO-A"); table.put("BB", "DEMO-B");
                        out.println("Aa and BB collide: " + (table.bucketIndex("Aa") == table.bucketIndex("BB")));
                        out.print(table.displayBuckets());
                        for (int i = 0; i < 8; i++) { table.put("DEMO-" + i, "Synthetic " + i); }
                        out.println("Resized buckets: " + table.capacity());
                    }
                    case "5" -> out.print(table.displayBuckets());
                    case "6" -> out.println("Valid: " + table.validate());
                    case "7" -> {
                        String id = input.read("Actual checkpoint ID: ");
                        var canonical = state.checkpoints().findById(id); var indexed = state.indexes().checkpoint(id);
                        out.println("Same canonical value: " + (canonical == indexed) + " | mode enabled: " + state.indexes().enabled());
                    }
                    case "8" -> { state.indexes().setEnabled(!state.indexes().enabled()); out.println("Index enabled: " + state.indexes().enabled()); }
                    case "9" -> { state.indexes().rebuild(); out.println("Canonical indexes valid: " + state.indexes().validate()); }
                    default -> out.println("Choose 0-9.");
                }
                out.println("Hash expected get/put/remove O(1), collisions worst O(n), resize O(n), insertion amortized expected O(1). Diagnostics O(capacity+n), full validate worst O(n^2). Canonical rebuild includes scanning stored observations.");
                out.print(table.displayBuckets());
            } catch (IllegalArgumentException bad) { out.println("Rejected: " + breadcrumbs.inspector.ContentSafety.terminal(bad.getMessage(), 500)); }
        }
    }
}
