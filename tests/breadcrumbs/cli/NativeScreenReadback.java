package breadcrumbs.cli;

import breadcrumbs.storage.WorkspaceStore;
import java.nio.file.Path;

/** Read-only reconciliation of the separately driven native demo. */
public final class NativeScreenReadback {
    private NativeScreenReadback() { }
    public static void main(String[] args) throws Exception {
        var store = new WorkspaceStore(Path.of(args[0]));
        var entries = store.list();
        if (entries.length != 1) { throw new AssertionError("Expected exactly one synthetic demo project"); }
        var state = store.load(entries[0].id());
        if (state.checkpoints().size() != 1
                || !state.checkpoints().getLatest().project().nextActions().equals("Confirm volunteer shifts before Friday")) {
            throw new AssertionError("Native demo progress count or exact saved next action differs");
        }
        System.out.println("PASS: read-only canonical load retains one actual progress update and its exact next action.");
    }
}
