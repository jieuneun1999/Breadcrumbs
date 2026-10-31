package breadcrumbs.cli;

import java.io.PrintStream;

/** Stateless startup presentation. Counts come from storage, never from sample product data. */
final class StartupRenderer {
    private static final String[] ACTIONS = {
        "[1]  Open a saved project",
        "[2]  New project / import a folder",
        "[7]  Continue working on your project",
        "[3]  More project tools / history",
        "[4]  DSA demonstration",
        "[0]  Exit"
    };
    private final PrintStream output;
    private final TerminalTheme theme;
    private final int columns;

    StartupRenderer(PrintStream output, TerminalTheme theme, int width) {
        this.output = output;
        this.theme = theme;
        columns = Math.max(28, width);
    }

    void ready(int saved, int unavailable) {
        line(TerminalTheme.Tone.SUCCESS, "[v] Ready to open or save your projects");
        line(null, "[v] Saved projects: " + saved);
        line(TerminalTheme.Tone.SECONDARY, "[i] You choose whether each project is saved on this computer.");
        if (unavailable > 0) {
            line(TerminalTheme.Tone.WARNING, "WARNING: " + unavailable
                    + " unreadable saved workspace(s), retained for recovery.");
        }
        sourceNotice();
    }

    void unavailable() {
        line(TerminalTheme.Tone.WARNING, "[!] The save folder is unavailable. You can still work here, but save your changes before closing.");
        sourceNotice();
    }

    void menu() {
        output.println();
        if (columns >= 48) {
            int inside = Math.min(columns - 4, theme.richBrandEnabled() && columns >= 160 ? 150 : 88);
            frame("/" + "-".repeat(inside) + "\\");
            for (int index = 0; index < ACTIONS.length; index++) {
                String content = " " + ACTIONS[index];
                String row = content + " ".repeat(inside - content.length() - 2) + "> ";
                // Gold marks the primary action, not a movable selection or arrow-key interaction.
                String action = index == 0 ? theme.label(TerminalTheme.Tone.PRIMARY_ACTION, row) : row;
                output.println("  " + theme.label(TerminalTheme.Tone.BREAD_SHADOW, "|") + action
                        + theme.label(TerminalTheme.Tone.BREAD_SHADOW, "|"));
            }
            frame("\\" + "-".repeat(inside) + "/");
        } else {
            for (String action : ACTIONS) { line(null, action); }
        }
        output.println();
    }

    private void sourceNotice() {
        line(TerminalTheme.Tone.SECONDARY, "[i] Opening a saved project does not check its files for changes.");
    }

    private void frame(String trustedText) {
        output.println("  " + theme.label(TerminalTheme.Tone.BREAD_SHADOW, trustedText));
    }

    /** Wrap trusted UI copy by printable cells before adding ANSI codes. */
    private void line(TerminalTheme.Tone tone, String trustedText) {
        int available = columns - 2;
        String remaining = trustedText;
        while (remaining.length() > available) {
            int end = remaining.lastIndexOf(' ', available);
            if (end < 1) { end = available; }
            write(tone, remaining.substring(0, end));
            remaining = remaining.substring(end).stripLeading();
        }
        write(tone, remaining);
    }

    private void write(TerminalTheme.Tone tone, String trustedText) {
        output.println("  " + (tone == null ? trustedText : theme.label(tone, trustedText)));
    }
}
