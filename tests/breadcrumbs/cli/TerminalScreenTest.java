package breadcrumbs.cli;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;

public final class TerminalScreenTest {
    private TerminalScreenTest() { }
    public static void main(String[] args) throws Exception { run(); }
    public static void run() throws Exception {
        inputRoutes();
        pagedFallback();
        var bytes = new ByteArrayOutputStream(); var raw = new PrintStream(bytes, true, StandardCharsets.UTF_8);
        var keys = new Keys();
        try (var screens = new TerminalScreenManager(raw, keys, false)) {
            var out = screens.output();
            out.println("Original report"); out.print("Home > ");
            check(bytes.size() == 0, "Composition does not leak half-drawn screens");
            screens.beginRead("Home > ");
            check(screens.screen().equals("Home"), "Current identity belongs to the presentation");
            screens.actions("[1] Open\n[13] Map\n[0] Back");
            check(screens.move(1).equals("13") && screens.move(1).equals("0") && screens.move(1).equals("1"), "Arrows resolve to original numeric tokens and wrap");
            screens.endRead(); out.println("Map content"); out.print("Map > "); screens.beginRead("Map > ");
            String text = bytes.toString(StandardCharsets.UTF_8);
            check(count(text, "\u001b[?1049h") == 1 && count(text, "\u001b[2J") == 2, "One alternate viewport replaces successive screens");
            check(!text.substring(text.lastIndexOf("\u001b[2J")).contains("Original report"), "New screen does not append previous content");
            screens.endRead();
            for (int i = 0; i < 90; i++) { out.println("Source locator " + i); }
            out.print("Record > "); keys.tick(4); keys.enter(2); screens.beginRead("Record > ");
            text = bytes.toString(StandardCharsets.UTF_8);
            for (int i = 0; i < 90; i++) { check(text.contains("Source locator " + i), "Report content remains reachable through paging"); }
            check(text.contains("Enter:next Left:back"), "Long reports disclose navigation instead of silently clipping");
            check(count(text, "Source locator 0" + System.lineSeparator()) == 1, "Unchanged keyboard ticks do not clear and repaint a report page");
            screens.beginOperation(); out.println("Real scan progress"); check(bytes.toString(StandardCharsets.UTF_8).stripTrailing().endsWith("Real scan progress"), "Real operations are revealed immediately"); screens.endOperation();
        }
        String closed = bytes.toString(StandardCharsets.UTF_8);
        check(closed.contains("\u001b[?25h\u001b[?1049l"), "Terminal and cursor restore on close");
        check(!closed.contains("\u001b[2m"), "Reduced motion retains replacement without transition frames");
        var failure = new ByteArrayOutputStream();
        try (var screens = new TerminalScreenManager(new PrintStream(failure, true, StandardCharsets.UTF_8), new Keys(), true)) {
            screens.output().print("Welcome\nPrompt > "); screens.beginRead("Prompt > ");
            screens.resize(28, 12);
            check(!screens.enabled(), "Small resize releases enhanced viewport conservatively");
        }
        check(count(failure.toString(StandardCharsets.UTF_8), "\u001b[?1049l") == 1, "Resize fallback restores once without late repaint");
        System.out.println("PASS: screen lifecycle, composition/replacement, paging, numeric arrow tokens, live operation and cleanup prototype");
    }
    private static void inputRoutes() throws Exception {
        for (int width : new int[] {40, 80, 120, 160}) {
            var bytes = new ByteArrayOutputStream(); var raw = new PrintStream(bytes, true, StandardCharsets.UTF_8);
            Keys keys = new Keys(width); keys.key(BreadSupport.DOWN); keys.key(13);
            var screens = new TerminalScreenManager(raw, keys, false);
            try (var bread = new BreadSupport(screens.output(), keys, width, 40, false, false, screens)) {
                var theme = new TerminalTheme(true, false, false, bread);
                var ui = new TerminalOutput(screens.output(), theme, width);
                ui.menu("[1] Summary\n[13] Project Map\n[0] Back"); ui.prompt("Workspace > ");
                check(bread.readLine(new java.io.StringReader(""), theme, "Workspace > ", 8192).equals("13"), "Arrow plus Enter dispatches the existing multi-digit menu route");
                check(bytes.toString(StandardCharsets.UTF_8).contains("\u001b[7m[13]"), "Focused action has visible highlighting");
                keys.key(13); ui.menu("[YES] Confirm\n[0] Cancel"); ui.prompt("Confirm: ");
                check(bread.readLine(new java.io.StringReader(""), theme, "Confirm: ", 8192).isEmpty(), "Enter alone must never silently confirm a highlighted action");
                keys.key('1'); keys.key(BreadSupport.DOWN); keys.key('2'); keys.key(13);
                ui.menu("[1] Summary\n[12] Save"); ui.prompt("Workspace > ");
                check(bread.readLine(new java.io.StringReader(""), theme, "Workspace > ", 8192).equals("12"), "Arrows during partial input do not alter numeric text");
                keys.key('A'); keys.key(BreadSupport.LEFT); keys.key('B'); keys.key(13); ui.prompt("Project goal: ");
                check(bread.readLine(new java.io.StringReader(""), theme, "Project goal: ", 8192).equals("AB"), "Native arrow events never leak private-use glyphs into project text");
            }
        }
    }
    private static int count(String text, String part) { return (text.length() - text.replace(part, "").length()) / part.length(); }
    private static void pagedFallback() throws Exception {
        for (boolean failure : new boolean[] {false, true}) {
            var bytes = new ByteArrayOutputStream(); var raw = new PrintStream(bytes, true, StandardCharsets.UTF_8);
            var keys = new Keys(); keys.fail = failure;
            var screens = new TerminalScreenManager(raw, keys, false);
            try (var bread = new BreadSupport(screens.output(), keys, 120, 40, false, false, screens)) {
                for (int i = 0; i < 60; i++) { screens.output().println("Retained report line " + i); }
                screens.output().print("Record > ");
                check(bread.readLine(new java.io.StringReader("12\n"), new TerminalTheme(false), "Record > ", 8192).equals("12"),
                        "Keyboard loss during report paging returns to usable portable input");
                check(bytes.toString(StandardCharsets.UTF_8).contains("Retained report line 59"), "Paging fallback retains the entire report");
            }
        }
        var bytes = new ByteArrayOutputStream(); var raw = new PrintStream(bytes, true, StandardCharsets.UTF_8);
        var keys = new Keys(); keys.resize(80, 24); keys.key('1'); keys.key(13);
        var screens = new TerminalScreenManager(raw, keys, false);
        try (var bread = new BreadSupport(screens.output(), keys, 120, 40, false, false, screens)) {
            for (int i = 0; i < 60; i++) { screens.output().println("Resize report line " + i); }
            screens.output().print("Record > ");
            check(bread.readLine(new java.io.StringReader(""), new TerminalTheme(false), "Record > ", 8192).equals("1"), "Resize during paging retains a usable input boundary");
            check(!screens.enabled() && bytes.toString(StandardCharsets.UTF_8).contains("Resize report line 59"), "Report resize releases safely without clipping unseen source lines");
        }
    }
    private static void check(boolean condition, String message) { if (!condition) { throw new AssertionError(message); } }
    private static final class Keys implements BreadSupport.Keyboard {
        private final ArrayDeque<BreadSupport.Event> events = new ArrayDeque<>();
        private final int width;
        boolean fail;
        Keys() { this(120); }
        Keys(int width) { this.width = width; }
        void key(int value) { events.add(new BreadSupport.Event(BreadSupport.EventKind.KEY, value, width, 40)); }
        void tick(int n) { for (int i = 0; i < n; i++) { events.add(new BreadSupport.Event(BreadSupport.EventKind.TICK, 0, width, 40)); } }
        void resize(int width, int height) { events.add(new BreadSupport.Event(BreadSupport.EventKind.TICK, 0, width, height)); }
        void enter(int n) { for (int i = 0; i < n; i++) { key(13); } }
        public int columns() { return width; } public int rows() { return 40; }
        public BreadSupport.Event next() throws java.io.IOException {
            if (fail) { throw new java.io.IOException("Synthetic native keyboard loss"); }
            return events.isEmpty() ? new BreadSupport.Event(BreadSupport.EventKind.END, 0, width, 40) : events.remove();
        }
        public void close() { }
    }
}
