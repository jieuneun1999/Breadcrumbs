package breadcrumbs.b7;

import breadcrumbs.storage.*;
import breadcrumbs.domain.*;
import breadcrumbs.checkpoint.*;
import breadcrumbs.continuity.*;
import breadcrumbs.cli.*;
import java.nio.file.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import static breadcrumbs.b4.Checks.*;

public final class ContinuityWorkflowTest {
    private ContinuityWorkflowTest() { }
    public static void main(String[] args) throws Exception { run(); }
    public static void run() throws Exception {
        var state = ContinuityTest.empty("W711", "General project");
        var quick = new QuickCheckpointService(); var continuity = new ContinuityService();
        require(continuity.brief(state, false).contains("B0 ONLY") && continuity.brief(state, false).contains("SESSION-ONLY"), "B0 brief never invents history or persistence");
        var proposal = quick.preview(state, "Drafted the venue shortlist", "Awaiting budget approval", "Ask the organizer to review costs", quick.proposed(state));
        require(state.checkpoints().isEmpty(), "quick preview does not reserve an ID or append");
        require(proposal.project().blockerState() == ProjectContext.BlockerState.BLOCKED, "explicit reported blocker retained");
        require(!new ProjectCheckpointService().confirm(state, proposal, false) && state.checkpoints().isEmpty(), "quick cancellation preserves canonical DLL");
        new ProjectCheckpointService().confirm(state, proposal, true);
        var next = quick.preview(state, "Updated the shortlist", "unknown", "Contact adviser", quick.proposed(state));
        require(next.project().blockerState() == ProjectContext.BlockerState.UNKNOWN && !next.project().blockers().contains("budget"), "old blockers cannot silently carry forward");
        String brief = continuity.brief(state, true);
        for (String expected : new String[] {"FOLLOW THE BREADCRUMBS", "CP-000001", "Drafted the venue", "Ask the organizer", "historical", "Live source freshness UNKNOWN", "USER_CONFIRMED"}) { require(brief.contains(expected), "truthful continuity brief " + expected); }
        require(continuity.health(state).contains("CTX-") && continuity.health(state).contains("does not measure project completion"), "health reuses B4 rule semantics");
        Path base = Files.createTempDirectory(Files.createDirectories(Path.of("build/b7-fixtures")), "daily-cli-").toAbsolutePath();
        var store = new WorkspaceStore(base.resolve("app")); store.save(ContinuityTest.empty("W712", "Daily workflow"));
        String input = "1\n1\n7\n2\nPrepared the meeting agenda\nnone\nSend the agenda for review\nyes\n1\n3\n0\n0\n";
        String text = session(store, input);
        require(text.contains("LEAVE A BREADCRUMB") && text.contains("Confirmed CP-000001") && text.contains("FOLLOW THE BREADCRUMBS"), "actual quick capture and resume CLI");
        var saved = store.load("W712"); require(saved.checkpoints().size() == 1 && saved.checkpoints().getLatest().project().nextActions().equals("Send the agenda for review"), "three-question capture survives restart");
        String restart = session(store, "1\n1\n7\n1\n2\nCancelled work\nunknown\nReview later\n0\n0\n0\n");
        require(restart.contains("Prepared the meeting agenda") && store.load("W712").checkpoints().size() == 1, "resume shows saved context and cancellation adds no history");
        Files.writeString(base.resolve("quick-follow.txt"), text); Files.writeString(base.resolve("restart-cancel.txt"), restart);
        Path source = Files.createDirectory(base.resolve("source")); Files.writeString(source.resolve("state.md"), "# Objective\nResearch field notes\n# Next Steps\nReview methodology\n");
        var imported = breadcrumbs.b4.WorkflowTest.imported(source, "W713"); var reference = imported.activeBaseline().claims()[0];
        breadcrumbs.b4.WorkflowTest.checkpoint(imported, "Source-backed original", new String[] {reference.evidenceId()});
        var importedStore = new WorkspaceStore(base.resolve("imported-app")); importedStore.save(imported);
        Files.writeString(source.resolve("state.md"), "# Objective\nRevised research field notes\n# Next Steps\nReview methodology\n");
        String currentHash = WorkspaceCodec.digest(Files.readAllBytes(source.resolve("state.md")));
        String reviewed = session(importedStore, "1\n1\n7\n4\nyes\n1\nyes\n1\n0\n0\n");
        require(reviewed.contains("Original SHA256") && reviewed.contains("Observed SHA256") && reviewed.contains("STALE"), "guided review shows real changed hashes and referenced staleness");
        var reviewedState = importedStore.load("W713");
        require(reviewedState.reviews().length == 1 && reviewedState.reviews()[0].entityId().equals("CHANGE_REVIEW"), "review acknowledgement is canonical and scoped");
        require(CurrentAssessment.statement(reviewedState, EvidenceClaim.Field.PROGRESS) == null, "change acknowledgement cannot become confirmed project progress");
        require(reviewedState.evidence(reviewedState.activeBaseline().snapshot().scanId(), reference.evidenceId()).fingerprint().equals(reference.fingerprint()), "old checkpoint source hashes remain immutable");
        require(currentHash.equals(WorkspaceCodec.digest(Files.readAllBytes(source.resolve("state.md")))), "guided review never writes source files");
        Files.move(source, base.resolve("source-offline"));
        require(continuity.brief(importedStore.load("W713"), true).contains("Live source freshness UNKNOWN"), "resume works with source folder unavailable");
        Files.writeString(base.resolve("guided-review.txt"), reviewed);
        System.out.println("PASS: B7B quick capture, continuity health, real CLI and restart | " + base);
    }
    private static String session(WorkspaceStore store, String input) throws Exception {
        var bytes = new ByteArrayOutputStream();
        new ProjectConsole(new StringReader(input), new PrintStream(bytes, true, StandardCharsets.UTF_8), store, new TerminalTheme(false), 80).run();
        return bytes.toString(StandardCharsets.UTF_8);
    }
}
