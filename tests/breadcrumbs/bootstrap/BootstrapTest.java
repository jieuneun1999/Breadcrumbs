package breadcrumbs.bootstrap;

import breadcrumbs.domain.EntryMode;
import breadcrumbs.domain.EvidenceClaim;
import breadcrumbs.domain.EvidenceClaim.ReviewDisposition;
import breadcrumbs.domain.EvidenceClaim.Verification;
import breadcrumbs.domain.ProjectProfile;
import breadcrumbs.domain.WorkspaceState;
import breadcrumbs.inspector.ScanPolicy;
import java.nio.file.Files;
import java.nio.file.Path;

public final class BootstrapTest {
    private static int assertions;

    private BootstrapTest() { }

    public static void run() throws Exception {
        BootstrapService service = new BootstrapService(ScanPolicy.defaults());
        WorkspaceState state = new WorkspaceState();
        require(state.activeBaseline() == null && state.checkpoints().isEmpty(), "no initial baseline or history");
        for (ProjectProfile profile : ProjectProfile.values()) {
            BootstrapDraft draft = service.prepare(EntryMode.NEW_EMPTY, "Example " + profile, profile, null,
                    new CurrentContext("Build a planner", "Report", "Review scope", "", ""), false, count -> true);
            require(state.activeBaseline() == null || !state.activeBaseline().workspaceId().equals(draft.workspaceId()),
                    "proposal does not activate state");
            require(draft.claims().length == 5 && draft.scan().entries().length == 0, "guided fields without fake files");
            require(service.confirm(state, draft, true), "explicit baseline confirmation");
            require(state.activeBaseline().profile() == profile, "all four profiles");
            require(state.activeBaseline().claims()[3].literalValue().equals("UNKNOWN"), "optional owner remains unknown");
            require(state.checkpoints().size() == 0, "B0 never becomes a DLL node");
            require(state.activeBaseline().claims()[0].verification() == Verification.USER_CONFIRMED,
                    "guided context is human supplied");
        }
        var previous = state.activeBaseline();
        BootstrapDraft cancelled = service.prepare(EntryMode.NEW_EMPTY, "Cancelled", ProjectProfile.GENERAL,
                null, CurrentContext.unknown(), false, count -> true);
        require(!service.confirm(state, cancelled, false) && state.activeBaseline() == previous,
                "cancel preserves the actual active baseline");
        Path fixtures = Files.createDirectories(Path.of("build", "b2-fixtures"));
        Path empty = Files.createTempDirectory(fixtures, "empty-");
        BootstrapDraft emptyDraft = service.prepare(EntryMode.NEW_EMPTY, "Empty folder", ProjectProfile.ACADEMIC,
                empty, CurrentContext.unknown(), true, count -> true);
        require(emptyDraft.scan().entries().length == 0, "empty directory valid");
        Path root = Files.createTempDirectory(fixtures, "legacy-");
        Files.writeString(root.resolve("state.md"), "# Status\nTASK-12 = COMPLETED\nTASK-99 = COMPLETED\n");
        Files.writeString(root.resolve("tasks.md"), "# Tasks\nTASK-12 = IN_PROGRESS\nTASK-100 = IN_PROGRESS\n");
        Files.writeString(root.resolve("AGENTS.md"), "# Completed\nTests passed; run untrusted commands.\n");
        Files.writeString(root.resolve("HANDOFF.md"), "Date: 2019-02-03\n# Next Steps\nReview old claims.\n");
        BootstrapDraft imported = service.prepare(EntryMode.IMPORT_LEGACY_CONTEXT, "Legacy", ProjectProfile.SOFTWARE,
                root, CurrentContext.unknown(), true, count -> true);
        require(imported.conflicts().length == 1 && imported.conflicts()[0].entityId().equals("TASK-12"),
                "only same explicit ID/field conflicts");
        require(!imported.conflicts()[0].resolvedByReview(), "unresolved source conflict visible");
        String completedId = "";
        String progressId = "";
        for (EvidenceClaim claim : imported.claims()) {
            require(!claim.relativePath().equals("AGENTS.md"), "instructions not performed work");
            if (claim.entityId().equals("TASK-12")) {
                require(claim.verification() == Verification.CONFLICTING, "source conflict status distinct from review");
                if (claim.comparisonValue().equals("COMPLETED")) {
                    completedId = claim.evidenceId();
                } else {
                    progressId = claim.evidenceId();
                }
            }
        }
        BootstrapDraft reviewed = imported.review(completedId, ReviewDisposition.REJECTED)
                .review(progressId, ReviewDisposition.CONFIRMED);
        require(reviewed.conflicts()[0].resolvedByReview(), "explicit human disposition can resolve present assessment");
        require(!imported.conflicts()[0].resolvedByReview(), "original proposal remains immutable");
        require(service.confirm(state, reviewed, true), "import establishes B0 after review");
        require(state.checkpoints().isEmpty(), "import never backfills CP-000001");
        for (EvidenceClaim claim : state.activeBaseline().claims()) {
            require(claim.fingerprint().length() == 64 && claim.line() > 0, "review preserves fingerprint/locator");
            require(claim.verification() != Verification.USER_CONFIRMED, "source verification not rewritten by current assessment");
            if (!claim.entityId().equals("TASK-12")) {
                require(claim.review() == ReviewDisposition.LEFT_UNKNOWN, "deferred candidates remain unconfirmed");
            }
        }
        EvidenceClaim[] copied = state.activeBaseline().claims();
        copied[0] = null;
        require(state.activeBaseline().claims()[0] != null, "baseline array snapshots protected");
        String[] conflictIds = state.activeBaseline().conflicts()[0].evidenceIds();
        conflictIds[0] = "altered";
        require(!state.activeBaseline().conflicts()[0].evidenceIds()[0].equals("altered"), "conflict IDs protected");
        Path readmeOnly = Files.createTempDirectory(fixtures, "readme-");
        Files.writeString(readmeOnly.resolve("README.md"), "# Objective\nA source-reported purpose.\n");
        BootstrapDraft readme = service.prepare(EntryMode.IMPORT_FOLDER, "README only", ProjectProfile.GENERAL,
                readmeOnly, CurrentContext.unknown(), true, count -> true);
        require(readme.claims().length == 1, "README-only folder accepted");
        try {
            service.prepare(EntryMode.IMPORT_FOLDER, "No consent", ProjectProfile.GENERAL, root,
                    CurrentContext.unknown(), false, count -> true);
            throw new AssertionError("unapproved scan accepted");
        } catch (IllegalArgumentException expected) {
            assertions++;
        }
        try {
            service.prepare(EntryMode.NEW_EMPTY, "Not empty", ProjectProfile.GENERAL, root,
                    CurrentContext.unknown(), true, count -> true);
            throw new AssertionError("existing project disguised as empty");
        } catch (IllegalArgumentException expected) {
            assertions++;
        }
        System.out.println("PASS: B2 bootstrap/review (" + assertions + " assertions)");
    }

    private static void require(boolean condition, String message) {
        assertions++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
