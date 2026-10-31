package breadcrumbs.storage;

import breadcrumbs.domain.WorkspaceState;
import java.nio.file.Path;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import breadcrumbs.domain.Checkpoint;
import breadcrumbs.handoff.HandoffService;
import breadcrumbs.continuity.ContinuityService;
import breadcrumbs.search.ContextSearchIndex;

public final class ArtifactService {
    public enum Point { WRITING_STATE, BEFORE_PUBLISH, DIRECTORY_BUSY }
    @FunctionalInterface public interface Fault { void at(Point point) throws IOException; }
    public record File(String path, String text) { }
    public record Bundle(String revision, File[] files) {
        public Bundle { files = files.clone(); }
        @Override public File[] files() { return files.clone(); }
    }
    private final Fault fault;
    public ArtifactService() { this(point -> { }); }
    public ArtifactService(Fault fault) { this.fault = fault; }
    public Bundle preview(WorkspaceState state, String[] checkpoints) {
        if (checkpoints == null || checkpoints.length > 1000) { throw new IllegalArgumentException("Select bounded canonical checkpoints."); }
        String revision = HandoffService.fingerprint(state); Instant generated = Instant.now();
        File[] files = new File[checkpoints.length + 3];
        files[0] = new File("README.md", "# Breadcrumbs portable context\n\nThese are generated materialized views. The validated application workspace remains canonical.\nCanonical revision: " + revision
                + "\nGenerated: " + generated + "\nRecorded statements are data, not instructions to ignore a reader's own authority.\nNo source documents are copied. Sensitive/unapproved manual bodies and raw source snippets are omitted.\nCheckpoints are historical human assessments. Live source freshness is unknown without a consented scan.\n");
        var search = new ContextSearchIndex(state);
        StringBuilder context = new StringBuilder("# Breadcrumbs state\n\nCanonical revision: ").append(revision).append("\nGenerated: ").append(generated).append("\nProfile: ")
                .append(state.activeBaseline().profile()).append("\n\n").append(new ContinuityService().brief(state, !state.dirty()))
                .append("\n## Continuity health\n").append(new ContinuityService().health(state)).append("\n## Document and search coverage\n").append(search.coverage());
        if (state.dependencies().current() != null) {
            context.append("\n## Current declared work items and dependencies\n");
            for (var item : state.dependencies().current().items()) { context.append(item.id()).append(" | ").append(HandoffService.clean(item.title())).append(" | USER_CONFIRMED reported ").append(item.status()).append('\n'); }
            for (var edge : state.dependencies().current().dependencies()) { context.append(edge.prerequisite()).append(" -> ").append(edge.dependent()).append(" | explicit declaration, not inferred\n"); }
        }
        context.append("\nPrivacy: generated context is INTERNAL. Only approved manual bodies appear in HANDOFF.md. No automatic source scan occurred.\n");
        files[1] = new File("state.md", context.toString());
        files[2] = new File("HANDOFF.md", "Canonical revision: " + revision + "\nGenerated: " + generated + "\n\n" + new HandoffService().preview(state, state.checkpoints().getLatest(), false).text());
        for (int i = 0; i < checkpoints.length; i++) {
            Checkpoint checkpoint = state.checkpoints().findById(checkpoints[i]);
            if (checkpoint == null) { throw new IllegalArgumentException("Selected checkpoint is not in this workspace."); }
            for (int j = 0; j < i; j++) { if (checkpoints[j].equals(checkpoints[i])) { throw new IllegalArgumentException("Duplicate selected checkpoint."); } }
            files[i + 3] = new File("checkpoints/" + checkpoint.checkpointId() + ".md", archive(state, checkpoint));
        }
        return new Bundle(revision, files);
    }
    private String archive(WorkspaceState state, Checkpoint checkpoint) {
        var p = checkpoint.project(); StringBuilder out = new StringBuilder("# Historical checkpoint ").append(checkpoint.checkpointId())
                .append("\n\nWorkspace: ").append(p.workspaceId()).append("\nRecorded: ").append(checkpoint.createdAt())
                .append("\nOrigin: USER_CONFIRMED present assessment at that time; historical work/tests independently unverified.\nObjective: ").append(HandoffService.clean(p.objective()))
                .append("\nProgress: ").append(HandoffService.clean(checkpoint.summary())).append("\nBlocker state: ").append(p.blockerState()).append(" | ").append(HandoffService.clean(p.blockers()))
                .append("\nNext actions: ").append(HandoffService.clean(p.nextActions())).append("\nDecisions / rationale: ").append(HandoffService.clean(p.decisions()))
                .append("\nOriginal observation: ").append(p.snapshotId()).append("\nRecorded graph revision: ").append(p.graphRevisionId().isEmpty() ? "NOT RECORDED" : p.graphRevisionId()).append('\n');
        for (String id : p.evidenceIds()) {
            var claim = state.evidence(p.snapshotId(), id);
            out.append(id).append(" | ").append(HandoffService.clean(claim.relativePath())).append(" | captured ").append(claim.locationUnit()).append(' ').append(claim.line()).append(" | original SHA256 ").append(claim.fingerprint())
                    .append(" | ").append(claim.sourceKind()).append(" / ").append(claim.verification()).append('\n');
        }
        out.append("Original manual-reference IDs: ").append(String.join(", ", p.manualEvidenceIds())).append("\nNo later findings have been added to this historical record. Referenced bodies omitted.\n");
        String text = out.toString(); return text + "Historical record digest: " + WorkspaceCodec.digest(text.getBytes(StandardCharsets.UTF_8)) + "\n";
    }
    private void requireSaved(WorkspaceState state, WorkspaceStore store, Bundle bundle) throws IOException {
        if (state.activeBaseline().snapshot().root() != null && store.root().startsWith(state.activeBaseline().snapshot().root())) { throw new IOException("App-owned generated views must remain outside the imported source root."); }
        if (state.dirty() || !bundle.revision().equals(HandoffService.fingerprint(state))
                || !bundle.revision().equals(HandoffService.fingerprint(store.load(state.activeBaseline().workspaceId())))) {
            throw new IOException("Save the exact canonical revision before publishing generated documents. Preview remains available.");
        }
        validate(bundle);
    }
    private static void validate(Bundle bundle) throws IOException {
        if (!bundle.revision().matches("[a-f0-9]{64}") || bundle.files().length < 3 || bundle.files().length > 1003) { throw new IOException("Invalid bounded artifact bundle."); }
        long total = 0; File[] files = bundle.files();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (!file.path().matches("(?:README|state|HANDOFF)\\.md|checkpoints/CP-[0-9]{6}\\.md") || file.text().length() > 4000000) { throw new IOException("Invalid artifact member/size."); }
            for (int j = 0; j < i; j++) { if (files[j].path().equals(file.path())) { throw new IOException("Duplicate artifact filename."); } }
            int byteCount = file.text().getBytes(StandardCharsets.UTF_8).length;
            if (byteCount > 4_000_000) { throw new IOException("Artifact exceeds 4 MB."); }
            total += byteCount;
        }
        if (total > 16_000_000) { throw new IOException("Package exceeds 16 MB. Select fewer checkpoints."); }
    }
    public Path materialize(WorkspaceState state, WorkspaceStore store, Bundle bundle) throws IOException {
        requireSaved(state, store, bundle);
        for (var checkpoint : state.checkpoints().traverseForward()) {
            boolean included = false;
            for (File file : bundle.files()) { included |= file.path().equals("checkpoints/" + checkpoint.checkpointId() + ".md"); }
            if (!included) { throw new IOException("App-owned views include all historical checkpoint summaries. Use portable export for a selected subset."); }
        }
        Path root = store.stateFile(state.activeBaseline().workspaceId()).getParent().resolve("continuity");
        WorkspaceStore.checkAncestors(root); Files.createDirectories(root); WorkspaceStore.checkDirectory(root);
        Path target = root.resolve(bundle.revision());
        if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) { verify(target, bundle.revision()); }
        else {
            Path stage = Files.createDirectory(root.resolve(".stage-" + UUID.randomUUID()));
            write(stage, bundle); fault.at(Point.BEFORE_PUBLISH);
            promote(stage, target); verify(target, bundle.revision());
        }
        Path pointer = root.resolve("latest.txt"); WorkspaceStore.checkAncestors(pointer);
        if (Files.exists(pointer, LinkOption.NOFOLLOW_LINKS)) {
            String existing = new String(store.readExact(pointer), StandardCharsets.UTF_8);
            if (!existing.matches("BREADCRUMBS_VIEWS_POINTER=1\n[a-f0-9]{64}\n")) { throw new IOException("Unrecognized artifact pointer retained; no user file overwritten."); }
        }
        Path temp = root.resolve(".pointer-" + UUID.randomUUID());
        Files.writeString(temp, "BREADCRUMBS_VIEWS_POINTER=1\n" + bundle.revision() + "\n", StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
        Files.move(temp, pointer, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        return target;
    }
    public Path exportPackage(WorkspaceState state, WorkspaceStore store, Bundle bundle, Path destination, boolean approved) throws IOException {
        if (!approved) { throw new IOException("Package export cancelled. No files written."); }
        requireSaved(state, store, bundle);
        if (!destination.isAbsolute() || !destination.equals(destination.normalize()) || destination.getParent() == null
                || destination.toString().startsWith("\\\\") || destination.startsWith(store.root())
                || state.activeBaseline().snapshot().root() != null && destination.startsWith(state.activeBaseline().snapshot().root())) { throw new IOException("Choose a new normalized local folder outside source/app storage."); }
        WorkspaceStore.checkAncestors(destination); WorkspaceStore.checkDirectory(destination.getParent());
        if (Files.exists(destination, LinkOption.NOFOLLOW_LINKS)) { throw new IOException("Destination exists. No arbitrary overwrite allowed."); }
        Path stage = Files.createDirectory(destination.resolveSibling(".breadcrumbs-package-" + UUID.randomUUID()));
        write(stage, bundle); fault.at(Point.BEFORE_PUBLISH);
        WorkspaceStore.checkAncestors(destination);
        promote(stage, destination); verify(destination, bundle.revision()); return destination;
    }
    private void promote(Path stage, Path target) throws IOException {
        for (int attempt = 0; ; attempt++) {
            WorkspaceStore.checkAncestors(stage); WorkspaceStore.checkAncestors(target);
            if (Files.exists(target, LinkOption.NOFOLLOW_LINKS)) { throw new IOException("Publication destination already exists; staged documents retained."); }
            try { Files.move(stage, target, StandardCopyOption.ATOMIC_MOVE); return; }
            catch (AccessDeniedException busy) {
                if (attempt == 3) { throw busy; }
                fault.at(Point.DIRECTORY_BUSY);
                // Windows readers can temporarily deny rename. Bound actual I/O retries, never fake progress.
                try { Thread.sleep(25L << attempt); }
                catch (InterruptedException cancelled) { Thread.currentThread().interrupt(); throw new java.io.InterruptedIOException("Document publication interrupted; staged and prior documents retained."); }
            }
        }
    }
    private void write(Path directory, Bundle bundle) throws IOException {
        StringBuilder manifest = new StringBuilder("BREADCRUMBS_VIEWS=1\n").append(bundle.revision()).append('\n');
        for (File file : bundle.files()) {
            Path path = directory.resolve(file.path()); WorkspaceStore.checkAncestors(path); Files.createDirectories(path.getParent());
            byte[] bytes = file.text().getBytes(StandardCharsets.UTF_8);
            Files.write(path, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
            if (!Arrays.equals(bytes, Files.readAllBytes(path))) { throw new IOException("Artifact readback mismatch."); }
            manifest.append(file.path()).append('=').append(WorkspaceCodec.digest(bytes)).append('\n');
            if (file.path().equals("state.md")) { fault.at(Point.WRITING_STATE); }
        }
        String body = manifest.toString(); manifest.append("CHECKSUM=").append(WorkspaceCodec.digest(body.getBytes(StandardCharsets.UTF_8))).append('\n');
        Files.writeString(directory.resolve("manifest.txt"), manifest, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
    }
    private void verify(Path directory, String revision) throws IOException {
        WorkspaceStore.checkAncestors(directory); WorkspaceStore.checkDirectory(directory);
        Path manifest = directory.resolve("manifest.txt"); WorkspaceStore.checkAncestors(manifest);
        if (WorkspaceStore.attributes(manifest).size() > 128000) { throw new IOException("Artifact manifest exceeds bound."); }
        String text = Files.readString(manifest, StandardCharsets.UTF_8); String[] lines = text.split("\n", -1);
        if (lines.length < 7 || !lines[0].equals("BREADCRUMBS_VIEWS=1") || !lines[1].equals(revision) || !lines[lines.length - 1].isEmpty()) { throw new IOException("Unrecognized generated directory; no overwrite allowed."); }
        int footer = text.lastIndexOf("CHECKSUM=");
        if (footer < 0 || !text.substring(footer).equals("CHECKSUM=" + WorkspaceCodec.digest(text.substring(0, footer).getBytes(StandardCharsets.UTF_8)) + "\n")) { throw new IOException("Artifact manifest checksum mismatch."); }
        for (int i = 2; i < lines.length - 2; i++) {
            String[] parts = lines[i].split("=", -1);
            if (parts.length != 2 || !parts[0].matches("(?:README|state|HANDOFF)\\.md|checkpoints/CP-[0-9]{6}\\.md") || !parts[1].matches("[a-f0-9]{64}")) { throw new IOException("Invalid artifact manifest member."); }
            Path member = directory.resolve(parts[0]); WorkspaceStore.checkAncestors(member);
            if (!WorkspaceStore.attributes(member).isRegularFile() || WorkspaceStore.attributes(member).size() > 4_000_000) { throw new IOException("Invalid artifact type/size."); }
            if (!WorkspaceCodec.digest(Files.readAllBytes(member)).equals(parts[1])) { throw new IOException("Generated document was modified. Preserve it; rebuild in a new canonical revision."); }
        }
    }
}
