package breadcrumbs.cli;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;

public final class BreadSupportTest {
    private BreadSupportTest() { }
    public static void run() throws Exception {
        frames(); palettes(); layout(); boxes(); inputLifecycle(); operationOwnership();
        System.out.println("PASS BreadSupportTest: assets, dimensions, palettes, layout, input and render ownership");
    }
    public static void main(String[] args) throws Exception { run(); }

    private static void frames() {
        for (var size : BreadFrames.Size.values()) {
            for (var mood : BreadFrames.Mood.values()) {
                for (int tick = 0; tick < 128; tick++) {
                    var frame = BreadFrames.at(size, mood, tick);
                    check(frame.width() == size.width() && frame.height() == size.height(), "Fixed frame dimensions");
                    var color = frame.render(new TerminalTheme(true)); var plain = frame.render(new TerminalTheme(false));
                    for (int row = 0; row < frame.height(); row++) {
                        check(plain.get(row).length() == size.width(), "ASCII cells stay aligned");
                        check(plain.get(row).chars().allMatch(c -> c >= 32 && c <= 126), "ASCII fallback has no width ambiguity");
                        check(color.get(row).replaceAll("\u001b\\[[0-9;]*m", "").equals(plain.get(row)), "Color and monochrome use identical marks");
                        check(occurrences(color.get(row), "\u001b[0m") * 2 == occurrences(color.get(row), "\u001b["), "Every SGR run resets");

                    }
                    check(frame == BreadFrames.at(size, mood, tick + 96), "Timeline loops without growing assets");
                }
                check(!BreadFrames.at(size, mood, 0).equals(BreadFrames.at(size, mood, 19)), "Every expression can blink");
            }
        }
        check(!BreadFrames.at(BreadFrames.Size.FULL, BreadFrames.Mood.IDLE, 0).equals(BreadFrames.at(BreadFrames.Size.FULL, BreadFrames.Mood.ERROR, 0)), "Failure has its own expression");
        try { new BreadFrames.Frame(java.util.List.of("aa", "a"), java.util.List.of("00", "00")); throw new AssertionError("Unequal rows accepted"); }
        catch (IllegalArgumentException expected) { }
    }

    private static void palettes() {
        var theme = new TerminalTheme(true, false, false, null, false);
        check(theme.companionText(0xfff0ce, "@k").equals("\u001b[97m@k\u001b[0m"), "Limited-color cream stays distinct from gold");
        check(theme.companionText(0xf4c25c, "@").contains("[93m"), "Limited-color surface is gold");
        check(theme.companionText(0x83512c, "@").contains("[33m"), "Limited-color crust is darker");
        check(theme.companionText(0xe49b9a, "/").contains("[91m"), "Limited-color cheeks stay rosy");
        check(theme.artText(0xfff0ce, "@").contains("1;38;2;255;240;206"), "Original brand RGB behavior is preserved");
    }

    private static void layout() throws Exception {
        for (int width : new int[]{28, 40, 80, 120, 160}) {
            for (int height : new int[]{0, 12, 24, 40}) {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream(); PrintStream out = stream(bytes);
                try (var bread = new BreadSupport(out, null, width, height, true, false)) {
                    TerminalTheme theme = new TerminalTheme(false, false, false, bread);
                    new TerminalOutput(out, theme, width).screen(BreadFrames.Mood.WELCOME, "Welcome back! Ready to continue?", "[1] Open saved project\n[2] View Project Map\n[0] Back", true);
                    String text = bytes.toString(StandardCharsets.UTF_8);
                    check(text.contains("[1]") && text.contains("[2]") && text.contains("[0]"), "Every option remains visible");
                    check(text.lines().allMatch(line -> line.length() <= width), "Composed layout never overflows");
                    check(!text.contains("\u001b"), "Static monochrome emits no controls");
                    if (height == 0 || height == 12 || width < 76) { check(!text.contains("@@"), "Unknown/small viewport omits artwork"); }
                }
            }
        }
    }

    private static void boxes() {
        for (int width : new int[]{28, 40, 80, 120}) {
            var lines = MenuBox.rows(new TerminalTheme(false), width, "Choose how to continue your project.",
                    "[1] Open saved project\n[13] View Project Map and original source references\n[0] Back");
            check(lines.get(0).startsWith("/") && lines.get(lines.size() - 1).endsWith("/"), "Actions have an enclosing menu frame");
            check(lines.stream().allMatch(line -> line.length() == width), "Every box row aligns at the selected width");
            String content = String.join("\n", lines);
            check(content.indexOf("[1]") < content.indexOf("[13]") && content.indexOf("[13]") < content.indexOf("[0]"), "Boxing preserves canonical choice order and numbers");
        }
        try (var bread = new BreadSupport(stream(new ByteArrayOutputStream()), null, 160, 52, true, true)) {
            check(!bread.present(new TerminalTheme(false), BreadFrames.Mood.IDLE, "Choose a project", "[1] 研究\n[0] Back", false),
                    "Ambiguous-width source labels use static plain navigation");
        }
        String actions = "[1] View project summary\n[2] Record progress update\n[13] View Project Map\n[0] Back";
        var paired = MenuBox.rows(new TerminalTheme(false), 100, "Choose what to do next.", actions, true);
        check(paired.size() < MenuBox.rows(new TerminalTheme(false), 100, "Choose what to do next.", actions).size(),
                "Result menus leave room for the project summary above");
        check(paired.stream().anyMatch(line -> line.contains("[1]") && line.contains("[2]")), "Wide result menus pair actions in their original order");
        check(paired.stream().allMatch(line -> line.length() == 100), "Paired result box stays aligned");
        check(!BreadFrames.at(BreadFrames.Size.FULL, BreadFrames.Mood.WELCOME, 10)
                .equals(BreadFrames.at(BreadFrames.Size.FULL, BreadFrames.Mood.WORKING, 10)), "Greeting and active work use different animations");
        check(!BreadFrames.at(BreadFrames.Size.COMPACT, BreadFrames.Mood.CURIOUS, 0)
                .equals(BreadFrames.at(BreadFrames.Size.COMPACT, BreadFrames.Mood.IDLE, 0)), "Exploration has a distinct compact expression");
    }

    private static void inputLifecycle() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(); PrintStream out = stream(bytes);
        Keys keys = new Keys(); keys.tick(20); keys.key('1'); keys.tick(7); keys.key('3'); keys.key(8); keys.key('2'); keys.key(13);
        try (var bread = new BreadSupport(out, keys, 120, 40, true, true)) {
            TerminalTheme theme = new TerminalTheme(true, false, true, bread);
            check(bread.present(theme, BreadFrames.Mood.IDLE, "Ready to continue?", "[12] Save project\n[0] Back", false), "Eligible screen is composed");
            String answer = bread.readLine(new StringReader("wrong\n"), theme, "Workspace > ", 32);
            check(answer.equals("12"), "Number, pause, correction and Enter preserve the selected route");
            check(bytes.toString(StandardCharsets.UTF_8).contains("\u001b7"), "Idle continues across partial input");
            String after = bytes.toString(StandardCharsets.UTF_8);
            keys.tick(16); keys.key(13);
            bread.readLine(new StringReader(""), theme, "Next action: ", 8192);
            check(occurrences(bytes.toString(StandardCharsets.UTF_8), "\u001b7") == occurrences(after, "\u001b7"), "Old menu cannot redraw during free-form input");
            keys.tickResize(40, 12); keys.tick(8); keys.key(13);
            bread.present(theme, BreadFrames.Mood.IDLE, "Ready?", "[0] Back", false);
            int beforeResize = occurrences(bytes.toString(StandardCharsets.UTF_8), "\u001b7");
            bread.readLine(new StringReader(""), theme, "Menu > ", 32);
            check(occurrences(bytes.toString(StandardCharsets.UTF_8), "\u001b7") == beforeResize, "Resize stops cursor animation before repainting input");
        }
        check(keys.closed, "Keyboard process is owned by the application scope");
        Keys broken = new Keys(); broken.fail = true;
        try (var bread = new BreadSupport(out, broken, 120, 40, true, true)) {
            TerminalTheme theme = new TerminalTheme(true, false, true, bread);
            bread.present(theme, BreadFrames.Mood.IDLE, "Ready?", "[0] Back", false);
            check(bread.readLine(new StringReader("retry\n"), theme, "Menu > ", 32).equals("retry"), "Broken live input retries through the ordinary Reader");
            check(broken.closed && !bread.controlledInput(), "Failed adapter is closed before portable input takes over");
            int saved = bytes.size(); bread.stop(); check(bytes.size() == saved, "Exception cleanup writes no late frames");
        }
        try (var bread = new BreadSupport(out, null, 80, 24, false, false)) {
            check(bread.readLine(new StringReader("long text\n"), new TerminalTheme(false), "", 8192).equals("long text"), "Ordinary Reader is preserved");
            try { bread.readLine(new StringReader(""), new TerminalTheme(false), "", 32); throw new AssertionError("EOF swallowed"); }
            catch (EOFException expected) { }
        }
    }

    private static void operationOwnership() {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(); PrintStream out = stream(bytes);
        var bread = new BreadSupport(out, null, 120, 40, true, true);
        TerminalTheme theme = new TerminalTheme(true, false, true, bread);
        try (bread) {
            BreadSupport.Screen first = bread.operation(theme, "Checking your files");
            check(first != null, "Working region created");
            BreadSupport.Screen second = bread.operation(theme, "Saving your project");
            int start = bytes.size(); first.tick(); check(bytes.size() == start, "Starting another screen disposes the first");
            second.mood(BreadFrames.Mood.SUCCESS); second.tick();
            check(bytes.size() > start, "Confirmed result changes the current expression");
            second.close(); start = bytes.size();
            for (int i = 0; i < 64; i++) { second.tick(); }
            check(bytes.size() == start, "No output after screen close");
        }
        check(!bread.present(theme, BreadFrames.Mood.IDLE, "Ready?", "[0] Back", false), "Closed presenter cannot restart");
    }

    private static final class Keys implements BreadSupport.Keyboard {
        private final ArrayDeque<BreadSupport.Event> events = new ArrayDeque<>();
        private boolean closed, fail;
        void key(int key) { events.add(new BreadSupport.Event(BreadSupport.EventKind.KEY, key, 120, 40)); }
        void tick(int count) { for (int i = 0; i < count; i++) { tickResize(120, 40); } }
        void tickResize(int width, int height) { events.add(new BreadSupport.Event(BreadSupport.EventKind.TICK, 0, width, height)); }
        @Override public int columns() { return 120; }
        @Override public int rows() { return 40; }
        @Override public BreadSupport.Event next() throws IOException {
            if (fail) { throw new IOException("controlled event failure"); }
            return events.isEmpty() ? new BreadSupport.Event(BreadSupport.EventKind.END, 0, 120, 40) : events.remove();
        }
        @Override public void close() { closed = true; }
    }
    private static PrintStream stream(ByteArrayOutputStream bytes) { return new PrintStream(bytes, true, StandardCharsets.UTF_8); }
    private static int occurrences(String text, String part) { return (text.length() - text.replace(part, "").length()) / part.length(); }
    private static void check(boolean passed, String message) { if (!passed) { throw new AssertionError(message); } }
}
