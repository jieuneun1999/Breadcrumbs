package breadcrumbs.cli;

import breadcrumbs.storage.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import static breadcrumbs.b4.Checks.*;

public final class CalmConsoleTest {
    private CalmConsoleTest() { }
    public static void main(String[] args) throws Exception { run(); }
    public static void run() throws Exception {
        Path base = Files.createTempDirectory(Files.createDirectories(Path.of("build/b7-fixtures")), "calm-cli-").toAbsolutePath();
        var store = new WorkspaceStore(base.resolve("app")); store.save(ContinuityTest.empty("W752", "Spacing check"));
        var bytes = new ByteArrayOutputStream(); var out = new PrintStream(bytes, true, StandardCharsets.UTF_8);
        new ProjectConsole(new StringReader("1\n1\n7\n1\n1\n0\n0\n"), out, store, new TerminalTheme(false), 80).run();
        String text = bytes.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
        require(occurrences(text, "[11] State / handoff documents") == 1, "daily full menu appears once rather than after every operation");
        require(occurrences(text, "[4]  DSA demonstration") == 1, "home full menu appears once per entry rather than after every action");
        require(text.contains("?=menu") && text.contains("\n\nNEXT ACTION"), "short action reminder and spaced report sections remain discoverable");
        bytes.reset();
        BrandRenderer.renderApproved(out, new TerminalTheme(true, true, true), 160);
        String animated = bytes.toString(StandardCharsets.UTF_8);
        require(occurrences(animated, "\u001b[22A") == 1 && !animated.contains("\u001b[2J"), "brand intro replaces one grouped loaf frame with one complete mark; never line-by-line or whole-screen clears");
        require(animated.endsWith(System.lineSeparator() + System.lineSeparator()), "brand leaves one static header with spacing");
        System.out.println("PASS: B7E compact repeated navigation, report spacing and two-part brand intro");
    }
    private static int occurrences(String text, String part) { int n = 0, at = 0; while ((at = text.indexOf(part, at)) >= 0) { n++; at += part.length(); } return n; }
}
