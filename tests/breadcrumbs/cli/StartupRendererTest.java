package breadcrumbs.cli;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/** Observable text, width and ANSI invariants protect basic terminals as well as enhanced ones. */
public final class StartupRendererTest {
    private static int assertions;
    private StartupRendererTest() { }

    public static void run() {
        for (int width : new int[] {28, 30, 40, 47, 48, 49, 56, 71, 72, 80, 100, 120}) {
            String plain = render(width, false, false);
            String color = render(width, true, false);
            require(plain.equals(stripAnsi(color)), "ANSI changes styling, not visible content or alignment");
            require(!plain.contains("\u001b"), "plain mode has no ANSI");
            require(color.contains("\u001b[1;38;5;214m"), "bread-gold branding enabled");
            require(color.contains("\u001b[38;5;245m"), "secondary UI uses centralized muted tone");
            boolean fits = true;
            boolean ascii = true;
            for (String line : plain.split("\n")) {
                fits &= line.length() <= width;
                for (int i = 0; i < line.length(); i++) {
                    ascii &= line.charAt(i) >= 32 && line.charAt(i) <= 126;
                }
            }
            require(fits, "startup respects " + width + " printable columns");
            require(ascii, "brand needs ordinary ASCII only");
            boolean resets = true;
            for (String line : color.split("\n")) {
                if (line.contains("\u001b")) {
                    resets &= line.startsWith("\u001b[0m", line.lastIndexOf("\u001b["));
                }
            }
            require(resets, "each styled line resets the terminal foreground");
            String failure = render(width, false, true);
            require(failure.replaceAll("\\s+", " ").contains("save folder is unavailable"), "failure has a readable status without color");
            boolean failureFits = true;
            for (String line : failure.split("\n")) { failureFits &= line.length() <= width; }
            require(failureFits, "failure notice wraps");
        }
        String wide = render(80, false, false);
        require(wide.contains("|_) |_) |_") && wide.contains("|_) | \\ |_"), "visible custom symbol-built wordmark");
        require(wide.contains(".' / / /  '.") && wide.contains("(_  .") && wide.contains("_)   ."),
                "scored loaf, missing bite and outgoing crumb trail visible");
        require(wide.contains("Saved projects: 2") && wide.contains("1 unreadable saved workspace"),
                "real counts and unavailable records stay explicit");
        require(wide.contains("[1]  Open a saved project") && wide.contains("[4]  DSA demonstration"),
                "keyboard actions remain discoverable");
        require(!wide.contains("Recent project:") && !wide.contains("App storage:"), "startup omits absent recent state and full diagnostic path");
        require(wide.contains("/----------------") && wide.contains("\\----------------"),
                "quiet ASCII menu frame matches the approved reference direction");
        require(render(80, true, false).contains("\u001b[1;38;5;222m")
                && render(80, true, false).contains("\u001b[38;5;130m"), "highlight and crust-shadow tones are centralized");
        for (int width : new int[] {28, 48, 79, 80, 100, 120, 159, 160, 180, 200}) {
            String enhanced = render(width, true, false, true);
            require(render(width, false, false, true).equals(render(width, false, false)),
                    "no-color overrides requested rich art at " + width + " columns");
            if (width < 160) {
                require(enhanced.equals(render(width, true, false)), "narrow rich mode falls back to ASCII");
            } else {
                require(enhanced.contains("\u001b[1;38;2;"), "wide art retains the reference's warm lighting bands");
                require(enhanced.contains("Breadcrumbs / project continuity")
                        && enhanced.contains("Leave context behind.") && enhanced.contains("Pick up where work stopped."),
                        "brand name and tagline remain readable text");
                require(enhanced.contains("Saved projects: 2") && enhanced.contains("[0]  Exit"),
                        "richer branding preserves real status and numbered actions");
                boolean fits = true;
                boolean resets = true;
                for (String line : enhanced.split("\n")) {
                    fits &= stripAnsi(line).length() <= width;
                    if (line.contains("\u001b")) {
                        resets &= line.startsWith("\u001b[0m", line.lastIndexOf("\u001b["));
                    }
                }
                require(fits, "rich art fits " + width + " terminal columns");
                require(resets, "rich foreground and background reset after every line");
                require(!enhanced.contains("\u001b[2J") && !enhanced.contains("\u001bP")
                        && !enhanced.contains("\u001b]"), "rich art uses no screen clear or image protocol");
                require(!enhanced.contains("\u2580") && !enhanced.contains("\u2584")
                        && !enhanced.contains("\u2588"), "wide art avoids filled pixel blocks");
            }
        }
        System.out.println("PASS: terminal presentation (" + assertions + " assertions; ASCII and enhanced layouts)");
    }

    private static String render(int width, boolean color, boolean failure) {
        return render(width, color, failure, false);
    }

    private static String render(int width, boolean color, boolean failure, boolean richBrand) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            TerminalTheme theme = new TerminalTheme(color, richBrand);
            BrandRenderer.render(output, theme, width);
            StartupRenderer startup = new StartupRenderer(output, theme, width);
            if (failure) { startup.unavailable(); } else { startup.ready(2, 1); }
            startup.menu();
        }
        return bytes.toString(StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    private static String stripAnsi(String text) { return text.replaceAll("\u001b\\[[0-9;]*m", ""); }
    private static void require(boolean condition, String message) {
        assertions++; if (!condition) { throw new AssertionError(message); }
    }
}
