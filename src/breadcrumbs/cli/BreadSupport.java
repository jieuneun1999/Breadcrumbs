package breadcrumbs.cli;

import breadcrumbs.cli.BreadFrames.Frame;
import breadcrumbs.cli.BreadFrames.Mood;
import breadcrumbs.cli.BreadFrames.Size;
import java.io.EOFException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.util.List;

/** One presentation owner. Menu frames advance inside controlled keyboard reads. */
public final class BreadSupport implements AutoCloseable {
    public enum EventKind { KEY, TICK, END }
    public record Event(EventKind kind, int character, int columns, int rows) { }
    public interface Keyboard extends AutoCloseable {
        int columns();
        int rows();
        Event next() throws IOException;
        @Override void close();
    }
    private final PrintStream out;
    private Keyboard keyboard;
    private final boolean visible, animation;
    private int columns, rows;
    private Screen active;
    private boolean closed;

    public BreadSupport(PrintStream out, Keyboard keyboard, int columns, int rows, boolean visible, boolean animation) {
        this.out = out; this.keyboard = keyboard;
        this.columns = keyboard == null ? columns : keyboard.columns();
        this.rows = keyboard == null ? rows : keyboard.rows();
        this.visible = visible; this.animation = animation;
    }
    public int columns() { return columns; }
    boolean boxedMenus() { return visible && !closed && columns >= 28 && rows >= 10; }
    public boolean controlledInput() { return keyboard != null && !closed; }

    final class Screen implements AutoCloseable {
        private final TerminalTheme theme;
        private final Size size;
        private final int distance, column;
        private int previousOffset;
        private Mood mood;
        private Frame previous;
        private List<String> previousLines;
        private long tick;
        private boolean stopped;
        Screen(TerminalTheme theme, Size size, Mood mood, int distance, int column) {
            this.theme = theme; this.size = size; this.mood = mood; this.distance = distance; this.column = column;
            previous = BreadFrames.at(size, mood, 0);
            previousLines = viewport(previous.render(theme), 0);
        }
        void tick() {
            synchronized (out) {
                if (stopped || closed || active != this || !animation || out.checkError()) { return; }
                Frame next = BreadFrames.at(size, mood, ++tick);
                int offset = offset(tick);
                if (next == previous && offset == previousOffset) { return; }
                List<String> rendered = viewport(next.render(theme), offset);
                for (int y = 0; y < size.height(); y++) {
                    if (rendered.get(y).equals(previousLines.get(y))) { continue; }
                    out.print("\u001b7\u001b[" + (distance - y) + "A\r\u001b[" + (column + 1) + "G" + rendered.get(y) + "\u001b8");
                }
                out.flush(); previous = next; previousLines = rendered; previousOffset = offset;
            }
        }
        void mood(Mood value) { synchronized (out) { mood = value; tick = 0; } }
        @Override public void close() { synchronized (out) { stopped = true; if (active == this) { active = null; } } }
    }

    boolean present(TerminalTheme theme, Mood mood, String message, String actions, boolean expanded) {
        return present(theme, mood, message, actions, expanded, false);
    }

    boolean result(TerminalTheme theme, Mood mood, String message, String actions) {
        return present(theme, mood, message, actions, false, true);
    }

    private boolean present(TerminalTheme theme, Mood mood, String message, String actions, boolean expanded, boolean result) {
        synchronized (out) {
            stop();
            if (closed || !visible || columns < 76 || rows < 18
                    || !MenuBox.fixedCells(message) || !MenuBox.fixedCells(actions)) { return false; }
            Size size = !result && columns >= 120 && rows >= 30 ? Size.FULL : Size.COMPACT;
            List<String> box;
            int regionWidth, right, count;
            while (true) {
                regionWidth = size.width() + 4;
                right = columns - regionWidth - 4;
                if (right < 24) { return false; }
                box = MenuBox.rows(theme, right, message, actions, result);
                count = Math.max(size.height(), box.size());
                if (count + 4 <= rows) { break; }
                if (size == Size.COMPACT) { return false; }
                size = Size.COMPACT;
            }
            boolean onRight = !expanded;
            List<String> art = viewport(BreadFrames.at(size, mood, 0).render(theme), 0);
            out.println();
            for (int y = 0; y < count; y++) {
                String marks = y < art.size() ? art.get(y) : " ".repeat(regionWidth);
                String menu = y < box.size() ? box.get(y) : " ".repeat(right);
                if (onRight) { out.print(menu + "   " + marks); }
                else { out.print(marks + "   " + menu); }
                out.println();
            }
            // The shared prompt contributes one newline before its final input row.
            active = new Screen(theme, size, mood, count + 1, onRight ? right + 3 : 0);
            return true;
        }
    }

    Screen operation(TerminalTheme theme, String message) {
        if (!present(theme, Mood.WORKING, message, "", false)) { return null; }
        synchronized (out) {
            Screen screen = active;
            // Operations have no prompt newline below the reserved region.
            active = new Screen(theme, screen.size, Mood.WORKING, screen.distance - 1, screen.column);
            screen.stopped = true;
            return active;
        }
    }

    String readLine(Reader fallback, TerminalTheme theme, String prompt, int limit) throws IOException {
        try {
            if (keyboard == null || closed) { return readPortable(fallback, limit); }
            StringBuilder line = new StringBuilder();
            boolean overflow = false;
            while (true) {
                Event event;
                try { event = keyboard.next(); }
                catch (IOException unavailable) { return portableAfterFailure(fallback, theme, prompt, limit); }
                if (event.kind() == EventKind.END) { return portableAfterFailure(fallback, theme, prompt, limit); }
                if (event.kind() == EventKind.TICK) {
                    if (event.columns() != columns || event.rows() != rows) {
                        stop(); columns = Math.max(1, event.columns()); rows = Math.max(1, event.rows());
                        paintInput(theme, prompt, line.toString());
                    } else if (active != null) { active.tick(); }
                    continue;
                }
                int key = event.character();
                if (key == 4 || key == 26) { throw new EOFException(); }
                if (key == 13 || key == 10) {
                    out.println(); out.flush();
                    if (overflow) { throw new IllegalArgumentException("Input exceeds " + limit + " characters."); }
                    return line.toString();
                }
                if (key == 27) { out.println(); return "/cancel"; }
                if (key == 8 || key == 127) {
                    if (!line.isEmpty()) { line.delete(line.offsetByCodePoints(line.length(), -1), line.length()); }
                } else if (key == 21) { line.setLength(0); }
                else if (key >= 32 && !Character.isISOControl(key) && Character.getType(key) != Character.FORMAT) {
                    if (line.length() < limit) { line.append((char) key); } else { overflow = true; }
                }
                paintInput(theme, prompt, line.toString());
            }
        } finally { stop(); }
    }

    private String portableAfterFailure(Reader fallback, TerminalTheme theme, String prompt, int limit) throws IOException {
        stop(); keyboard.close(); keyboard = null;
        out.println("\nInput was interrupted. Please enter this answer again.");
        new TerminalOutput(out, theme, columns).prompt(prompt);
        return readPortable(fallback, limit);
    }

    private void paintInput(TerminalTheme theme, String prompt, String value) {
        synchronized (out) {
            int available = Math.max(1, columns - 1);
            String tail = prompt;
            int uiWidth = Math.max(28, Math.min(columns, 120));
            while (tail.length() > uiWidth) {
                int split = tail.lastIndexOf(' ', uiWidth); if (split < 1) { split = uiWidth; }
                tail = tail.substring(split).stripLeading();
            }
            if (tail.length() > available - 8) { tail = available > 1 ? "> " : ">"; }
            StringBuilder echo = new StringBuilder();
            // Ambiguous-width input keeps its original value; only its live echo uses ASCII.
            for (int i = 0; i < value.length(); i++) { char c = value.charAt(i); echo.append(c >= 32 && c <= 126 ? c : '?'); }
            int room = Math.max(0, available - tail.length());
            String text = echo.toString();
            if (text.length() > room) { text = room > 2 ? "< " + text.substring(text.length() - room + 2) : text.substring(text.length() - room); }
            out.print("\r\u001b[2K" + theme.label(TerminalTheme.Tone.BREAD_GOLD, tail) + text); out.flush();
        }
    }

    private static String readPortable(Reader input, int limit) throws IOException {
        StringBuilder line = new StringBuilder(); int c; boolean overflow = false;
        while ((c = input.read()) != -1 && c != '\n') {
            if (c != '\r') { if (line.length() < limit) { line.append((char) c); } else { overflow = true; } }
        }
        if (c == -1 && line.isEmpty() && !overflow) { throw new EOFException(); }
        if (overflow) { throw new IllegalArgumentException("Input exceeds " + limit + " characters."); }
        return line.toString();
    }

    private static List<String> viewport(List<String> art, int offset) {
        return art.stream().map(line -> " ".repeat(2 + offset) + line + " ".repeat(2 - offset)).toList();
    }
    private static int offset(long tick) {
        int phase = Math.floorMod(tick, 96);
        if (phase >= 28 && phase <= 31 || phase >= 36 && phase <= 39) { return -1; }
        if (phase >= 32 && phase <= 35) { return -2; }
        if (phase >= 44 && phase <= 47 || phase >= 52 && phase <= 55) { return 1; }
        if (phase >= 48 && phase <= 51) { return 2; }
        return 0;
    }
    void stop() { synchronized (out) { if (active != null) { active.close(); } } }
    @Override public void close() {
        synchronized (out) { if (closed) { return; } stop(); closed = true; }
        if (keyboard != null) { keyboard.close(); }
    }
}
