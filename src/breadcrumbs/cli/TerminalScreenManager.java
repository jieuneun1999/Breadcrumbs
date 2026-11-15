package breadcrumbs.cli;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Transient viewport over existing presentation. Never owns project records. */
public final class TerminalScreenManager extends OutputStream {
    private enum Phase { CAPTURE, LIVE, CLOSED }
    private static final Pattern SGR = Pattern.compile("\u001b\\[[0-9;]*m");
    private static final int MAX_BYTES = 1_048_576;
    private final PrintStream raw;
    private final BreadSupport.Keyboard keyboard;
    private final boolean motion;
    private final ByteArrayOutputStream pending = new ByteArrayOutputStream();
    private final List<String> keys = new ArrayList<>();
    private List<String> shown = List.of();
    private Phase phase = Phase.CAPTURE;
    private int columns, rows, selected, lastPageLines;
    private String screen = "Home";
    private boolean entered, fallback, arrowUsed;

    public TerminalScreenManager(PrintStream raw, BreadSupport.Keyboard keyboard, boolean motion) {
        this.raw = raw; this.keyboard = keyboard; this.motion = motion;
        columns = keyboard.columns(); rows = keyboard.rows();
    }
    public PrintStream output() { return new PrintStream(this, true, StandardCharsets.UTF_8); }
    PrintStream liveOutput() { return raw; }
    String screen() { return screen; }
    boolean enabled() { return !fallback && phase != Phase.CLOSED; }
    void actions(String actions) {
        for (String line : actions.split("\n")) {
            if (!line.startsWith("[")) { continue; }
            int end = line.indexOf(']');
            if (end > 1 && end < 12) {
                String key = line.substring(1, end);
                if (key.matches("[0-9A-Za-z?]+") && !keys.contains(key)) { keys.add(key); }
            }
        }
    }
    @Override public synchronized void write(int value) {
        if (phase == Phase.CLOSED) { return; }
        if (phase == Phase.LIVE || fallback) { raw.write(value); return; }
        if (pending.size() >= MAX_BYTES) { classic("Large report: continuing in classic mode."); raw.write(value); return; }
        pending.write(value);
    }
    @Override public synchronized void write(byte[] data, int start, int length) {
        if (phase == Phase.CLOSED) { return; }
        if (phase == Phase.LIVE || fallback) { raw.write(data, start, length); return; }
        if (pending.size() + length > MAX_BYTES) { classic("Large report: continuing in classic mode."); raw.write(data, start, length); return; }
        pending.write(data, start, length);
    }
    @Override public void flush() { if (phase == Phase.LIVE || fallback) { raw.flush(); } }
    void beginRead(String prompt) throws IOException {
        if (!enabled()) { return; }
        screen = title(prompt);
        String text = pending.toString(StandardCharsets.UTF_8); pending.reset();
        List<String> lines = new ArrayList<>(text.lines().toList());
        if (lines.isEmpty()) { lines.add(prompt); }
        int capacity = Math.max(4, rows - 5), page = 0;
        int pages = Math.max(1, (lines.size() + capacity - 1) / capacity);
        while (true) {
            int start = page == pages - 1 ? Math.max(0, lines.size() - capacity) : page * capacity;
            int end = Math.min(lines.size(), start + capacity);
            shown = List.copyOf(lines.subList(start, end));
            replace(page == 0);
            for (int i = 0; i < shown.size(); i++) {
                raw.print(shown.get(i));
                if (page < pages - 1 || i < shown.size() - 1) { raw.println(); }
            }
            if (page == pages - 1) { lastPageLines = shown.size(); raw.flush(); return; }
            raw.print("\n" + (page + 1) + "/" + pages + " Enter:next Left:back Esc:actions > "); raw.flush();
            BreadSupport.Event event;
            do {
                try {
                    event = keyboard.next();
                    if (event.kind() == BreadSupport.EventKind.END) { throw new IOException("Native keyboard ended during report paging"); }
                } catch (IOException unavailable) {
                    reportInClassic(lines, "Report input interrupted; continuing in classic mode.");
                    throw unavailable;
                }
                if (event.kind() == BreadSupport.EventKind.TICK) {
                    if (event.columns() != columns || event.rows() != rows) {
                        resize(event.columns(), event.rows());
                        reportInClassic(lines, "Window size changed; this report continues in classic mode.");
                        return;
                    }
                }
            } while (event.kind() == BreadSupport.EventKind.TICK);
            int key = event.character();
            if (key == 4 || key == 26) { throw new java.io.EOFException(); }
            if (key == 27) { page = pages - 1; }
            else if (key == BreadSupport.LEFT || key == BreadSupport.UP || key == 'p' || key == 'P') { page = Math.max(0, page - 1); }
            else if (key == 13 || key == 10 || key == ' ' || key == BreadSupport.RIGHT || key == BreadSupport.DOWN || key == 'n' || key == 'N') { page++; }
        }
    }
    boolean fitsLiveRegion(int lines) { return enabled() && lastPageLines >= lines; }
    String move(int direction) {
        if (!enabled() || keys.isEmpty()) { return ""; }
        arrowUsed = true;
        selected = Math.floorMod(selected + direction, keys.size());
        for (int row = 0; row < shown.size(); row++) {
            String line = shown.get(row);
            if (keys.stream().noneMatch(key -> line.contains("[" + key + "]"))) { continue; }
            String marked = line.replace("[" + keys.get(selected) + "]", "\u001b[7m[" + keys.get(selected) + "]\u001b[0m");
            raw.print("\u001b7\u001b[" + (row + 3) + ";1H\u001b[2K" + marked + "\u001b8");
        }
        raw.flush();
        return keys.get(selected);
    }
    String selected() { return !arrowUsed || keys.isEmpty() ? "" : keys.get(selected); }
    void endRead() { keys.clear(); selected = 0; arrowUsed = false; shown = List.of(); }
    void resize(int width, int height) {
        columns = width; rows = height;
        if (width < 40 || height < 15) { classic("Viewport is small; classic navigation continues."); }
    }
    void beginOperation() {
        if (!enabled()) { return; }
        if (pending.size() > 0) { replace(false); raw.print(pending.toString(StandardCharsets.UTF_8)); pending.reset(); }
        phase = Phase.LIVE;
    }
    void beginIntro() { if (enabled()) { replace(false); phase = Phase.LIVE; } }
    void endOperation() { if (phase != Phase.CLOSED) { phase = Phase.CAPTURE; } }
    private void reportInClassic(List<String> lines, String reason) {
        if (enabled()) { classic(reason); }
        raw.println(String.join("\n", lines)); raw.flush();
    }
    void fallback() { if (enabled()) { classic("Classic input continues."); } }
    private void replace(boolean animate) {
        if (!entered) { raw.print("\u001b[?1049h"); entered = true; }
        if (motion && animate) {
            for (int step = 0; step < 3; step++) {
                int position = 1 + step * Math.max(1, (columns - 12) / 3);
                raw.print("\u001b7\u001b[1;1H\u001b[2m" + "-".repeat(Math.max(1, columns - 2))
                        + "\u001b[0m\u001b[1;" + position + "H\u001b[93m..::>>::..\u001b[0m\u001b8"); raw.flush();
                try { Thread.sleep(35); } catch (InterruptedException stopped) { Thread.currentThread().interrupt(); break; }
            }
        }
        raw.print("\u001b[0m\u001b[?25h\u001b[H\u001b[2J");
        String title = " BREADCRUMBS / " + screen;
        raw.println("\u001b[93m" + title.substring(0, Math.min(title.length(), Math.max(1, columns - 1))) + "\u001b[0m");
        raw.println("-".repeat(Math.max(1, columns - 1)));
    }
    private static String title(String prompt) {
        String clean = SGR.matcher(prompt).replaceAll("").strip();
        return clean.endsWith(">") ? clean.substring(0, clean.length() - 1).strip() : clean.replaceAll(":$", "");
    }
    private void classic(String reason) {
        if (entered) { raw.print("\u001b[0m\u001b[?25h\u001b[?1049l"); entered = false; }
        raw.println(reason); raw.print(pending.toString(StandardCharsets.UTF_8)); pending.reset(); fallback = true; raw.flush();
    }
    @Override public void close() {
        if (phase == Phase.CLOSED) { return; }
        if (entered) { raw.print("\u001b[0m\u001b[?25h\u001b[?1049l"); }
        raw.print(pending.toString(StandardCharsets.UTF_8)); pending.reset();
        raw.flush(); phase = Phase.CLOSED;
    }
}
