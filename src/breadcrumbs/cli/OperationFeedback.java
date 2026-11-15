package breadcrumbs.cli;

import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.*;
import breadcrumbs.inspector.ContentSafety;

public final class OperationFeedback implements AutoCloseable {
    private static final String[] PULSE = {"[.  ] ", "[.. ] ", "[...] ", "[ ..] "};
    public enum State { STARTED, RUNNING, COMPLETED, COMPLETED_WITH_WARNINGS, FAILED, CANCELLED }
    public enum Operation {
        INSPECT("Reading your project files", "Project files checked.", "Some files need your attention.", "Your project files could not be checked."),
        RESCAN("Checking your files for changes", "File check finished.", "Some files could not be fully checked.", "Your files could not be checked for changes."),
        REBUILD("Refreshing search", "Search is ready.", "Search needs your attention.", "Search could not be refreshed."),
        SAVE("Saving your project", "Your project is saved on this computer.", "Your project was saved with a warning.", "Your project could not be saved."),
        LOAD("Opening your project", "Your project is open.", "Your project needs your attention.", "Your project could not be opened."),
        RECOVER("Restoring your saved project", "Your saved project has been restored.", "Your project was restored with a warning.", "Your project could not be restored."),
        EXPORT("Preparing your handoff files", "Your handoff files are ready.", "Your handoff files need your attention.", "Your handoff files could not be prepared."),
        AUDIT("Checking your project notes", "Your project notes have been checked.", "Some project notes need your attention.", "Your project notes could not be checked."),
        COMPARE("Comparing the previous and latest file checks", "File changes compared.", "Some file changes need your attention.", "The file checks could not be compared.");
        final String label, completed, warning, failure;
        Operation(String label, String completed, String warning, String failure) { this.label = label; this.completed = completed; this.warning = warning; this.failure = failure; }
    }
    @FunctionalInterface public interface Work<T> { T run() throws IOException; }
    private final PrintStream out;
    private final TerminalTheme theme;
    private final int width;
    private final Operation operation;
    private final ScheduledExecutorService timer;
    private final BreadSupport.Screen breadScreen;
    private final BreadSupport managed;
    private volatile State state = State.STARTED;
    private long count = -1;
    private int frame;
    private boolean paused;
    public OperationFeedback(PrintStream out, TerminalTheme theme, int width, Operation operation) {
        this.out = out; this.theme = theme; this.width = Math.max(28, Math.min(120, width)); this.operation = operation;
        managed = theme.companion() != null && theme.companion().screenBased() ? theme.companion() : null;
        breadScreen = theme.companion() == null ? null : theme.companion().operation(theme, operation.label);
        if (theme.animationsEnabled()) {
            timer = Executors.newSingleThreadScheduledExecutor(task -> { Thread thread = new Thread(task, "breadcrumbs-operation-feedback"); thread.setDaemon(true); return thread; });
            timer.scheduleAtFixedRate(this::redraw, 200, breadScreen == null ? 120 : 250, TimeUnit.MILLISECONDS);
        } else { timer = null; if (managed == null) { print(operation.label + "...", TerminalTheme.Tone.BREAD_GOLD); } }
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
            if (managed != null) {
                managed.feedbackStatus(operation.label + (count >= 0 ? " | " + count + " items checked" : "..."));
                if (breadScreen != null) { breadScreen.tick(); }
                return;
            }
            if (breadScreen != null) { breadScreen.tick(); return; }
            String suffix = count >= 0 ? " | " + count + " items checked" : "";
            if (suffix.length() > width - 6) { suffix = " | " + count; }
            String label = operation.label.substring(0, Math.min(operation.label.length(), Math.max(0, width - suffix.length() - 6)));
            String text = PULSE[frame++ % PULSE.length] + label + suffix;
            out.print("\r\u001b[2K" + theme.label(TerminalTheme.Tone.BREAD_GOLD, text)); out.flush();
        }
    }
    public void pause() { synchronized (out) { paused = true; if (breadScreen != null) { breadScreen.close(); } if (managed != null) { managed.feedbackStatus(""); } else if (breadScreen == null && timer != null && active()) { out.print("\r\u001b[2K"); out.flush(); } } }
    public void resume() { synchronized (out) { paused = false; } }
    public void complete(boolean warnings, String details) { finish(warnings ? State.COMPLETED_WITH_WARNINGS : State.COMPLETED, details); }
    public void failed(String details) { finish(State.FAILED, details); }
    public void cancelled(String details) { finish(State.CANCELLED, details); }
    private void finish(State result, String details) {
        synchronized (out) {
            if (!active()) { return; } state = result;
            if (timer != null) { timer.shutdownNow(); if (managed == null && breadScreen == null) { out.print("\r\u001b[2K"); } }
            if (breadScreen != null) {
                breadScreen.mood(switch (result) {
                    case COMPLETED -> BreadFrames.Mood.SUCCESS;
                    case FAILED -> BreadFrames.Mood.ERROR;
                    case COMPLETED_WITH_WARNINGS -> BreadFrames.Mood.NEEDS_REVIEW;
                    default -> BreadFrames.Mood.ENCOURAGING;
                });
                breadScreen.tick(); breadScreen.close();
            }
            TerminalTheme.Tone tone = result == State.COMPLETED ? TerminalTheme.Tone.SUCCESS : result == State.FAILED ? TerminalTheme.Tone.ERROR : TerminalTheme.Tone.WARNING;
            String message = switch (result) {
                case COMPLETED -> "[Done] " + operation.completed;
                case COMPLETED_WITH_WARNINGS -> "[Needs attention] " + operation.warning;
                case FAILED -> "[Could not finish] " + operation.failure;
                case CANCELLED -> "[Cancelled] The action was stopped.";
                default -> throw new IllegalArgumentException("A final operation result is required.");
            };
            if (managed != null) { managed.feedbackStatus(message); }
            print(message, tone);
            if (!details.isBlank()) { print(ContentSafety.terminal(details, 1000), null); } out.flush();
        }
    }
    private void print(String text, TerminalTheme.Tone tone) {
        while (text.length() > width) {
            int end = text.lastIndexOf(' ', width);
            if (end < 1) { end = width; }
            if (Character.isHighSurrogate(text.charAt(end - 1))) { end--; }
            String line = text.substring(0, end); out.println(tone == null ? line : theme.label(tone, line)); text = text.substring(end).stripLeading();
        }
        out.println(tone == null ? text : theme.label(tone, text));
    }
    @Override public void close() { cancelled("Review your changes before continuing."); }
    public static <T> T run(PrintStream out, TerminalTheme theme, int width, Operation operation, Work<T> work) throws IOException {
        try (OperationFeedback feedback = new OperationFeedback(out, theme, width, operation)) {
            try { T result = work.run(); feedback.complete(false, ""); return result; }
            catch (IOException | RuntimeException failure) { feedback.failed("Try this action again after checking the message below."); throw failure; }
        }
    }
}
