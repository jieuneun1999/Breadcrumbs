package breadcrumbs.cli;

import breadcrumbs.bootstrap.BootstrapService;
import breadcrumbs.checkpoint.ProjectCheckpointService;
import breadcrumbs.domain.*;
import breadcrumbs.inspector.ContentSafety;
import breadcrumbs.inspector.ScanPolicy;
import breadcrumbs.storage.WorkspaceStore;
import java.io.*;
import java.util.Locale;
import java.util.UUID;

/** Owns navigation and persistence consent. All project chronology stays in WorkspaceState. */
public final class ProjectConsole {
    private final BufferedReader input;
    private final PrintStream output;
    private final WorkspaceStore store;
    private final TerminalTheme theme;
    private final int width;
    private final StartupRenderer startup;
    private final TerminalOutput ui;
    private final WorkspaceState state = new WorkspaceState();
    private breadcrumbs.search.ContextSearchIndex search = new breadcrumbs.search.ContextSearchIndex(state);
    private final ProjectCheckpointService checkpoints = new ProjectCheckpointService();
    private boolean persistence;
    private boolean storageReady;

    public ProjectConsole(Reader input, PrintStream output, WorkspaceStore store, TerminalTheme theme, int width) {
        this.input = input instanceof BufferedReader buffered ? buffered : new BufferedReader(input);
        this.output = output; this.store = store; this.theme = theme; this.width = width;
        startup = new StartupRenderer(output, theme, width);
        ui = new TerminalOutput(output, theme, width);
    }

    public void run() throws IOException {
        BrandRenderer.renderApproved(output, theme, width);
        refresh();
        try {
            boolean running = true;
            boolean showMenu = true;
            while (running) {
                if (showMenu) {
                    startup.menu(); ui.menu("[5] Inspect / recover saved generations\n[6] Continue active project in memory");
                    showMenu = false;
                } else { ui.note("\n1=open  2=create  7=daily  ?=menu  0=exit"); }
                try {
                    switch (read("breadcrumbs > ")) {
                        case "0" -> running = !leave();
                        case "?" -> showMenu = true;
                        case "1" -> open();
                        case "2" -> create();
                        case "3" -> project();
                        case "4" -> new ConsoleMenu(input, output, theme, width).run();
                        case "5" -> {
                            WorkspaceState recovered = new RecoveryConsole(this::read, output, store, theme, width).run(this::leave);
                            if (recovered != null) { state.replaceWith(recovered); search = new breadcrumbs.search.ContextSearchIndex(state); persistence = true; status(); }
                        }
                        case "6" -> {
                            persistence = false;
                            ui.plain("Explicit in-memory mode. Active context retained; unsaved changes will not survive exit. Choose Create/import if no project is active.");
                        }
                        case "7" -> daily();
                        default -> ui.plain("Choose 0-4 for project tools, 5 for recovery, 6 for in-memory operation, or 7 for daily workflows.");
                    }
                } catch (IllegalArgumentException invalid) {
                    ui.error("Operation cancelled or rejected."); ui.plain(safe(invalid.getMessage()));
                } catch (IOException failure) {
                    if (failure instanceof EOFException) { throw failure; }
                    ui.error("Storage/input operation failed.");
                    ui.note("Active project preserved. Check path, format and permissions; retry Save.");
                }
            }
        } catch (EOFException end) {
            ui.plain("End of input. Unconfirmed proposals cancelled.");
            if (state.dirty() && persistence) { save(); }
            if (state.dirty()) { ui.plain("WARNING: unsaved session state will be discarded."); }
        }
        ui.plain("Goodbye.");
    }

    private void refresh() {
        try {
            WorkspaceStore.SavedWorkspace[] saved = store.list(); storageReady = true;
            int unavailable = 0;
            for (var item : saved) {
                if (!item.problem().isEmpty()) { unavailable++; }
            }
            startup.ready(saved.length, unavailable);
        } catch (IOException | IllegalArgumentException unavailable) {
            storageReady = false;
            startup.unavailable();
        }
    }

    private void create() throws IOException {
        if (!leave()) { return; }
        WorkspaceState candidate = new WorkspaceState();
        BootstrapService bootstrap = new BootstrapService(ScanPolicy.defaults(),
                () -> "W-" + UUID.randomUUID().toString().replace("-", ""));
        new BootstrapConsole(this::read, output, candidate, bootstrap, true, theme, width).bootstrap();
        if (candidate.activeBaseline() == null) { return; }
        boolean consent = read("Save this workspace to local app storage? Type yes; Enter keeps session-only: ").equalsIgnoreCase("yes");
        state.replaceWith(candidate); search = new breadcrumbs.search.ContextSearchIndex(state); persistence = consent;
        if (persistence) { save(); }
        status();
    }

    private void open() throws IOException {
        WorkspaceStore.SavedWorkspace[] saved = store.list();
        if (saved.length == 0) { ui.plain("No saved workspaces. Choose Create or import."); return; }
        for (int i = 0; i < saved.length; i++) {
            ui.plain("[" + (i + 1) + "] " + safe(saved[i].name()) + " | " + safe(saved[i].id())
                    + (saved[i].problem().isEmpty() ? "" : " | UNAVAILABLE"));
        }
        String choice = read("Project number (0 = cancel): ");
        if (choice.equals("0")) { return; }
        int index;
        try { index = Integer.parseInt(choice) - 1; }
        catch (NumberFormatException bad) { throw new IllegalArgumentException("Choose a listed project number."); }
        if (index < 0 || index >= saved.length) { throw new IllegalArgumentException("Choose a listed project number."); }
        // Load and validate before even asking to leave the current project.
        store.load(saved[index].id());
        if (!leave()) { return; }
        // Leaving may save this same workspace. Reload that newly saved state, not the earlier preview.
        WorkspaceState loaded = OperationFeedback.run(output, theme, width, OperationFeedback.Operation.LOAD, () -> store.load(saved[index].id()));
        state.replaceWith(loaded); search = new breadcrumbs.search.ContextSearchIndex(state); persistence = true;
        ui.plain("Opened saved workspace. No source rescan. Freshness UNKNOWN for live sources; saved observations can be compared.");
        status();
        new TerminalOutput(output, theme, width).plain(new breadcrumbs.continuity.ContinuityService().brief(state, persistence));
        ui.plain("Choose 7 for the daily workspace, or 3 for the original advanced project tools.");
    }

    private void daily() throws IOException {
        new DailyConsole(this::read, output, state, persistence, () -> { if (persistence) { save(); } },
                this::checkpoint, this::project, theme, width, store, search).run();
    }

    private void project() throws IOException {
        if (state.activeBaseline() == null) { ui.plain("No active workspace. Create/import or open one."); return; }
        boolean back = false;
        while (!back) {
            status();
            ui.heading("ADVANCED PROJECT TOOLS");
            ui.menu("[16] Work items / explicit dependencies\n[17] Hash demonstration / canonical indexes\n[18] Search project context");
            ui.menu("[1] View B0 / sources\n[2] Create confirmed checkpoint\n[3] Latest checkpoint\n[4] History (forward and backward)\n[5] Save project\n[6] Switch project\n[7] Project information\n[8] DLL diagnostics\n[9] Context audit\n[10] Guided present-context review\n[11] Consent and rescan sources\n[12] Compare saved observations\n[13] Relevant checkpoint retrieval\n[14] Resume / handoff preview and export\n[15] Coordination applicability\n[0] Back");
            try {
                String choice = read("project > ");
                switch (choice) {
                    case "0" -> back = true;
                    case "1" -> new BootstrapConsole(this::read, output, state,
                            new BootstrapService(ScanPolicy.defaults()), true, theme, width).showBaseline();
                    case "2" -> checkpoint();
                    case "3" -> show(state.checkpoints().getLatest());
                    case "4" -> {
                        ui.plain("History forward:");
                        for (Checkpoint c : state.checkpoints().traverseForward()) { show(c); }
                        ui.plain("History backward:");
                        for (Checkpoint c : state.checkpoints().traverseBackward()) { ui.plain(c.checkpointId()); }
                    }
                    case "5" -> {
                        if (persistence || read("Enable local persistence? Type yes: ").equalsIgnoreCase("yes")) {
                            persistence = true; save();
                        }
                    }
                    case "6" -> { open(); back = true; }
                    case "7" -> {
                        ui.plain("App storage: " + safe(store.root().toString()));
                        ui.plain("Profile: " + state.activeBaseline().profile() + " | Mode: " + state.activeBaseline().entryMode());
                        ui.plain("Next native number: " + state.nextCheckpointNumber());
                        ui.plain("Live source freshness UNKNOWN until a consented scan. B0 is a saved observation, never a native checkpoint.");
                        ui.plain("Current saved observation: " + state.currentObservationId() + "; later observations: " + state.laterObservations().length);
                        ui.plain("Use Context audit / Resume for checkpoint-specific saved comparison results. No live rescan occurs here.");
                    }
                    case "8" -> diagnostics();
                    case "16" -> new DependencyConsole(this::read, output, state, theme, width).run();
                    case "17" -> new HashConsole(this::read, output, state, theme, width).run();
                    case "18" -> new SearchConsole(this::read, output, search, width, theme).run();
                    case "9", "10", "11", "12", "13", "14", "15" -> {
                        new ContextConsole(this::read, output, state, store.root(), theme, width).perform(choice);
                    }
                    default -> ui.plain("Choose 0-18.");
                }
            } catch (IllegalArgumentException invalid) { ui.plain("Operation cancelled or rejected. " + safe(invalid.getMessage())); }
        }
    }

    private void checkpoint() throws IOException {
        ui.plain("Human present-context statements only. Blank optional fields stay UNKNOWN. /cancel returns.");
        String title = read("Checkpoint title: "), progress = read("Progress summary: "), objective = read("Objective: ");
        String blocker = read("Blockers [UNKNOWN, NONE_REPORTED, BLOCKED]: ");
        ProjectContext.BlockerState blockerState;
        try { blockerState = blocker.isEmpty() ? ProjectContext.BlockerState.UNKNOWN
                : ProjectContext.BlockerState.valueOf(blocker.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException bad) { throw new IllegalArgumentException("Choose a listed blocker state."); }
        String details = read("Blocker details: "), next = read("Next actions: "), decisions = read("Decisions / rationale: "), owner = read("Owner: ");
        ui.plain("Available current observation evidence (source-reported claims are not independent verification):");
        for (EvidenceClaim c : state.claimsAt(state.currentObservationId())) {
            ui.plain(c.evidenceId() + " | " + c.field() + " | " + c.sourceKind() + " | " + c.review());
        }
        String selected = read("Evidence IDs separated by commas (blank = none): ");
        String[] refs = selected.isBlank() ? new String[0] : selected.split(",", -1);
        for (int i = 0; i < refs.length; i++) { refs[i] = refs[i].strip(); }
        Checkpoint preview = checkpoints.preview(state, title, progress, objective, blockerState, details, next, decisions, owner, refs);
        ui.plain("CHECKPOINT PREVIEW. No mutation yet."); show(preview);
        boolean approved = read("Create this native checkpoint? Type yes: ").equalsIgnoreCase("yes");
        if (checkpoints.confirm(state, preview, approved)) {
            ui.plain("Confirmed " + preview.checkpointId()); diagnostics();
            if (persistence) { save(); }
        } else { ui.plain("Cancelled. Project history unchanged."); }
    }

    private boolean save() {
        try (var feedback = new OperationFeedback(output, theme, width, OperationFeedback.Operation.SAVE)) {
            try {
                store.save(state); storageReady = true;
                feedback.complete(false, "Saved. Verified local file readback.");
            } catch (IOException | IllegalArgumentException failure) {
                feedback.failed("NOT SAVED. In-memory state retained. Check storage path/permissions and integrity, then retry Save.");
                return false;
            }
        }
        if (state.manualContext().artifactsAllowed()) {
            try {
                var service = new breadcrumbs.storage.ArtifactService(); var checkpoints = state.checkpoints().traverseForward();
                String[] ids = new String[checkpoints.length]; for (int i = 0; i < ids.length; i++) { ids[i] = checkpoints[i].checkpointId(); }
                var bundle = service.preview(state, ids);
                OperationFeedback.run(output, theme, width, OperationFeedback.Operation.EXPORT, () -> service.materialize(state, store, bundle));
                ui.success("Continuity documents generated.");
            } catch (IOException | IllegalArgumentException exportFailure) {
                ui.warning(state.checkpoints().isEmpty()
                        ? "Context saved. Continuity export failed." : "Checkpoint saved. Continuity export failed.");
                ui.plain("Canonical history remains saved. Previous views retained; retry document generation.");
                ui.plain("Export diagnostic: " + safe(exportFailure.getMessage()));
            }
        }
        return true;
    }

    private boolean leave() throws IOException {
        if (!state.dirty()) { return true; }
        String action = read("Unsaved workspace. [s] save, [d] discard on leaving, Enter cancels: ");
        if (action.equalsIgnoreCase("d")) { return true; }
        if (action.equalsIgnoreCase("s")) { persistence = true; return save(); }
        ui.plain("Cancelled. Active workspace unchanged."); return false;
    }

    private void status() {
        if (state.activeBaseline() == null) { return; }
        ui.plain("\nACTIVE PROJECT | " + safe(state.activeBaseline().displayName()) + " | " + state.activeBaseline().workspaceId());
        ui.line("Storage status: ", !persistence ? "SESSION-ONLY / UNSAVED" : state.dirty() ? "UNSAVED / retry Save" : "SAVED");
        if (!storageReady) { ui.plain("Storage unavailable at startup; retry Save if access is restored."); }
    }
    private void diagnostics() {
        ui.line("DLL structure: ", state.checkpoints().displayStructure());
        ui.line("Integrity: ", Boolean.toString(state.checkpoints().validateIntegrity()));
        ui.plain("Core append O(n) including duplicate check; latest O(1); traversal O(n).");
        ui.plain("Structure diagnostic O(n); full integrity O(n^2) including uniqueness.");
    }
    private void show(Checkpoint c) {
        if (c == null) {
            ui.plain("No native checkpoint. Review B0 for baseline context, not historical resume."); return;
        }
        ProjectContext p = c.project();
        ui.plain(c.checkpointId() + " | " + c.createdAt() + " | " + safe(c.title()));
        ui.plain("Workspace: " + p.workspaceId() + " | snapshot: " + safe(p.snapshotId()));
        ui.plain("Objective: " + safe(p.objective()) + "\nProgress: " + safe(c.summary()));
        ui.plain("Blocker state: " + p.blockerState() + " | " + safe(p.blockers()));
        ui.plain("Next actions: " + safe(p.nextActions()) + "\nDecisions: " + safe(p.decisions()) + "\nOwner: " + safe(p.owner()));
        ui.plain("Origin: USER_CONFIRMED present assessment. Historical/source outcomes remain unverified.");
        for (String id : p.evidenceIds()) {
            for (EvidenceClaim claim : state.claimsAt(p.snapshotId())) {
                if (claim.evidenceId().equals(id)) { new BaselineRenderer(output, true, theme, width).claim(claim); }
            }
        }
        ui.plain("Unknown/unresolved B0 claims remain available in View B0 / sources. Freshness UNKNOWN without a later comparison.");
        ui.plain("Use Resume / handoff for referenced freshness against the latest saved observation.");
    }
    private String read(String prompt) throws IOException {
        output.print(theme.label(TerminalTheme.Tone.BREAD_GOLD, prompt)); output.flush();
        // Bound reads before building a String, including redirected and malformed input.
        StringBuilder line = new StringBuilder(); int c;
        while ((c = input.read()) != -1 && c != '\n') {
            if (line.length() >= 8192) {
                while (c != -1 && c != '\n') { c = input.read(); }
                throw new IllegalArgumentException("Input exceeds 8192 characters.");
            }
            if (c != '\r') { line.append((char) c); }
        }
        if (c == -1 && line.isEmpty()) { throw new EOFException(); }
        String value = line.toString().strip();
        if (value.equalsIgnoreCase("/cancel")) { throw new IllegalArgumentException("Cancelled."); }
        return value;
    }
    private static String safe(String value) { return ContentSafety.terminal(value, 4000); }
}
