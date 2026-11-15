package breadcrumbs.cli;

import java.io.PrintStream;

/** Selects a bounded text-only splash; ordinary terminals keep the complete ASCII fallback. */
public final class BrandRenderer {
    private static final String[] LOAF = {
        "     .-~~~~-.",
        "   .' / / /  '.",
        "  / .-====-.  _)",
        " | :######:  (_  .",
        " | :######:   _)   .",
        "  '---------'  .  ."
    };
    private static final String[] WORDMARK = {
        " _   _   _   _   _   _   _            _   __",
        "|_) |_) |_  |_| | \\ /   |_) | | |\\/| |_) (_",
        "|_) | \\ |_  | | |_/ \\_  | \\ |_| |  | |_) __)"
    };
    private static final String SIGNATURE = "Breadcrumbs / project continuity";

    private BrandRenderer() { }

    /** Production branding uses only the approved B3 artwork; small terminals get text, never alternate art. */
    public static void renderApproved(PrintStream out, TerminalTheme theme, int width) {
        BreadSupport bread = theme.companion();
        if (bread != null) { bread.beginIntro(); }
        try { approvedFrame(out, theme, width); }
        finally { if (bread != null) { bread.endIntro(); } }
    }

    private static void approvedFrame(PrintStream out, TerminalTheme theme, int width) {
        int columns = Math.max(28, width);
        out.println();
        if (columns >= 160) {
            if (theme.animationsEnabled()) {
                out.print(RichBrandRenderer.frame(theme, true)); out.flush();
                try { Thread.sleep(120); }
                catch (InterruptedException stopped) { Thread.currentThread().interrupt(); }
                out.print("\u001b[" + ReferenceBrandArt.MARKS.length + "A\r");
            }
            RichBrandRenderer.render(out, theme); out.flush();
        }
        else {
            out.println("  " + theme.label(TerminalTheme.Tone.BREAD_GOLD, "BREADCRUMBS"));
            if (columns >= 36) { out.println("  " + theme.label(TerminalTheme.Tone.SECONDARY, SIGNATURE)); }
            out.println("  Leave context behind." + (columns >= 51 ? " Pick up where work stopped." : ""));
            if (columns < 51) { out.println("  Pick up where work stopped."); }
            out.println("  " + theme.label(TerminalTheme.Tone.SECONDARY, "Logo: use COLUMNS=160."));
        }
        out.println();
    }

    /** Historical presentation contract retained for the original B3 fixture suite. Production uses renderApproved. */
    public static void render(PrintStream out, TerminalTheme theme, int width) {
        int columns = Math.max(28, width);
        out.println();
        if (theme.richBrandEnabled() && columns >= 160) {
            RichBrandRenderer.render(out, theme);
            out.println();
            return;
        }
        if (columns >= 80) {
            // Fixed cell padding keeps both pieces aligned even when SGR adds nonprinting bytes.
            for (int row = 0; row < LOAF.length; row++) {
                String icon = "  " + loafRow(theme, LOAF[row]);
                String gap = " ".repeat(22 - LOAF[row].length());
                String lettering = row == 0 ? theme.label(TerminalTheme.Tone.SECONDARY, SIGNATURE)
                        : row >= 2 && row <= 4 ? theme.label(TerminalTheme.Tone.BREAD_GOLD, WORDMARK[row - 2]) : "";
                out.println((icon + gap + lettering).stripTrailing());
            }
        } else if (columns >= 48) {
            gold(out, theme, "   .-~~-.");
            gold(out, theme, " /      _)  .");
            gold(out, theme, " '-----'  .  .");
            for (String line : WORDMARK) { gold(out, theme, line); }
            out.println("  " + theme.label(TerminalTheme.Tone.SECONDARY, SIGNATURE));
        } else {
            gold(out, theme, "   .-~~-.");
            gold(out, theme, " /      _)  BREADCRUMBS");
            gold(out, theme, " '-----'  .  .");
        }
        if (columns >= "  Leave context behind. Pick up where work stopped.".length()) {
            out.println("  Leave context behind. Pick up where work stopped.");
        } else {
            out.println("  Leave context behind.");
            if (columns >= "  Pick up where work stopped.".length()) {
                out.println("  Pick up where work stopped.");
            } else {
                out.println("  Pick up where work");
                out.println("  stopped.");
            }
        }
        out.println();
    }

    private static void gold(PrintStream out, TerminalTheme theme, String line) {
        out.println("  " + theme.label(TerminalTheme.Tone.BREAD_GOLD, line));
    }

    private static String loafRow(TerminalTheme theme, String trustedArt) {
        int crumbStart = trustedArt.indexOf('#');
        if (crumbStart < 0) { return theme.label(TerminalTheme.Tone.BREAD_GOLD, trustedArt); }
        int crumbEnd = trustedArt.lastIndexOf('#') + 1;
        return theme.label(TerminalTheme.Tone.BREAD_GOLD, trustedArt.substring(0, crumbStart))
                + theme.label(TerminalTheme.Tone.BREAD_LIGHT, trustedArt.substring(crumbStart, crumbEnd))
                + theme.label(TerminalTheme.Tone.BREAD_GOLD, trustedArt.substring(crumbEnd));
    }
}
