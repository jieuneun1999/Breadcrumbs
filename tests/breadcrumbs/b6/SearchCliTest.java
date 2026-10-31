package breadcrumbs.b6;

import breadcrumbs.b4.WorkflowTest;
import breadcrumbs.cli.*;
import breadcrumbs.storage.WorkspaceStore;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import static breadcrumbs.b4.Checks.*;

public final class SearchCliTest {
    private SearchCliTest() { }
    public static void main(String[] args) throws Exception { run(); }
    public static void run() throws Exception {
        Path base = Files.createTempDirectory(Files.createDirectories(Path.of("build/b6-fixtures")), "cli-").toAbsolutePath();
        Path source = Files.createDirectory(base.resolve("source"));
        Files.writeString(source.resolve("notes.md"), "# Objective\nmethod methodology methodology methods\n");
        WorkspaceStore store = new WorkspaceStore(base.resolve("app"));
        var state = WorkflowTest.imported(source, "W611");
        WorkflowTest.checkpoint(state, "Methodology checkpoint", new String[0]); store.save(state);
        String input = "1\n1\n3\n18\n6\nyes\n4\n1\nmethodology\n0\n2\nmeth\n2\n0\n3\n5\n7\n1\nmethodology\n7\n1\nmissingword\n1\n\n0\n0\n0\n";
        String text = session(store, input, 80);
        for (String expected : new String[] {"SEARCH PROJECT CONTEXT", "Exact term: methodology", "SOURCE_REPORTED", "line 2", "Occurrences: 2",
                "HISTORICAL CHECKPOINT", "methodology", "captured fields only", "body not searched", "canonical valid=true", "DISABLED", "No indexed matches", "Rejected", "Goodbye"}) {
            require(text.contains(expected), "search CLI behavior " + expected);
        }
        require(!text.contains("\u001b"), "plain/no-color search output");
        String narrow = session(store, "1\n1\n3\n18\n6\nyes\n4\n1\nmethodology\n0\n0\n0\n0\n", 40);
        int start = narrow.indexOf("SEARCH PROJECT CONTEXT"), end = narrow.lastIndexOf("search > ");
        for (String line : narrow.substring(start, end).split("\\R")) {
            require(line.codePointCount(0, line.length()) <= 40, "narrow search line is bounded");
        }
        String restart = session(store, "1\n1\n3\n18\n4\n1\nmethodology\n0\n0\n0\n0\n", 80);
        require(restart.contains("INDEX_UNAVAILABLE") && restart.contains("HISTORICAL CHECKPOINT"), "restart rebuild from canonical fields");
        Files.writeString(base.resolve("search-cli.txt"), text); Files.writeString(base.resolve("narrow.txt"), narrow);
        Files.writeString(base.resolve("restart.txt"), restart);
        System.out.println("PASS: B6 actual search CLI, narrow output and restart | " + base);
    }
    private static String session(WorkspaceStore store, String input, int width) throws Exception {
        var bytes = new ByteArrayOutputStream();
        new ProjectConsole(new StringReader(input), new PrintStream(bytes, true, StandardCharsets.UTF_8), store, new TerminalTheme(false), width).run();
        return bytes.toString(StandardCharsets.UTF_8);
    }
}
