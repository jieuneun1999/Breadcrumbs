package breadcrumbs.b7;

import breadcrumbs.cli.*;
import breadcrumbs.storage.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import static breadcrumbs.b4.Checks.*;

public final class InboxArtifactsCliTest {
    private InboxArtifactsCliTest() { }
    public static void main(String[] args) throws Exception { run(); }
    public static void run() throws Exception {
        Path base = Files.createTempDirectory(Files.createDirectories(Path.of("build/b7-fixtures")), "inbox-cli-").toAbsolutePath();
        var store = new WorkspaceStore(base.resolve("app")); Path portable = base.resolve("portable");
        String input = "2\n1\nCommunity gathering\nGENERAL\nPlan an accessible meetup\nMeeting agenda\nAsk the host\n\n\n\n4\nyes\nyes\n7\n10\n1\n3\nClient requested an accessible entrance\nClient meeting, verbal report\n\n\nc\nyes\nyes\nyes\nyes\nRecorded the meeting request\nAwaiting review\nPrepare the revised layout\nyes\n11\n5\nyes\n6\nCP-000001\n" + portable + "\nyes\n0\n5\n4\n1\naccessible\n0\n0\n1\n0\n0\n";
        String text = session(store, input);
        String readable = text.replaceAll("\\s+", " ");
        Files.writeString(base.resolve("inbox-artifacts.txt"), text);
        require(text.contains("Project notes") && text.contains("Your note has been added") && text.contains("Your update has been added"), "NEW_EMPTY project captures a meeting report and real linked checkpoint");
        require(readable.contains("has not checked whether") && readable.contains("Your project summary and handoff files are up to date") && readable.contains("Handoff package exported successfully"), "actual origin labels, generation and consented package flow");
        var saved = store.load(store.list()[0].id());
        require(saved.manualContext().captures()[0].type() == breadcrumbs.domain.ManualContext.Type.REPORTED_STATEMENT, "reported-note provenance is preserved in canonical state");
        require(saved.checkpoints().getLatest().project().manualEvidenceIds()[0].equals("IN-000001") && saved.manualContext().artifactsAllowed(), "manual links and opt-in persist canonically");
        require(Files.exists(portable.resolve("state.md")) && Files.exists(portable.resolve("HANDOFF.md")) && Files.exists(portable.resolve("checkpoints/CP-000001.md")), "portable output readable without application");
        String restart = session(store, "1\n1\n7\n5\n4\n1\naccessible\n0\n0\n1\n0\n0\n");
        require(restart.contains("A note you added") && restart.replaceAll("\\s+", " ").contains("has not checked whether"), "restart search resolves manual origin with truthful provenance");
        Files.writeString(base.resolve("inbox-artifacts.txt"), text); Files.writeString(base.resolve("restart-manual-search.txt"), restart);
        System.out.println("PASS: B7C NEW_EMPTY Inbox, linked checkpoint, generated views, package and restart search | " + base);
    }
    private static String session(WorkspaceStore store, String input) throws Exception {
        var bytes = new ByteArrayOutputStream();
        new ProjectConsole(new StringReader(input), new PrintStream(bytes, true, StandardCharsets.UTF_8), store, new TerminalTheme(false), 80).run();
        return bytes.toString(StandardCharsets.UTF_8);
    }
}
