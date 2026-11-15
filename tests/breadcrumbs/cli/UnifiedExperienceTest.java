package breadcrumbs.cli;

import breadcrumbs.storage.WorkspaceStore;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;

public final class UnifiedExperienceTest {
    private UnifiedExperienceTest() { }
    public static void main(String[] args) throws Exception { run(); }
    public static void run() throws Exception {
        opening(); generousHome(); fields(); navigation(); dockedReports(); narrowActions(); managedOperation(); completeFallback();
        System.out.println("PASS: unified Home, stable fields, navigation focus, managed work and complete fallback");
    }
    private static void opening() throws Exception {
        var bytes = new ByteArrayOutputStream(); var raw = new PrintStream(bytes, true, StandardCharsets.UTF_8);
        var keys = new Keys(160, 52, "0\n"); var screens = new TerminalScreenManager(raw, keys, false);
        Path root = Files.createTempDirectory(Files.createDirectories(Path.of("build")), "unified-home-");
        try (var bread = new BreadSupport(screens.output(), keys, 160, 52, true, false, screens)) {
            var theme = new TerminalTheme(true, true, false, bread);
            new ProjectConsole(new StringReader(""), screens.output(), new WorkspaceStore(root.resolve("app")), theme, 160).run();
        }
        String text = bytes.toString(StandardCharsets.UTF_8);
        check(count(text, "\u001b[2J") == 1, "Opening is one composed Home, never an erased splash followed by another screen");
        var surface = TerminalTestSurface.read(text, 160, 52);
        check(surface.line(49).contains("Breadcrumbs >"), "Input owns the fixed footer even in a tall viewport");
        String home = text.substring(text.lastIndexOf("\u001b[2J")).replaceAll("\u001b\\[[0-9;]*m", "");
        check(home.contains("Leave context behind. Pick up where work stopped.") && home.contains("Saved projects:"), "Brand identity and real availability remain in the Home composition");
        for (String line : RichBrandRenderer.frame(new TerminalTheme(true), false).lines().toList()) {
            check(home.contains(line.replaceAll("\u001b\\[[0-9;]*m", "").strip()), "Normal160x52 Home retains every complete B3 logo row");
        }
        for (int key = 0; key <= 8; key++) { check(home.contains("[" + key + "]"), "Every Home action remains visible"); }
        check(home.contains(BreadFrames.at(BreadFrames.Size.FULL, BreadFrames.Mood.WELCOME, 0).rows().get(8).strip()), "Approved bread shares the same Home composition");
    }
    private static void generousHome() throws Exception {
        var bytes = new ByteArrayOutputStream(); var keys = new Keys(200, 90, "0\n");
        var screens = new TerminalScreenManager(new PrintStream(bytes, true, StandardCharsets.UTF_8), keys, false);
        try (var bread = new BreadSupport(screens.output(), keys, 200, 90, true, false, screens)) {
            var theme = new TerminalTheme(true, false, false, bread); var ui = new TerminalOutput(screens.output(), theme, 200);
            ui.screen(BreadFrames.Mood.WELCOME, "Your context is ready.", "[1] Open project\n[0] Exit", true); ui.prompt("Breadcrumbs > ");
            bread.readLine(new StringReader(""), theme, "Breadcrumbs > ", 8192);
            String actual = bytes.toString(StandardCharsets.UTF_8).replaceAll("\u001b\\[[0-9;]*m", "");
            for (String line : RichBrandRenderer.frame(theme, false).lines().toList()) {
                check(actual.contains(line.replaceAll("\u001b\\[[0-9;]*m", "").strip()), "A sufficiently generous Home includes every complete approved B3 row");
            }
            check(count(actual, "\u001b[2J") == 1, "Full B3 identity remains inside the single Home composition");
        }
    }
    private static void fields() throws Exception {
        var bytes = new ByteArrayOutputStream(); var raw = new PrintStream(bytes, true, StandardCharsets.UTF_8);
        var keys = new Keys(120, 40, "Prepared the checklist\nConfirm volunteers\n");
        var screens = new TerminalScreenManager(raw, keys, true);
        try (var bread = new BreadSupport(screens.output(), keys, 120, 40, true, true, screens)) {
            var theme = new TerminalTheme(true, false, true, bread); var ui = new TerminalOutput(screens.output(), theme, 120);
            ui.view(ScreenId.PROGRESS_UPDATE); ui.heading("Record progress"); ui.prompt("Progress summary: ");
            check(bread.readLine(new StringReader(""), theme, "Progress summary: ", 8192).equals("Prepared the checklist"), "First field value remains exact");
            check(TerminalTestSurface.read(bytes.toString(StandardCharsets.UTF_8), 120, 40).cursorRow() == 28, "First field cursor stays inside the fixed footer");
            ui.prompt("Next action: ");
            check(bread.readLine(new StringReader(""), theme, "Next action: ", 8192).equals("Confirm volunteers"), "Next field value remains exact");
        }
        check(TerminalTestSurface.read(bytes.toString(StandardCharsets.UTF_8), 120, 40).line(28).contains("Next action: Confirm volunteers"), "A second field updates the same readable input row");
        check(count(bytes.toString(StandardCharsets.UTF_8), "\u001b[2J") == 1, "Ordinary fields do not cause another full-screen clear or title animation");
    }
    private static void navigation() throws Exception {
        var bytes = new ByteArrayOutputStream(); var theme = new TerminalTheme(true);
        try (var screens = new TerminalScreenManager(new PrintStream(bytes, true, StandardCharsets.UTF_8), new Keys(120, 40, ""), true)) {
            var out = screens.output(); screens.view(ScreenId.PROJECT_WORKSPACE, theme);
            screens.actions("[1] Summary\n[13] Map\n[0] Back"); out.print("[1] Summary\n[13] Map\n[0] Back\nWorkspace > "); screens.beginRead("Workspace > ");
            screens.move(1); screens.completeRead("13"); screens.endRead();
            screens.view(ScreenId.PROJECT_MAP, theme); out.print("Recorded project map\nMap > "); screens.beginRead("Map > "); screens.endRead();
            check(bytes.toString(StandardCharsets.UTF_8).contains("\u001b[2m"), "Meaningful navigation uses content-only dim/reveal");
            screens.view(ScreenId.PROJECT_WORKSPACE, theme); screens.actions("[1] Summary\n[13] Map\n[0] Back");
            out.print("[1] Summary\n[13] Map\n[0] Back\nWorkspace > "); screens.beginRead("Workspace > ");
            check(screens.selected().isEmpty(), "Remembered focus never silently approves on Enter");
            check(screens.move(1).equals("0"), "Returning to a view remembers the previous action focus"); screens.endRead();
            int before = count(bytes.toString(StandardCharsets.UTF_8), "\u001b[2m");
            screens.urgent(); screens.view(ScreenId.RECOVERY, theme); out.print("Save could not finish\nRecovery > "); screens.beginRead("Recovery > ");
            check(count(bytes.toString(StandardCharsets.UTF_8), "\u001b[2m") == before, "Urgent errors do not wait for a transition");
            var surface = TerminalTestSurface.read(bytes.toString(StandardCharsets.UTF_8), 120, 40);
            check(surface.line(1).contains("BREADCRUMBS / Restore project") && surface.line(30).contains("Recovery >"), "Screen identity changes inside the same shell and footer");
        }
    }
    private static void dockedReports() throws Exception {
        var bytes = new ByteArrayOutputStream(); var keys = new Keys(160, 52, "\n\n\n\n\n");
        var screens = new TerminalScreenManager(new PrintStream(bytes, true, StandardCharsets.UTF_8), keys, false);
        try (var bread = new BreadSupport(screens.output(), keys, 160, 52, true, false, screens)) {
            var theme = new TerminalTheme(true, false, false, bread); var ui = new TerminalOutput(screens.output(), theme, 160);
            ui.screen(BreadFrames.Mood.WELCOME, "Your project context is here.", "[0] Exit", true); ui.prompt("Breadcrumbs > "); screens.beginRead("Breadcrumbs > "); screens.endRead();
            ui.view(ScreenId.PROJECT_MAP);
            for (int i = 0; i < 80; i++) { ui.plain("Recorded context line " + i); }
            ui.menu("[1] Review record\n[0] Back"); ui.prompt("Map > ");
            int[] pages = {0};
            String signature = BreadFrames.at(BreadFrames.Size.COMPACT, BreadFrames.Mood.IDLE, 0).rows().get(8).strip();
            keys.onNext = () -> {
                var surface = TerminalTestSurface.read(bytes.toString(StandardCharsets.UTF_8), 160, 52);
                check(surface.line(1).charAt(159) == ' ', "Leaving full-width Home clears its final border column");
                check(surface.line(12).contains(signature), "Portrait stays docked at the same row on every report page"); pages[0]++;
            };
            screens.beginRead("Map > "); keys.onNext = null;
            check(pages[0] >= 3 && screens.actorRow() == 5, "Long reports never push the portrait down to a later page"); screens.endRead();
            ui.view(ScreenId.PROJECT_MAP_DETAIL); ui.value("Project goal", "Coordinate the food drive"); ui.menu("[T] Technical record links\n[0] Back"); ui.prompt("Record > "); screens.beginRead("Record > ");
            var surface = TerminalTestSurface.read(bytes.toString(StandardCharsets.UTF_8), 160, 52);
            check(surface.line(12).contains(signature), "A text-led detail view keeps the same approved bread visible");
            check(surface.line(24).contains("Record >") && surface.line(29).isBlank(), "Short details use a smaller frame and clear the previous footer");
        }
    }
    private static void narrowActions() throws Exception {
        var bytes = new ByteArrayOutputStream(); var keys = new Keys(80, 24, "\n\n\n");
        var screens = new TerminalScreenManager(new PrintStream(bytes, true, StandardCharsets.UTF_8), keys, false);
        try (var bread = new BreadSupport(screens.output(), keys, 80, 24, true, false, screens)) {
            var theme = new TerminalTheme(true, false, false, bread); var ui = new TerminalOutput(screens.output(), theme, 80);
            ui.view(ScreenId.PROJECT_WORKSPACE);
            StringBuilder actions = new StringBuilder();
            for (int i = 1; i <= 13; i++) { actions.append('[').append(i).append("] Review this project action\n"); }
            actions.append("[0] Back\n[?] Show menu");
            ui.screen(BreadFrames.Mood.IDLE, "Choose the action you need.", actions.toString(), false); ui.prompt("Workspace > "); screens.beginRead("Workspace > ");
            check(screens.enabled() && screens.actorRow() < 0, "An 80-column action-heavy view keeps paging/navigation instead of forcing classic mode to retain art");
            String actual = TerminalTestSurface.read(bytes.toString(StandardCharsets.UTF_8), 80, 24).text();
            for (int i = 1; i <= 13; i++) { check(actual.contains("[" + i + "]"), "Every narrow workspace choice remains on the same action page"); }
        }
    }
    private static void managedOperation() throws Exception {
        var bytes = new ByteArrayOutputStream(); var keys = new Keys(120, 40, "Prepared checklist\nConfirm volunteers\n");
        var screens = new TerminalScreenManager(new PrintStream(bytes, true, StandardCharsets.UTF_8), keys, true);
        try (var bread = new BreadSupport(screens.output(), keys, 120, 40, true, true, screens)) {
            var theme = new TerminalTheme(true, false, true, bread); var ui = new TerminalOutput(screens.output(), theme, 120);
            ui.form(ScreenId.PROGRESS_UPDATE, "Record what changed, then choose the next action."); ui.note("Include only progress you observed."); ui.prompt("Progress summary: ");
            bread.readLine(new StringReader(""), theme, "Progress summary: ", 8192);
            int anchor = screens.actorRow(); check(anchor == 5, "Explanatory text after the portrait cannot shift its live anchor");
            ui.prompt("Next action: "); bread.readLine(new StringReader(""), theme, "Next action: ", 8192);
            check(screens.actorRow() == anchor, "Changing fields retains the portrait anchor");
            try (var feedback = new OperationFeedback(screens.output(), theme, 120, OperationFeedback.Operation.SAVE)) {
                check(TerminalTestSurface.read(bytes.toString(StandardCharsets.UTF_8), 120, 40).line(27).contains("Saving your project"), "Real work appears immediately in its owned status row");
                check(!bytes.toString(StandardCharsets.UTF_8).contains("[Done]"), "Work does not claim success before completion");
                feedback.running(3);
                long deadline = System.nanoTime() + 2_000_000_000L;
                while (!bytes.toString(StandardCharsets.UTF_8).contains("3 items checked") && System.nanoTime() < deadline) { Thread.sleep(10); }
                check(bytes.toString(StandardCharsets.UTF_8).contains("3 items checked"), "Managed feedback displays actual progress without a competing log spinner");
                feedback.complete(false, "Prepared checklist");
            }
            check(TerminalTestSurface.read(bytes.toString(StandardCharsets.UTF_8), 120, 40).line(27).contains("[Done]"), "Completion replaces the same status row");
            check(!bytes.toString(StandardCharsets.UTF_8).contains("\r\u001b[2K"), "Managed work never writes a second cursor-owning spinner");
        }
        long deadline = System.nanoTime() + 2_000_000_000L;
        while (Thread.getAllStackTraces().keySet().stream().anyMatch(t -> t.getName().equals("breadcrumbs-operation-feedback")) && System.nanoTime() < deadline) { Thread.sleep(10); }
        check(Thread.getAllStackTraces().keySet().stream().noneMatch(t -> t.getName().equals("breadcrumbs-operation-feedback")), "Managed operation leaves no scheduler behind");
    }
    private static void completeFallback() throws Exception {
        var bytes = new ByteArrayOutputStream();
        try (var screens = new TerminalScreenManager(new PrintStream(bytes, true, StandardCharsets.UTF_8), new Keys(80, 24, ""), false)) {
            StringBuilder actions = new StringBuilder();
            for (int i = 1; i <= 20; i++) { actions.append("[").append(i).append("] Listed action ").append(i).append('\n'); }
            screens.actions(actions.toString()); screens.output().print(actions + "Choose > "); screens.beginRead("Choose > ");
            check(!screens.enabled() && bytes.toString(StandardCharsets.UTF_8).contains("[20] Listed action 20"), "An oversized action group releases all choices without hiding any");
        }
        for (boolean resize : new boolean[] {false, true}) {
            bytes.reset();
            try (var screens = new TerminalScreenManager(new PrintStream(bytes, true, StandardCharsets.UTF_8), new Keys(80, 24, ""), false)) {
                screens.output().print("[13] Explore Project Map\nChoose > "); screens.beginRead("Choose > ");
                if (resize) { screens.resize(28, 12); } else { screens.fallback(); }
                String released = bytes.toString(StandardCharsets.UTF_8);
                check(released.substring(released.lastIndexOf("\u001b[?1049l")).contains("[13] Explore Project Map"), "Small-window and keyboard fallbacks reprint complete choices after restoring scrollback");
            }
        }
        bytes.reset();
        try (var screens = new TerminalScreenManager(new PrintStream(bytes, true, StandardCharsets.UTF_8), new Keys(80, 24, ""), false)) {
            String prompt = "Responsible person [Enter if unknown; this records your account and does not verify it]: ";
            screens.output().print(prompt); screens.beginRead(prompt);
            String actual = TerminalTestSurface.read(bytes.toString(StandardCharsets.UTF_8), 80, 24).text();
            check(actual.replace("|", " ").replaceAll("\\s+", " ").contains(prompt.strip()), "Long field instructions remain complete above the bounded footer");
        }
        bytes.reset();
        try (var screens = new TerminalScreenManager(new PrintStream(bytes, true, StandardCharsets.UTF_8), new Keys(80, 24, ""), false)) {
            screens.output().print("Project: \u5b66\u7fd2\u8a08\u753b\nChoose > "); screens.beginRead("Choose > ");
            check(!screens.enabled() && bytes.toString(StandardCharsets.UTF_8).contains("\u5b66\u7fd2\u8a08\u753b"), "Ambiguous cell width preserves actual content through classic fallback");
        }
    }
    private static int count(String text, String part) { return (text.length() - text.replace(part, "").length()) / part.length(); }
    private static void check(boolean value, String message) { if (!value) { throw new AssertionError(message); } }
    private static final class Keys implements BreadSupport.Keyboard {
        private final int width, height;
        private final ArrayDeque<BreadSupport.Event> events = new ArrayDeque<>();
        Runnable onNext;
        Keys(int width, int height, String values) {
            this.width = width; this.height = height;
            values.chars().forEach(c -> events.add(new BreadSupport.Event(BreadSupport.EventKind.KEY, c, width, height)));
        }
        public int columns() { return width; } public int rows() { return height; }
        public BreadSupport.Event next() { if (onNext != null) { onNext.run(); } return events.isEmpty() ? new BreadSupport.Event(BreadSupport.EventKind.END, 0, width, height) : events.remove(); }
        public void close() { }
    }
}
