package breadcrumbs.cli;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class BootstrapConsoleTest {
    private static int assertions;

    private BootstrapConsoleTest() { }

    public static void run() throws Exception {
        String guided = "13\n1\nPlanner\nACADEMIC\nBuild a planner\nReport\nReview scope\n\n\n\nyes\n";
        String newOutput = session(guided + "14\n\n0\n");
        contains(newOutput, "Baseline B0 established", "USER_SUPPLIED", "OWNER: UNKNOWN",
                "Project native checkpoints: 0", "not a historical resume", "not saved after exit");
        require(!newOutput.contains("Success: CP-000001"), "guided project creates no native checkpoint");
        String sandbox = session("12\n" + guided + "10\n0\n");
        contains(sandbox, "Loaded 5", "Project native checkpoints: 0", "Size: 5");
        String cancelled = session(guided.replace("\nyes\n", "\nno\n") + "14\n0\n");
        contains(cancelled, "Proposal discarded", "No confirmed B0");
        String cancelledProject = session("13\n1\nPartial\n/cancel\n14\n0\n");
        contains(cancelledProject, "Cancelled", "No confirmed B0");
        require(!cancelledProject.contains("Time complexity:"), "project errors are not reported as DLL operations");
        contains(session("13\n0\n0\n"), "Goodbye");
        Path fixtures = Files.createDirectories(Path.of("build", "b2-fixtures"));
        Path readme = Files.createTempDirectory(fixtures, "cli-readme-").toAbsolutePath();
        Files.writeString(readme.resolve("README.md"), "# Objective\nBuild a reading tracker.\n");
        String imported = session("13\n2\nREADME project\nGENERAL\n" + readme
                + "\nyes\ns\nyes\n14\nyes\n0\n");
        contains(imported, "Entry mode: IMPORT_FOLDER", "SUPPORTED: 1", "SOURCE_REPORTED",
                "Source: README.md line 2", "LEFT_UNKNOWN", "CURRENT_STATE: UNKNOWN",
                "Project native checkpoints: 0");
        String noScan = session("13\n2\nNo consent\nGENERAL\n" + readme + "\nno\n14\n0\n");
        contains(noScan, "Cancelled before scanning", "No confirmed B0");
        String invalidPath = session("13\n2\nMissing\nGENERAL\n" + readme.resolve("missing")
                + "\nyes\n14\n0\n");
        contains(invalidPath, "Directory inspection failed", "No confirmed B0");
        Path legacy = Files.createTempDirectory(fixtures, "cli-legacy-").toAbsolutePath();
        Files.writeString(legacy.resolve("state.md"), "# Status\nTASK-12 = COMPLETED\n");
        Files.writeString(legacy.resolve("tasks.md"), "# Tasks\nTASK-12 = IN_PROGRESS\n");
        String legacyOutput = session("13\n3\nLegacy\nSOFTWARE\n" + legacy + "\nyes\ns\nyes\n14\nyes\n0\n");
        contains(legacyOutput, "Entry mode: IMPORT_LEGACY_CONTEXT", "TASK-12 TASK_STATUS - CONFLICTING",
                "Source: state.md line 2", "Source: tasks.md line 2", "unresolved: 1",
                "Project native checkpoints: 0");
        String switchMode = session("13\n2\nLegacy option\nOPERATIONS\n" + legacy
                + "\nyes\nyes\ns\nyes\n0\n");
        contains(switchMode, "Possible state/handoff/instruction artifacts", "Entry mode: IMPORT_LEGACY_CONTEXT");
        String keepMode = session("13\n2\nKeep folder mode\nGENERAL\n" + legacy
                + "\nyes\n\ns\nyes\n0\n");
        contains(keepMode, "Entry mode: IMPORT_FOLDER");
        require(!keepMode.contains("Entry mode: IMPORT_LEGACY_CONTEXT"), "no automatic mode switch");
        String replaceCancelled = session(guided + "13\n2\nCancelled replacement\nGENERAL\n"
                + readme + "\nyes\ns\nno\n14\n\n0\n");
        int finalBaseline = replaceCancelled.lastIndexOf("BREADCRUMBS - INITIAL PROJECT BASELINE B0");
        require(finalBaseline >= 0 && replaceCancelled.substring(finalBaseline).contains("Planner"),
                "cancelled replacement preserves confirmed active project");
        require(!replaceCancelled.substring(finalBaseline).contains("| Cancelled replacement"),
                "cancelled project is not canonical state");
        System.out.println("PASS: B2 CLI (" + assertions + " assertions)");
    }

    private static String session(String commands) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            new ConsoleMenu(new StringReader(commands), output).run();
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    private static void contains(String output, String... expected) {
        for (String fragment : expected) {
            require(output.contains(fragment), "Missing CLI result " + fragment + "\n" + output);
        }
    }

    private static void require(boolean condition, String reason) {
        assertions++;
        if (!condition) {
            throw new AssertionError(reason);
        }
    }
}
