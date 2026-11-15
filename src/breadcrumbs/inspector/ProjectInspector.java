package breadcrumbs.inspector;

import breadcrumbs.domain.EvidenceClaim;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.DirectoryIteratorException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.util.function.LongSupplier;

/** Owns read-only filesystem access. Adapters never receive a filesystem handle. */
public final class ProjectInspector {
    private final LongSupplier monotonicTime;
    private final Consumer<Path> afterRead;

    public ProjectInspector() {
        this(System::nanoTime, path -> { });
    }

    // This package-local seam lets tests change a real fixture precisely between reading and final metadata checks.
    ProjectInspector(LongSupplier monotonicTime, Consumer<Path> afterRead) {
        this.monotonicTime = monotonicTime;
        this.afterRead = afterRead;
    }

    public ScanResult inspect(Path selectedRoot, String workspaceId, ScanPolicy policy,
                              IntPredicate continueScanning) throws IOException {
        Path root = validateRoot(selectedRoot);
        ScanSession session = new ScanSession(root, workspaceId, policy, continueScanning);
        session.scanDirectory(root, 0);
        return session.finish();
    }

    public static Path validateRoot(Path selectedRoot) throws IOException {
        Path root = selectedRoot.toAbsolutePath().normalize();
        if (root.equals(root.getRoot()) || root.toString().startsWith("\\\\")
                || root.toString().startsWith("//") || ContentSafety.excludedName(root.getFileName().toString())) {
            throw new IllegalArgumentException("Select a bounded local project folder, not a drive, network, or excluded root.");
        }
        // Check each ancestor as well as the final component; a junction in a parent is also a boundary escape.
        Path component = root.getRoot();
        for (Path part : root) {
            component = component.resolve(part);
            BasicFileAttributes attributes = attributes(component);
            if (attributes.isSymbolicLink() || attributes.isOther()) {
                throw new IllegalArgumentException("Linked or reparse-point roots are not inspected.");
            }
        }
        if (!attributes(root).isDirectory()) {
            throw new IllegalArgumentException("The selected path is not a directory.");
        }
        return root.toRealPath();
    }

    private static BasicFileAttributes attributes(Path path) throws IOException {
        return Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
    }

    private final class ScanSession {
        final Path root;
        final String workspaceId;
        final ScanPolicy policy;
        final IntPredicate continueScanning;
        final long started = monotonicTime.getAsLong();
        final Instant observedAt = Instant.now();
        final SnapshotEntry[] entries;
        final EvidenceClaim[] claims;
        final String[] diagnostics = new String[16];
        int entryCount;
        int visited;
        int claimCount;
        int diagnosticCount;
        long bytesRead;
        boolean complete = true;
        boolean stopped;

        ScanSession(Path root, String workspaceId, ScanPolicy policy, IntPredicate continueScanning) {
            this.root = root;
            this.workspaceId = workspaceId;
            this.policy = policy;
            this.continueScanning = continueScanning;
            entries = new SnapshotEntry[policy.maxEntries()];
            claims = new EvidenceClaim[policy.maxClaims()];
        }

        boolean withinTime() {
            if (!stopped && monotonicTime.getAsLong() - started >= policy.maxDurationMillis() * 1_000_000L) {
                stop("Scan duration limit reached; unvisited coverage is UNKNOWN.");
            }
            return !stopped;
        }

        void stop(String reason) {
            complete = false;
            stopped = true;
            diagnostic(reason);
        }

        void diagnostic(String reason) {
            if (diagnosticCount < diagnostics.length) {
                diagnostics[diagnosticCount++] = reason;
            }
        }

        void scanDirectory(Path directory, int depth) throws IOException {
            if (!withinTime()) {
                return;
            }
            try (DirectoryStream<Path> children = Files.newDirectoryStream(directory)) {
                for (Path child : children) {
                    if (!withinTime()) {
                        return;
                    }
                    if (!continueScanning.test(visited)) {
                        stop("Scan cancelled between entries; unvisited coverage is UNKNOWN.");
                        return;
                    }
                    if (visited == policy.maxEntries()) {
                        stop("Entry limit reached; unvisited coverage is UNKNOWN.");
                        return;
                    }
                    visited++;
                    inspectEntry(child, depth);
                    if (stopped) {
                        return;
                    }
                }
            } catch (IOException | DirectoryIteratorException | SecurityException failure) {
                if (directory.equals(root)) {
                    throw new IOException("The selected folder cannot be enumerated.", failure);
                }
                add(directory, null, "", SnapshotEntry.Status.FAILED, SnapshotEntry.Adapter.DIRECTORY,
                        "Directory could not be enumerated.");
                complete = false;
            }
        }

        void inspectEntry(Path path, int depth) {
            BasicFileAttributes before = null;
            try {
                if (ContentSafety.excludedName(path.getFileName().toString())) {
                    add(path, null, "", SnapshotEntry.Status.SKIPPED, SnapshotEntry.Adapter.METADATA_ONLY,
                            "Excluded sensitive or generated name; contents not read.");
                    return;
                }
                before = attributes(path);
                if (before.isSymbolicLink() || before.isOther()) {
                    add(path, before, "", SnapshotEntry.Status.SKIPPED, SnapshotEntry.Adapter.METADATA_ONLY,
                            "Symbolic link, reparse point, or special entry; not followed.");
                    return;
                }
                ensureContained(path);
                if (before.isDirectory()) {
                    if (depth >= policy.maxDepth()) {
                        add(path, before, "", SnapshotEntry.Status.SKIPPED, SnapshotEntry.Adapter.DIRECTORY,
                                "Directory depth limit reached; descendants not inspected.");
                        complete = false;
                    } else {
                        scanDirectory(path, depth + 1);
                    }
                    return;
                }
                if (!before.isRegularFile()) {
                    add(path, before, "", SnapshotEntry.Status.SKIPPED, SnapshotEntry.Adapter.METADATA_ONLY,
                            "Not an ordinary file.");
                    return;
                }
                inspectFile(path, before);
            } catch (IOException | SecurityException failure) {
                add(path, before, "", SnapshotEntry.Status.FAILED, SnapshotEntry.Adapter.METADATA_ONLY,
                        "Entry access failed; contents and underlying error text are not displayed.");
                complete = false;
            }
        }

        void ensureContained(Path path) throws IOException {
            Path normalized = path.toAbsolutePath().normalize();
            if (!normalized.startsWith(root)) {
                throw new IOException("Path escaped root.");
            }
            Path component = root;
            for (Path part : root.relativize(normalized)) {
                component = component.resolve(part);
                BasicFileAttributes current = attributes(component);
                if (current.isSymbolicLink() || current.isOther()) {
                    throw new IOException("Linked component.");
                }
            }
            if (!normalized.toRealPath().startsWith(root)) {
                throw new IOException("Resolved path escaped root.");
            }
        }

        void inspectFile(Path path, BasicFileAttributes before) throws IOException {
            SnapshotEntry.Adapter adapter = adapter(path);
            if (before.size() > policy.maxFileBytes() || before.size() > policy.maxTotalBytes() - bytesRead) {
                add(path, before, "", SnapshotEntry.Status.SKIPPED, adapter, "File or total byte budget exceeded; not read.");
                complete = false;
                return;
            }
            MessageDigest digest;
            try {
                digest = MessageDigest.getInstance("SHA-256");
            } catch (NoSuchAlgorithmException unavailable) {
                throw new IllegalStateException("Required JDK SHA-256 is unavailable.", unavailable);
            }
            ByteArrayOutputStream textBytes = new ByteArrayOutputStream();
            long fileBytesRead = 0;
            // A fixed-size buffer bounds memory; metadata bounds and post-read checks detect normal source changes.
            try (SeekableByteChannel channel = Files.newByteChannel(path, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS)) {
                ByteBuffer buffer = ByteBuffer.allocate(4096);
                while (fileBytesRead < before.size()) {
                    if (!withinTime()) {
                        add(path, before, "", SnapshotEntry.Status.SKIPPED, adapter, "Scan stopped before fingerprint completed.");
                        return;
                    }
                    buffer.clear();
                    buffer.limit((int) Math.min(buffer.capacity(), before.size() - fileBytesRead));
                    int count = channel.read(buffer);
                    if (count < 0) {
                        break;
                    }
                    if (count == 0) {
                        continue;
                    }
                    fileBytesRead += count;
                    bytesRead += count;
                    digest.update(buffer.array(), 0, count);
                    if (adapter != SnapshotEntry.Adapter.METADATA_ONLY) {
                        textBytes.write(buffer.array(), 0, count);
                    }
                }
            }
            afterRead.accept(path);
            BasicFileAttributes after;
            try {
                ensureContained(path);
                after = attributes(path);
            } catch (IOException | SecurityException changed) {
                add(path, before, "", SnapshotEntry.Status.UNSTABLE, adapter, "File disappeared or changed identity during inspection.");
                complete = false;
                return;
            }
            if (fileBytesRead != before.size() || after.size() != before.size()
                    || !after.lastModifiedTime().equals(before.lastModifiedTime())
                    || !after.creationTime().equals(before.creationTime())) {
                add(path, before, "", SnapshotEntry.Status.UNSTABLE, adapter, "File metadata changed during inspection; fingerprint discarded.");
                complete = false;
                return;
            }
            String hash = HexFormat.of().formatHex(digest.digest());
            if (adapter == SnapshotEntry.Adapter.METADATA_ONLY) {
                add(path, before, hash, SnapshotEntry.Status.UNSUPPORTED, adapter, "Metadata and fingerprint only; content not understood.");
                return;
            }
            ClaimExtractor.Extraction extraction = ClaimExtractor.extract(textBytes.toByteArray(), adapter,
                    workspaceId, relative(path), hash, observedAt, policy, claimCount + 1, claims.length - claimCount);
            for (EvidenceClaim claim : extraction.claims()) {
                claims[claimCount++] = claim;
            }
            complete &= !extraction.limited();
            // A suffix alone is not adapter discovery. A non-package has only metadata coverage.
            if (adapter == SnapshotEntry.Adapter.DOCX && extraction.reason().startsWith("DOCX extension has no supported ZIP")) { adapter = SnapshotEntry.Adapter.METADATA_ONLY; }
            add(path, before, hash, extraction.status(), adapter, extraction.reason());
        }

        SnapshotEntry.Adapter adapter(Path path) {
            String lower = path.getFileName().toString().toLowerCase(Locale.ROOT);
            if (lower.endsWith(".txt")) { return SnapshotEntry.Adapter.TXT; }
            if (lower.endsWith(".md")) { return SnapshotEntry.Adapter.MARKDOWN; }
            if (lower.endsWith(".csv")) { return SnapshotEntry.Adapter.CSV; }
            if (lower.endsWith(".docx")) { return SnapshotEntry.Adapter.DOCX; }
            return SnapshotEntry.Adapter.METADATA_ONLY;
        }

        String relative(Path path) {
            return root.relativize(path.toAbsolutePath().normalize()).toString().replace('\\', '/');
        }

        void add(Path path, BasicFileAttributes metadata, String hash, SnapshotEntry.Status status,
                 SnapshotEntry.Adapter adapter, String reason) {
            if (entryCount == entries.length) {
                stop("Inventory limit reached; further coverage is UNKNOWN.");
                return;
            }
            String relativePath = relative(path);
            entries[entryCount++] = new SnapshotEntry(relativePath, metadata == null ? -1 : metadata.size(),
                    metadata == null ? null : metadata.lastModifiedTime().toInstant(), hash, status, adapter,
                    ClaimExtractor.role(relativePath), Instant.now(), reason);
        }

        ScanResult finish() {
            withinTime();
            SnapshotEntry[] inventory = new SnapshotEntry[entryCount];
            EvidenceClaim[] extractedClaims = new EvidenceClaim[claimCount];
            String[] warnings = new String[diagnosticCount];
            System.arraycopy(entries, 0, inventory, 0, entryCount);
            System.arraycopy(claims, 0, extractedClaims, 0, claimCount);
            System.arraycopy(diagnostics, 0, warnings, 0, diagnosticCount);
            return new ScanResult(root, "SCAN-" + workspaceId, observedAt, policy, inventory,
                    extractedClaims, warnings, complete, bytesRead);
        }
    }
}
