package breadcrumbs.storage;

import breadcrumbs.domain.WorkspaceState;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

/** Immutable full-workspace generations. Pointer/snapshot disagreement requires explicit recovery. */
final class GenerationStore {
    private final WorkspaceStore store;
    private record Manifest(String generation, Instant savedAt, String digest) { }
    GenerationStore(WorkspaceStore store) { this.store = store; }
    private Path directory(String id) { return store.stateFile(id).getParent(); }
    private Path generations(String id) { return directory(id).resolve("recovery"); }
    WorkspaceState load(String id) throws IOException {
        Path current = directory(id).resolve("current.txt");
        Path[] entries = entries(id);
        if (!Files.exists(current, LinkOption.NOFOLLOW_LINKS)) {
            if (pending(entries, "")) { throw required(); }
            return store.readState(store.stateFile(id), id);
        }
        Manifest pointer = parse(current);
        if (pending(entries, pointer.generation())) { throw required(); }
        Manifest recorded = parse(generations(id).resolve(pointer.generation()).resolve("manifest.txt"));
        if (!pointer.equals(recorded)) { throw required(); }
        WorkspaceState generation = read(id, pointer.generation());
        store.readState(store.stateFile(id), id);
        if (!WorkspaceCodec.digest(store.readExact(store.stateFile(id))).equals(pointer.digest())) { throw required(); }
        return generation;
    }
    private static IOException required() { return new IOException("RECOVERY_REQUIRED. Saved generations retained. Inspect candidates and acknowledge recovery; no silent rollback."); }
    private static boolean pending(Path[] entries, String current) {
        for (Path path : entries) {
            String name = path.getFileName().toString();
            if (name.startsWith(".stage-") || name.matches("G-[0-9]{6}") && name.compareTo(current) > 0) { return true; }
        }
        return false;
    }
    private Path[] entries(String id) throws IOException {
        Path root = generations(id); WorkspaceStore.checkAncestors(root);
        if (!Files.exists(root, LinkOption.NOFOLLOW_LINKS)) { return new Path[0]; }
        WorkspaceStore.checkDirectory(root); Path[] paths = new Path[250]; int count = 0;
        try (DirectoryStream<Path> directory = Files.newDirectoryStream(root)) {
            for (Path path : directory) {
                if (count == paths.length) { throw new IOException("Recovery entry limit reached. Inspect app-owned recovery directory."); }
                WorkspaceStore.checkDirectory(path); paths[count++] = path;
            }
        }
        paths = Arrays.copyOf(paths, count);
        Arrays.sort(paths, (a, b) -> b.getFileName().toString().compareTo(a.getFileName().toString()));
        return paths;
    }
    WorkspaceStore.RecoveryCandidate[] candidates(String id) throws IOException {
        Path[] entries = entries(id); var candidates = new WorkspaceStore.RecoveryCandidate[entries.length + 1]; int count = 0, valid = 0;
        for (Path path : entries) {
            String name = path.getFileName().toString(); if (!name.matches("G-[0-9]{6}")) { continue; }
            try {
                Manifest manifest = parse(path.resolve("manifest.txt")); WorkspaceState state = read(id, name);
                candidates[count++] = new WorkspaceStore.RecoveryCandidate(name, manifest.savedAt(), true, state.checkpoints().size(), ""); valid++;
            } catch (IOException invalid) { candidates[count++] = new WorkspaceStore.RecoveryCandidate(name, null, false, 0, "Invalid schema/checksum/reference or unreadable generation."); }
        }
        if (valid == 0) {
            try {
                WorkspaceState snapshot = store.readState(store.stateFile(id), id);
                candidates[count++] = new WorkspaceStore.RecoveryCandidate("SNAPSHOT", snapshot.checkpoints().isEmpty()
                        ? snapshot.activeBaseline().confirmedAt() : snapshot.checkpoints().getLatest().createdAt(), true, snapshot.checkpoints().size(), "Validated compatibility snapshot; timestamp is recorded context time, not a generation save time.");
            } catch (IOException absent) { /* No validated compatibility snapshot is available. */ }
        }
        candidates = Arrays.copyOf(candidates, count);
        Arrays.sort(candidates, (a, b) -> a.valid() == b.valid() ? b.generation().compareTo(a.generation()) : a.valid() ? -1 : 1);
        return candidates;
    }
    WorkspaceState read(String id, String generation) throws IOException {
        if (generation.equals("SNAPSHOT")) { return store.readState(store.stateFile(id), id); }
        if (!generation.matches("G-[0-9]{6}")) { throw new IOException("Choose a listed generation identity."); }
        Path path = generations(id).resolve(generation); WorkspaceStore.checkAncestors(path); WorkspaceStore.checkDirectory(path);
        Manifest manifest = parse(path.resolve("manifest.txt"));
        if (!manifest.generation().equals(generation)) { throw new IOException("Generation directory/manifest identity mismatch."); }
        WorkspaceState state = store.readState(path.resolve("state.txt"), id);
        if (!WorkspaceCodec.digest(store.readExact(path.resolve("state.txt"))).equals(manifest.digest())) { throw new IOException("Generation checksum mismatch."); }
        return state;
    }
    WorkspaceState recover(String id, String generation, boolean approved) throws IOException {
        if (!approved) { throw new IOException("Recovery cancelled. No saved state changed."); }
        var candidates = candidates(id);
        if (candidates.length == 0 || !candidates[0].valid() || !candidates[0].generation().equals(generation)) {
            throw new IOException("Recover the newest validated candidate. Older generations are read-only previews; monotonic IDs/history must not roll back past intact data.");
        }
        WorkspaceState selected = read(id, generation); store.save(selected, true);
        for (Path path : entries(id)) {
            if (path.getFileName().toString().startsWith(".stage-")) {
                Files.move(path, path.resolveSibling(".retained-" + UUID.randomUUID()), StandardCopyOption.ATOMIC_MOVE);
            }
        }
        return store.load(id);
    }
    void commit(String id, byte[] bytes) throws IOException {
        store.at(WorkspaceStore.RecoveryPoint.BEFORE_STAGE);
        Path root = generations(id); WorkspaceStore.checkAncestors(root); Files.createDirectories(root); WorkspaceStore.checkDirectory(root);
        int maximum = 0, count = 0;
        for (Path path : entries(id)) {
            String name = path.getFileName().toString();
            if (name.matches("G-[0-9]{6}")) { maximum = Math.max(maximum, Integer.parseInt(name.substring(2))); count++; }
        }
        if (count >= 100 || maximum >= 999999) { throw new IOException("Generation capacity reached. Explicitly prune older retained generations before saving."); }
        String name = String.format(java.util.Locale.ROOT, "G-%06d", maximum + 1);
        Path stage = Files.createDirectory(root.resolve(".stage-" + UUID.randomUUID()));
        forceWrite(stage.resolve("state.txt"), bytes, WorkspaceStore.RecoveryPoint.DURING_WRITE);
        store.at(WorkspaceStore.RecoveryPoint.BEFORE_VALIDATION);
        WorkspaceState validated = store.readState(stage.resolve("state.txt"), id);
        if (!WorkspaceCodec.digest(store.readExact(stage.resolve("state.txt"))).equals(WorkspaceCodec.digest(bytes))
                || !validated.activeBaseline().workspaceId().equals(id)) { throw new IOException("Staging readback failed."); }
        byte[] manifest = encode(new Manifest(name, Instant.now(), WorkspaceCodec.digest(bytes)));
        forceWrite(stage.resolve("manifest.txt"), manifest, null); parse(stage.resolve("manifest.txt"));
        store.at(WorkspaceStore.RecoveryPoint.BEFORE_PROMOTION);
        WorkspaceStore.checkAncestors(root.resolve(name));
        Files.move(stage, root.resolve(name), StandardCopyOption.ATOMIC_MOVE);
        store.at(WorkspaceStore.RecoveryPoint.AFTER_PROMOTION);
        atomicReplace(store.stateFile(id), bytes, null);
        atomicReplace(directory(id).resolve("current.txt"), manifest, WorkspaceStore.RecoveryPoint.DURING_POINTER);
        if (!parse(directory(id).resolve("current.txt")).equals(parse(root.resolve(name).resolve("manifest.txt")))) { throw required(); }
        store.readState(store.stateFile(id), id);
        if (!WorkspaceCodec.digest(store.readExact(store.stateFile(id))).equals(WorkspaceCodec.digest(bytes))) { throw new IOException("Saved compatibility snapshot readback failed."); }
    }
    private void forceWrite(Path file, byte[] bytes, WorkspaceStore.RecoveryPoint point) throws IOException {
        WorkspaceStore.checkAncestors(file);
        try (FileChannel channel = FileChannel.open(file, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS)) {
            int split = bytes.length / 2; ByteBuffer first = ByteBuffer.wrap(bytes, 0, split);
            while (first.hasRemaining()) { channel.write(first); } channel.force(true);
            if (point != null) { store.at(point); }
            ByteBuffer rest = ByteBuffer.wrap(bytes, split, bytes.length - split);
            while (rest.hasRemaining()) { channel.write(rest); } channel.force(true);
        }
    }
    private void atomicReplace(Path target, byte[] bytes, WorkspaceStore.RecoveryPoint point) throws IOException {
        Path temp = target.resolveSibling(target.getFileName() + "-" + UUID.randomUUID() + ".tmp");
        forceWrite(temp, bytes, point); WorkspaceStore.checkAncestors(target);
        Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }
    private static byte[] encode(Manifest manifest) {
        String body = "GENERATION_FORMAT=1\n" + manifest.generation() + "\n" + manifest.savedAt() + "\n" + manifest.digest() + "\n";
        return (body + WorkspaceCodec.digest(body.getBytes(StandardCharsets.UTF_8)) + "\n").getBytes(StandardCharsets.UTF_8);
    }
    private Manifest parse(Path file) throws IOException {
        WorkspaceStore.checkAncestors(file); var attrs = WorkspaceStore.attributes(file);
        if (!attrs.isRegularFile() || attrs.size() > 1024) { throw new IOException("Invalid generation manifest type/size."); }
        byte[] bytes;
        try (var input = Files.newInputStream(file, LinkOption.NOFOLLOW_LINKS)) { bytes = input.readNBytes(1025); }
        try {
            String[] lines = new String(bytes, StandardCharsets.UTF_8).split("\n", -1);
            if (lines.length != 6 || !lines[0].equals("GENERATION_FORMAT=1") || !lines[1].matches("G-[0-9]{6}")
                    || !lines[3].matches("[a-f0-9]{64}") || !lines[5].isEmpty()) { throw new IllegalArgumentException(); }
            Manifest manifest = new Manifest(lines[1], Instant.parse(lines[2]), lines[3]);
            if (!Arrays.equals(bytes, encode(manifest))) { throw new IllegalArgumentException(); } return manifest;
        } catch (RuntimeException invalid) { throw new IOException("Invalid generation pointer/manifest. Recovery required."); }
    }
    void prune(String id, int keep, boolean approved) throws IOException {
        if (!approved || keep < 2 || keep > 100) { throw new IOException("Explicit consent and retention of at least two valid generations required."); }
        store.at(WorkspaceStore.RecoveryPoint.PRUNING); load(id);
        String current = parse(directory(id).resolve("current.txt")).generation(); int retained = 0;
        for (var candidate : candidates(id)) {
            if (!candidate.valid() || candidate.generation().equals("SNAPSHOT")) { continue; }
            if (++retained <= keep || candidate.generation().equals(current)) { continue; }
            Path path = generations(id).resolve(candidate.generation()); WorkspaceStore.checkAncestors(path);
            try (DirectoryStream<Path> members = Files.newDirectoryStream(path)) {
                for (Path member : members) {
                    String name = member.getFileName().toString();
                    if (!name.equals("state.txt") && !name.equals("manifest.txt")) { throw new IOException("Unexpected recovery contents retained; no recursive deletion."); }
                    if (!WorkspaceStore.attributes(member).isRegularFile()) { throw new IOException("Non-file recovery member retained."); }
                }
            }
            store.at(WorkspaceStore.RecoveryPoint.PRUNING);
            Files.delete(path.resolve("state.txt")); Files.delete(path.resolve("manifest.txt")); Files.delete(path);
        }
    }
}
