package breadcrumbs.cli;

import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.*;
import breadcrumbs.inspector.ContentSafety;

public final class OperationFeedback implements AutoCloseable {
    public enum State { STARTED, RUNNING, COMPLETED, COMPLETED_WITH_WARNINGS, FAILED, CANCELLED }
    public enum Operation {
        INSPECT("Inspecting project files"), RESCAN("Rescanning approved sources"), REBUILD("Rebuilding search index"),
        SAVE("Saving workspace"), LOAD("Loading saved workspace"), RECOVER("Recovering validated history"),
        EXPORT("Generating continuity documents"), AUDIT("Checking continuity health"), COMPARE("Comparing saved observations");
        final String label; Operation(String label) { this.label = label; }
    }
    @FunctionalInterface public interface Work<T> { T run() throws IOException; }
    private final PrintStream out;
    private final TerminalTheme theme;
    private final int width;
    private final Operation operation;
    private final ScheduledExecutorService timer;
    private volatile State state = State.STARTED;
    private long count = -1;
    private int frame;
    private boolean paused;
    public OperationFeedback(PrintStream out, TerminalTheme theme, int width, Operation operation) {
        this.out = out; this.theme = theme; this.width = Math.max(28, Math.min(120, width)); this.operation = operation;
        if (theme.animationsEnabled()) {
            timer = Executors.newSingleThreadScheduledExecutor(task -> { Thread thread = new Thread(task, "breadcrumbs-operation-feedback"); thread.setDaemon(true); return thread; });
            timer.scheduleAtFixedRate(this::redraw, 200, 120, TimeUnit.MILLISECONDS);
        } else { timer = null; print("[STARTED] " + operation.label, TerminalTheme.Tone.BREAD_GOLD); }
    }
    public State state() { return state; }
    private boolean active() { return state == State.STARTED || state == State.RUNNING; }
    public void running(long count) {
        if (count < 0) { throw new IllegalArgumentException("A real nonnegative progress count is required."); }
        synchronized (out) { if (active()) { this.count = count; state = State.RUNNING; } }
    }
    private void redraw() {
        synchronized (out) {
            if (!active() || paused) { return; }
            String suffix = count >= 0 ? " | " + count + " items visited" : "...";
            if (suffix.length() > width - 6) { suffix = " | " + count; }
            String label = operation.label.substring(0, Math.min(operation.label.length(), width - suffix.length() - 2));
            String text = "|/-\\".charAt(frame++ % 4) + " " + label + suffix;
            out.print("\r\u001b[2K" + theme.label(TerminalTheme.Tone.BREAD_GOLD, text)); out.flush();
        }
    }
    public void pause() { synchronized (out) { paused = true; if (timer != null && active()) { out.print("\r\u001b[2K"); out.flush(); } } }
    public void resume() { synchronized (out) { paused = false; } }
    public void complete(boolean warnings, String details) { finish(warnings ? State.COMPLETED_WITH_WARNINGS : State.COMPLETED, details); }
    public void failed(String details) { finish(State.FAILED, details); }
    public void cancelled(String details) { finish(State.CANCELLED, details); }
    private void finish(State result, String details) {
        synchronized (out) {
            if (!active()) { return; } state = result;
            if (timer != null) { timer.shutdownNow(); out.print("\r\u001b[2K"); }
            TerminalTheme.Tone tone = result == State.COMPLETED ? TerminalTheme.Tone.SUCCESS : result == State.FAILED ? TerminalTheme.Tone.ERROR : TerminalTheme.Tone.WARNING;
            print("[" + result + "] " + operation.label, tone);
            print(ContentSafety.terminal(details, 1000), null); out.flush();
        }
    }
    private void print(String text, TerminalTheme.Tone tone) {
        while (text.length() > width) {
            int end = width;
            if (Character.isHighSurrogate(text.charAt(end - 1))) { end--; }
            String line = text.substring(0, end); out.println(tone == null ? line : theme.label(tone, line)); text = text.substring(end);
        }
        out.println(tone == null ? text : theme.label(tone, text));
    }
    @Override public void close() { cancelled("Operation closed without a completion result."); }
    public static <T> T run(PrintStream out, TerminalTheme theme, int width, Operation operation, Work<T> work) throws IOException {
        try (OperationFeedback feedback = new OperationFeedback(out, theme, width, operation)) {
            try { T result = work.run(); feedback.complete(false, "Operation completed."); return result; }
            catch (IOException | RuntimeException failure) { feedback.failed("Operation failed. See error details."); throw failure; }
        }
    }
}
