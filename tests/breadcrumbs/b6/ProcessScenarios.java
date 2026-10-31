package breadcrumbs.b6;

import breadcrumbs.b4.WorkflowTest;
import breadcrumbs.b4.Checks;
import breadcrumbs.storage.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import static breadcrumbs.b4.Checks.*;

/** Production Main processes, synthetic source roots and actual consented rescans. */
public final class ProcessScenarios {
    private ProcessScenarios() { }
    public static void main(String[] args) throws Exception {
        Path base = Files.createTempDirectory(Files.createDirectories(Path.of("build/b6-process")), "run-").toAbsolutePath();
        Path source = Files.createDirectory(base.resolve("source"));
        Files.writeString(source.resolve("notes.md"), "# Objective\nmethod methods methodology methodology sourceonlyold\n");
        Files.writeString(source.resolve("plan.txt"), "Next action: methodology\nuncapturedword\n");
        Files.writeString(source.resolve("AGENTS.md"), "# Objective\ninstructiononly\n");
        Files.writeString(source.resolve(".env"), "secret=excludedfixtureword\n");
        Files.writeString(source.resolve("sensitive.txt"), "Objective: password=privatefixtureword\n");
        Files.writeString(source.resolve("escape.md"), "# Objective\n\u001b[31munsafeonlyword\n");
        for (String extension : new String[] {"pdf", "docx", "xlsx", "pptx"}) { Files.writeString(source.resolve("unparsed." + extension), "bodyonlyword"); }
        var store = new WorkspaceStore(base.resolve("app")); var state = WorkflowTest.imported(source, "W631");
        WorkflowTest.checkpoint(state, "Methodology checkpoint", new String[0]); store.save(state);
        String protectedBefore = fingerprint(source);
        String text = app(base, "a-b-c-e-h", "1\n1\n3\n18\n6\nyes\n4\n2\nmeth\n2\n0\n1\nmethodology\n0\n3\n1\nbodyonlyword\n1\ninstructiononly\n2\nexcluded\n2\nprivatefixture\n1\nunsafeonlyword\n5\n0\n0\n0\n");
        require(text.contains("line 2") && text.contains("Occurrences: 2") && text.contains("SOURCE_REPORTED"), "A actual captured TXT/MD source locators and occurrences");
        require(text.contains("[1] method") && text.contains("[2] methodology") && text.contains("[3] methods"), "B deterministic Trie suggestions");
        require(text.contains("HISTORICAL CHECKPOINT") && text.contains("CP-000001"), "C evidence/checkpoint records in one dictionary");
        require(text.contains("metadata-only files=4") && text.contains("body not searched"), "E unsupported Office/PDF body coverage");
        require(!text.contains("privatefixtureword") && !text.contains("excludedfixtureword") && !text.contains("\u001b"), "H excluded contents and ANSI never displayed");
        require(fingerprint(source).equals(protectedBefore), "A-H source bytes untouched by application");
        Files.writeString(source.resolve("notes.md"), "# Objective\nreplacementword\n");
        Files.delete(source.resolve("plan.txt"));
        String changedBefore = fingerprint(source);
        String rescan = app(base, "d-rescan", "1\n1\n3\n18\n6\nyes\n4\n1\nsourceonlyold\n0\n0\n11\nyes\n\n18\n1\nreplacementword\n4\n1\nsourceonlyold\n1\nreplacementword\n0\n0\n5\n0\n0\n");
        require(rescan.contains("INDEX_OUTDATED") && rescan.contains("OBS-W631-0001"), "D consented rescan makes existing index outdated");
        int rebuilt = rescan.lastIndexOf("Rebuild completed=true");
        String afterRebuild = rescan.substring(rebuilt);
        require(afterRebuild.contains("Exact term: sourceonlyold") && afterRebuild.contains("No indexed matches")
                && afterRebuild.contains("Exact term: replacementword") && afterRebuild.contains("line 2"), "D removed term absent and new term resolved after rebuild");
        require(fingerprint(source).equals(changedBefore), "D rescan/search read only");
        String restored = app(base, "f-restart", "1\n1\n3\n18\n6\nyes\n4\n1\nreplacementword\n0\n0\n0\n0\n");
        require(restored.contains("INDEX_UNAVAILABLE") && restored.contains("OBS-W631-0001") && restored.contains("line 2"), "F restart reconstructs search from saved observation");
        var saved = store.load("W631"); require(saved.checkpoints().size() == 1 && saved.laterObservations().length == 1, "F no duplicate history");
        Path second = Files.createDirectory(base.resolve("second-source")); Files.writeString(second.resolve("README.md"), "# Objective\notherworkspaceword\n");
        store.save(WorkflowTest.imported(second, "W632"));
        String switched = app(base, "g-isolation", "1\n1\n3\n18\n6\nyes\n4\n1\nreplacementword\n0\n0\n6\n2\n3\n18\n4\n1\notherworkspaceword\n6\nyes\n4\n1\nreplacementword\n1\notherworkspaceword\n0\n0\n0\n0\n");
        int change = switched.indexOf("Opened saved workspace", switched.indexOf("Opened saved workspace") + 1);
        String other = switched.substring(change);
        require(other.contains("W632") && other.contains("INDEX_UNAVAILABLE") && other.contains("No indexed matches"), "G switching resets projection and source opt-in");
        require(other.contains("EV-W632") && !other.contains("EV-W631"), "G no cross-workspace posting leakage");
        System.out.println("PASS: B6 A-H separate Main processes (" + Checks.count() + " assertions) | " + base);
    }
    private static String fingerprint(Path root) throws Exception {
        StringBuilder hashes = new StringBuilder();
        try (var files = Files.list(root)) {
            for (Path path : files.sorted().toArray(Path[]::new)) { hashes.append(path.getFileName()).append(WorkspaceCodec.digest(Files.readAllBytes(path))); }
        }
        return hashes.toString();
    }
    private static String app(Path base, String name, String input) throws Exception {
        Path log = base.resolve(name + ".txt"); Files.writeString(base.resolve(name + ".input.txt"), input);
        Path executable = Path.of(System.getProperty("java.home"), "bin", System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java");
        ProcessBuilder builder = new ProcessBuilder(executable.toString(), "-cp", "build/app", "breadcrumbs.Main", "--no-color", "--storage-root=" + base.resolve("app"));
        builder.environment().put("NO_COLOR", "1");
        Process process = builder.redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try (var writer = process.outputWriter(StandardCharsets.UTF_8)) { writer.write(input); }
        if (!process.waitFor(45, TimeUnit.SECONDS)) { process.destroyForcibly(); throw new AssertionError("B6 process timeout " + name); }
        require(process.exitValue() == 0, "production exit " + name);
        String text = Files.readString(log); require(text.contains("Goodbye."), "clean CLI exit " + name); return text;
    }
}
