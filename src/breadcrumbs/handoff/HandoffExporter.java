package breadcrumbs.handoff;

import breadcrumbs.domain.WorkspaceState;
import breadcrumbs.inspector.ContentSafety;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/** Explicit create-only export. The destination directory must already exist and be reviewed. */
public final class HandoffExporter {
    public Path export(WorkspaceState state, HandoffService.Preview preview, Path directory,
                       String fileName, Path appStorage, boolean approved) throws IOException {
        if (!approved) { throw new IllegalArgumentException("Export cancelled; terminal preview remains available."); }
        if (!preview.workspaceId().equals(state.activeBaseline().workspaceId())
                || !preview.observationId().equals(state.currentObservationId())
                || !preview.stateFingerprint().equals(HandoffService.fingerprint(state))) {
            throw new IllegalArgumentException("Preview is stale or belongs to another workspace.");
        }
        if (fileName == null || !fileName.matches("[A-Za-z0-9][A-Za-z0-9_-]{0,79}\\.(?:md|txt)")
                || ContentSafety.excludedName(fileName)
                || fileName.matches("(?i)(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])\\..*")) {
            throw new IllegalArgumentException("Use a simple non-reserved .md or .txt filename.");
        }
        if (!directory.isAbsolute() || directory.getParent() == null || !directory.equals(directory.normalize()) || directory.toString().startsWith("\\\\")) {
            throw new IllegalArgumentException("Use a normalized absolute local export directory.");
        }
        Path source = state.activeBaseline().snapshot().root();
        if (source != null && directory.startsWith(source) || directory.startsWith(appStorage.toAbsolutePath().normalize())) {
            throw new IllegalArgumentException("Export outside the imported root and app state directory.");
        }
        Path component = directory.getRoot();
        for (Path part : directory) {
            component = component.resolve(part);
            BasicFileAttributes attributes = Files.readAttributes(component, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (!attributes.isDirectory() || attributes.isSymbolicLink() || attributes.isOther()) {
                throw new IOException("Export directory has a linked, special or unavailable component.");
            }
        }
        byte[] bytes = preview.text().getBytes(StandardCharsets.UTF_8);
        if (bytes.length > 4_000_000) { throw new IllegalArgumentException("Export exceeds 4 MB."); }
        Path target = directory.resolve(fileName);
        // CREATE_NEW prevents accidental replacement, including existing link entries.
        Files.write(target, bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS);
        if (!java.util.Arrays.equals(bytes, Files.readAllBytes(target))) { throw new IOException("Export readback failed."); }
        return target;
    }
}
