package breadcrumbs.storage;

import breadcrumbs.bootstrap.*;
import breadcrumbs.checkpoint.ProjectCheckpointService;
import breadcrumbs.domain.*;
import breadcrumbs.inspector.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;

/** Behavioral round trips and adversarial files all use disposable, injected storage. */
public final class ContinuityTest {
    private static int assertions;
    private ContinuityTest() { }
    public static void run() throws Exception {
        Path fixtures = Files.createDirectories(Path.of("build", "b3-fixtures"));
        Path storage = Files.createTempDirectory(fixtures, "storage-");
        WorkspaceStore store = new WorkspaceStore(storage);
        require(store.list().length == 0, "first run initializes empty app storage");
        WorkspaceState state = empty("W001", "Planner | 日本語");
        require(state.checkpoints().isEmpty(), "B0 is not CP-000001");
        store.save(state);
        require(!state.dirty(), "successful save clears dirty flag");
        WorkspaceState restored = store.load("W001");
        require(Arrays.equals(WorkspaceCodec.encode(state), WorkspaceCodec.encode(restored)), "deterministic complete B0 roundtrip");
        require(restored.activeBaseline().displayName().equals("Planner | 日本語"), "Unicode and delimiter name retained");
        var service = new ProjectCheckpointService();
        Checkpoint preview = preview(service, restored, "First", new String[] {restored.activeBaseline().claims()[0].evidenceId()});
        require(preview.checkpointId().equals("CP-000001") && restored.checkpoints().isEmpty(), "preview does not reserve ID");
        require(!service.confirm(restored, preview, false) && restored.checkpoints().isEmpty(), "decline does not mutate");
        require(service.confirm(restored, preview, true), "explicit confirmation appends first real CP");
        require(restored.checkpoints().getLatest().project().nextActions().equals("Next | action\n続ける\tcheck"), "multiline context retained");
        Checkpoint stale = preview(service, restored, "Second", new String[0]);
        service.confirm(restored, stale, true);
        WorkspaceState beforeReload = restored;
        reject(() -> service.confirm(beforeReload, stale, true), "stale preview cannot duplicate confirmation");
        store.save(restored);
        restored = store.load("W001");
        require(restored.checkpoints().size() == 2 && restored.checkpoints().validateIntegrity(), "DLL rebuilt and valid");
        require(restored.checkpoints().traverseForward()[0].checkpointId().equals("CP-000001"), "forward ordering");
        require(restored.checkpoints().traverseBackward()[0].checkpointId().equals("CP-000002"), "backward ordering");
        require(restored.checkpoints().getLatest().title().equals("Second"), "latest restored");
        require(preview(service, restored, "Third", new String[0]).checkpointId().equals("CP-000003"), "next ID after restart");
        restored.restoreNextCheckpointNumber(10);
        store.save(restored);
        require(preview(service, store.load("W001"), "Tenth", new String[0]).checkpointId().equals("CP-000010"), "counter not derived from current size/tail");
        WorkspaceState exhausted = store.load("W001"); exhausted.restoreNextCheckpointNumber(1000000);
        reject(() -> preview(service, exhausted, "Exhausted", new String[0]), "exhausted ID rejected");
        WorkspaceState second = empty("W002", "Separate"); store.save(second);
        require(store.list().length == 2 && store.load("W002").checkpoints().isEmpty(), "workspace histories isolated");
        require(store.load("W001").checkpoints().size() == 2, "saving another workspace preserves first");
        reject(() -> store.load("../W001"), "path traversal blocked");
        reject(() -> store.load("W999"), "missing workspace controlled");
        WorkspaceState finalRestored = restored;
        reject(() -> preview(service, finalRestored, "Foreign", new String[] {"EV-FOREIGN"}), "foreign evidence rejected");
        reject(() -> preview(service, finalRestored, "password=synthetic", new String[0]), "sensitive user context rejected");

        Path imported = Files.createTempDirectory(fixtures, "legacy-").toAbsolutePath();
        Files.writeString(imported.resolve("state.md"), "# Status\nTASK-12 = COMPLETED\n# Test Results\nTests passed yesterday.\n");
        Files.writeString(imported.resolve("tasks.md"), "# Tasks\nTASK-12 = IN_PROGRESS\n");
        Files.writeString(imported.resolve("HANDOFF.md"), "# Next Steps\nReview scope.\n");
        String originalHash = WorkspaceCodec.digest(Files.readAllBytes(imported.resolve("state.md")));
        BootstrapService bootstrap = new BootstrapService(ScanPolicy.defaults(), () -> "W003");
        BootstrapDraft draft = bootstrap.prepare(EntryMode.IMPORT_LEGACY_CONTEXT, "Legacy", ProjectProfile.SOFTWARE,
                imported, CurrentContext.unknown(), true, count -> true);
        draft = draft.review(draft.claims()[0].evidenceId(), EvidenceClaim.ReviewDisposition.CONFIRMED);
        WorkspaceState legacy = new WorkspaceState(); bootstrap.confirm(legacy, draft, true);
        store.save(legacy);
        WorkspaceState loadedLegacy = store.load("W003");
        require(loadedLegacy.checkpoints().isEmpty(), "imported history creates no native checkpoint");
        require(loadedLegacy.activeBaseline().conflicts().length == 1, "conflict reconstructed");
        require(!loadedLegacy.activeBaseline().conflicts()[0].resolvedByReview(), "unresolved conflict preserved");
        EvidenceClaim c = loadedLegacy.activeBaseline().claims()[0];
        require(c.review() == EvidenceClaim.ReviewDisposition.CONFIRMED && c.sourceKind() == EvidenceClaim.SourceKind.SOURCE_REPORTED,
                "confirmed present review retains source-reported origin");
        require(c.verification() != EvidenceClaim.Verification.USER_CONFIRMED, "source verification never upgraded");
        require(c.line() == draft.claims()[0].line() && c.fingerprint().equals(draft.claims()[0].fingerprint()), "locator and hash retained");
        require(Arrays.equals(WorkspaceCodec.encode(legacy), WorkspaceCodec.encode(loadedLegacy)), "complete source/review roundtrip");
        Checkpoint linked = preview(service, legacy, "Source-linked review", new String[] {c.evidenceId()});
        service.confirm(legacy, linked, true); store.save(legacy);
        Checkpoint reopenedLinked = store.load("W003").checkpoints().getLatest();
        require(reopenedLinked.project().evidenceIds()[0].equals(c.evidenceId()), "native checkpoint preserves actual imported evidence reference");
        require(reopenedLinked.project().snapshotId().equals(legacy.activeBaseline().snapshot().scanId()), "native checkpoint preserves observed snapshot reference");
        String[] externalRefs = reopenedLinked.project().evidenceIds(); externalRefs[0] = "EV-FOREIGN";
        require(reopenedLinked.project().evidenceIds()[0].equals(c.evidenceId()), "checkpoint evidence references are immutable defensive snapshots");
        Files.move(imported, imported.resolveSibling(imported.getFileName() + "-gone"));
        require(store.load("W003").activeBaseline().claims().length > 0, "reopen works without original source folder");
        Path gone = imported.resolveSibling(imported.getFileName() + "-gone");
        require(originalHash.equals(WorkspaceCodec.digest(Files.readAllBytes(gone.resolve("state.md")))), "imported bytes unchanged");

        byte[] valid = WorkspaceCodec.encode(store.load("W001"));
        reject(() -> WorkspaceCodec.decode(new byte[0]), "empty file rejected");
        reject(() -> WorkspaceCodec.decode(Arrays.copyOf(valid, valid.length - 7)), "truncation rejected");
        byte[] damaged = valid.clone(); damaged[40] ^= 1;
        reject(() -> WorkspaceCodec.decode(damaged), "checksum detects changed field");
        reject(() -> WorkspaceCodec.decode(new byte[] {(byte) 0xc3, 0x28}), "invalid UTF-8 rejected");
        reject(() -> WorkspaceCodec.decode(new byte[WorkspaceCodec.MAX_BYTES + 1]), "file bound rejected");
        reject(() -> WorkspaceCodec.decode(new String(valid, StandardCharsets.UTF_8).replace("FORMAT_VERSION=1", "FORMAT_VERSION=2").getBytes(StandardCharsets.UTF_8)), "unsupported version rejected");
        rejectField(valid, "created", "not-a-timestamp", "invalid timestamp");
        rejectField(valid, "nextCheckpoint", "1", "counter less than existing ID");
        rejectField(valid, "checkpoints", "1001", "count bound");
        rejectField(valid, "references", "2001", "reference count bound");
        rejectField(valid, "reference", "EV-NONEXISTENT", "broken evidence reference");
        rejectField(valid, "snapshot", "SCAN-FOREIGN", "wrong snapshot reference");
        rejectField(valid, "root", "../relative", "invalid source path");
        rejectField(valid, "id", "CP-000002", "duplicate or unordered CP ID");
        rejectField(valid, "origin", "DEMO", "demo cannot persist as project history");
        rejectField(valid, "title", "escape\u001b[31m", "terminal escape rejected");
        rejectField(valid, "name", "api_key=synthetic", "sensitive saved context rejected");
        reject(() -> WorkspaceCodec.decode(reseal(new String(valid, StandardCharsets.UTF_8).replaceFirst("name=[^\n]*", "name=%%%"))), "bad field encoding");
        WorkspaceState active = store.load("W002");
        Path file = storage.resolve("workspaces/W001/state.txt"); Files.writeString(file, "corrupt disposable fixture");
        try { active.replaceWith(store.load("W001")); } catch (IOException expected) { /* Active state is replaced only after load returns. */ }
        require(active.activeBaseline().workspaceId().equals("W002"), "invalid load preserves active workspace");
        require(store.list().length == 3 && !store.list()[0].problem().isEmpty(), "corrupt workspace disclosed without hiding good projects");
        WorkspaceState unsaved = empty("W001", "Unwritten");
        reject(() -> store.save(unsaved), "corrupt existing file not overwritten");
        require(unsaved.dirty() && Files.readString(file).equals("corrupt disposable fixture"), "failed save retains dirty state and old bytes");
        Path inside = gone.resolve("must-not-create");
        WorkspaceState insideState = new WorkspaceState();
        var b = legacy.activeBaseline();
        var s = b.snapshot();
        insideState.activate(new BaselineB0(b.workspaceId(), b.displayName(), b.profile(), b.entryMode(),
                new ScanResult(gone, s.scanId(), s.observedAt(), s.policy(), s.entries(), s.claims(), s.diagnostics(), s.complete(), s.bytesRead()),
                b.claims(), b.conflicts(), b.confirmedAt()));
        reject(() -> new WorkspaceStore(inside).save(insideState), "storage inside imported root rejected");
        require(!Files.exists(inside), "rejection creates no source directory");
        Path blocked = fixtures.resolve("blocked-" + System.nanoTime()); Files.writeString(blocked, "not a directory");
        reject(() -> new WorkspaceStore(blocked).initialize(), "unusable storage initialization controlled");
        Path aclRoot = Files.createTempDirectory(fixtures, "acl-");
        WorkspaceStore aclStore = new WorkspaceStore(aclRoot);
        WorkspaceState aclState = empty("W004", "ACL fixture"); aclStore.save(aclState);
        Path aclFile = aclRoot.resolve("workspaces/W004/state.txt");
        var view = Files.getFileAttributeView(aclFile, java.nio.file.attribute.AclFileAttributeView.class);
        if (view != null) {
            var originalAcl = view.getAcl();
            var deny = java.nio.file.attribute.AclEntry.newBuilder().setType(java.nio.file.attribute.AclEntryType.DENY)
                    .setPrincipal(view.getOwner()).setPermissions(java.nio.file.attribute.AclEntryPermission.READ_DATA).build();
            var restricted = new java.nio.file.attribute.AclEntry[originalAcl.size() + 1];
            restricted[0] = deny;
            for (int i = 0; i < originalAcl.size(); i++) { restricted[i + 1] = originalAcl.get(i); }
            try {
                // The JDK ACL API requires List transport; project history still uses only the custom DLL.
                view.setAcl(java.util.List.of(restricted));
                reject(() -> aclStore.load("W004"), "actual ACL load failure controlled");
                reject(() -> aclStore.save(aclState), "actual ACL save failure controlled");
                require(aclState.dirty(), "failed resave changes saved status to unsaved");
                require(!aclStore.list()[0].problem().isEmpty(), "permission denied workspace discovery disclosed");
                System.out.println("PASS: B3 actual Windows ACL read/save denial");
            } finally { view.setAcl(originalAcl); }
        } else { System.out.println("SKIP: B3 ACL fixture unavailable on this provider"); }
        String junction = System.getProperty("breadcrumbs.b3Junction");
        if (junction != null) {
            reject(() -> new WorkspaceStore(Path.of(junction)).initialize(), "actual Windows storage junction rejected");
            System.out.println("PASS: B3 actual storage junction rejection");
        } else { System.out.println("SKIP: B3 storage junction fixture not supplied; run B3 Windows harness"); }
        System.out.println("PASS: B3 continuity/storage (" + assertions + " assertions)");
    }

    public static WorkspaceState empty(String id, String name) throws Exception {
        BootstrapService bootstrap = new BootstrapService(ScanPolicy.defaults(), () -> id);
        var draft = bootstrap.prepare(EntryMode.NEW_EMPTY, name, ProjectProfile.ACADEMIC, null,
                new CurrentContext("Write report", "Report", "Review", "", ""), false, count -> true);
        WorkspaceState state = new WorkspaceState(); bootstrap.confirm(state, draft, true); return state;
    }
    private static Checkpoint preview(ProjectCheckpointService service, WorkspaceState state, String title, String[] refs) {
        return service.preview(state, title, "Progress | 日本語", "Objective\nwith details", ProjectContext.BlockerState.UNKNOWN,
                "", "Next | action\n続ける\tcheck", "Rationale", "", refs);
    }
    private static void rejectField(byte[] bytes, String key, String value, String message) throws Exception {
        String text = new String(bytes, StandardCharsets.UTF_8);
        String encoded = Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
        byte[] invalid = reseal(text.replaceFirst("(?m)^" + key + "=[^\n]*", key + "=" + encoded));
        reject(() -> WorkspaceCodec.decode(invalid), message);
    }
    private static byte[] reseal(String text) {
        String body = text.substring(0, text.lastIndexOf("CHECKSUM="));
        return (body + "CHECKSUM=" + WorkspaceCodec.digest(body.getBytes(StandardCharsets.UTF_8)) + "\n").getBytes(StandardCharsets.UTF_8);
    }
    private interface Action { void run() throws Exception; }
    private static void reject(Action action, String message) throws Exception {
        boolean rejected = false;
        try { action.run(); } catch (IOException | IllegalArgumentException expected) { rejected = true; }
        require(rejected, message);
    }
    private static void require(boolean condition, String message) {
        assertions++; if (!condition) { throw new AssertionError(message); }
    }
}
