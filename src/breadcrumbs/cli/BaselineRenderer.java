package breadcrumbs.cli;

import breadcrumbs.domain.BaselineB0;
import breadcrumbs.domain.EntryMode;
import breadcrumbs.domain.ProjectProfile;
import breadcrumbs.domain.ConflictGroup;
import breadcrumbs.domain.EvidenceClaim;
import breadcrumbs.domain.EvidenceClaim.Field;
import breadcrumbs.domain.EvidenceClaim.ReviewDisposition;
import breadcrumbs.inspector.ContentSafety;
import breadcrumbs.inspector.ScanResult;
import breadcrumbs.inspector.SnapshotEntry;
import java.io.PrintStream;

/** Bounded terminal views keep source reports separate from current human assessments. */
final class BaselineRenderer {
    private final PrintStream output;
    private final boolean projectShell;
    private final TerminalOutput ui;

    BaselineRenderer(PrintStream output) {
        this(output, false);
    }

    BaselineRenderer(PrintStream output, boolean projectShell) {
        this(output, projectShell, new TerminalTheme(false), 80);
    }
    BaselineRenderer(PrintStream output, boolean projectShell, TerminalTheme theme, int width) {
        this.output = output;
        this.projectShell = projectShell;
        ui = new TerminalOutput(output, theme, width);
    }

    void summary(String workspaceId, String name, ProjectProfile profile, EntryMode entryMode, ScanResult scan,
                 EvidenceClaim[] claims, ConflictGroup[] conflicts, boolean established, int checkpoints) {
        ui.heading("\nBREADCRUMBS - INITIAL PROJECT BASELINE " + (established ? "B0" : "PREVIEW"));
        ui.plain("Workspace: " + workspaceId + " | " + safe(name, 120));
        ui.plain("Entry mode: " + entryMode + " | Profile: " + profile);
        ui.plain("Observed at: " + scan.observedAt() + " | Scan: " + (scan.scanId().isEmpty() ? "NONE" : scan.scanId()));
        ui.plain("Policy: " + scan.policy().version() + " | " + scan.policy());
        ui.plain("Inventory entries: " + scan.entries().length + " (files and skipped directories)");
        for (SnapshotEntry.Status status : SnapshotEntry.Status.values()) {
            ui.plain(status + ": " + scan.count(status));
        }
        ui.plain("Bytes read: " + scan.bytesRead() + " | Traversal complete: " + scan.complete());
        ui.plain("Supported means adapter processed bounded text, not that the project is correct.");
        ui.plain("Excluded directories hide descendants; unvisited coverage is UNKNOWN.");
        for (String diagnostic : scan.diagnostics()) {
            ui.plain("Limit/coverage: " + safe(diagnostic, 200));
        }
        ui.plain("Documented context and present assessments:");
        for (Field field : new Field[] {Field.OBJECTIVE, Field.DELIVERABLE, Field.CURRENT_STATE,
                Field.NEXT_ACTION, Field.OWNER, Field.BLOCKERS}) {
            int found = 0;
            for (EvidenceClaim claim : claims) {
                if (claim.field() == field && claim.entityId().isEmpty()
                        && claim.review() != ReviewDisposition.REJECTED
                        && !claim.literalValue().equalsIgnoreCase("UNKNOWN")) {
                    if (found < 2) {
                        ui.plain("  " + field + ": " + safe(claim.literalValue(), 160) + " ["
                                + (claim.userSupplied() ? "USER_SUPPLIED" : "SOURCE_REPORTED") + ", "
                                + claim.review() + ", " + claim.evidenceId() + "]");
                    }
                    found++;
                }
            }
            if (found == 0) {
                ui.plain("  " + field + ": UNKNOWN (no accepted current fact is inferred)");
            } else if (found > 2) {
                ui.plain("  " + field + ": " + (found - 2) + " more candidates in details.");
            }
        }
        int unresolved = 0;
        for (ConflictGroup conflict : conflicts) {
            if (!conflict.resolvedByReview()) {
                unresolved++;
            }
        }
        ui.plain("Explicit-ID conflicts: " + conflicts.length + "; unresolved: " + unresolved);
        for (int index = 0; index < Math.min(conflicts.length, 10); index++) {
            ConflictGroup conflict = conflicts[index];
            ui.plain("  " + safe(conflict.entityId(), 80) + " " + conflict.field() + " - "
                    + (conflict.resolvedByReview() ? "USER_RESOLVED present assessment" : "CONFLICTING"));
        }
        ui.plain("Project native checkpoints: " + checkpoints);
        ui.plain("B0 is the first observed baseline. This is not a historical resume.");
        ui.plain("Human confirmation does not independently verify source claims or past tests.");
        ui.plain(projectShell ? "Saved observation only. Source freshness UNKNOWN; see project storage status."
                : "Session-local: not saved after exit. No automatic CP-000001 or imported history.");
    }

    void claim(EvidenceClaim claim) {
        ui.plain(claim.evidenceId() + " | " + claim.field() + " | " + safe(claim.entityId(), 80));
        ui.plain("  " + (claim.userSupplied() ? "USER_SUPPLIED" : "SOURCE_REPORTED") + " | "
                + claim.provenance() + " | source verification: " + claim.verification()
                + " | review: " + claim.review());
        ui.plain("  Source: " + safe(claim.relativePath(), 180) + " " + claim.locationUnit() + " " + claim.line()
                + " [" + safe(claim.section(), 100) + "] | " + claim.artifactRole());
        ui.line("  Literal: ", safe(claim.literalValue(), 600));
        if (!claim.fingerprint().isEmpty()) {
            ui.plain("  SHA-256: " + claim.fingerprint());
        }
        if (!claim.sourceStatedDate().isEmpty()) {
            ui.plain("  Source-stated date (unverified): " + safe(claim.sourceStatedDate(), 120));
        }
    }

    void entry(SnapshotEntry entry) {
        ui.plain(safe(entry.relativePath(), 180) + " | " + entry.status() + " | "
                + entry.adapter() + " | role: " + entry.artifactRole());
        ui.plain("  Size: " + entry.sizeBytes() + " | Modified metadata: " + entry.lastModified());
        ui.plain("  SHA-256: " + (entry.sha256().isEmpty() ? "NOT_AVAILABLE" : entry.sha256()));
        ui.plain("  " + safe(entry.reason(), 200));
    }

    void baseline(BaselineB0 baseline, int checkpoints) {
        summary(baseline.workspaceId(), baseline.displayName(), baseline.profile(), baseline.entryMode(),
                baseline.snapshot(), baseline.claims(), baseline.conflicts(), true, checkpoints);
        ui.plain("Confirmed at: " + baseline.confirmedAt());
    }

    private static String safe(String text, int length) {
        return ContentSafety.terminal(text, length);
    }
}
