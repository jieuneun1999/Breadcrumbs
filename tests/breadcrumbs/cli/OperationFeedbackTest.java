package breadcrumbs.cli;

import java.io.*;
import java.nio.charset.StandardCharsets;
import static breadcrumbs.b4.Checks.*;

public final class OperationFeedbackTest {
    private OperationFeedbackTest() { }
    public static void main(String[] args) throws Exception { run(); }
    public static void run() throws Exception {
        var bytes = new ByteArrayOutputStream(); var out = new PrintStream(bytes, true, StandardCharsets.UTF_8);
        var plain = new TerminalTheme(false);
        try (var feedback = new OperationFeedback(out, plain, 40, OperationFeedback.Operation.INSPECT)) {
            feedback.running(4); feedback.complete(true, "4 entries; 2 metadata-only.");
            require(feedback.state() == OperationFeedback.State.COMPLETED_WITH_WARNINGS, "warnings are not green success");
        }
        String text = bytes.toString(StandardCharsets.UTF_8);
        require(text.contains("STARTED") && text.contains("COMPLETED_WITH_WARNINGS") && !text.contains("\u001b") && !text.replace("\r\n", "\n").contains("\r"), "redirected plain start/final without cursor controls");
        require(text.lines().count() < 6, "running frames do not bloat fallback logs");
        bytes.reset();
        try (var feedback = new OperationFeedback(out, plain, 40, OperationFeedback.Operation.SAVE)) { feedback.complete(false, "Saved only after work completed."); }
        require(bytes.toString(StandardCharsets.UTF_8).contains("COMPLETED"), "normal completion");
        bytes.reset();
        reject(() -> OperationFeedback.run(out, plain, 40, OperationFeedback.Operation.SAVE, () -> { throw new IOException("injected"); }), "operation exception propagated");
        require(bytes.toString(StandardCharsets.UTF_8).contains("FAILED") && !bytes.toString(StandardCharsets.UTF_8).contains("[COMPLETED]"), "exception cleanup cannot invent success");
        bytes.reset(); try (var feedback = new OperationFeedback(out, plain, 40, OperationFeedback.Operation.EXPORT)) { require(feedback.state() == OperationFeedback.State.STARTED, "started state before completion"); }
        require(bytes.toString(StandardCharsets.UTF_8).contains("CANCELLED"), "uncompleted feedback closes as cancelled");
        bytes.reset();
        try (var feedback = new OperationFeedback(out, new TerminalTheme(true), 40, OperationFeedback.Operation.REBUILD)) { feedback.complete(false, "untrusted\u001b[31mvalue"); }
        require(!bytes.toString(StandardCharsets.UTF_8).contains("untrusted\u001b[31m"), "untrusted details cannot inject ANSI");
        require(!new TerminalTheme(false, false, true).animationsEnabled(), "no-color disables animation");
        for (var tone : TerminalTheme.Tone.values()) { require(!plain.label(tone, "label").contains("\u001b") && new TerminalTheme(true).label(tone, "label").endsWith("\u001b[0m"), "theme tone reset and plain fallback"); }
        require(!TerminalTheme.enabled(false, true, true, true, "xterm", true, true), "NO_COLOR environment wins");
        bytes.reset();
        try (var feedback = new OperationFeedback(out, new TerminalTheme(true, false, true), 40, OperationFeedback.Operation.INSPECT)) {
            feedback.running(7);
            long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(2);
            while (!bytes.toString(StandardCharsets.UTF_8).contains("\r\u001b[2K") && System.nanoTime() < deadline) { Thread.sleep(10); }
            require(bytes.toString(StandardCharsets.UTF_8).contains("7 items visited"), "capable terminal redraw uses real counters");
            feedback.complete(false, "7 items observed.");
        }
        int finishedLength = bytes.size();
        long cleanupDeadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(2);
        while (Thread.getAllStackTraces().keySet().stream().anyMatch(thread -> thread.getName().equals("breadcrumbs-operation-feedback")) && System.nanoTime() < cleanupDeadline) { Thread.sleep(10); }
        require(bytes.size() == finishedLength && Thread.getAllStackTraces().keySet().stream().noneMatch(thread -> thread.getName().equals("breadcrumbs-operation-feedback")), "spinner stops before subsequent input and leaves no orphan thread");
        System.out.println("PASS: B7 terminal feedback states, plain output, safety and cleanup");
    }
}
