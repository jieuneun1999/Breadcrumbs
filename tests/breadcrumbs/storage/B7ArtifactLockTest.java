package breadcrumbs.storage;

import java.nio.file.*;
import java.nio.channels.FileChannel;
import static breadcrumbs.b4.Checks.*;

public final class B7ArtifactLockTest {
    private B7ArtifactLockTest() { }
    public static void main(String[] args) throws Exception { run(); }
    public static void run() throws Exception {
        if (!System.getProperty("os.name").startsWith("Windows")) { System.out.println("NOT_PROVEN: native Windows artifact lock"); return; }
        Path base = Files.createTempDirectory(Files.createDirectories(Path.of("build/b7-fixtures")), "locked-artifact-").toAbsolutePath();
        Path stage = Files.createDirectory(base.resolve("stage")), target = base.resolve("target");
        Path file = Files.writeString(stage.resolve("state.md"), "synthetic content");
        boolean denied = false;
        OpenOption noShareDelete = (OpenOption) Class.forName("com.sun.nio.file.ExtendedOpenOption").getField("NOSHARE_DELETE").get(null);
        try (var lock = FileChannel.open(file, StandardOpenOption.READ, noShareDelete)) {
            require(lock.isOpen(), "controlled OS handle opened");
            try { Files.move(stage, target, StandardCopyOption.ATOMIC_MOVE); }
            catch (AccessDeniedException expected) { denied = true; }
        }
        require(denied && Files.exists(stage.resolve("state.md")) && !Files.exists(target), "a transient non-sharing reader blocks directory promotion without data loss");
        Files.move(stage, target, StandardCopyOption.ATOMIC_MOVE);
        require(Files.readString(target.resolve("state.md")).equals("synthetic content"), "promotion succeeds when the real OS handle releases");
        var state = ContinuityTest.empty("W732", "Lock test"); var store = new WorkspaceStore(base.resolve("app")); store.save(state);
        FileChannel[] held = {null}; int[] retries = {0};
        var service = new ArtifactService(point -> {
            if (point == ArtifactService.Point.BEFORE_PUBLISH) {
                try (var paths = Files.list(base)) {
                    Path member = paths.filter(p -> p.getFileName().toString().startsWith(".breadcrumbs-package-")).findFirst().orElseThrow().resolve("state.md");
                    held[0] = FileChannel.open(member, StandardOpenOption.READ, noShareDelete);
                }
            } else if (point == ArtifactService.Point.DIRECTORY_BUSY) { retries[0]++; held[0].close(); }
        });
        try { service.exportPackage(state, store, service.preview(state, new String[0]), base.resolve("portable"), true); }
        finally { if (held[0] != null) { held[0].close(); } }
        require(retries[0] == 1 && Files.exists(base.resolve("portable/state.md")) && !state.dirty(), "actual locked publication retries only after AccessDenied and converges after handle release");
        Path permanent = Files.createDirectory(base.resolve("permanent")); retries[0] = 0;
        var blocked = new ArtifactService(point -> {
            if (point == ArtifactService.Point.BEFORE_PUBLISH) {
                try (var paths = Files.list(permanent)) { held[0] = FileChannel.open(paths.findFirst().orElseThrow().resolve("state.md"), StandardOpenOption.READ, noShareDelete); }
            } else if (point == ArtifactService.Point.DIRECTORY_BUSY) { retries[0]++; }
        });
        try { reject(() -> blocked.exportPackage(state, store, blocked.preview(state, new String[0]), permanent.resolve("package"), true), "permanent Windows lock fails within bounded retries"); }
        finally { held[0].close(); }
        require(retries[0] == 3 && !Files.exists(permanent.resolve("package")) && store.load("W732").checkpoints().isEmpty(), "permanent lock preserves canonical state and staged files without false export success");
        System.out.println("PASS: native Windows directory-sharing premise | " + base);
    }
}
