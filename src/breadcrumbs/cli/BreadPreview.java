package breadcrumbs.cli;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;

/** Uses the exact production frames without opening storage or creating sample progress. */
public final class BreadPreview {
    private BreadPreview() { }

    public static void run(PrintStream out, Reader input, TerminalTheme theme, int width, String mode) throws IOException {
        if (!java.util.Set.of("all", "full", "compact", "idle").contains(mode)) { throw new IllegalArgumentException("Unknown bread preview mode"); }
        out.println("Bread Support preview. Character artwork only. No project data is opened.");
        if (mode.equals("idle")) {
            new TerminalOutput(out, theme, width).screen(BreadFrames.Mood.IDLE,
                    "Bread Support. Type 0 and press Enter to leave this preview.", "[0] Exit preview", true);
            if (theme.companion() != null && theme.companion().controlledInput()) {
                new TerminalOutput(out, theme, width).prompt("Preview > ");
                theme.companion().readLine(input, theme, "Preview > ", 32);
            } else { out.println("Static fallback. Continuous input animation requires a supported interactive console."); }
            return;
        }
        for (var size : BreadFrames.Size.values()) {
            if (mode.equals("full") && size != BreadFrames.Size.FULL || mode.equals("compact") && size != BreadFrames.Size.COMPACT) { continue; }
            for (var mood : BreadFrames.Mood.values()) {
                out.println("\n" + size + " / " + mood + " / " + size.width() + " x " + size.height());
                for (String line : BreadFrames.at(size, mood, 0).render(theme)) { out.println(line); }
            }
            if (mode.equals("all")) {
                int[] ticks = {19, 54, 86}; String[] names = {"BLINK", "PAW / WAVE", "SMILE / CRUMBS"};
                for (int i = 0; i < ticks.length; i++) {
                    out.println("\n" + size + " / " + names[i]);
                    for (String line : BreadFrames.at(size, BreadFrames.Mood.WELCOME, ticks[i]).render(theme)) { out.println(line); }
                }
            }
        }
    }
}
