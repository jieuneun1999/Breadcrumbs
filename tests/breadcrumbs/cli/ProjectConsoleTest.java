package breadcrumbs.cli;

import breadcrumbs.storage.ContinuityTest;
import breadcrumbs.storage.WorkspaceStore;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

public final class ProjectConsoleTest {
    private static int assertions;
    private ProjectConsoleTest() { }
    public static void run() throws Exception {
        Path base = Files.createDirectories(Path.of("build", "b3-cli-fixtures"));
        Path root = Files.createTempDirectory(base, "storage-");
        String empty = session(root, "x\n1\n3\n0\n", false, 80);
        contains(empty, "B R E A D C R U M B S", "Leave context behind. Pick up where work stopped.",
                "Saved projects: 0", "No saved workspaces", "No active workspace", "Choose 0-4", "Goodbye");
        require(!empty.contains("\u001b"), "plain fallback emits no ANSI escapes");
        String narrow = session(root, "0\n", false, 30);
        contains(narrow, "o . o  BREADCRUMBS", "Leave context behind.\nPick up where work stopped.");
        String colored = session(root, "0\n", true, 80);
        contains(colored, "\u001b[1;38;5;214m", "\u001b[0m", "\u001b[32mStorage: Ready\u001b[0m");
        require(!TerminalTheme.enabled(true, true, true, false, "xterm", false, true), "no-color wins explicit always");
        require(!TerminalTheme.enabled(false, true, true, true, "xterm", false, true), "NO_COLOR wins always");
        require(!TerminalTheme.enabled(false, false, false, false, "xterm", false, true), "redirected suppresses color");
        require(!TerminalTheme.enabled(false, false, true, false, "dumb", false, true), "limited terminal suppresses color");
        require(!TerminalTheme.enabled(false, false, true, false, "xterm", true, false), "uncertain CMD suppresses color");
        require(TerminalTheme.enabled(false, true, false, false, "dumb", true, false), "explicit always enables supported diagnostic path");
        require(TerminalTheme.enabled(false, false, true, false, "xterm", false, false), "known interactive terminal enables color");
        require(TerminalTheme.enabled(false, false, true, false, null, true, true), "Windows Terminal supports auto color without TERM");
        contains(session(root, "4\n12\n11\n0\n0\n", false, 80), "Loaded 5", "Integrity valid", "SYNTHETIC");
        String guided = "2\n1\nPlanner\nACADEMIC\nWrite report\nReport\nReview\n\n\n\nyes\nyes\n";
        String create = session(root, guided + "3\n3\n2\nDraft\nStarted\nWrite report\nUNKNOWN\n\nReview\n\n\n\nno\n8\n0\n0\n", false, 80);
        contains(create, "Baseline B0 established", "Saved. Verified", "No native checkpoint", "Cancelled. Project history unchanged",
                "DLL structure: NULL", "Integrity: true");
        WorkspaceStore store = new WorkspaceStore(root);
        require(store.list().length == 1, "first B0 saved from actual CLI");
        require(store.load(store.list()[0].id()).checkpoints().isEmpty(), "declined native checkpoint not saved");
        String cp = session(root, "1\n1\n3\n2\nFirst real\nDraft ready\nWrite report\nNONE_REPORTED\nnone reported\nReview\nChose structure\nStudent\n\nyes\n3\n4\n8\n0\n0\n", false, 80);
        contains(cp, "Opened saved workspace", "No source rescan", "CHECKPOINT PREVIEW", "Confirmed CP-000001", "History backward:",
                "CP-000001", "Blocker state: NONE_REPORTED", "Storage status: SAVED", "Integrity: true");
        require(store.load(store.list()[0].id()).checkpoints().size() == 1, "confirmed native CP persisted from CLI");
        String restart = session(root, "1\n1\n3\n3\n7\n0\n0\n", false, 80);
        contains(restart, "Saved projects: 1", "First real", "Next native number: 2", "Freshness UNKNOWN");
        String sessionOnly = guided.replace("yes\nyes\n", "yes\n\n");
        Path isolated = Files.createTempDirectory(base, "unsaved-");
        String guard = session(isolated, sessionOnly + "2\n\n3\n7\n0\n0\n\n0\nd\n", false, 80);
        contains(guard, "SESSION-ONLY / UNSAVED", "Cancelled. Active workspace unchanged", "Planner", "Unsaved workspace");
        require(new WorkspaceStore(isolated).list().length == 0, "discarded session-only state not written");
        Path multiple = Files.createTempDirectory(base, "multiple-");
        WorkspaceStore many = new WorkspaceStore(multiple);
        many.save(ContinuityTest.empty("W001", "One")); many.save(ContinuityTest.empty("W002", "Two"));
        String switched = session(multiple, "1\n1\n3\n6\n2\n3\n7\n0\n0\n", false, 80);
        contains(switched, "ACTIVE PROJECT | One | W001", "ACTIVE PROJECT | Two | W002");
        Path broken = multiple.resolve("workspaces/W002/state.txt"); Files.writeString(broken, "broken");
        String corrupt = session(multiple, "1\n1\n1\n2\n3\n7\n0\n0\n", false, 80);
        contains(corrupt, "unreadable saved workspace", "Storage/input operation failed", "ACTIVE PROJECT | One | W001");
        Path blocked = base.resolve("blocked-" + System.nanoTime()); Files.writeString(blocked, "file");
        String noStorage = session(blocked, guided + "3\n7\n0\n0\ns\n0\nd\n", false, 80);
        contains(noStorage, "Storage: Unavailable", "NOT SAVED. In-memory state retained", "UNSAVED / retry Save", "Goodbye");
        String eof = session(Files.createTempDirectory(base, "eof-"), sessionOnly, false, 80);
        contains(eof, "End of input", "unsaved session state will be discarded", "Goodbye");
        String cancelled = session(root, "2\n1\n/cancel\n0\n", false, 80);
        contains(cancelled, "Operation cancelled", "Goodbye");
        String injection = session(root, "2\n1\n\u001b[31mEVIL\nGENERAL\n\n\n\n\n\n\nyes\nyes\n0\n", false, 80);
        require(!injection.contains("\u001b"), "untrusted title cannot inject ANSI into terminal output");
        contains(session(root, "a".repeat(8193) + "\n0\n", false, 80), "Input exceeds 8192", "Goodbye");
        retrySaveAndOpenSameProject(base);
        System.out.println("PASS: B3 CLI/brand (" + assertions + " assertions)");
    }
    private static String session(Path root, String input, boolean color, int width) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream out = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            new ProjectConsole(new StringReader(input), out, new WorkspaceStore(root), new TerminalTheme(color), width).run();
        }
        return bytes.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    private static void retrySaveAndOpenSameProject(Path base) throws Exception {
        Path root = Files.createTempDirectory(base, "retry-");
        WorkspaceStore store = new WorkspaceStore(root);
        store.save(ContinuityTest.empty("W001", "Retry project"));
        Path file = root.resolve("workspaces/W001/state.txt");
        byte[] original = Files.readAllBytes(file);
        String script = "1\n1\n3\n2\nRetry checkpoint\nRecorded progress\nObjective\nUNKNOWN\n\nNext\n\n\n\nyes\n6\n1\ns\n3\n3\n7\n0\n0\n";
        int breakAt = script.indexOf("Retry checkpoint");
        int restoreAt = script.indexOf("6\n1\ns\n");
        Reader scripted = new Reader() {
            private int position;
            @Override public int read(char[] buffer, int offset, int length) throws IOException {
                if (length == 0) { return 0; }
                if (position == script.length()) { return -1; }
                if (position == breakAt) { Files.writeString(file, "disposable corrupt record"); }
                if (position == restoreAt) { Files.write(file, original); }
                buffer[offset] = script.charAt(position++); return 1;
            }
            @Override public void close() { }
        };
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream out = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            new ProjectConsole(scripted, out, store, new TerminalTheme(false), 80).run();
        }
        String text = bytes.toString(StandardCharsets.UTF_8);
        int lastOpen = text.lastIndexOf("Opened saved workspace");
        require(lastOpen >= 0 && text.substring(lastOpen).contains("Retry checkpoint"),
                "saving before reopening same project retains the newly saved checkpoint in active state");
        contains(text, "NOT SAVED. In-memory state retained", "Unsaved workspace", "Next native number: 2");
        require(store.load("W001").checkpoints().size() == 1, "retry saved checkpoint is durable");
    }
    private static void contains(String text, String... fragments) {
        for (String fragment : fragments) { require(text.contains(fragment), "missing output: " + fragment + "\n" + text); }
    }
    private static void require(boolean condition, String message) {
        assertions++; if (!condition) { throw new AssertionError(message); }
    }
}
