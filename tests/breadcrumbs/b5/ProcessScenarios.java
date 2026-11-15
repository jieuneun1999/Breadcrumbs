package breadcrumbs.b5;

import breadcrumbs.b4.*;
import breadcrumbs.domain.*;
import breadcrumbs.storage.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import static breadcrumbs.b4.Checks.*;

/** Separate production Main processes exercise saved historical graphs, Hash demo and handoff. */
public final class ProcessScenarios {
    private ProcessScenarios() { }
    public static void main(String[] args) throws Exception {
        Path base = Files.createTempDirectory(Files.createDirectories(Path.of("build/b5-process")), "run-").toAbsolutePath();
        Path source = Files.createDirectory(base.resolve("source")); Files.writeString(source.resolve("README.md"), "# Next Steps\nReview synthetic task dependencies.\n");
        var state = WorkflowTest.imported(source, "W510");
        var store = new WorkspaceStore(base.resolve("app")); store.save(state);
        String input = "1\n1\n3\n16\n";
        for (String name : new String[] {"Budget", "Supplier", "Event"}) { input += "2\n" + name + "\nExplicit assessment\nUNKNOWN\n\n\n\nyes\n"; }
        input += "3\nT-000001\nT-000002\n\nBudget prerequisite\nyes\n3\nT-000002\nT-000003\n\nSupplier prerequisite\nyes\n5\n7\nT-000001\n8\nT-000001\n3\nT-000003\nT-000001\n\nCycle attempt\nyes\n9\n0\n5\n0\n0\n";
        String a = app(base, "a-chain-cycle", input);
        require(a.contains("Unique downstream: 2") && a.contains("would create a cycle"), "process chain/cycle gate");
        state = store.load("W510"); var first = WorkflowTest.checkpoint(state, "Original graph", new String[0]); store.save(state);
        String historical = first.project().graphRevisionId();
        app(base, "b-remove-edge", "1\n1\n3\n16\n4\nT-000002\nT-000003\nyes\n5\n0\n5\n0\n0\n");
        state = store.load("W510"); WorkflowTest.checkpoint(state, "Changed graph", new String[0]); store.save(state);
        require(!state.checkpoints().getLatest().project().graphRevisionId().equals(historical), "different CP revisions after process change/restart");
        String handoff = app(base, "c-historical-handoff", "1\n1\n3\n14\nCP-000001\n\n\n17\n4\n6\n7\nCP-000001\n8\n7\nCP-000001\n9\n0\n0\n0\n");
        require(handoff.contains("Reported graph revision: " + historical) && handoff.contains("T-000002 -> T-000003") && handoff.contains("Potentially affected"), "historical handoff original dependency");
        require(handoff.contains("Aa and BB collide: true") && handoff.contains("Same canonical value: true") && handoff.contains("Canonical indexes valid: true"), "process Hash collision/resize/parity");
        System.out.println("PASS: B5 separate Main processes (" + Checks.count() + " assertions) | " + base);
    }
    private static String app(Path base, String name, String input) throws Exception {
        Path log = base.resolve(name + ".txt"); Files.writeString(base.resolve(name + ".input.txt"), input);
        Path java = Path.of(System.getProperty("java.home"), "bin", "java.exe");
        Process process = new ProcessBuilder(java.toString(), "-cp", "build/app", "breadcrumbs.Main", "--no-color", "--storage-root=" + base.resolve("app"))
                .redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try (var writer = process.outputWriter(StandardCharsets.UTF_8)) { writer.write(input); }
        if (!process.waitFor(30, TimeUnit.SECONDS)) { process.destroyForcibly(); throw new AssertionError("Process timeout"); }
        require(process.exitValue() == 0, "process exit " + name);
        String text = Files.readString(log); require(text.contains("Goodbye."), "process clean exit"); return text;
    }
}
