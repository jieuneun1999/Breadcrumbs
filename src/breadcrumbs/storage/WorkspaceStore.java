package breadcrumbs.storage;

import breadcrumbs.domain.WorkspaceState;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.io.InputStream;

/** App-owned files only. No method inspects or executes the saved source-root reference. */
public final class WorkspaceStore {
    public record SavedWorkspace(String id, String name, String problem) { }
    private final Path root;

    public WorkspaceStore(Path root) {
        this.root = root.toAbsolutePath().normalize();
        if (this.root.getParent() == null || this.root.toString().startsWith("\\\\")) {
            throw new IllegalArgumentException("Choose a local app storage subdirectory, not a drive/network root.");
        }
    }
    public Path root() { return root; }

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
        Path file = stateFile(id);
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
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) {
            // A corrupt previous file requires manual recovery; do not silently erase its evidence.
            load(state.activeBaseline().workspaceId());
        }
        Path temp = Files.createTempFile(target.getParent(), "state-", ".tmp");
        try {
            Files.write(temp, bytes, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, LinkOption.NOFOLLOW_LINKS);
            checkAncestors(target);
            try {
                Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unsupported) {
                // B3 fails closed instead of deleting the last good file with a weaker fallback.
                throw new IOException("Atomic replacement unavailable. Workspace remains unsaved; use another filesystem.");
            }
            if (!WorkspaceCodec.digest(bytes).equals(WorkspaceCodec.digest(readExact(target)))) {
                throw new IOException("Save readback mismatch. Workspace remains unsaved.");
            }
            state.markSaved();
        } finally { Files.deleteIfExists(temp); }
    }

    private byte[] readExact(Path target) throws IOException {
        checkAncestors(target);
        try (InputStream in = Files.newInputStream(target, LinkOption.NOFOLLOW_LINKS)) {
            return in.readNBytes(WorkspaceCodec.MAX_BYTES + 1);
        }
    }

    private Path stateFile(String id) {
        validateId(id);
        return root.resolve("workspaces").resolve(id).resolve("state.txt");
    }

    public static void validateId(String id) {
        if (id == null || !id.matches("W(?:[0-9]{3,9}|-[a-f0-9]{32})")) {
            throw new IllegalArgumentException("Invalid workspace identifier.");
        }
    }

    private static BasicFileAttributes attributes(Path path) throws IOException {
        BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
        if (attrs.isSymbolicLink() || attrs.isOther()) { throw new IOException("Linked/reparse storage paths are not allowed."); }
        return attrs;
    }
    private static void checkDirectory(Path path) throws IOException {
        if (!attributes(path).isDirectory()) { throw new IOException("Storage path is not a directory."); }
    }
    private static void checkAncestors(Path path) throws IOException {
        Path current = path.getRoot();
        for (Path part : path) {
            current = current.resolve(part);
            if (Files.exists(current, LinkOption.NOFOLLOW_LINKS)) { attributes(current); }
        }
    }
}
