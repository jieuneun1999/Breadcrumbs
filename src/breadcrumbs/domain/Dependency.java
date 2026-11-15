package breadcrumbs.domain;

import java.time.Instant;

/** An accepted prerequisite -> dependent declaration, never inferred from imported text. */
public record Dependency(String workspaceId, String prerequisite, String dependent,
                         String observationId, String evidenceId, String declaration, Instant confirmedAt) {
    public Dependency {
        if (workspaceId == null || prerequisite == null || dependent == null || observationId == null
                || evidenceId == null || confirmedAt == null || declaration == null || declaration.isBlank()) {
            throw new IllegalArgumentException("Explicit dependency declaration and confirmation metadata required.");
        }
        declaration = ProjectContext.note(declaration);
    }
}
