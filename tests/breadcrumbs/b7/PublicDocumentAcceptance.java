package breadcrumbs.b7;

import breadcrumbs.inspector.*;
import breadcrumbs.b4.WorkflowTest;
import breadcrumbs.domain.*;
import breadcrumbs.search.*;
import breadcrumbs.storage.*;
import java.nio.file.*;
import static breadcrumbs.b4.Checks.*;

/** Opt-in local gate for the exact owner-authorized public sample, never a network test. */
public final class PublicDocumentAcceptance {
    private PublicDocumentAcceptance() { }
    public static void main(String[] args) throws Exception {
        if (args.length != 1) { throw new IllegalArgumentException("Pass only the prepared public sample directory."); }
        Path root = Path.of(args[0]).toAbsolutePath(); Path file = root.resolve("project_management_plan.docx");
        String before = WorkspaceCodec.digest(Files.readAllBytes(file));
        var state = WorkflowTest.imported(root, "W799");
        require(state.checkpoints().isEmpty(), "public template begins with no Breadcrumbs native session");
        EvidenceClaim expected = null;
        for (var claim : state.activeBaseline().claims()) { if (claim.relativePath().equals(file.getFileName().toString()) && claim.line() == 154 && claim.literalValue().equals("Define the purpose and scope of the project.")) { expected = claim; } }
        require(expected != null && expected.paragraphLocated() && expected.verification() == EvidenceClaim.Verification.UNVERIFIED, "independently checked public paragraph 154 captured exactly without verifying project work");
        state.addReview(new ContextReview(state.currentObservationId(), expected.evidenceId(), expected.field(), expected.entityId(), expected.literalValue(), EvidenceClaim.ReviewDisposition.CONFIRMED, java.time.Instant.now()));
        var search = new ContextSearchIndex(state); search.rebuild(SearchPolicy.defaults(), () -> false);
        require(search.searchExact("scope", 10).hits().length == 1 && search.searchExact("scope", 10).hits()[0].location().locator().contains("paragraph 154"), "approved public source searchable with truthful locator");
        var cp = WorkflowTest.checkpoint(state, "Reviewed public template", new String[] {expected.evidenceId()});
        var store = new WorkspaceStore(root.resolveSibling(root.getFileName() + "-acceptance-app")); store.save(state);
        var restart = store.load("W799");
        require(restart.checkpoints().size() == 1 && restart.evidence(restart.currentObservationId(), expected.evidenceId()).fingerprint().equals(before), "public-source provenance survives canonical restart");
        require(before.equals(WorkspaceCodec.digest(Files.readAllBytes(file))), "downloaded original unchanged after scan/review/search/save/restart");
        require(!state.activeBaseline().snapshot().complete() && state.activeBaseline().claims().length == 200, "large public template reports partial 200-claim coverage honestly");
        System.out.println("PASS: authorized public DOCX first import, exact paragraph, approved search, checkpoint/restart and unchanged original");
        System.out.println("SHA256 " + before + " | bytes " + Files.size(file) + " | " + breadcrumbs.b4.Checks.count() + " assertions");
        System.out.println("NOT COMPLETE: 453 source paragraphs; 200 total claim budget; fields/TOC and unsupported PDF body omitted. Full representative-format/owner acceptance remains pending.");
    }
}
