package breadcrumbs.cli;

import breadcrumbs.storage.*;
import breadcrumbs.domain.*;
import breadcrumbs.b4.WorkflowTest;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import static breadcrumbs.b4.Checks.*;

public final class BeginnerExperienceTest {
    private BeginnerExperienceTest() { }
    public static void main(String[] args) throws Exception { run(); }
    public static void run() throws Exception {
        Path base = Files.createTempDirectory(Files.createDirectories(Path.of("build/b7-fixtures")), "beginner-").toAbsolutePath();
        var store = new WorkspaceStore(base.resolve("app"));
        String created = session(store, "2\n1\nCommunity food drive\nGENERAL\nPrepare 100 food packs\nA pickup plan\nConfirm the hall booking\nSam\nAwaiting delivery quote\n\nyes\nyes\n7\n2\nBooked the hall and drafted the volunteer schedule\nWaiting for the delivery quote\nAsk the delivery company for a quote\nyes\n1\n0\n0\n");
        require(created.contains("Your project is saved on this computer.") && created.contains("Record an update") && created.contains("PROJECT SUMMARY"), "ordinary create/save/update/summary speaks in beginner task language");
        for (String jargon : new String[] {"canonical", "CTX-", "SOURCE_REPORTED", "UNVERIFIED", "DLL structure", "graph revision"}) { require(!created.contains(jargon), "primary workflow does not expose internal terminology " + jargon); }
        var saved = store.load(store.list()[0].id());
        require(saved.checkpoints().size() == 1 && saved.checkpoints().getLatest().project().nextActions().equals("Ask the delivery company for a quote"), "friendly saving retains the actual confirmed checkpoint");
        byte[] before = WorkspaceCodec.encode(saved);
        String restart = session(store, "1\n1\n7\n1\n?\n9\n8\n0\n0\n0\n");
        require(restart.contains("Ask the delivery company for a quote") && restart.contains("Waiting for the delivery quote") && restart.contains("DLL structure"), "restart restores useful context and technical diagnostics remain reachable");
        require(java.util.Arrays.equals(before, WorkspaceCodec.encode(store.load(saved.activeBaseline().workspaceId()))), "presentation and diagnostics do not mutate saved state");
        var failed = new WorkspaceStore(store.root(), point -> { if (point == WorkspaceStore.RecoveryPoint.BEFORE_STAGE) { throw new IOException("Controlled save failure"); } });
        String failure = session(failed, "1\n1\n7\n2\nDrafted pickup instructions\nunknown\nReview the pickup plan\nyes\n0\n0\nd\n");
        require(failure.contains("Your project could not be saved.") && failure.contains("Your changes are still open here"), "save error explains what failed and how to avoid losing pending changes");
        require(store.load(saved.activeBaseline().workspaceId()).checkpoints().size() == 1, "failed save cannot pretend the new update was persisted");
        var state = store.load(saved.activeBaseline().workspaceId()); state.manualContext().setArtifactsAllowed(true); store.save(state);
        Path continuity = Files.createDirectories(store.root().resolve("workspaces").resolve(saved.activeBaseline().workspaceId()).resolve("continuity"));
        Files.writeString(continuity.resolve("latest.txt"), "unrecognized file, preserve it");
        String exportFailure = session(store, "1\n1\n7\n2\nReviewed the pickup plan\nnone\nSend the plan for review\nyes\n0\n0\n");
        require(exportFailure.contains("Your update is saved.") && exportFailure.contains("The handoff files could not be updated"), "handoff failure remains distinct from successful save");
        require(store.load(saved.activeBaseline().workspaceId()).checkpoints().size() == 2 && Files.readString(continuity.resolve("latest.txt")).equals("unrecognized file, preserve it"), "saved history and unrecognized output survive a failed handoff refresh");
        Files.writeString(base.resolve("ordinary.txt"), created); Files.writeString(base.resolve("restart.txt"), restart);
        Files.writeString(base.resolve("save-failure.txt"), failure); Files.writeString(base.resolve("handoff-failure.txt"), exportFailure);
        projectionTruth(base);
        System.out.println("PASS: beginner create/save/resume, diagnostic access and truthful failure recovery | " + base);
    }
    private static void projectionTruth(Path base) throws Exception {
        Path source = Files.createDirectory(base.resolve("projection-source"));
        Files.writeString(source.resolve("plan.md"), "# Next Action\nAsk the delivery company for a quote\n");
        var state = WorkflowTest.imported(source, "W790");
        require(ProjectPresentation.summary(state, false).nextStep().basis() == ProjectPresentation.Basis.NOT_PROVIDED,
                "unreviewed document statements cannot silently become the current plan");
        var claim = WorkflowTest.find(state, "plan.md", EvidenceClaim.Field.NEXT_ACTION);
        state.addReview(new ContextReview(state.currentObservationId(), claim.evidenceId(), claim.field(), claim.entityId(), claim.literalValue(), EvidenceClaim.ReviewDisposition.CONFIRMED, java.time.Instant.now()));
        byte[] original = WorkspaceCodec.encode(state);
        var summary = ProjectPresentation.summary(state, true);
        require(summary.nextStep().basis() == ProjectPresentation.Basis.DOCUMENT && summary.nextStep().text().equals(claim.literalValue()),
                "reviewed document wording retains its document origin");
        for (boolean color : new boolean[] {false, true}) {
            var bytes = new ByteArrayOutputStream();
            new ProjectPresentation(new PrintStream(bytes, true, StandardCharsets.UTF_8), new TerminalTheme(color), 28).showSummary(state, true);
            String text = bytes.toString(StandardCharsets.UTF_8);
            for (String line : text.split("\\r?\\n")) { require(line.replaceAll("\u001b\\[[0-9;]*m", "").length() <= 28, "summary fits a narrow terminal with either theme"); }
            require(text.replaceAll("\u001b\\[[0-9;]*m", "").replaceAll("\\s+", " ").contains("come from your documents"), "readable source caveat survives wrapping");
        }
        require(java.util.Arrays.equals(original, WorkspaceCodec.encode(state)), "projection rendering is canonically read-only");
        state.addReview(new ContextReview(state.currentObservationId(), "", EvidenceClaim.Field.NEXT_ACTION, "", "NOT_DOCUMENTED", EvidenceClaim.ReviewDisposition.CONFIRMED, java.time.Instant.now()));
        require(ProjectPresentation.summary(state, true).nextStep().text().equals("Not documented yet"), "explicitly undocumented remains distinct from not provided");
        state.addReview(new ContextReview(state.currentObservationId(), "", EvidenceClaim.Field.NEXT_ACTION, "", "Ask Sam", EvidenceClaim.ReviewDisposition.CONFIRMED, java.time.Instant.now()));
        require(ProjectPresentation.summary(state, true).nextStep().basis() == ProjectPresentation.Basis.YOUR_UPDATE, "owner assessment remains distinct from a document statement");
    }
    private static String session(WorkspaceStore store, String input) throws Exception {
        var bytes = new ByteArrayOutputStream();
        new ProjectConsole(new StringReader(input), new PrintStream(bytes, true, StandardCharsets.UTF_8), store, new TerminalTheme(false), 80).run();
        return bytes.toString(StandardCharsets.UTF_8);
    }
}
