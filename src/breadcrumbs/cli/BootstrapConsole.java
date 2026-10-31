package breadcrumbs.cli;

import breadcrumbs.bootstrap.BootstrapDraft;
import breadcrumbs.bootstrap.BootstrapService;
import breadcrumbs.bootstrap.CurrentContext;
import breadcrumbs.domain.BaselineB0;
import breadcrumbs.domain.EntryMode;
import breadcrumbs.domain.EvidenceClaim;
import breadcrumbs.domain.EvidenceClaim.ArtifactRole;
import breadcrumbs.domain.EvidenceClaim.ReviewDisposition;
import breadcrumbs.domain.ProjectProfile;
import breadcrumbs.domain.WorkspaceState;
import breadcrumbs.inspector.ScanPolicy;
import breadcrumbs.inspector.SnapshotEntry;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.util.Locale;

/** Thin interaction layer. A preview or partial review cannot mutate the active workspace. */
final class BootstrapConsole {
    @FunctionalInterface
    interface Prompt {
        String read(String prompt) throws IOException;
    }

    private final Prompt input;
    private final PrintStream output;
    private final WorkspaceState workspaceState;
    private final BootstrapService service;
    private final BaselineRenderer renderer;
    private final TerminalTheme theme;
    private final int width;

    BootstrapConsole(Prompt input, PrintStream output, WorkspaceState workspaceState) {
        this(input, output, workspaceState, new BootstrapService(ScanPolicy.defaults()), false);
    }

    BootstrapConsole(Prompt input, PrintStream output, WorkspaceState workspaceState,
                     BootstrapService service, boolean projectShell) {
        this(input, output, workspaceState, service, projectShell, new TerminalTheme(false), 80);
    }
    BootstrapConsole(Prompt input, PrintStream output, WorkspaceState workspaceState,
                     BootstrapService service, boolean projectShell, TerminalTheme theme, int width) {
        this.input = input;
        this.output = output;
        this.workspaceState = workspaceState;
        this.service = service;
        renderer = new BaselineRenderer(output, projectShell);
        this.theme = theme; this.width = width;
    }

    void bootstrap() throws IOException {
        output.println("\nADD OR CREATE PROJECT");
        output.println("[1] Start a new empty project");
        output.println("[2] Import an existing folder");
        output.println("[3] Import existing notes / handoff context");
        output.println("[0] Back. /cancel at any prompt discards the proposal.");
        String choice = input.read("Entry mode: ");
        if (choice.equals("0")) {
            return;
        }
        EntryMode mode = switch (choice) {
            case "1" -> EntryMode.NEW_EMPTY;
            case "2" -> EntryMode.IMPORT_FOLDER;
            case "3" -> EntryMode.IMPORT_LEGACY_CONTEXT;
            default -> throw new IllegalArgumentException("Choose entry mode 0-3.");
        };
        String name = input.read("Project display name: ");
        String selectedProfile = input.read("Profile [GENERAL, ACADEMIC, OPERATIONS, SOFTWARE; blank = GENERAL]: ");
        ProjectProfile profile;
        try {
            profile = selectedProfile.isEmpty() ? ProjectProfile.GENERAL
                    : ProjectProfile.valueOf(selectedProfile.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException invalid) {
            throw new IllegalArgumentException("Choose one of the four listed project profiles.");
        }
        CurrentContext context = CurrentContext.unknown();
        if (mode == EntryMode.NEW_EMPTY) {
            context = new CurrentContext(input.read("Objective (blank = UNKNOWN): "),
                    input.read("Intended deliverable (blank = UNKNOWN): "),
                    input.read("Current next action (blank = UNKNOWN): "),
                    input.read("Optional owner (blank = UNKNOWN): "),
                    input.read("Optional blockers (blank = UNKNOWN; enter none only if stated): "));
        }
        String selectedPath = input.read(mode == EntryMode.NEW_EMPTY
                ? "Optional existing EMPTY directory (blank = no directory observation): "
                : "Existing local project directory: ");
        Path root = selectedPath.isEmpty() ? null : Path.of(selectedPath);
        boolean scanApproved = false;
        if (root != null) {
            output.println("READ-ONLY inspection. Imported files are never executed or written.");
            output.println("Links/reparse points, sensitive names and generated folders are skipped.");
            output.println("TXT/Markdown/CSV configured fields and bounded DOCX main paragraphs. Other formats receive metadata/fingerprints, not content understanding.");
            output.println("Limits: " + ScanPolicy.defaults());
            output.println("Use a quiescent LOCAL folder. Portable JDK checks are not an OS sandbox against hostile filesystem races.");
            if (!input.read("Approve bounded read-only scan? Type yes: ").equalsIgnoreCase("yes")) {
                output.println("Cancelled before scanning. Active workspace unchanged.");
                return;
            }
            scanApproved = true;
        }
        BootstrapDraft draft;
        if (root == null) {
            draft = service.prepare(mode, name, profile, null, context, scanApproved, visited -> true);
        } else try (var progress = new OperationFeedback(output, theme, width, OperationFeedback.Operation.INSPECT)) {
          try {
            draft = service.prepare(mode, name, profile, root, context, scanApproved, visited -> {
                progress.running(visited);
                // Cancellation occurs at a file boundary, never halfway through committing a baseline.
                if (visited > 0 && visited % 25 == 0) {
                    try {
                        progress.pause();
                        try { return input.read("Inspected " + visited + " entries. Enter continues; /cancel aborts: ").isEmpty(); }
                        finally { progress.resume(); }
                    } catch (IOException failure) {
                        throw new UncheckedIOException(failure);
                    }
                }
                return true;
            });
            progress.complete(!draft.scan().complete(), draft.scan().entries().length + " entries inventoried; " + draft.claims().length + " captured claims. Review coverage.");
          } catch (IOException | RuntimeException failure) { progress.failed("Inspection failed or cancelled. Existing active workspace retained."); throw failure; }
        } catch (UncheckedIOException failure) {
            throw failure.getCause();
        } catch (IOException inaccessible) {
            output.println("Directory inspection failed. Check the local path and access permissions.");
            return;
        }
        if (mode == EntryMode.IMPORT_FOLDER && hasLegacyArtifacts(draft)) {
            output.println("Possible state/handoff/instruction artifacts were found. Their presence proves no progress.");
            if (input.read("Select legacy-context review mode? Type yes, or Enter to keep IMPORT_FOLDER: ")
                    .equalsIgnoreCase("yes")) {
                draft = new BootstrapDraft(draft.workspaceId(), draft.displayName(), draft.profile(),
                        EntryMode.IMPORT_LEGACY_CONTEXT, draft.scan(), draft.claims());
            }
        }
        preview(draft);
        if (mode != EntryMode.NEW_EMPTY) {
            draft = review(draft);
            preview(draft);
        }
        if (workspaceState.activeBaseline() != null) {
            output.println("Confirmation replaces the active session workspace; no persistence is available.");
        }
        boolean approved = input.read("Establish B0 from this reviewed proposal? Type yes: ").equalsIgnoreCase("yes");
        if (service.confirm(workspaceState, draft, approved)) {
            output.println("Baseline B0 established. No native checkpoint was created.");
            renderer.baseline(workspaceState.activeBaseline(), workspaceState.checkpoints().size());
        } else {
            output.println("Cancelled. Proposal discarded; active workspace unchanged.");
        }
    }

    void showBaseline() throws IOException {
        BaselineB0 baseline = workspaceState.activeBaseline();
        if (baseline == null) {
            output.println("No confirmed B0. Use option 13 to add or create a project.");
            return;
        }
        renderer.baseline(baseline, workspaceState.checkpoints().size());
        if (!input.read("Show paged file inventory and source claims? Type yes, or Enter to return: ")
                .equalsIgnoreCase("yes")) {
            return;
        }
        int shown = 0;
        for (SnapshotEntry entry : baseline.snapshot().entries()) {
            renderer.entry(entry);
            if (++shown % 10 == 0 && !input.read("Enter for next inventory page; any text returns: ").isEmpty()) {
                return;
            }
        }
        shown = 0;
        for (EvidenceClaim claim : baseline.claims()) {
            renderer.claim(claim);
            if (++shown % 5 == 0 && !input.read("Enter for next claims page; any text returns: ").isEmpty()) {
                return;
            }
        }
    }

    private BootstrapDraft review(BootstrapDraft proposal) throws IOException {
        BootstrapDraft reviewed = proposal;
        for (EvidenceClaim candidate : proposal.claims()) {
            renderer.claim(candidate);
            String choice;
            do {
                choice = input.read("Review [c = confirm present assessment, r = reject, u/Enter = unknown, s = defer remaining]: ")
                        .toLowerCase(Locale.ROOT);
            } while (!choice.isEmpty() && !choice.equals("c") && !choice.equals("r")
                    && !choice.equals("u") && !choice.equals("s"));
            if (choice.equals("s")) {
                break;
            }
            reviewed = reviewed.review(candidate.evidenceId(), switch (choice) {
                case "c" -> ReviewDisposition.CONFIRMED;
                case "r" -> ReviewDisposition.REJECTED;
                default -> ReviewDisposition.LEFT_UNKNOWN;
            });
        }
        return reviewed;
    }

    private void preview(BootstrapDraft draft) {
        renderer.summary(draft.workspaceId(), draft.displayName(), draft.profile(), draft.entryMode(),
                draft.scan(), draft.claims(), draft.conflicts(), false, 0);
        SnapshotEntry[] entries = draft.scan().entries();
        for (int index = 0; index < Math.min(entries.length, 10); index++) {
            renderer.entry(entries[index]);
        }
        if (entries.length > 10) {
            output.println((entries.length - 10) + " more inventory entries; option 14 offers paged details after confirmation.");
        }
    }

    private boolean hasLegacyArtifacts(BootstrapDraft draft) {
        for (SnapshotEntry entry : draft.scan().entries()) {
            if (entry.artifactRole() != ArtifactRole.UNKNOWN) {
                return true;
            }
        }
        return false;
    }
}
