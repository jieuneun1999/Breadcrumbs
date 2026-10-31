package breadcrumbs.b5;

import breadcrumbs.b4.WorkflowTest;
import breadcrumbs.cli.*;
import breadcrumbs.storage.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import static breadcrumbs.b4.Checks.*;

public final class GraphCliTest {
    private GraphCliTest() { }
    public static void main(String[] args) throws Exception { run(); }
    public static void run() throws Exception {
        Path base = Files.createTempDirectory(Files.createDirectories(Path.of("build/b5-fixtures")), "cli-").toAbsolutePath();
        Path source = Files.createDirectory(base.resolve("source"));
        Files.writeString(source.resolve("README.md"), "# Tasks\n- [ ] TASK-9 Candidate\n");
        var store = new WorkspaceStore(base.resolve("app")); store.save(WorkflowTest.imported(source, "W502"));
        String input = "1\n1\n3\n16\n1\n";
        for (String title : new String[] {"Approve budget", "Book supplier", "Finalize event"}) {
            input += "2\n" + title + "\nSynthetic school demo\nUNKNOWN\n\n\n\nyes\n";
        }
        input += "3\nT-000001\nT-000002\n\nExplicit budget prerequisite\nyes\n3\nT-000002\nT-000003\n\nExplicit supplier prerequisite\nyes\n5\n6\nT-000001\n7\nT-000001\n8\nT-000001\n3\nT-000003\nT-000001\n\nCycle attempt\nyes\n9\n0\n5\n0\n0\n";
        String text = session(store, input);
        for (String expected : new String[] {"No canonical work items", "Created T-000003", "Unique downstream: 2", "would create a cycle", "Graph valid: true", "Saved. Verified", "Goodbye"}) {
            require(text.contains(expected), "Graph actual CLI " + expected);
        }
        var restored = store.load("W502"); require(restored.dependencies().graph().edgeCount() == 2, "CLI graph persisted");
        String reopened = session(store, "1\n1\n3\n16\n5\n9\n0\n0\n0\n");
        require(reopened.contains("T-000001 -> T-000002") && reopened.contains("Graph valid: true"), "CLI restart graph reconstruction");
        Files.writeString(base.resolve("graph-cli.txt"), text); Files.writeString(base.resolve("restart.txt"), reopened);
        System.out.println("PASS: B5A actual CLI and restart | " + base);
    }
    private static String session(WorkspaceStore store, String input) throws Exception {
        var bytes = new ByteArrayOutputStream();
        new ProjectConsole(new StringReader(input), new PrintStream(bytes, true, StandardCharsets.UTF_8), store, new TerminalTheme(false), 80).run();
        return bytes.toString(StandardCharsets.UTF_8);
    }
}
