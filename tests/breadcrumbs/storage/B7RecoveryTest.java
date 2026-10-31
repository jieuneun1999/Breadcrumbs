package breadcrumbs.storage;

import breadcrumbs.b4.WorkflowTest;
import breadcrumbs.domain.WorkspaceState;
import java.nio.file.*;
import static breadcrumbs.b4.Checks.*;

public final class B7RecoveryTest {
    private B7RecoveryTest() { }
    public static void main(String[] args) throws Exception { run(); }
    public static void run() throws Exception {
        Path base = Files.createTempDirectory(Files.createDirectories(Path.of("build/b7-fixtures")), "recovery-").toAbsolutePath();
        var store = new WorkspaceStore(base.resolve("normal"));
        WorkspaceState state = ContinuityTest.empty("W701", "Recovery example"); store.save(state);
        WorkflowTest.checkpoint(state, "First recorded progress", new String[0]); store.save(state);
        require(store.recoveryCandidates("W701").length == 2, "each confirmed save retains an immutable generation");
        require(store.load("W701").checkpoints().size() == 1, "normal pointer/generation/snapshot agreement");
        byte[] before = Files.readAllBytes(store.root().resolve("workspaces/W701/state.txt"));
        for (var candidate : store.recoveryCandidates("W701")) { require(candidate.valid(), "complete candidates validate"); }
        require(store.readRecovery("W701", "G-000001").checkpoints().isEmpty(), "prior generation read-only preview");
        require(java.util.Arrays.equals(before, Files.readAllBytes(store.root().resolve("workspaces/W701/state.txt"))), "preview never activates or writes");
        reject(() -> store.recover("W701", "G-000001", true), "cannot roll back past a newer valid generation");
        for (var point : new WorkspaceStore.RecoveryPoint[] {WorkspaceStore.RecoveryPoint.BEFORE_STAGE,
                WorkspaceStore.RecoveryPoint.DURING_WRITE, WorkspaceStore.RecoveryPoint.BEFORE_VALIDATION,
                WorkspaceStore.RecoveryPoint.BEFORE_PROMOTION, WorkspaceStore.RecoveryPoint.AFTER_PROMOTION,
                WorkspaceStore.RecoveryPoint.DURING_POINTER}) {
            Path storage = base.resolve(point.name()); var good = new WorkspaceStore(storage);
            var prior = ContinuityTest.empty("W702", "Fault " + point); WorkflowTest.checkpoint(prior, "Original", new String[0]); good.save(prior);
            String original = WorkspaceCodec.digest(Files.readAllBytes(storage.resolve("workspaces/W702/recovery/G-000001/state.txt")));
            WorkflowTest.checkpoint(prior, "Interrupted update", new String[0]);
            var faulty = new WorkspaceStore(storage, where -> { if (where == point) { throw new java.io.IOException("Injected " + point); } });
            reject(() -> faulty.save(prior), "save fault " + point);
            require(prior.dirty(), "failed save retains in-memory dirty context " + point);
            require(original.equals(WorkspaceCodec.digest(Files.readAllBytes(storage.resolve("workspaces/W702/recovery/G-000001/state.txt")))), "last-good generation bytes survive " + point);
            var candidates = good.recoveryCandidates("W702");
            var selected = candidates[0]; require(selected.valid(), "latest candidate is validated " + point);
            if (point != WorkspaceStore.RecoveryPoint.BEFORE_STAGE) { reject(() -> good.load("W702"), "interruption is explicit " + point); }
            reject(() -> good.recover("W702", selected.generation(), false), "recovery requires acknowledgement");
            var restored = good.recover("W702", selected.generation(), true);
            require(restored.checkpoints().size() == (point == WorkspaceStore.RecoveryPoint.AFTER_PROMOTION || point == WorkspaceStore.RecoveryPoint.DURING_POINTER ? 2 : 1), "recover newest intact history " + point);
            require(good.load("W702").nextCheckpointNumber() == restored.nextCheckpointNumber(), "monotonic checkpoint counter after recovery " + point);
        }
        Path current = store.root().resolve("workspaces/W701/current.txt"); Files.writeString(current, "corrupt pointer");
        reject(() -> store.load("W701"), "invalid pointer never silently falls back");
        require(store.recoveryCandidates("W701")[0].valid(), "read-only candidate discovery survives pointer corruption");
        store.recover("W701", "G-000002", true);
        require(store.load("W701").checkpoints().size() == 1, "acknowledged recovery restores complete history");
        reject(() -> store.prune("W701", 2, false), "retention prune requires consent");
        store.prune("W701", 2, true); require(store.recoveryCandidates("W701").length == 2, "prune keeps current and previous valid generation");
        for (var point : new WorkspaceStore.RecoveryPoint[] {WorkspaceStore.RecoveryPoint.STARTUP_READ, WorkspaceStore.RecoveryPoint.CORRUPT_READ, WorkspaceStore.RecoveryPoint.PRUNING}) {
            var faulty = new WorkspaceStore(store.root(), where -> { if (where == point) { throw new java.io.IOException("Injected read/retention fault"); } });
            if (point == WorkspaceStore.RecoveryPoint.PRUNING) { reject(() -> faulty.prune("W701", 2, true), "prune failure controlled"); }
            else { reject(() -> faulty.load("W701"), "read failure controlled"); }
            require(store.load("W701").checkpoints().size() == 1, "read/prune fault cannot delete history");
        }
        for (String fixture : new String[] {"tests/fixtures/b3-v1/imported-workspace.txt", "tests/fixtures/b4-v2/workspace.txt"}) {
            byte[] bytes = Files.readAllBytes(Path.of(fixture)); var legacy = WorkspaceCodec.decode(bytes);
            Path legacyRoot = base.resolve("old-" + legacy.activeBaseline().workspaceId()); var legacyStore = new WorkspaceStore(legacyRoot);
            Path legacyFile = Files.createDirectories(legacyRoot.resolve("workspaces").resolve(legacy.activeBaseline().workspaceId())).resolve("state.txt");
            Files.write(legacyFile, bytes); require(java.util.Arrays.equals(bytes, WorkspaceCodec.encode(legacyStore.load(legacy.activeBaseline().workspaceId()))), "genuine legacy load stays exact");
            legacyStore.save(legacy); require(java.util.Arrays.equals(bytes, WorkspaceCodec.encode(legacyStore.load(legacy.activeBaseline().workspaceId()))), "generation wrapper preserves canonical old version");
        }
        System.out.println("PASS: B7A generation recovery and injected failures | " + base);
    }
}
