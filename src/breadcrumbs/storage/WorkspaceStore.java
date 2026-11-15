package breadcrumbs.storage;

import breadcrumbs.domain.WorkspaceState;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.io.InputStream;

/** App-owned files only. No method inspects or executes the saved source-root reference. */
public final class WorkspaceStore {
    public enum RecoveryPoint { BEFORE_STAGE, DURING_WRITE, BEFORE_VALIDATION, BEFORE_PROMOTION, AFTER_PROMOTION, DURING_POINTER, STARTUP_READ, CORRUPT_READ, PRUNING }
    @FunctionalInterface public interface RecoveryFault { void at(RecoveryPoint point) throws IOException; }
    public record RecoveryCandidate(String generation, java.time.Instant recordedAt, boolean valid, int checkpoints, String problem) { }
    public RecoveryCandidate[] recoveryCandidates(String id) throws IOException { return new GenerationStore(this).candidates(id); }
    public WorkspaceState readRecovery(String id, String generation) throws IOException { return new GenerationStore(this).read(id, generation); }
    public WorkspaceState recover(String id, String generation, boolean approved) throws IOException { return new GenerationStore(this).recover(id, generation, approved); }
    public void prune(String id, int keep, boolean approved) throws IOException { new GenerationStore(this).prune(id, keep, approved); }
    public record SavedWorkspace(String id, String name, String problem) { }
    private final Path root;
    private final RecoveryFault fault;

    public WorkspaceStore(Path root) { this(root, point -> { }); }
    public WorkspaceStore(Path root, RecoveryFault fault) {
        if (fault == null) { throw new IllegalArgumentException("Recovery fault observer required."); }
        this.fault = fault;
        this.root = root.toAbsolutePath().normalize();
        if (this.root.getParent() == null || this.root.toString().startsWith("\\\\")) {
            throw new IllegalArgumentException("Choose a local app storage subdirectory, not a drive/network root.");
        }
    }
    public Path root() { return root; }
    void at(RecoveryPoint point) throws IOException { fault.at(point); }

    public void initialize() throws IOException {
        checkAncestors(root);
        Files.createDirectories(root);
        checkDirectory(root);
        Path projects = root.resolve("workspaces");
        checkAncestors(projects);
        Files.createDirectories(projects);
        checkDirectory(projects);
    }

    public SavedWorkspace[] list() throws IOException {
        initialize();
        SavedWorkspace[] found = new SavedWorkspace[100];
        int size = 0;
        try (DirectoryStream<Path> paths = Files.newDirectoryStream(root.resolve("workspaces"))) {
            for (Path path : paths) {
                if (size == found.length) { throw new IOException("Storage has more than 100 workspace entries. Review it manually."); }
                String id = path.getFileName().toString();
                try {
                    validateId(id);
                    WorkspaceState state = load(id);
                    found[size++] = new SavedWorkspace(id, state.activeBaseline().displayName(), "");
                } catch (IOException | IllegalArgumentException bad) {
                    found[size++] = new SavedWorkspace(id, "Unavailable", "Invalid or unreadable saved workspace. Retain it for recovery.");
                }
            }
        }
        // A small bounded array is discovery transport, never a checkpoint chronology.
        for (int i = 1; i < size; i++) {
            SavedWorkspace entry = found[i]; int j = i - 1;
            while (j >= 0 && found[j].id().compareTo(entry.id()) > 0) { found[j + 1] = found[j--]; }
            found[j + 1] = entry;
        }
        SavedWorkspace[] result = new SavedWorkspace[size];
        System.arraycopy(found, 0, result, 0, size);
        return result;
    }

    public WorkspaceState load(String id) throws IOException {
        at(RecoveryPoint.STARTUP_READ);
        return new GenerationStore(this).load(id);
    }
    WorkspaceState readState(Path file, String id) throws IOException {
        at(RecoveryPoint.CORRUPT_READ);
        checkAncestors(file);
        BasicFileAttributes attrs = attributes(file);
        if (!attrs.isRegularFile() || attrs.size() > WorkspaceCodec.MAX_BYTES) { throw new IOException("Invalid workspace file type or size."); }
        byte[] bytes;
        try (InputStream input = Files.newInputStream(file, LinkOption.NOFOLLOW_LINKS)) {
            bytes = input.readNBytes(WorkspaceCodec.MAX_BYTES + 1);
        }
        WorkspaceState state = WorkspaceCodec.decode(bytes);
        if (!id.equals(state.activeBaseline().workspaceId())) { throw new IOException("Workspace directory/record identity mismatch."); }
        return state;
    }

    public void save(WorkspaceState state) throws IOException {
        save(state, false);
    }
    void save(WorkspaceState state, boolean recovery) throws IOException {
        state.markUnsaved();
        byte[] bytes = WorkspaceCodec.encode(state);
        // Validate the exact transport before it can replace an existing record.
        WorkspaceCodec.decode(bytes);
        Path source = state.activeBaseline().snapshot().root();
        if (source != null && root.startsWith(source.toAbsolutePath().normalize())) {
            throw new IOException("Storage must be outside the imported project. Select a different storage root.");
        }
        initialize();
        Path target = stateFile(state.activeBaseline().workspaceId());
        checkAncestors(target);
        if (!Files.exists(target.getParent(), LinkOption.NOFOLLOW_LINKS) && list().length >= 100) {
            throw new IOException("Storage capacity is 100 workspaces. Use another app storage directory.");
        }
        Files.createDirectories(target.getParent());
        checkDirectory(target.getParent());
        if (!recovery && (Files.exists(target, LinkOption.NOFOLLOW_LINKS)
                || Files.exists(target.getParent().resolve("current.txt"), LinkOption.NOFOLLOW_LINKS)
                || Files.exists(target.getParent().resolve("recovery"), LinkOption.NOFOLLOW_LINKS))) {
            // A corrupt previous file requires manual recovery; do not silently erase its evidence.
            load(state.activeBaseline().workspaceId());
        }
        new GenerationStore(this).commit(state.activeBaseline().workspaceId(), bytes);
        state.markSaved();
    }

    byte[] readExact(Path target) throws IOException {
        checkAncestors(target);
        try (InputStream in = Files.newInputStream(target, LinkOption.NOFOLLOW_LINKS)) {
            return in.readNBytes(WorkspaceCodec.MAX_BYTES + 1);
        }
    }

    Path stateFile(String id) {
        validateId(id);
        return root.resolve("workspaces").resolve(id).resolve("state.txt");
    }

    public static void validateId(String id) {
        if (id == null || !id.matches("W(?:[0-9]{3,9}|-[a-f0-9]{32})")) {
            throw new IllegalArgumentException("Invalid workspace identifier.");
        }
    }

    static BasicFileAttributes attributes(Path path) throws IOException {
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (attrs.isSymbolicLink() || attrs.isOther()) { throw new IOException("Linked/reparse storage paths are not allowed."); }
        return attrs;
    }
    static void checkDirectory(Path path) throws IOException {
        if (!attributes(path).isDirectory()) { throw new IOException("Storage path is not a directory."); }
    }
    static void checkAncestors(Path path) throws IOException {
        Path current = path.getRoot();
        for (Path part : path) {
            current = current.resolve(part);
            if (Files.exists(current, LinkOption.NOFOLLOW_LINKS)) { attributes(current); }
        }
    }
}
