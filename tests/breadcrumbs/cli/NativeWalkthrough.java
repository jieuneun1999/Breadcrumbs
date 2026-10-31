package breadcrumbs.cli;

import breadcrumbs.storage.WorkspaceStore;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;

/** Developer capture harness: production console, real native output, visibly scripted Reader. */
public final class NativeWalkthrough {
    private NativeWalkthrough() { }
    public static void main(String[] args) throws Exception {
        if (args.length != 3 || System.console() == null || !"1".equals(System.getenv("BREADCRUMBS_NATIVE_VT"))) {
            throw new IllegalArgumentException("Capture requires a real console with verified process-local VT, input file, storage and log paths.");
        }
        String[] answers = Files.readString(Path.of(args[0]), StandardCharsets.UTF_8).split("\n", -1);
        try (var log = Files.newOutputStream(Path.of(args[2]), StandardOpenOption.CREATE_NEW)) {
            var output = new PrintStream(new OutputStream() {
                @Override public void write(int value) throws IOException { System.out.write(value); log.write(value); }
                @Override public void write(byte[] bytes, int offset, int length) throws IOException { System.out.write(bytes, offset, length); log.write(bytes, offset, length); }
                @Override public void flush() throws IOException { System.out.flush(); log.flush(); }
            }, true, StandardCharsets.UTF_8);
            Reader input = new Reader() {
                int line, column;
                @Override public int read(char[] buffer, int offset, int length) throws IOException {
                    if (length == 0) { return 0; }
                    if (line >= answers.length - 1) { return -1; }
                    if (column == 0) {
                        try { Thread.sleep(line == 0 ? 6000 : answers[line].equals("yes") ? 2200 : 1000); }
                        catch (InterruptedException stopped) { Thread.currentThread().interrupt(); throw new InterruptedIOException("Capture input interrupted."); }
                        output.println(answers[line]);
                    }
                    String text = answers[line] + "\n"; buffer[offset] = text.charAt(column++);
                    if (column == text.length()) { line++; column = 0; }
                    return 1;
                }
                @Override public void close() { }
            };
            new ProjectConsole(input, output, new WorkspaceStore(Path.of(args[1])), new TerminalTheme(true, true, true), 160).run();
        }
    }
}
