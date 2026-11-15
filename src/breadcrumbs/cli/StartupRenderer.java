package breadcrumbs.cli;

import java.io.PrintStream;

/** Stateless startup presentation. Counts come from storage, never from sample product data. */
final class StartupRenderer {
    private static final String[] ACTIONS = {
        "[1]  Open saved project",
        "[2]  Create or import project",
        "[3]  Open advanced project tools",
        "[4]  Run DSA demonstration",
        "[5]  Restore saved project",
        "[6]  Switch to session-only mode",
        "[7]  Open project workspace",
        "[8]  Save active project",
        "[0]  Exit",
        "[?]  Show menu"
    };
    private final PrintStream output;
    private final TerminalTheme theme;
    private final int columns;
    private int saved, unavailable;
    private boolean storageAvailable = true;

    StartupRenderer(PrintStream output, TerminalTheme theme, int width) {
        this.output = output;
        this.theme = theme;
        columns = Math.max(28, width);
    }

    void ready(int saved, int unavailable) {
        this.saved = saved; this.unavailable = unavailable; storageAvailable = true;
        if (theme.companion() != null && theme.companion().screenBased()) { return; }
        line(TerminalTheme.Tone.SUCCESS, "[v] Project storage available");
        line(null, "[v] Saved projects: " + saved);
        if (saved == 0) {
            line(TerminalTheme.Tone.BREAD_LIGHT, "Welcome to Breadcrumbs. No saved projects yet.");
            line(null, "Understand recorded project context, remember progress, and continue later.");
            line(null, "Choose 2 to import a folder or start a project without files.");
        }
        line(TerminalTheme.Tone.SECONDARY, "[i] Local saving is optional for each project.");
        if (unavailable > 0) {
            line(TerminalTheme.Tone.WARNING, "WARNING: " + unavailable
                    + " unreadable saved workspace(s), retained for recovery.");
        }
        sourceNotice();
    }

    void unavailable() {
        storageAvailable = false;
        if (theme.companion() != null && theme.companion().screenBased()) { return; }
        line(TerminalTheme.Tone.WARNING, "[!] The save folder is unavailable. You can still work here, but save your changes before closing.");
        sourceNotice();
    }

    void menu() {
        if (theme.companion() != null && theme.companion().screenBased()) {
            var ui = new TerminalOutput(output, theme, columns); ui.view(ScreenId.HOME);
            if (!storageAvailable) { ui.error("The save folder is unavailable. Keep changes open until you can save them."); }
            else {
                ui.line("Saved projects: ", Integer.toString(saved));
                if (unavailable > 0) { ui.warning(unavailable + " unreadable saved workspace(s), retained for recovery."); }
            }
            ui.plain(saved == 0 ? "Welcome to Breadcrumbs. No saved projects yet." : "Open a saved project to continue where you left off.");
            ui.note("Opening saved context does not check its files for changes. Local saving is optional.");
            ui.screen(BreadFrames.Mood.WELCOME,
                    saved == 0 ? "Keep project context, record progress and return to a clear next action." : "Your recorded context is ready when you are.",
                    String.join("\n", ACTIONS), true);
            return;
        }
        if (theme.companion() != null) {
            new TerminalOutput(output, theme, columns).screen(BreadFrames.Mood.WELCOME,
                    "What would you like to do?", String.join("\n", ACTIONS), true);
            return;
        }
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

    void compactMenu() {
        if (theme.companion() != null && theme.companion().screenBased()) { menu(); return; }
        new TerminalOutput(output, theme, columns).resultScreen(BreadFrames.Mood.IDLE,
                "Your project context is here when you need it.", String.join("\n", ACTIONS));
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
