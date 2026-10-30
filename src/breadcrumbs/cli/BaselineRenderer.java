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

    BaselineRenderer(PrintStream output) {
        this.output = output;
    }

    void summary(String workspaceId, String name, ProjectProfile profile, EntryMode entryMode, ScanResult scan,
                 EvidenceClaim[] claims, ConflictGroup[] conflicts, boolean established, int checkpoints) {
        output.println("\nBREADCRUMBS - INITIAL PROJECT BASELINE " + (established ? "B0" : "PREVIEW"));
        output.println("Workspace: " + workspaceId + " | " + safe(name, 120));
        output.println("Entry mode: " + entryMode + " | Profile: " + profile);
        output.println("Observed at: " + scan.observedAt() + " | Scan: " + (scan.scanId().isEmpty() ? "NONE" : scan.scanId()));
        output.println("Policy: " + scan.policy().version() + " | " + scan.policy());
        output.println("Inventory entries: " + scan.entries().length + " (files and skipped directories)");
        for (SnapshotEntry.Status status : SnapshotEntry.Status.values()) {
            output.println(status + ": " + scan.count(status));
        }
        output.println("Bytes read: " + scan.bytesRead() + " | Traversal complete: " + scan.complete());
        output.println("Supported means adapter processed bounded text, not that the project is correct.");
        output.println("Excluded directories hide descendants; unvisited coverage is UNKNOWN.");
        for (String diagnostic : scan.diagnostics()) {
            output.println("Limit/coverage: " + safe(diagnostic, 200));
        }
        output.println("Documented context and present assessments:");
        for (Field field : new Field[] {Field.OBJECTIVE, Field.DELIVERABLE, Field.CURRENT_STATE,
                Field.NEXT_ACTION, Field.OWNER, Field.BLOCKERS}) {
            int found = 0;
            for (EvidenceClaim claim : claims) {
                if (claim.field() == field && claim.entityId().isEmpty()
                        && claim.review() != ReviewDisposition.REJECTED
                        && !claim.literalValue().equalsIgnoreCase("UNKNOWN")) {
                    if (found < 2) {
                        output.println("  " + field + ": " + safe(claim.literalValue(), 160) + " ["
                                + (claim.userSupplied() ? "USER_SUPPLIED" : "SOURCE_REPORTED") + ", "
                                + claim.review() + ", " + claim.evidenceId() + "]");
                    }
                    found++;
                }
            }
            if (found == 0) {
                output.println("  " + field + ": UNKNOWN (no accepted current fact is inferred)");
            } else if (found > 2) {
                output.println("  " + field + ": " + (found - 2) + " more candidates in details.");
            }
        }
        int unresolved = 0;
        for (ConflictGroup conflict : conflicts) {
            if (!conflict.resolvedByReview()) {
                unresolved++;
            }
        }
        output.println("Explicit-ID conflicts: " + conflicts.length + "; unresolved: " + unresolved);
        for (int index = 0; index < Math.min(conflicts.length, 10); index++) {
            ConflictGroup conflict = conflicts[index];
            output.println("  " + safe(conflict.entityId(), 80) + " " + conflict.field() + " - "
                    + (conflict.resolvedByReview() ? "USER_RESOLVED present assessment" : "CONFLICTING"));
        }
        output.println("Project native checkpoints: " + checkpoints);
        output.println("B0 is the first observed baseline. This is not a historical resume.");
        output.println("Human confirmation does not independently verify source claims or past tests.");
        output.println("Session-local: not saved after exit. No automatic CP-000001 or imported history.");
    }

    void claim(EvidenceClaim claim) {
        output.println(claim.evidenceId() + " | " + claim.field() + " | " + safe(claim.entityId(), 80));
        output.println("  " + (claim.userSupplied() ? "USER_SUPPLIED" : "SOURCE_REPORTED") + " | "
                + claim.provenance() + " | source verification: " + claim.verification()
                + " | review: " + claim.review());
        output.println("  Source: " + safe(claim.relativePath(), 180) + " line " + claim.line()
                + " [" + safe(claim.section(), 100) + "] | " + claim.artifactRole());
        output.println("  Literal: " + safe(claim.literalValue(), 600));
        if (!claim.fingerprint().isEmpty()) {
            output.println("  SHA-256: " + claim.fingerprint());
        }
        if (!claim.sourceStatedDate().isEmpty()) {
            output.println("  Source-stated date (unverified): " + safe(claim.sourceStatedDate(), 120));
        }
    }

    void entry(SnapshotEntry entry) {
        output.println(safe(entry.relativePath(), 180) + " | " + entry.status() + " | "
                + entry.adapter() + " | role: " + entry.artifactRole());
        output.println("  Size: " + entry.sizeBytes() + " | Modified metadata: " + entry.lastModified());
        output.println("  SHA-256: " + (entry.sha256().isEmpty() ? "NOT_AVAILABLE" : entry.sha256()));
        output.println("  " + safe(entry.reason(), 200));
    }

    void baseline(BaselineB0 baseline, int checkpoints) {
        summary(baseline.workspaceId(), baseline.displayName(), baseline.profile(), baseline.entryMode(),
                baseline.snapshot(), baseline.claims(), baseline.conflicts(), true, checkpoints);
        output.println("Confirmed at: " + baseline.confirmedAt());
    }

    private static String safe(String text, int length) {
        return ContentSafety.terminal(text, length);
    }
}
