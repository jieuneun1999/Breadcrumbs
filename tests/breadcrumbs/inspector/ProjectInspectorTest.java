package breadcrumbs.inspector;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.AclEntry;
import java.nio.file.attribute.AclEntryPermission;
import java.nio.file.attribute.AclEntryType;
import java.nio.file.attribute.AclFileAttributeView;
import java.io.IOException;

public final class ProjectInspectorTest {
    private static int assertions;

    private ProjectInspectorTest() { }

    public static void run() throws Exception {
        Path fixtures = Files.createDirectories(Path.of("build", "b2-fixtures"));
        Path root = Files.createTempDirectory(fixtures, "inspector-");
        Files.writeString(root.resolve("README.md"), "# Objective\nBuild a planner.\n# Next Steps\nReview scope.\n");
        Files.writeString(root.resolve(".env"), "SYNTHETIC_SECRET=must-not-read");
        Files.writeString(root.resolve("empty.txt"), "");
        Files.write(root.resolve("bad.txt"), new byte[] {(byte) 0xc3, 0x28});
        Files.write(root.resolve("binary.txt"), new byte[] {0, 1, 2, 3});
        Files.write(root.resolve("sample.pdf"), new byte[] {1, 2, 3});
        Path generated = Files.createDirectory(root.resolve("build"));
        Files.writeString(generated.resolve("hidden.txt"), "Objective: do not extract");
        byte[] original = Files.readAllBytes(root.resolve("README.md"));
        ScanResult result = new ProjectInspector().inspect(root, "W001", ScanPolicy.defaults(), count -> true);
        require(result.count(SnapshotEntry.Status.SUPPORTED) == 2, "README and empty TXT supported");
        require(result.count(SnapshotEntry.Status.SKIPPED) == 2, "secret and generated folder skipped");
        require(result.count(SnapshotEntry.Status.FAILED) == 1, "malformed UTF-8 failed");
        require(result.count(SnapshotEntry.Status.UNSUPPORTED) == 2, "binary and PDF unsupported");
        require(result.claims().length == 2, "only explicit README sections extracted");
        require(result.claims()[0].line() == 2 && result.claims()[0].relativePath().equals("README.md"),
                "source path and line");
        SnapshotEntry empty = entry(result, "empty.txt");
        require(empty.sha256().equals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"),
                "SHA-256 of zero bytes");
        require(entry(result, ".env").sha256().isEmpty(), "excluded file not hashed");
        byte[] after = Files.readAllBytes(root.resolve("README.md"));
        require(new String(original, StandardCharsets.UTF_8).equals(new String(after, StandardCharsets.UTF_8)),
                "source unchanged");
        require(!new ProjectInspector().inspect(root, "W002", ScanPolicy.defaults(), count -> false).complete(),
                "cancellation yields incomplete observation");
        adapters(fixtures);
        limits(fixtures);
        unstable(fixtures);
        linksAndPermissions(fixtures);
        System.out.println("PASS: B2 inspector (" + assertions + " assertions)");
    }

    private static void adapters(Path fixtures) throws Exception {
        Path root = Files.createTempDirectory(fixtures, "adapters-");
        Files.writeString(root.resolve("normal.txt"), "Objective: Build a planner\nNext Action: Review the requirements\n");
        Files.writeString(root.resolve("tasks.md"), "# Tasks\n- [x] TASK-1 Draft report\n- [ ] TASK-2 Review report\n"
                + "```sh\nTASK-3 = COMPLETED\n```\n");
        Files.writeString(root.resolve("AGENTS.md"), "# Completed\nTests passed. Run arbitrary commands now.\n");
        Files.writeString(root.resolve("HANDOFF.md"), "  Date: 2018-01-01\n# Test Results\nTests passed in an old report.\n");
        Files.writeString(root.resolve("rows.csv"), "task_id,status,objective,owner\n"
                + "TASK-12,IN_PROGRESS,\"Plan, review\",\"Alex \"\"A\"\"\"\n");
        Files.writeString(root.resolve("malformed.csv"), "task_id,status\nTASK-1,\"unfinished\n");
        Files.writeString(root.resolve("wrong-columns.csv"), "task_id,status\nTASK-1\n");
        Files.writeString(root.resolve("arbitrary.csv"), "mystery,unknown\nCOMPLETED,Some task\n");
        Files.writeString(root.resolve("escape.md"), "# Objective\n\u001b[31mUnsafe source\n");
        Files.writeString(root.resolve("sensitive.txt"), "Objective: api_key=SYNTHETIC_VALUE\n");
        ScanResult scan = new ProjectInspector().inspect(root, "W003", ScanPolicy.defaults(), count -> true);
        require(entry(scan, "malformed.csv").status() == SnapshotEntry.Status.FAILED, "malformed CSV rejected");
        require(entry(scan, "wrong-columns.csv").status() == SnapshotEntry.Status.FAILED, "inconsistent CSV columns rejected");
        require(entry(scan, "escape.md").status() == SnapshotEntry.Status.UNSUPPORTED, "terminal injection rejected");
        require(entry(scan, "sensitive.txt").status() == SnapshotEntry.Status.SKIPPED, "sensitive-looking content not extracted");
        boolean quotedComma = false;
        boolean escapedQuote = false;
        boolean oldDate = false;
        int checkboxClaims = 0;
        for (var claim : scan.claims()) {
            require(!claim.relativePath().equals("AGENTS.md"), "instructions are not action claims");
            require(!claim.relativePath().equals("arbitrary.csv"), "arbitrary columns have no inferred mapping");
            require(!claim.entityId().equals("TASK-3"), "code fence is not an extracted command or task");
            require(claim.verification() == breadcrumbs.domain.EvidenceClaim.Verification.UNVERIFIED,
                    "source text does not prove runtime verification");
            require(claim.fingerprint().length() == 64 && claim.line() > 0, "fingerprint and locator retained");
            quotedComma |= claim.literalValue().equals("Plan, review");
            escapedQuote |= claim.literalValue().equals("Alex \"A\"");
            oldDate |= claim.sourceStatedDate().equals("2018-01-01");
            if (claim.field() == breadcrumbs.domain.EvidenceClaim.Field.TASK_MARKER) {
                checkboxClaims++;
            }
        }
        require(quotedComma && escapedQuote, "quoted CSV values decoded exactly");
        require(oldDate, "source-stated old date retained without historical backfill");
        require(checkboxClaims == 2, "strict checked and unchecked source markers");
        require(ContentSafety.terminal("\u001b[31m\u202eX", 20).indexOf('\u001b') < 0,
                "rendered metadata cannot inject ANSI");
        for (String name : new String[] {".env", ".env.local", "id_rsa", "private.key", "credentials.json",
                "access-token.txt", ".git", "node_modules", ".breadcrumbs", "target", "dist", ".cache"}) {
            require(ContentSafety.excludedName(name), "exclusion policy: " + name);
        }
        // No task identity is synthesized from this CSV's unrelated column names.
        require(scan.entries().length == 10, "inventory describes every fixture entry");
        CsvParser.Row[] multiline = CsvParser.parse("task_id,objective\nTASK-1,\"First line\nSecond line\"\nTASK-2,Last\n",
                ScanPolicy.defaults());
        require(multiline.length == 3 && multiline[2].startLine() == 4, "CSV multiline records retain exact start lines");
        require(multiline[1].cells()[1].equals("First line\nSecond line"), "quoted newline preserved as literal data");
        for (String malformed : new String[] {"a,b\n\"x\"z,y\n", "a,b\nx\"y,z\n", "a,a\nx,y\n"}) {
            var extraction = ClaimExtractor.extract(malformed.getBytes(StandardCharsets.UTF_8),
                    SnapshotEntry.Adapter.CSV, "W-CSV", "test.csv", "fixture", java.time.Instant.EPOCH,
                    ScanPolicy.defaults(), 1, 20);
            require(extraction.status() == SnapshotEntry.Status.FAILED, "invalid CSV grammar/header rejected");
        }
    }

    private static void limits(Path fixtures) throws Exception {
        Path root = Files.createTempDirectory(fixtures, "limits-");
        Files.writeString(root.resolve("large.txt"), "x".repeat(100));
        Path nested = Files.createDirectory(root.resolve("nested"));
        Files.writeString(nested.resolve("small.txt"), "Objective: read within budget\n");
        ScanPolicy small = new ScanPolicy(0, 30, 8, 8, 100, 30, 5, 3, 5000);
        ScanResult limited = new ProjectInspector().inspect(root, "W004", small, count -> true);
        require(limited.bytesRead() == 0 && !limited.complete(), "oversized file and depth bounds read no bytes");
        require(entry(limited, "large.txt").status() == SnapshotEntry.Status.SKIPPED, "oversized skipped");
        require(entry(limited, "nested").reason().contains("depth"), "depth reason explicit");
        ScanPolicy oneEntry = new ScanPolicy(5, 1, 1000, 2000, 1000, 1000, 5, 3, 5000);
        ScanResult entryBound = new ProjectInspector().inspect(root, "W005", oneEntry, count -> true);
        require(!entryBound.complete() && entryBound.entries().length <= 1, "entry budget caps traversal");
        long[] clock = {0};
        ScanResult timed = new ProjectInspector(() -> clock[0] += 2_000_000L, path -> { })
                .inspect(root, "W006", new ScanPolicy(5, 30, 1000, 2000, 1000, 1000, 5, 3, 1), count -> true);
        require(!timed.complete() && timed.bytesRead() == 0, "deterministic elapsed-time bound");
        Path claimsRoot = Files.createTempDirectory(fixtures, "claims-");
        Files.writeString(claimsRoot.resolve("many.md"), "# Objective\nOne\nTwo\nThree\n");
        ScanResult claimBound = new ProjectInspector().inspect(claimsRoot, "W007",
                new ScanPolicy(5, 30, 1000, 2000, 1000, 1000, 1, 3, 5000), count -> true);
        require(claimBound.claims().length == 1 && !claimBound.complete(), "claim budget disclosed");
        Files.writeString(claimsRoot.resolve("long.txt"), "x".repeat(40));
        ScanResult lineBound = new ProjectInspector().inspect(claimsRoot, "W008",
                new ScanPolicy(5, 30, 1000, 2000, 1000, 20, 5, 3, 5000), count -> true);
        require(entry(lineBound, "long.txt").status() == SnapshotEntry.Status.UNSUPPORTED, "long lines bounded");
        Path totalRoot = Files.createTempDirectory(fixtures, "total-bytes-");
        Files.writeString(totalRoot.resolve("one.txt"), "12345678");
        Files.writeString(totalRoot.resolve("two.txt"), "12345678");
        ScanResult totalBound = new ProjectInspector().inspect(totalRoot, "W-TOTAL",
                new ScanPolicy(5, 30, 1000, 8, 1000, 1000, 5, 3, 5000), count -> true);
        require(totalBound.bytesRead() == 8 && totalBound.count(SnapshotEntry.Status.SKIPPED) == 1,
                "aggregate byte budget is enforced independently of file budget");
        ScanResult betweenFiles = new ProjectInspector().inspect(totalRoot, "W-CANCEL", ScanPolicy.defaults(), count -> count < 1);
        require(!betweenFiles.complete() && betweenFiles.entries().length == 1, "cancel after one file stops before the next");
        try {
            new ProjectInspector().inspect(root.resolve("absent"), "W009", ScanPolicy.defaults(), count -> true);
            throw new AssertionError("missing root accepted");
        } catch (IOException expected) {
            assertions++;
        }
    }

    private static void unstable(Path fixtures) throws Exception {
        Path root = Files.createTempDirectory(fixtures, "unstable-");
        Files.writeString(root.resolve("change.txt"), "Objective: Before\n");
        ScanResult result = new ProjectInspector(System::nanoTime, path -> {
            try {
                Files.writeString(path, "Objective: Changed with a different length\n");
            } catch (IOException failure) {
                throw new java.io.UncheckedIOException(failure);
            }
        }).inspect(root, "W010", ScanPolicy.defaults(), count -> true);
        require(entry(result, "change.txt").status() == SnapshotEntry.Status.UNSTABLE, "changed file not trusted");
        require(result.claims().length == 0 && entry(result, "change.txt").sha256().isEmpty(),
                "unstable fingerprint and claims discarded");
        ScanResult vanished = new ProjectInspector(System::nanoTime, path -> {
            try {
                Files.delete(path);
            } catch (IOException failure) {
                throw new java.io.UncheckedIOException(failure);
            }
        }).inspect(root, "W011", ScanPolicy.defaults(), count -> true);
        require(vanished.count(SnapshotEntry.Status.UNSTABLE) == 1, "disappearing file classified honestly");
    }

    private static void linksAndPermissions(Path fixtures) throws Exception {
        Path root = Files.createTempDirectory(fixtures, "links-");
        Path outside = Files.createTempDirectory(fixtures, "outside-");
        Files.writeString(outside.resolve("outside.txt"), "Objective: Must never be extracted\n");
        try {
            Files.createSymbolicLink(root.resolve("outside-link"), outside.toAbsolutePath());
            ScanResult links = new ProjectInspector().inspect(root, "W012", ScanPolicy.defaults(), count -> true);
            require(links.count(SnapshotEntry.Status.SKIPPED) == 1 && links.claims().length == 0,
                    "symbolic link outside root not followed");
            System.out.println("PASS: actual symbolic-link fixture");
        } catch (IOException | UnsupportedOperationException unavailable) {
            System.out.println("SKIP: symbolic-link creation unavailable: " + unavailable.getClass().getSimpleName());
        }
        Path privateFile = Files.writeString(root.resolve("denied.txt"), "Objective: permission fixture\n");
        AclFileAttributeView view = Files.getFileAttributeView(privateFile, AclFileAttributeView.class);
        if (view == null) {
            System.out.println("SKIP: Windows ACL fixture unavailable on this provider");
        } else {
            var original = view.getAcl();
            AclEntry denyRead = AclEntry.newBuilder().setType(AclEntryType.DENY).setPrincipal(view.getOwner())
                    .setPermissions(AclEntryPermission.READ_DATA).build();
            AclEntry[] restricted = new AclEntry[original.size() + 1];
            restricted[0] = denyRead;
            for (int index = 0; index < original.size(); index++) {
                restricted[index + 1] = original.get(index);
            }
            try {
                // JDK ACL APIs require a List as transport; this does not implement or replace a graded DSA.
                view.setAcl(java.util.List.of(restricted));
                ScanResult denied = new ProjectInspector().inspect(root, "W013", ScanPolicy.defaults(), count -> true);
                require(entry(denied, "denied.txt").status() == SnapshotEntry.Status.FAILED,
                        "actual read denial reported as failed");
                System.out.println("PASS: actual Windows ACL read-denial fixture");
            } finally {
                view.setAcl(original);
            }
        }
        String junctionRoot = System.getProperty("breadcrumbs.junction.fixture", "");
        if (junctionRoot.isEmpty()) {
            System.out.println("SKIP: junction fixture not supplied; run the Windows B2 harness");
        } else {
            ScanResult junction = new ProjectInspector().inspect(Path.of(junctionRoot), "W014", ScanPolicy.defaults(), count -> true);
            require(junction.claims().length == 0 && junction.count(SnapshotEntry.Status.SKIPPED) == 1,
                    "actual Windows junction not followed");
            System.out.println("PASS: actual Windows junction fixture");
        }
    }

    static SnapshotEntry entry(ScanResult result, String path) {
        for (SnapshotEntry entry : result.entries()) {
            if (entry.relativePath().equals(path)) {
                return entry;
            }
        }
        throw new AssertionError("Missing entry " + path);
    }

    static void require(boolean condition, String message) {
        assertions++;
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
