package breadcrumbs.cli;

import breadcrumbs.storage.*;
import breadcrumbs.domain.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import static breadcrumbs.b4.Checks.*;

public final class TerminalConsistencyTest {
    private TerminalConsistencyTest() { }
    public static void main(String[] args) throws Exception { run(); }
    public static void run() throws Exception {
        for (boolean color : new boolean[] {false, true}) {
            var actual = new ByteArrayOutputStream(); var expected = new ByteArrayOutputStream();
            var theme = new TerminalTheme(color);
            BrandRenderer.renderApproved(new PrintStream(actual, true, StandardCharsets.UTF_8), theme, 160);
            RichBrandRenderer.render(new PrintStream(expected, true, StandardCharsets.UTF_8), theme);
            require(actual.toString(StandardCharsets.UTF_8).strip().equals(expected.toString(StandardCharsets.UTF_8).strip()), "production logo is exactly the approved B3 transcription, including monochrome");
            actual.reset(); BrandRenderer.renderApproved(new PrintStream(actual, true, StandardCharsets.UTF_8), theme, 30);
            String small = actual.toString(StandardCharsets.UTF_8);
            require(small.contains("BREADCRUMBS") && !small.contains(".-~~-.") && !small.contains("|_)"), "narrow production branding uses text, never a different logo");
        }
        var state = ContinuityTest.empty("W751", "Terminal check");
        for (boolean color : new boolean[] {false, true}) {
            var bytes = new ByteArrayOutputStream(); var out = new PrintStream(bytes, true, StandardCharsets.UTF_8); var theme = new TerminalTheme(color);
            var ui = new TerminalOutput(out, theme, 28); ui.line("Trusted: ", "neutral\u001b[31minjection");
            require(!bytes.toString(StandardCharsets.UTF_8).contains("neutral\u001b"), "untrusted terminal values stay neutral");
            bytes.reset();
            new DependencyConsole(prompt -> "0", out, state, theme, 28).run();
            new HashConsole(prompt -> "0", out, state, theme, 28).run();
            new BaselineRenderer(out, true, theme, 28).baseline(state.activeBaseline(), 0);
            new ContextConsole(prompt -> "", out, state, java.nio.file.Path.of("build/terminal-test-storage"), theme, 28).perform("9");
            String text = bytes.toString(StandardCharsets.UTF_8);
            require(text.contains("PROJECT DEPENDENCIES") && text.contains("SYNTHETIC HASH DEMO") && text.contains("CONTEXT AUDIT"), "advanced workflow screens rendered");
            require(color == text.contains("\u001b["), "same theme reaches all advanced screens");
            for (String line : text.split("\r?\n")) { require(line.replaceAll("\u001b\\[[0-9;]*m", "").length() <= 28, "advanced output fits narrow terminal"); }
        }
        var bytes = new ByteArrayOutputStream(); var out = new PrintStream(bytes, true, StandardCharsets.UTF_8);
        var answers = new ArrayDeque<String>(java.util.List.of("1", "2", "sensitivefixturebody", "Private origin", "", "s", "c", "no", "0"));
        new InboxConsole(prompt -> { require(!prompt.contains("present-context assessment") && !prompt.contains("current description"), "sensitive Inbox body cannot be copied into exportable present-context reviews"); return answers.removeFirst(); }, out, state, () -> { }, new TerminalTheme(false), 80).run();
        require(state.manualContext().captures().length == 1 && state.reviews().length == 0 && !new breadcrumbs.handoff.HandoffService().preview(state, null, false).text().contains("sensitivefixturebody"), "sensitive manual capture remains isolated from handoff bodies");
        System.out.println("PASS: B7E shared terminal styles, narrow fallback and sensitive Inbox isolation");
    }
}
