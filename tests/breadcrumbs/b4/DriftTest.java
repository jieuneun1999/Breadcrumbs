package breadcrumbs.b4;

import breadcrumbs.continuity.DriftEngine;
import breadcrumbs.continuity.DriftEngine.Status;
import breadcrumbs.domain.EvidenceClaim;
import breadcrumbs.inspector.*;
import java.nio.file.Path;
import java.time.Instant;
import static breadcrumbs.b4.Checks.*;

public final class DriftTest {
    private DriftTest() { }
    private static final String A = "a".repeat(64), B = "b".repeat(64);
    public static void run() {
        DriftEngine engine = new DriftEngine();
        SnapshotEntry old = file("one.md", A, SnapshotEntry.Status.SUPPORTED);
        require(status(engine, scan(true, old), scan(true, old)) == Status.UNCHANGED, "same reliable hash");
        require(status(engine, scan(true, old), scan(true, file("one.md", B, SnapshotEntry.Status.SUPPORTED))) == Status.MODIFIED, "different reliable hash");
        require(status(engine, scan(true, old), scan(true)) == Status.DELETED, "complete comparable deletion");
        require(status(engine, scan(true), scan(true, old)) == Status.ADDED, "complete comparable addition");
        require(status(engine, scan(true, old), scan(true, file("new.md", A, SnapshotEntry.Status.SUPPORTED))) == Status.RENAMED_EXACT, "unique rename");
        var ambiguous = engine.compare(scan(true, old), scan(true, file("new.md", A, SnapshotEntry.Status.SUPPORTED), file("copy.md", A, SnapshotEntry.Status.SUPPORTED)));
        require(ambiguous.changes().length == 1 && ambiguous.changes()[0].status() == Status.AMBIGUOUS_RENAME, "one-to-many rename is ambiguous");
        var many = engine.compare(scan(true, old, file("two.md", A, SnapshotEntry.Status.SUPPORTED)), scan(true, file("new.md", A, SnapshotEntry.Status.SUPPORTED)));
        require(many.changes().length == 2 && many.changes()[0].status() == Status.AMBIGUOUS_RENAME && many.changes()[1].status() == Status.AMBIGUOUS_RENAME, "many-to-one rename ambiguous on both sources");
        require(status(engine, scan(true, old), scan(false)) == Status.UNKNOWN, "partial scan cannot delete");
        require(status(engine, scan(false), scan(true, old)) == Status.UNKNOWN, "incomplete prior scan cannot add");
        require(status(engine, scan(false, old), scan(false, old)) == Status.UNCHANGED, "same path reliable hash still comparable under partial coverage");
        for (SnapshotEntry.Status status : new SnapshotEntry.Status[] {SnapshotEntry.Status.FAILED, SnapshotEntry.Status.SKIPPED, SnapshotEntry.Status.UNSTABLE}) {
            require(status(engine, scan(true, old), scan(true, file("one.md", A, status))) == Status.UNKNOWN, "unreliable status " + status);
        }
        require(status(engine, scan(true, old), scan(true, file("one.md", "", SnapshotEntry.Status.SUPPORTED))) == Status.UNKNOWN, "empty hash is not empty-file hash");
        var unsupported = file("binary.bin", A, SnapshotEntry.Status.UNSUPPORTED);
        require(status(engine, scan(true, unsupported), scan(true, unsupported)) == Status.UNCHANGED, "reliable unsupported file content hash still comparable");
        ScanResult before = scan(true, old);
        ScanResult otherRoot = new ScanResult(Path.of("build", "different").toAbsolutePath(), "OTHER", Instant.now(), before.policy(), before.entries(), before.claims(), new String[0], true, 0);
        require(status(engine, before, otherRoot) == Status.UNKNOWN, "foreign root not comparable");
        ScanResult policyChanged = new ScanResult(before.root(), "OTHER", Instant.now(), new ScanPolicy(0, 1, 100, 100, 100, 100, 1, 1, 100), before.entries(), before.claims(), new String[0], true, 0);
        require(status(engine, before, policyChanged) == Status.UNKNOWN, "different policy conservative unknown");
        var sorted = engine.compare(scan(true, file("z.md", A, SnapshotEntry.Status.SUPPORTED), old), scan(true));
        require(sorted.changes()[0].oldPath().equals("one.md"), "deterministic report order");
        var mutation = sorted.changes(); mutation[0] = null; require(sorted.changes()[0] != null, "comparison is immutable");
        System.out.println("PASS: B4 drift classification and uncertainty");
    }
    private static Status status(DriftEngine engine, ScanResult before, ScanResult after) { return engine.compare(before, after).changes()[0].status(); }
    private static SnapshotEntry file(String path, String hash, SnapshotEntry.Status status) {
        return new SnapshotEntry(path, 1, Instant.EPOCH, hash, status, SnapshotEntry.Adapter.MARKDOWN,
                EvidenceClaim.ArtifactRole.UNKNOWN, Instant.now(), "Synthetic fixture");
    }
    private static ScanResult scan(boolean complete, SnapshotEntry... entries) {
        return new ScanResult(Path.of("build", "synthetic-drift").toAbsolutePath(), "TEST", Instant.now(), ScanPolicy.defaults(),
                entries, new EvidenceClaim[0], new String[0], complete, 0);
    }
}
