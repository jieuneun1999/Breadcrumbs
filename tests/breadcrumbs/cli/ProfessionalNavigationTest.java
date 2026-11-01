package breadcrumbs.cli;

import breadcrumbs.storage.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import static breadcrumbs.b4.Checks.*;

public final class ProfessionalNavigationTest {
    private ProfessionalNavigationTest() { }
    public static void main(String[] args) throws Exception { run(); }
    public static void run() throws Exception {
        Path root = Files.createTempDirectory(Files.createDirectories(Path.of("build/b7-fixtures")), "navigation-");
        var store = new WorkspaceStore(root);
        store.save(ContinuityTest.empty("W795", "Navigation review"));
        String script = "1\n1\n7\n1\n0\n0\n";
        var bytes = new ByteArrayOutputStream();
        String[] beforeSelection = {""};
        int checkAt = script.indexOf("\n0\n") + 1;
        Reader reader = new Reader() {
            int position;
            @Override public int read(char[] buffer, int offset, int length) {
                if (length == 0) { return 0; }
                if (position == checkAt) { beforeSelection[0] = bytes.toString(StandardCharsets.UTF_8); }
                if (position == script.length()) { return -1; }
                buffer[offset] = script.charAt(position++); return 1;
            }
            @Override public void close() { }
        };
        new ProjectConsole(reader, new PrintStream(bytes, true, StandardCharsets.UTF_8), store, new TerminalTheme(false), 120).run();
        String snapshot = beforeSelection[0];
        String afterSummary = snapshot.substring(snapshot.toLowerCase(java.util.Locale.ROOT).lastIndexOf("project summary"));
        for (int i = 1; i <= 12; i++) { require(afterSummary.contains("[" + i + "]"), "Every workspace action remains visible after a result: " + i); }
        require(afterSummary.contains("[0]") && afterSummary.contains("[?]"), "navigation and help are visible at the decision point");
        var wrapped = new ByteArrayOutputStream();
        new TerminalOutput(new PrintStream(wrapped, true, StandardCharsets.UTF_8), new TerminalTheme(false), 28)
                .plain("Review the project summary before recording another progress update.");
        String text = wrapped.toString(StandardCharsets.UTF_8);
        require(text.replaceAll("\\s+", " ").strip().equals("Review the project summary before recording another progress update."), "word wrapping retains complete readable words");
        require(store.load("W795").checkpoints().isEmpty(), "navigation does not create project history");
        System.out.println("PASS: visible workspace actions, professional navigation and word-aware spacing");
    }
}
