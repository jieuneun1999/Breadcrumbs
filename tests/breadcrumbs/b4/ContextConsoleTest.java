package breadcrumbs.b4;

import breadcrumbs.cli.ProjectConsole;
import breadcrumbs.cli.TerminalTheme;
import breadcrumbs.continuity.ReferenceFreshness;
import breadcrumbs.domain.EvidenceClaim;
import breadcrumbs.storage.WorkspaceStore;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import static breadcrumbs.b4.Checks.*;

/** Exercises the actual project menu through redirected input, including cancellation and EOF. */
public final class ContextConsoleTest {
    private ContextConsoleTest() { }
    public static void run() throws Exception {
        Path base = Files.createTempDirectory(Files.createDirectories(Path.of("build", "b4-cli-fixtures")), "cli-").toAbsolutePath();
        Path source = Files.createDirectory(base.resolve("source"));
        Files.writeString(source.resolve("README.md"), "# Purpose\nSynthetic CLI project.\n# Next Steps\nReview paper.\n");
        var state = WorkflowTest.imported(source, "W403");
        var store = new WorkspaceStore(base.resolve("app")); store.save(state);
        String baseline = session(store, "1\n1\n3\n9\n13\n\n14\nB0\n\n\n0\n0\n");
        require(baseline.contains("CONTEXT AUDIT") && baseline.contains("Assessable coverage:"), "audit CLI shows score and coverage");
        require(baseline.contains("Baseline context report — not a historical resume") && baseline.contains("No native checkpoints"), "baseline-only CLI and retrieval");
        require(baseline.contains("CTX-FRESH | NOT_APPLICABLE") && baseline.contains("Goodbye"), "baseline freshness and clean exit");
        String cancelled = session(store, "1\n1\n3\n11\nno\n12\n\n14\nMISSING\n13\nEV-FOREIGN\n0\n0\n");
        require(cancelled.contains("Rescan cancelled. No source access") && cancelled.contains("No later observation"), "rescan consent cancellation");
        require(cancelled.contains("Checkpoint not found") && cancelled.contains("Query evidence belongs"), "invalid selection and cross workspace query graceful");
        require(store.load("W403").laterObservations().length == 0, "cancelled scan leaves app storage unchanged");
        String reviewed = session(store, "1\n1\n3\n10\nconfirm\nResearch current objective\nconfirm\n15\nsolo\n5\n0\n0\n");
        require(reviewed.contains("Present review only") && reviewed.contains("Review finished") && reviewed.contains("Your project is saved on this computer."), "guided high value review and explicit save");
        var reviewedState = store.load("W403");
        require(reviewedState.reviews().length == 2 && reviewedState.soloOwner(), "present amendments and applicability persisted");
        String scan = session(store, "1\n1\n3\n11\nyes\n\n5\n9\n0\n0\n");
        require(scan.contains("OBS-W403-0001") && scan.contains("UNCHANGED"), "actual bounded scan CLI");
        require(store.load("W403").laterObservations().length == 1, "consented observation explicitly saved");
        var latestState = store.load("W403");
        var claim = WorkflowTest.find(latestState, "README.md", EvidenceClaim.Field.OBJECTIVE);
        WorkflowTest.checkpoint(latestState, "Synthetic checkpoint", new String[] {claim.evidenceId()}); store.save(latestState);
        Path exports = Files.createDirectory(base.resolve("exports"));
        String exportInput = "1\n1\n3\n14\n\n\nyes\n" + exports + "\ncontext.md\nyes\n0\n0\n";
        String exported = session(store, exportInput);
        require(exported.contains("Historical resume") && exported.contains("Exported and read back"), "CLI preview, exact destination confirmation, export");
        require(Files.readString(exports.resolve("context.md")).contains("CP-000001"), "export is standalone readable context");
        String unavailable = session(store, "1\n1\n3\n14\n\n\nyes\n" + exports.resolve("missing") + "\ncontext.md\nyes\n0\n0\n");
        require(unavailable.contains("That action could not be completed.") && unavailable.contains("Goodbye"), "unwritable export retains active project and menu");
        String cancelledReview = session(store, "1\n1\n3\n10\ncancel\n0\n0\n");
        require(cancelledReview.contains("Review stopped") && cancelledReview.contains("Goodbye"), "guided cancel is explicit");
        String eof = session(store, "1\n1\n3\n14\n");
        require(eof.contains("End of input") && eof.contains("Goodbye"), "EOF cancels incomplete handoff prompt");
        var finalState = store.load("W403");
        require(new ReferenceFreshness().assess(finalState, finalState.checkpoints().getLatest()).state()
                == ReferenceFreshness.State.UNKNOWN, "repeated CLI opening does not scan or improve freshness");
        System.out.println("PASS: B4 actual project CLI flows");
    }
    private static String session(WorkspaceStore store, String input) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            new ProjectConsole(new StringReader(input), output, store, new TerminalTheme(false), 80).run();
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }
}
