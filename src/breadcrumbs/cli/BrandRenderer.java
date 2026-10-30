package breadcrumbs.cli;

import java.io.PrintStream;

/** Compact ordinary ASCII survives CMD, redirected logs, and narrow terminal windows. */
public final class BrandRenderer {
    private BrandRenderer() { }
    public static void render(PrintStream out, TerminalTheme theme, int width) {
        out.println();
        out.println(theme.label(TerminalTheme.Tone.BREAD_GOLD,
                width >= 52 ? "o -- . -- o    B R E A D C R U M B S" : "o . o  BREADCRUMBS"));
        if (width >= 52) {
            out.println("Leave context behind. Pick up where work stopped.");
        } else {
            out.println("Leave context behind.");
            out.println("Pick up where work stopped.");
        }
        out.println();
    }
}
