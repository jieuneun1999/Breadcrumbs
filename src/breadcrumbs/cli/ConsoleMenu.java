package breadcrumbs.cli;

import breadcrumbs.bootstrap.BootstrapService;
import breadcrumbs.demo.DemoData;
import breadcrumbs.domain.Checkpoint;
import breadcrumbs.domain.Checkpoint.Origin;
import breadcrumbs.domain.Checkpoint.Status;
import breadcrumbs.domain.WorkspaceState;
import breadcrumbs.dsa.dll.CheckpointList;
import breadcrumbs.inspector.ScanPolicy;
import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.time.Instant;
import java.util.Locale;

public final class ConsoleMenu {
    private final BufferedReader input;
    private final PrintStream output;
    // This classroom sandbox is not the active project's source-linked checkpoint history.
    private final CheckpointList list = new CheckpointList();
    private final BootstrapConsole bootstrapConsole;
    private final TerminalTheme theme;
    private final TerminalOutput ui;
    private int nextNativeId = 1;
    private final String classroomGroup;
    private final int width;

    /** Uses the real DLL in an isolated, preloaded classroom session; no storage is opened. */
    public static ConsoleMenu classroom(Reader input, PrintStream output, TerminalTheme theme, int width, String group) {
        if (group == null || group.isBlank() || group.length() > 60
                || group.chars().anyMatch(c -> Character.isISOControl(c) || Character.getType(c) == Character.FORMAT)) {
            throw new IllegalArgumentException("Group name must contain 1-60 printable characters.");
        }
        return new ConsoleMenu(input, output, theme, width, group.strip());
    }

    public ConsoleMenu(Reader input, PrintStream output) {
        this(input, output, new TerminalTheme(false), 80);
    }
    public ConsoleMenu(Reader input, PrintStream output, TerminalTheme theme, int width) {
        this(input, output, theme, width, null);
    }
    private ConsoleMenu(Reader input, PrintStream output, TerminalTheme theme, int width, String classroomGroup) {
        this.classroomGroup = classroomGroup;
        this.width = width;
        this.input = input instanceof BufferedReader buffered ? buffered : new BufferedReader(input);
        this.output = output;
        this.theme = theme;
        ui = new TerminalOutput(output, theme, width);
        bootstrapConsole = classroomGroup == null
                ? new BootstrapConsole(this::read, output, new WorkspaceState(),
                        new BootstrapService(ScanPolicy.defaults()), false, theme, width)
                : null;
    }

    public void run() throws IOException {
        if (classroomGroup != null) {
            DemoData.load(list);
            if (theme.colorEnabled() && width >= 160) { BrandRenderer.renderApproved(output, theme, width); }
            output.println("Five SYNTHETIC demo records loaded before the first menu. No real project storage is used.");
            output.println("Primary structure: custom Doubly Linked List. Records are discarded on exit.");
        } else {
            output.println("B1 session-local DSA foundation. Checkpoints are not saved between runs.");
            output.println("B2 bootstrap is available in option 13. Use the project shell for B3 persistence/continuity.");
            output.println("Options 1-12 are the B1 checkpoint sandbox, separate from the active project's B0/history.");
            output.println("DEMO records are SYNTHETIC, not evidence of Breadcrumbs project progress.");
        }
        try {
            boolean running = true;
            while (running) {
                ui.view(ScreenId.ADVANCED_TOOLS);
                menu();
                String operation = "Menu input";
                try {
                    String choice = read("Choose an option: ");
                    if (classroomGroup != null && (choice.equals("13") || choice.equals("14"))) {
                        throw new IllegalArgumentException("Choose a listed DLL operation (0-12).");
                    }
                    operation = operationName(choice);
                    switch (choice) {
                        case "0" -> running = false;
                        case "1" -> create(false, false);
                        case "2" -> create(true, false);
                        case "3" -> create(true, true);
                        case "4" -> {
                            Checkpoint checkpoint = list.findById(read("Checkpoint ID: "));
                            show(checkpoint);
                            report(operation, checkpoint == null ? "Not found" : "Found "
                                    + checkpoint.checkpointId(), "O(n)", "Linear search by ID.");
                        }
                        case "5" -> delete();
                        case "6", "7" -> {
                            Checkpoint[] checkpoints = choice.equals("6")
                                    ? list.traverseForward() : list.traverseBackward();
                            if (checkpoints.length == 0) {
                                output.println("No checkpoints.");
                            }
                            for (Checkpoint checkpoint : checkpoints) {
                                show(checkpoint);
                            }
                            report(operation, "Success", "O(n)", "Visit nodes in link order; O(n) result array.");
                        }
                        case "8" -> {
                            Checkpoint checkpoint = list.getLatest();
                            show(checkpoint);
                            report(operation, checkpoint == null ? "No latest checkpoint" : "Success",
                                    "O(1)", "Read the tail; latest means list position, not maximum timestamp.");
                        }
                        case "9" -> report(operation, "Success", "O(n)", "Render each node once.");
                        case "10" -> report(operation, "Size: " + list.size() + "; empty: " + list.isEmpty(),
                                "O(1)", "Read stored size.");
                        case "11" -> report(operation, list.validateIntegrity()
                                ? "Integrity valid" : "Integrity FAILED", "O(n^2)",
                                "O(n) links/cycle/count checks; O(n^2) pairwise ID checks; O(1) extra space.");
                        case "12" -> {
                            int added = DemoData.load(list);
                            report(operation, "Loaded " + added + " SYNTHETIC checkpoints; skipped " + (5 - added),
                                    "O(n)", "Five fixed fixtures, each using linear duplicate checks.");
                        }
                        case "13" -> bootstrapConsole.bootstrap();
                        case "14" -> bootstrapConsole.showBaseline();
                        default -> throw new IllegalArgumentException(classroomGroup == null ? "Invalid menu option. Choose 0-14." : "Invalid menu option. Choose 0-12.");
                    }
                } catch (IllegalArgumentException invalid) {
                    if (operation.equals("Project bootstrap") || operation.equals("View project baseline")) {
                        // Bootstrap is not a DLL operation, so a rejected form must not claim DLL complexity.
                        output.println("Project operation rejected: " + invalid.getMessage());
                        output.println("Returned to the main menu.");
                    } else if (classroomGroup != null && operation.equals("Menu input")) {
                        report(operation, "Rejected: " + invalid.getMessage(), "O(1)", "Check menu choice without accessing the DLL.");
                    } else {
                        report(operation, "Rejected: " + invalid.getMessage(), "O(n) upper bound",
                                "Validation rejects the request before mutation; duplicate lookup can scan the list.");
                    }
                }
            }
        } catch (EOFException end) {
            output.println("End of input. Pending operation cancelled.");
        }
        output.println("Goodbye. Session-local records are discarded.");
    }

    private void create(boolean demo, boolean positional) throws IOException {
        int position = list.size();
        if (positional) {
            try {
                position = Integer.parseInt(read("Position (0-based, 0-" + list.size() + "): "));
            } catch (NumberFormatException invalid) {
                throw new IllegalArgumentException("Position must be between 0 and " + list.size() + ".");
            }
            if (position < 0 || position > list.size()) {
                throw new IllegalArgumentException("Position must be between 0 and " + list.size() + ".");
            }
        }
        if (!demo && nextNativeId > 999999) {
            throw new IllegalArgumentException("Native ID range exhausted for this session.");
        }
        String id = demo ? read("Synthetic checkpoint ID (DEMO-*): ")
                : String.format(Locale.ROOT, classroomGroup == null ? "CP-%06d" : "DEMO-NEW-%06d", nextNativeId);
        if (classroomGroup != null && demo && id.startsWith("DEMO-NEW-")) {
            throw new IllegalArgumentException("DEMO-NEW- is reserved for automatically numbered tail inserts.");
        }
        String title = read("Title (required, max 120): ");
        if (title.isBlank()) {
            throw new IllegalArgumentException("Title must not be blank.");
        }
        String summary = read("Summary (blank = UNKNOWN, max 600): ");
        String statusInput = read("Status [PLANNED, IN_PROGRESS, COMPLETED, BLOCKED, UNKNOWN]: ");
        Status status;
        try {
            status = Status.valueOf(statusInput.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException invalid) {
            throw new IllegalArgumentException("Invalid status. Use one of the listed values.");
        }
        Checkpoint checkpoint = new Checkpoint(id, title, summary, status, Instant.now(),
                demo || classroomGroup != null ? Origin.DEMO : Origin.USER_CONFIRMED);
        output.println("Preview. User statements only; no file evidence or baseline is attached.");
        show(checkpoint);
        String operation = positional ? "Insert at position" : demo ? "Prepend checkpoint" : "Append checkpoint";
        if (!read("Confirm creation? Type yes (anything else cancels): ").equalsIgnoreCase("yes")) {
            report(operation, "Cancelled", "O(1)", "No mutation.");
            return;
        }
        if (positional) {
            list.insertAt(position, checkpoint);
        } else if (demo) {
            list.prepend(checkpoint);
        } else {
            list.append(checkpoint);
            nextNativeId++;
        }
        report(operation, "Success: " + id, "O(n)",
                "Linear duplicate check; O(1) pointer insertion once located. insertAt also locates position.");
    }

    private void delete() throws IOException {
        String id = read("Checkpoint ID to delete: ");
        if (list.findById(id) == null) {
            report("Delete by ID", "Not found", "O(n)", "Linear search; no mutation.");
            return;
        }
        if (!read("Remove from this session? Type yes (anything else cancels): ").equalsIgnoreCase("yes")) {
            report("Delete by ID", "Cancelled", "O(n)", "Located record; no mutation.");
            return;
        }
        list.removeById(id);
        report("Delete by ID", "Deleted " + id, "O(n)", "Linear search plus O(1) pointer unlink.");
    }

    private void show(Checkpoint checkpoint) {
        if (checkpoint == null) {
            return;
        }
        output.println(checkpoint.checkpointId() + " | " + checkpoint.title());
        output.println("  " + checkpoint.status() + " | " + checkpoint.createdAt() + " | "
                + checkpoint.origin() + (checkpoint.origin() == Origin.DEMO ? " (SYNTHETIC)" : ""));
        output.println("  " + checkpoint.summary());
    }

    private void report(String operation, String result, String complexity, String explanation) {
        if (classroomGroup != null) { output.println(); }
        ui.line("Operation: ", operation);
        ui.line("Result: ", result);
        if (classroomGroup == null) { output.println("DLL structure: " + list.displayStructure()); }
        else if (!list.validateIntegrity()) { output.println("Integrity FAILED. Structure is not displayed as valid."); }
        else {
            String head = list.getOldest() == null ? "NULL" : list.getOldest().checkpointId();
            String tail = list.getLatest() == null ? "NULL" : list.getLatest().checkpointId();
            output.println("HEAD=" + head + " | TAIL=" + tail + " | previous <- -> next");
            output.println("DLL structure: " + list.displayStructure());
        }
        output.println("Time complexity: " + complexity);
        if (classroomGroup != null) {
            String space = switch (operation) {
                case "Traverse forward", "Traverse backward" -> "O(n) returned array";
                case "Display structure" -> "O(n) output";
                default -> "O(1) core operation";
            };
            output.println("Auxiliary space: " + space);
        }
        output.println("Explanation: " + explanation);
        output.println("Structure diagnostic: O(n) time and O(n) output space, additional to the core operation.");
        if (classroomGroup != null) { output.println("Classroom safety diagnostic also validates links/uniqueness: O(n^2) time, O(1) auxiliary space."); }
    }

    private String read(String prompt) throws IOException {
        output.print(theme.label(TerminalTheme.Tone.BREAD_GOLD, prompt));
        output.flush();
        if (theme.companion() != null) {
            String value = theme.companion().readLine(input, theme, prompt, 1024).strip();
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                if (Character.isISOControl(c) || Character.getType(c) == Character.FORMAT) { throw new IllegalArgumentException("Input must not contain control characters."); }
            }
            if (value.equalsIgnoreCase("/cancel")) { throw new IllegalArgumentException("Cancelled. Returned to menu without mutation."); }
            return value;
        }
        StringBuilder line = new StringBuilder();
        boolean overflow = false;
        int character;
        while ((character = input.read()) != -1 && character != '\n' && character != '\r') {
            if (line.length() < 1024) {
                line.append((char) character);
            } else {
                overflow = true;
            }
        }
        if (character == '\r') {
            input.mark(1);
            if (input.read() != '\n') {
                input.reset();
            }
        }
        if (character == -1 && line.length() == 0 && !overflow) {
            throw new EOFException();
        }
        if (overflow) {
            throw new IllegalArgumentException("Input exceeds 1024 characters.");
        }
        for (int i = 0; i < line.length(); i++) {
            char value = line.charAt(i);
            if (Character.isISOControl(value) || Character.getType(value) == Character.FORMAT) {
                throw new IllegalArgumentException("Input must not contain control characters.");
            }
        }
        String value = line.toString().strip();
        if (value.equalsIgnoreCase("/cancel")) {
            throw new IllegalArgumentException("Cancelled. Returned to menu without mutation.");
        }
        return value;
    }

    private String operationName(String choice) {
        return switch (choice) {
            case "1" -> "Append checkpoint";
            case "2" -> "Prepend checkpoint";
            case "3" -> "Insert at position";
            case "4" -> "Search by ID";
            case "5" -> "Delete by ID";
            case "6" -> "Traverse forward";
            case "7" -> "Traverse backward";
            case "8" -> "View latest checkpoint";
            case "9" -> "Display structure";
            case "10" -> "Inspect list size";
            case "11" -> "Validate integrity";
            case "12" -> "Load DEMO checkpoints";
            case "13" -> "Project bootstrap";
            case "14" -> "View project baseline";
            default -> "Menu input";
        };
    }

    private void menu() {
        if (classroomGroup != null) {
            output.println();
            ui.heading("BREADCRUMBS | " + classroomGroup + " | DOUBLY LINKED LIST");
            output.println("We are the bread and we ain't letting no crumbs");
            output.println("SYNTHETIC classroom session | Records: " + list.size());
            String actions = "[1] Append at tail\n[2] Prepend at head\n[3] Insert at position\n[4] Search by ID\n[5] Delete by ID\n[6] Traverse forward\n[7] Traverse backward\n[8] Get latest (tail)\n[9] Display structure\n[10] Count records\n[11] Validate integrity\n[12] Reload missing demo records\n[0] Exit";
            if (theme.companion() != null && theme.companion().boxedMenus()) {
                int menuWidth = Math.min(theme.companion().columns(), 80);
                MenuBox.rows(theme, menuWidth, "", actions, true).forEach(output::println);
            } else {
                ui.choices(actions);
            }
            output.println("/cancel returns without mutation. Confirmations require yes.");
            return;
        }
        output.println("\n========================================");
        ui.heading("              BREADCRUMBS");
        output.println("       Project Checkpoint Management");
        output.println("========================================");
        ui.menu("[1] Create / append session-only CP (B1 sandbox)\n[2] Prepend SYNTHETIC checkpoint\n[3] Insert SYNTHETIC checkpoint at position\n[4] Search checkpoint by ID\n[5] Delete checkpoint by ID\n[6] Traverse forward\n[7] Traverse backward\n[8] View latest checkpoint (tail)\n[9] Display DLL structure\n[10] View list size\n[11] Validate DLL integrity\n[12] Load five SYNTHETIC DEMO checkpoints\n[13] Add or create a project / establish B0\n[14] View B0 and paged inventory / claims\n[0] Exit");
        output.println("Enter /cancel at any prompt to return here. Confirmations accept only yes.");
    }
}
