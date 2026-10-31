package breadcrumbs.storage;

import breadcrumbs.b4.WorkflowTest;
import breadcrumbs.cli.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.util.concurrent.TimeUnit;
import static breadcrumbs.b4.Checks.*;

public final class B7RecoveryProcessTest {
    private B7RecoveryProcessTest() { }
    public static void main(String[] args) throws Exception {
        if (args.length == 1) {
            var faulty = new WorkspaceStore(Path.of(args[0]), point -> { if (point == WorkspaceStore.RecoveryPoint.AFTER_PROMOTION) { Runtime.getRuntime().halt(47); } });
            var state = faulty.load("W703"); WorkflowTest.checkpoint(state, "Interrupted but intact generation", new String[0]); faulty.save(state); return;
        }
        run();
    }
    public static void run() throws Exception {
        Path base = Files.createTempDirectory(Files.createDirectories(Path.of("build/b7-fixtures")), "process-recovery-").toAbsolutePath();
        var store = new WorkspaceStore(base.resolve("app")); var state = ContinuityTest.empty("W703", "Interrupted save demo");
        WorkflowTest.checkpoint(state, "First intact checkpoint", new String[0]); store.save(state);
        Path executable = Path.of(System.getProperty("java.home"), "bin", "java.exe");
        Process process = new ProcessBuilder(executable.toString(), "-cp", System.getProperty("java.class.path"), B7RecoveryProcessTest.class.getName(), store.root().toString())
                .redirectErrorStream(true).redirectOutput(base.resolve("interrupted.txt").toFile()).start();
        require(process.waitFor(30, TimeUnit.SECONDS) && process.exitValue() == 47, "separate Java process interrupted after generation promotion");
        reject(() -> store.load("W703"), "restart detects uncommitted newer generation");
        var bytes = new ByteArrayOutputStream();
        new ProjectConsole(new StringReader("5\nW703\nG-000002\nRECOVER G-000002\n3\n3\n0\n0\n"), new PrintStream(bytes, true, StandardCharsets.UTF_8), store, new TerminalTheme(false), 80).run();
        String text = bytes.toString(StandardCharsets.UTF_8);
        require(text.contains("RESTORE A SAVED PROJECT") && text.contains("Your checked project copy has been restored") && text.contains("CP-000002"), "actual CLI acknowledges recovery and resumes intact history");
        require(store.load("W703").checkpoints().size() == 2, "recovered published state survives another restart");
        Files.writeString(base.resolve("recovery-cli.txt"), text);
        System.out.println("PASS: B7A interrupted Java process and real recovery CLI | " + base);
    }
}
