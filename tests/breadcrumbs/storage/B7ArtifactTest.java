package breadcrumbs.storage;

import breadcrumbs.domain.*;
import breadcrumbs.b4.WorkflowTest;
import java.nio.file.*;
import static breadcrumbs.b4.Checks.*;

public final class B7ArtifactTest {
    private B7ArtifactTest() { }
    public static void main(String[] args) throws Exception { run(); }
    public static void run() throws Exception {
        Path base = Files.createTempDirectory(Files.createDirectories(Path.of("build/b7-fixtures")), "artifacts-").toAbsolutePath();
        var state = ContinuityTest.empty("W722", "Portable project");
        var capture = state.manualContext().capture(ManualContext.Type.REPORTED_STATEMENT, "Client requested an accessible entrance", "Meeting report", "", ManualContext.Sensitivity.INTERNAL);
        state.manualContext().review(capture.id(), EvidenceClaim.ReviewDisposition.CONFIRMED, true, true);
        var privateNote = state.manualContext().capture(ManualContext.Type.CURRENT_STATE, "privateappointmentphrase", "Personal note", "", ManualContext.Sensitivity.SENSITIVE);
        state.manualContext().review(privateNote.id(), EvidenceClaim.ReviewDisposition.CONFIRMED, false, false);
        WorkflowTest.checkpoint(state, "First checkpoint", new String[0]);
        var service = new ArtifactService(); var store = new WorkspaceStore(base.resolve("app"));
        var preview = service.preview(state, new String[] {"CP-000001"});
        require(preview.files().length == 4 && preview.files()[0].text().contains("materialized"), "portable preview has readable identity and ownership notice");
        final var unsavedPreview = preview;
        reject(() -> service.materialize(state, store, unsavedPreview), "unsaved canonical state cannot publish materialized context");
        store.save(state); preview = service.preview(state, new String[] {"CP-000001"});
        Path first = service.materialize(state, store, preview);
        require(Files.exists(first.resolve("state.md")) && Files.exists(first.resolve("HANDOFF.md")) && Files.exists(first.resolve("checkpoints/CP-000001.md")), "saved canonical state generates complete Markdown bundle");
        String handoff = Files.readString(first.resolve("HANDOFF.md"));
        require(handoff.contains("SOURCE_REPORTED") && handoff.contains("Client requested an accessible entrance") && !handoff.contains("privateappointmentphrase"), "manual export permissions and provenance enforced");
        String originalArchive = Files.readString(first.resolve("checkpoints/CP-000001.md"));
        WorkflowTest.checkpoint(state, "Second checkpoint", new String[0]);
        require(Files.readString(first.resolve("checkpoints/CP-000001.md")).equals(originalArchive), "new context cannot rewrite old summary");
        reject(() -> service.materialize(state, store, service.preview(state, new String[] {"CP-000001"})), "unsaved update cannot falsely publish persisted checkpoint");
        store.save(state);
        Path second = service.materialize(state, store, service.preview(state, new String[] {"CP-000001", "CP-000002"}));
        require(Files.readString(second.resolve("checkpoints/CP-000001.md")).equals(originalArchive), "historical summary remains deterministic across generated revisions");
        Files.writeString(first.getParent().resolve("latest.txt"), "BREADCRUMBS_VIEWS_POINTER=1\n" + first.getFileName() + "\n");
        service.materialize(state, store, service.preview(state, new String[] {"CP-000001", "CP-000002"}));
        require(Files.readString(first.getParent().resolve("latest.txt")).contains(second.getFileName().toString()), "retry after promoted bundle repairs a lagging latest pointer");
        String pointer = Files.readString(first.getParent().resolve("latest.txt"));
        state.manualContext().capture(ManualContext.Type.NEXT_ACTION_CANDIDATE, "Review access routes", "User", "", ManualContext.Sensitivity.INTERNAL); store.save(state);
        boolean[] faultObserved = {false};
        var failed = new ArtifactService(point -> { if (point == ArtifactService.Point.BEFORE_PUBLISH) { faultObserved[0] = true; throw new java.io.IOException("Injected export failure"); } });
        reject(() -> failed.materialize(state, store, failed.preview(state, new String[] {"CP-000001", "CP-000002"})), "export failure is separate from successful canonical save");
        require(faultObserved[0], "test exercised the actual publication-failure boundary");
        require(!state.dirty() && store.load("W722").checkpoints().size() == 2 && Files.readString(first.getParent().resolve("latest.txt")).equals(pointer), "saved checkpoint and previous views survive export failure");
        var portable = service.preview(state, new String[] {"CP-000002"}); Path destination = base.resolve("portable-handoff");
        final var approvedPreview = portable;
        reject(() -> service.exportPackage(state, store, approvedPreview, destination, false), "portable package requires consent");
        require(!Files.exists(destination), "no export before consent");
        service.exportPackage(state, store, portable, destination, true);
        require(Files.exists(destination.resolve("checkpoints/CP-000002.md")) && !Files.exists(destination.resolve("checkpoints/CP-000001.md")), "only selected checkpoint summaries exported");
        require(!Files.readString(destination.resolve("HANDOFF.md")).contains(store.root().toString()) && !Files.readString(destination.resolve("state.md")).contains(base.toString()), "package leaks no absolute source/app roots");
        reject(() -> service.exportPackage(state, store, approvedPreview, destination, true), "portable export cannot overwrite existing user files");
        reject(() -> service.exportPackage(state, store, approvedPreview, store.root().resolve("unsafe-export"), true), "portable export avoids canonical app storage");
        require(service.materialize(state, store, service.preview(state, new String[] {"CP-000001", "CP-000002"})).toString().contains("continuity"), "failed generated bundle can be rebuilt");
        System.out.println("PASS: B7C materialized artifacts, immutable history, failure separation and portable export | " + base);
    }
}
