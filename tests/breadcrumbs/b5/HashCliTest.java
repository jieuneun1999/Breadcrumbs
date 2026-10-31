package breadcrumbs.b5;

import breadcrumbs.b4.WorkflowTest;
import breadcrumbs.cli.*;
import breadcrumbs.storage.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.*;
import static breadcrumbs.b4.Checks.*;

public final class HashCliTest {
    private HashCliTest() { }
    public static void run() throws Exception {
        Path base = Files.createTempDirectory(Files.createDirectories(Path.of("build/b5-fixtures")), "hash-cli-").toAbsolutePath();
        var state = WorkflowTest.imported(base, "W505"); WorkflowTest.checkpoint(state, "Synthetic", new String[0]);
        var store = new WorkspaceStore(base.resolveSibling(base.getFileName() + "-app")); store.save(state);
        var bytes = new ByteArrayOutputStream();
        String input = "1\n1\n3\n17\n4\n1\nTEST\nFirst\n1\nTEST\nUpdated\n2\nTEST\n3\nTEST\n6\n7\nCP-000001\n8\n7\nCP-000001\n9\n0\n0\n0\n";
        new ProjectConsole(new StringReader(input), new PrintStream(bytes, true, StandardCharsets.UTF_8), store, new TerminalTheme(false), 80).run();
        String text = bytes.toString(StandardCharsets.UTF_8);
        for (String expected : new String[] {"Aa and BB collide: true", "Resized buckets", "Found: Updated", "Removed: Updated", "Valid: true", "Same canonical value: true", "Index enabled: false", "Canonical indexes valid: true", "Goodbye"}) {
            require(text.contains(expected), "Hash CLI " + expected);
        }
        Files.writeString(base.resolve("hash-cli.txt"), text);
        System.out.println("PASS: B5B real Hash CLI | " + base);
    }
}
