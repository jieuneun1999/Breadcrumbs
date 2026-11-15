package breadcrumbs.b7;

import breadcrumbs.domain.*;
import breadcrumbs.storage.*;
import breadcrumbs.search.*;
import breadcrumbs.checkpoint.*;
import java.nio.file.*;
import java.util.Arrays;
import static breadcrumbs.b4.Checks.*;

public final class ManualContextTest {
    private ManualContextTest() { }
    public static void main(String[] args) throws Exception { run(); }
    public static void run() throws Exception {
        var state = ContinuityTest.empty("W721", "Meeting project");
        byte[] old = WorkspaceCodec.encode(state);
        var inbox = state.manualContext();
        var note = inbox.capture(ManualContext.Type.REPORTED_STATEMENT, "Client requested a revised layout", "Client meeting, verbal report", "Meeting note A", ManualContext.Sensitivity.INTERNAL);
        require(note != null && inbox.captures().length == 1 && note.id().equals("IN-000001"), "manual capture has stable canonical identity");
        require(note.type() == ManualContext.Type.REPORTED_STATEMENT && note.origin().equals("Client meeting, verbal report"), "manual origin never fabricates a file locator");
        reject(() -> inbox.restore(new ManualContext.Capture("W999", "IN-000002", note.type(), "Other project", "User", "", note.recordedAt(), note.sensitivity())), "cross-workspace manual capture rejected");
        reject(() -> inbox.restore(new ManualContext.Review("IN-999999", EvidenceClaim.ReviewDisposition.CONFIRMED, false, false, note.recordedAt())), "unresolved manual review rejected");
        reject(() -> inbox.validateReferences(new String[] {note.id()}, note.recordedAt().minusSeconds(1)), "future manual reference cannot rewrite checkpoint history");
        require(inbox.disposition(note.id()) == EvidenceClaim.ReviewDisposition.PENDING, "unreviewed manual capture stays pending");
        var search = new ContextSearchIndex(state); search.rebuild(SearchPolicy.defaults(), () -> false);
        require(search.searchExact("revised", 20).hits().length == 0, "pending reported statements excluded");
        inbox.review(note.id(), EvidenceClaim.ReviewDisposition.CONFIRMED, false, false);
        require(search.status() == SearchReport.Status.INDEX_OUTDATED, "manual review invalidates search");
        search.rebuild(SearchPolicy.defaults(), () -> false);
        require(search.searchExact("revised", 20).hits().length == 0, "confirmed third-party report still needs explicit indexing approval");
        inbox.review(note.id(), EvidenceClaim.ReviewDisposition.CONFIRMED, true, true);
        search.rebuild(SearchPolicy.defaults(), () -> false);
        var hit = search.searchExact("revised", 20).hits();
        require(hit.length == 1 && hit[0].location().kind() == SearchLocation.Kind.MANUAL && hit[0].location().truth().contains("SOURCE_REPORTED"), "approved manual search retains reported truth");
        require(!hit[0].location().locator().contains("line ") && !hit[0].location().locator().contains("SHA256"), "no fake physical source location or hash");
        var own = inbox.capture(ManualContext.Type.PERSONAL_OBSERVATION, "The venue has accessible seating", "Personal visit", "", ManualContext.Sensitivity.INTERNAL);
        inbox.review(own.id(), EvidenceClaim.ReviewDisposition.CONFIRMED, false, false);
        search.rebuild(SearchPolicy.defaults(), () -> false);
        require(search.searchExact("accessible", 20).hits().length == 1, "confirmed personal context uses default manual policy");
        var privateNote = inbox.capture(ManualContext.Type.CURRENT_STATE, "Private appointment details", "Personal note", "", ManualContext.Sensitivity.SENSITIVE);
        reject(() -> inbox.review(privateNote.id(), EvidenceClaim.ReviewDisposition.CONFIRMED, true, true), "sensitive capture cannot be approved for indexing/export");
        inbox.review(privateNote.id(), EvidenceClaim.ReviewDisposition.CONFIRMED, false, false);
        search.rebuild(SearchPolicy.defaults().withUnreviewed(true), () -> false);
        require(search.searchExact("appointment", 20).hits().length == 0, "source opt-in cannot expose sensitive manual context");
        reject(() -> inbox.capture(ManualContext.Type.CURRENT_STATE, "password=forbidden", "Note", "", ManualContext.Sensitivity.SENSITIVE), "credentials rejected rather than collected");
        var service = new ProjectCheckpointService();
        var cp = service.preview(state, "Meeting follow-up", "Recorded the requested revision", "Prepare revised layout", ProjectContext.BlockerState.UNKNOWN, "", "Review the client request", "UNKNOWN", "UNKNOWN", new String[0], new String[] {note.id()});
        service.confirm(state, cp, true);
        require(cp.project().manualEvidenceIds()[0].equals(note.id()) && cp.project().evidenceIds().length == 0, "checkpoint manual references remain distinct from source evidence");
        byte[] version4 = WorkspaceCodec.encode(state);
        require(new String(version4, java.nio.charset.StandardCharsets.UTF_8).startsWith("FORMAT_VERSION=4\n"), "manual context explicitly upgrades canonical schema");
        var restored = WorkspaceCodec.decode(version4);
        require(Arrays.equals(version4, WorkspaceCodec.encode(restored)) && restored.manualContext().captures().length == 3 && restored.manualContext().reviews().length == 4, "v4 captures/reviews/checkpoint references roundtrip exactly");
        Path base = Files.createTempDirectory(Files.createDirectories(Path.of("build/b7-fixtures")), "manual-").toAbsolutePath();
        var store = new WorkspaceStore(base.resolve("app")); store.save(restored);
        var restarted = store.load("W721");
        require(restarted.manualContext().capture(ManualContext.Type.NEXT_ACTION_CANDIDATE, "Ask for review", "User", "", ManualContext.Sensitivity.INTERNAL).id().equals("IN-000004"), "manual high-water counter survives generation restart");
        for (String path : new String[] {"tests/fixtures/b3-v1/imported-workspace.txt", "tests/fixtures/b4-v2/workspace.txt"}) {
            byte[] bytes = Files.readAllBytes(Path.of(path)); var legacy = WorkspaceCodec.decode(bytes);
            require(Arrays.equals(bytes, WorkspaceCodec.encode(legacy)), "old versions stay exact with an empty Inbox");
            legacy.manualContext().capture(ManualContext.Type.DECISION, "Chose the smaller scope", "User agreement", "", ManualContext.Sensitivity.INTERNAL);
            var upgrade = WorkspaceCodec.decode(WorkspaceCodec.encode(legacy));
            require(upgrade.checkpoints().size() == legacy.checkpoints().size() && upgrade.manualContext().captures().length == 1, "old workspace upgrade preserves history and manual origin");
        }
        require(new String(old, java.nio.charset.StandardCharsets.UTF_8).startsWith("FORMAT_VERSION=1"), "no schema upgrade before new canonical information");
        System.out.println("PASS: B7C manual origin, privacy, search, v4 and generation restart | " + base);
    }
}
