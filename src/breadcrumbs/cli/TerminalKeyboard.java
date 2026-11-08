package breadcrumbs.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

/** Optional Windows console events. The ordinary Reader remains the portable baseline. */
public final class TerminalKeyboard implements BreadSupport.Keyboard {
    private final Process process;
    private final BufferedReader events;
    private final int columns, rows;
    private final boolean rgb;
    private int observedColumns, observedRows;
    private long lastTickNanos;

    private TerminalKeyboard(Process process, BufferedReader events, int columns, int rows, boolean rgb) {
        this.rgb = rgb;
        this.process = process; this.events = events; this.columns = columns; this.rows = rows;
        observedColumns = columns; observedRows = rows;
    }

    public static TerminalKeyboard open(boolean enabled) {
        return open(enabled, ignored -> { });
    }
    public static TerminalKeyboard open(boolean enabled, java.util.function.Consumer<String> diagnostic) {
        if (!enabled || System.console() == null || !System.getProperty("os.name").startsWith("Windows")) {
            diagnostic.accept("Portable input selected. Live keyboard is disabled or unavailable on this host."); return null;
        }
        Process child = null;
        String reason = "";
        try {
            Path location = Path.of(TerminalKeyboard.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            Path helper = (java.nio.file.Files.isDirectory(location) ? location : location.getParent()).resolve("bread-keys.exe");
            if (!java.nio.file.Files.isRegularFile(helper)) { diagnostic.accept("Static fallback. Optional Windows keyboard helper is not built."); return null; }
            child = new ProcessBuilder(helper.toString(), Long.toString(ProcessHandle.current().pid()))
                    .redirectInput(ProcessBuilder.Redirect.INHERIT).start();
            BufferedReader events = new BufferedReader(new InputStreamReader(child.getInputStream(), StandardCharsets.UTF_8));
            FutureTask<String> ready = new FutureTask<>(events::readLine);
            Thread handshake = new Thread(ready, "breadcrumbs-keyboard-handshake"); handshake.setDaemon(true); handshake.start();
            String header = ready.get(4, TimeUnit.SECONDS);
            String[] fields = header == null ? new String[0] : header.split(" ");
            if (fields.length != 4 || !fields[0].equals("READY")
                    || !(fields[3].equals("ANSI") || fields[3].equals("RGB"))) { throw new IOException("Console keyboard unavailable"); }
            int columns = Integer.parseInt(fields[1]), rows = Integer.parseInt(fields[2]);
            if (columns < 20 || rows < 10 || columns > 1000 || rows > 1000) { throw new IOException("Unusable viewport"); }
            diagnostic.accept("Native keyboard connected at " + columns + " x " + rows + ".");
            return new TerminalKeyboard(child, events, columns, rows, fields[3].equals("RGB"));
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } catch (Exception unavailable) {
            reason = unavailable.getClass().getSimpleName() + ": " + unavailable.getMessage();
        }
        if (child != null) {
            child.destroyForcibly();
            try {
                if (child.waitFor(2, TimeUnit.SECONDS)) { reason += " " + new String(child.getErrorStream().readNBytes(2048), StandardCharsets.UTF_8); }
            } catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); }
            catch (IOException ignored) { }
        }
        diagnostic.accept("Static fallback. " + breadcrumbs.inspector.ContentSafety.terminal(reason, 2200));
        return null;
    }

    public boolean rgbSupported() { return rgb; }
    @Override public int columns() { return columns; }
    @Override public int rows() { return rows; }
    @Override public BreadSupport.Event next() throws IOException {
        while (true) {
        String line = events.readLine();
        if (line == null) { return new BreadSupport.Event(BreadSupport.EventKind.END, 0, columns, rows); }
        String[] fields = line.split(" ");
        try {
            if (fields.length == 3 && fields[0].equals("T")) {
                int width = Integer.parseInt(fields[1]), height = Integer.parseInt(fields[2]);
                long now = System.nanoTime();
                if (width == observedColumns && height == observedRows && lastTickNanos != 0
                        && now - lastTickNanos < TimeUnit.MILLISECONDS.toNanos(120)) { continue; }
                observedColumns = width; observedRows = height; lastTickNanos = now;
                return new BreadSupport.Event(BreadSupport.EventKind.TICK, 0, width, height);
            }
            if (fields.length == 2 && fields[0].equals("K")) {
                int key = Integer.parseInt(fields[1]);
                if (key >= 0 && key <= Character.MAX_VALUE) { return new BreadSupport.Event(BreadSupport.EventKind.KEY, key, columns, rows); }
            }
        } catch (NumberFormatException invalid) { throw new IOException("Invalid console event", invalid); }
        throw new IOException("Invalid console event");
        }
    }
    @Override public void close() {
        process.destroyForcibly();
        try {
            if (!process.waitFor(2, TimeUnit.SECONDS)) { process.destroyForcibly(); }
            events.close();
        } catch (InterruptedException stopped) { Thread.currentThread().interrupt(); }
        catch (IOException ignored) { }
    }
}
