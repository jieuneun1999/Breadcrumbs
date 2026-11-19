package breadcrumbs.cli;

import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.regex.Pattern;

/** Owns the optional terminal shell, never canonical project records. */
public final class TerminalScreenManager extends OutputStream {
    private enum Phase { CAPTURE, CLOSED }
    private static final Pattern SGR = Pattern.compile("\u001b\\[[0-9;]*m");
    private static final int MAX_BYTES = 1_048_576, BODY_TOP = 5;
    private final PrintStream raw;
    private final BreadSupport.Keyboard keyboard;
    private final boolean motion;
    private final ByteArrayOutputStream pending = new ByteArrayOutputStream();
    private final List<String> keys = new ArrayList<>();
    private final EnumMap<ScreenId, String> focus = new EnumMap<>(ScreenId.class);
    private TerminalTheme theme = new TerminalTheme(true);
    private ScreenId view = ScreenId.HOME, rendered;
    private List<String> shown = List.of(), previousBody = List.of(), portrait = List.of(), brand = List.of();
    private Phase phase = Phase.CAPTURE;
    private int columns, rows, selected, renderedRows, renderedWidth, renderedInset = 3;
    private boolean portraitOnRight = true, portraitChanged;
    private int actorTop = -1, actorColumn, actorWidth, actorHeight;
    private boolean entered, fallback, arrowUsed, urgent, fullBrand;

    public TerminalScreenManager(PrintStream raw, BreadSupport.Keyboard keyboard, boolean motion) {
        this.raw = raw; this.keyboard = keyboard; this.motion = motion;
        columns = keyboard.columns(); rows = keyboard.rows();
    }
    public PrintStream output() { return new PrintStream(this, true, StandardCharsets.UTF_8); }
    PrintStream liveOutput() { return raw; }
    String screen() { return view.title(); }
    boolean enabled() { return !fallback && phase != Phase.CLOSED; }
    private boolean fullHomeRoom() { return view == ScreenId.HOME && columns >= 160 && rows >= 52; }
    private int frameWidth() { return fullHomeRoom() ? columns : columns - 1; }
    private int inset() { return fullHomeRoom() ? 2 : 3; }
    int contentColumns() { return Math.max(1, columns - (fullHomeRoom() ? 2 : 5)); }
    private int frameRows() {
        if (fullHomeRoom()) { return Math.min(rows - 1, 52); }
        int preferred = switch (view) {
            case PROJECT_MAP_DETAIL -> 26;
            case PROJECT_SETUP, PROGRESS_UPDATE, PROJECT_MAP, PROJECT_MAP_SECTION -> 30;
            case PROJECT_WORKSPACE -> 34;
            default -> 32;
        };
        return Math.min(rows, preferred);
    }
    int contentRows() { return Math.max(4, frameRows() - 8); }
    int inputRow() { return frameRows() - 1; }
    ScreenId view() { return view; }
    void view(ScreenId value, TerminalTheme colors) {
        if (view != value) { keys.clear(); portrait = List.of(); brand = List.of(); fullBrand = false; }
        view = value; theme = colors;
    }
    void portrait(List<String> lines, boolean onRight) { portrait = List.copyOf(lines); portraitOnRight = onRight; portraitChanged = true; }
    private int portraitWidth() { return portrait.isEmpty() ? 0 : plain(portrait.get(0)).length(); }
    private boolean portraitFits() { return !portrait.isEmpty() && portrait.size() <= contentRows() && contentColumns() - portraitWidth() - 3 >= 28; }
    int textColumns() { return portraitFits() ? contentColumns() - portraitWidth() - 3 : contentColumns(); }
    int portraitColumn() { return portraitOnRight ? textColumns() + 3 : 0; }
    int actorStartColumn() { return renderedInset + actorColumn; }
    void urgent() { urgent = true; }
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
        if (fallback) { raw.write(value); return; }
        if (pending.size() >= MAX_BYTES) { classic("Large report: continuing in classic mode."); raw.write(value); return; }
        pending.write(value);
    }
    @Override public synchronized void write(byte[] data, int start, int length) {
        if (phase == Phase.CLOSED) { return; }
        if (fallback) { raw.write(data, start, length); return; }
        if (pending.size() + length > MAX_BYTES) { classic("Large report: continuing in classic mode."); raw.write(data, start, length); return; }
        pending.write(data, start, length);
    }
    @Override public void flush() { if (fallback) { raw.flush(); } }

    /** Composes staged output once at an existing input boundary. Text pages independently of the docked portrait and complete action group. */
    void beginRead(String prompt) throws IOException {
        if (!enabled()) { return; }
        List<String> original = pending.toString(StandardCharsets.UTF_8).lines().toList(); pending.reset();
        List<String> body = new ArrayList<>(original);
        String target = plain(prompt).strip().replaceAll("\\s+", " ");
        for (int n = 1; n <= body.size(); n++) {
            String tail = plain(String.join(" ", body.subList(body.size() - n, body.size()))).strip().replaceAll("\\s+", " ");
            if (tail.equals(target)) { body.subList(body.size() - n, body.size()).clear(); break; }
        }
        while (!body.isEmpty() && plain(body.get(body.size() - 1)).isBlank()) { body.remove(body.size() - 1); }
        boolean reuse = rendered == view && keys.isEmpty() && body.stream().allMatch(line -> plain(line).isBlank());
        if (reuse) { body = new ArrayList<>(previousBody); }
        if (body.stream().anyMatch(line -> !MenuBox.fixedCells(plain(line)))) {
            reportInClassic(body, "This content needs classic navigation to preserve its characters."); raw.print(prompt); raw.flush(); return;
        }
        body = wrap(body, textColumns());
        if (plain(prompt).length() > contentColumns() - 8) { body.add(""); body.addAll(wrap(List.of(plain(prompt).strip()), textColumns())); }
        if (!reuse) {
            previousBody = List.copyOf(body); brand = List.of(); fullBrand = false;
            if (fullHomeRoom()) {
                List<String> candidate = RichBrandRenderer.frame(theme, false).lines().map(line -> line.substring(2)).toList();
                if (candidate.size() + 1 + Math.max(body.size(), portraitFits() ? portrait.size() : 0) <= contentRows()
                        && candidate.stream().allMatch(line -> plain(line).length() <= contentColumns())) {
                    brand = new ArrayList<>(candidate); brand.add(""); fullBrand = true;
                }
            }
        }
        int capacity = contentRows() - brand.size(), page = 0;
        int pages = Math.max(1, (body.size() + capacity - 1) / capacity);
        int firstAction = -1, lastAction = -1;
        for (int i = 0; i < body.size(); i++) {
            String line = plain(body.get(i));
            if (keys.stream().anyMatch(key -> line.contains("[" + key + "]"))) {
                if (firstAction < 0) { firstAction = i; } lastAction = i;
            }
        }
        if (firstAction >= 0 && lastAction - firstAction + 1 > capacity) {
            reportInClassic(body, "This action group needs more room; all choices continue in classic mode."); raw.print(prompt); raw.flush(); return;
        }
        selected = Math.max(0, keys.indexOf(focus.get(view)));
        while (true) {
            int start = page == pages - 1 ? Math.max(0, body.size() - capacity) : page * capacity;
            if (page == pages - 1 && firstAction >= 0 && firstAction < start) { start = firstAction; }
            List<String> next = List.copyOf(body.subList(Math.min(start, body.size()), Math.min(body.size(), start + capacity)));
            render(next, rendered != view && page == 0 && !urgent);
            if (page == pages - 1) {
                navigation(keys.isEmpty() ? "Type your answer. /cancel discards the proposal." : "Type a listed option, or use arrows + Enter." + (keys.contains("0") ? "  0 Back / Exit" : ""));
                paintInput(theme, prompt, ""); highlight(); urgent = false; return;
            }
            String pager = (page + 1) + "/" + pages + " Enter:next Left:back Esc:actions > ";
            navigation("Report pages retain the complete recorded content."); paintInput(theme, pager, "");
            BreadSupport.Event event;
            do {
                try {
                    event = keyboard.next();
                    if (event.kind() == BreadSupport.EventKind.END) { throw new IOException("Native keyboard ended during report paging"); }
                } catch (IOException unavailable) { reportInClassic(body, "Report input interrupted; continuing in classic mode."); throw unavailable; }
                if (event.kind() == BreadSupport.EventKind.TICK && (event.columns() != columns || event.rows() != rows)) {
                    resize(event.columns(), event.rows()); reportInClassic(body, "Window size changed; this report continues in classic mode."); raw.print(prompt); raw.flush(); return;
                }
            } while (event.kind() == BreadSupport.EventKind.TICK);
            int key = event.character();
            if (key == 4 || key == 26) { throw new EOFException(); }
            if (key == 27) { page = pages - 1; }
            else if (key == BreadSupport.LEFT || key == BreadSupport.UP || key == 'p' || key == 'P') { page = Math.max(0, page - 1); }
            else if (key == 13 || key == 10 || key == ' ' || key == BreadSupport.RIGHT || key == BreadSupport.DOWN || key == 'n' || key == 'N') { page++; }
        }
    }
    private static List<String> wrap(List<String> lines, int width) {
        List<String> result = new ArrayList<>();
        for (String styled : lines) {
            String line = plain(styled).stripTrailing();
            if (line.length() <= width) { result.add(styled.stripTrailing()); continue; }
            // Legacy reports may have been formatted before a portrait was selected.
            // Keep every word; neutral wrapped values need no cursor/style inference.
            while (line.length() > width) {
                int end = line.lastIndexOf(' ', width); if (end < 1) { end = width; }
                result.add(line.substring(0, end)); line = line.substring(end).stripLeading();
            }
            result.add(line);
        }
        return result;
    }
    private List<String> compose(List<String> text) {
        List<String> result = new ArrayList<>(brand);
        if (!portraitFits()) { result.addAll(text); return result; }
        for (int n = 0; n < Math.max(text.size(), portrait.size()); n++) {
            String line = n < text.size() ? text.get(n) : "";
            String art = n < portrait.size() ? portrait.get(n) : " ".repeat(portraitWidth());
            String padded = line + " ".repeat(Math.max(0, textColumns() - plain(line).length()));
            result.add(portraitOnRight ? padded + "   " + art : art + "   " + padded);
        }
        return result;
    }
    private void render(List<String> text, boolean transition) {
        List<String> next = compose(text);
        if (!entered) { raw.print("\u001b[?1049h\u001b[H\u001b[2J"); entered = true; }
        if (motion && transition && rendered != null) { dimContent(); pause(60); }
        if (renderedWidth > frameWidth()) {
            for (int n = 1; n <= Math.max(renderedRows, frameRows()); n++) { raw.print("\u001b[" + n + ";" + (frameWidth() + 1) + "H\u001b[K"); }
        }
        if (renderedRows > frameRows()) {
            for (int n = frameRows() + 1; n <= renderedRows; n++) { raw.print("\u001b[" + n + ";1H\u001b[2K"); }
        }
        if (rendered != view || shown.isEmpty()) { shell(); }
        for (int n = 0; n < contentRows(); n++) {
            String line = n < next.size() ? next.get(n) : "";
            boolean freshPortrait = portraitChanged && n >= brand.size() && n < brand.size() + portrait.size();
            if (freshPortrait || n >= shown.size() || !line.equals(shown.get(n))) { row(BODY_TOP + n, line); }
        }
        shown = next; rendered = view; renderedRows = frameRows(); renderedWidth = frameWidth(); renderedInset = inset(); portraitChanged = false;
        if (portraitFits()) {
            actorTop = BODY_TOP + brand.size(); actorColumn = portraitColumn(); actorWidth = portraitWidth(); actorHeight = portrait.size();
        } else { actorTop = -1; }
        raw.flush();
        if (motion && transition) { pause(60); }
    }
    private void shell() {
        border(1); row(2, theme.label(TerminalTheme.Tone.BREAD_GOLD, "BREADCRUMBS") + " / " + view.title());
        String context = view == ScreenId.HOME && !fullBrand ? "Leave context behind. Pick up where work stopped." : "Recorded context. Clear next actions.";
        row(3, theme.label(TerminalTheme.Tone.SECONDARY, fit(context))); border(4); border(frameRows() - 3); border(frameRows());
    }
    private void dimContent() {
        for (int n = 0; n < shown.size(); n++) {
            String line = plain(shown.get(n)); int from = 0, to = line.length();
            if (actorTop >= 0 && BODY_TOP + n >= actorTop && BODY_TOP + n < actorTop + actorHeight) {
                if (actorColumn > 0) { to = Math.min(to, actorColumn); }
                else { from = Math.min(to, actorWidth); }
            }
            raw.print("\u001b[" + (BODY_TOP + n) + ";" + (renderedInset + from) + "H\u001b[2m" + line.substring(from, to) + "\u001b[0m");
        }
        // Dimmed rows must be revealed even when their content is unchanged.
        shown = List.of(); raw.flush();
    }
    private static void pause(long millis) {
        try { Thread.sleep(millis); } catch (InterruptedException stopped) { Thread.currentThread().interrupt(); }
    }
    private void border(int row) {
        int width = frameWidth();
        raw.print("\u001b[" + row + ";1H" + theme.label(TerminalTheme.Tone.BREAD_SHADOW,
                (row == 1 ? "/" : row == frameRows() ? "\\" : "+") + "-".repeat(Math.max(1, width - 2))
                + (row == 1 ? "\\" : row == frameRows() ? "/" : "+")));
    }
    private void row(int number, String line) {
        int pad = Math.max(0, contentColumns() - plain(line).length());
        String left = fullHomeRoom() ? "|" : "| ", right = fullHomeRoom() ? "|" : " |";
        // Explicit positioning follows every row; the full-width Home border never
        // emits a printable character after the final cell before repositioning.
        raw.print("\u001b[" + number + ";1H" + theme.label(TerminalTheme.Tone.BREAD_SHADOW, left) + line
                + " ".repeat(pad) + theme.label(TerminalTheme.Tone.BREAD_SHADOW, right));
    }
    private String fit(String text) { return text.length() <= contentColumns() ? text : text.substring(0, Math.max(1, contentColumns() - 3)) + "..."; }
    private static String plain(String text) { return SGR.matcher(text).replaceAll(""); }
    private void navigation(String hint) { row(frameRows() - 2, theme.label(TerminalTheme.Tone.SECONDARY, fit(hint))); }
    void paintInput(TerminalTheme colors, String prompt, String value) {
        if (!enabled()) { return; }
        String label = plain(prompt); int available = contentColumns();
        if (label.length() > available - 8) { label = label.substring(0, Math.max(1, available - 12)) + "... "; }
        StringBuilder safe = new StringBuilder();
        for (int i = 0; i < value.length(); i++) { char c = value.charAt(i); safe.append(c >= 32 && c <= 126 ? c : '?'); }
        String echo = safe.toString(); int room = Math.max(0, available - label.length());
        if (echo.length() > room) { echo = room > 2 ? "< " + echo.substring(echo.length() - room + 2) : echo.substring(echo.length() - room); }
        row(inputRow(), colors.label(TerminalTheme.Tone.BREAD_GOLD, label) + echo);
        raw.print("\u001b[" + inputRow() + ";" + (inset() + label.length() + echo.length()) + "H\u001b[?25h"); raw.flush();
    }
    boolean menuInput() { return !keys.isEmpty(); }
    boolean fitsLiveRegion(int height, int column, int width) {
        return enabled() && actorTop >= BODY_TOP && height == actorHeight && width == actorWidth;
    }
    int actorRow() { return actorTop; }
    String move(int direction) {
        if (!enabled() || keys.isEmpty()) { return ""; }
        arrowUsed = true; selected = Math.floorMod(selected + direction, keys.size()); highlight(); return keys.get(selected);
    }
    private void highlight() {
        if (keys.isEmpty() || !enabled()) { return; }
        raw.print("\u001b7");
        for (int n = 0; n < shown.size(); n++) {
            String line = shown.get(n);
            if (keys.stream().noneMatch(key -> line.contains("[" + key + "]"))) { continue; }
            row(BODY_TOP + n, line.replace("[" + keys.get(selected) + "]", "\u001b[7m[" + keys.get(selected) + "]\u001b[0m"));
        }
        raw.print("\u001b8"); raw.flush();
    }
    String selected() { return !arrowUsed || keys.isEmpty() ? "" : keys.get(selected); }
    void completeRead(String value) { if (keys.contains(value.toUpperCase(java.util.Locale.ROOT))) { focus.put(view, value.toUpperCase(java.util.Locale.ROOT)); } }
    void endRead() { keys.clear(); selected = 0; arrowUsed = false; }
    void resize(int width, int height) {
        columns = width; rows = height; actorTop = -1;
        if (width < 40 || height < 15) { reportInClassic(previousBody, "Viewport is small; complete choices continue in classic mode."); }
        else if (previousBody.size() > contentRows() || previousBody.stream().anyMatch(line -> plain(line).length() > contentColumns())) {
            reportInClassic(previousBody, "The resized window needs a simpler layout; complete content continues in classic mode.");
        } else { rendered = null; shown = List.of(); render(previousBody, false); }
    }
    void status(String message) { if (enabled()) { navigation(message); raw.flush(); } }
    private void reportInClassic(List<String> lines, String reason) { if (enabled()) { classic(reason); } raw.println(String.join("\n", lines)); raw.flush(); }
    void fallback() { if (enabled()) { reportInClassic(previousBody, "Classic input continues with the complete recorded content."); } }
    private void classic(String reason) {
        if (entered) { raw.print("\u001b[0m\u001b[?25h\u001b[?1049l"); entered = false; }
        raw.println(reason); raw.print(pending.toString(StandardCharsets.UTF_8)); pending.reset(); fallback = true; raw.flush();
    }
    @Override public void close() {
        if (phase == Phase.CLOSED) { return; }
        if (entered) { raw.print("\u001b[0m\u001b[?25h\u001b[?1049l"); }
        raw.print(pending.toString(StandardCharsets.UTF_8)); pending.reset(); raw.flush(); phase = Phase.CLOSED;
    }
}
