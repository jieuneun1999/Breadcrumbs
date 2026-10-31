package breadcrumbs.cli;

import breadcrumbs.inspector.ContentSafety;
import java.io.PrintStream;

/** Fixed UI labels receive theme colors; values are sanitized and retain default foreground. */
final class TerminalOutput {
    private final PrintStream out; private final TerminalTheme theme; private final int width;
    TerminalOutput(PrintStream out, TerminalTheme theme, int width) { this.out = out; this.theme = theme; this.width = Math.max(28, Math.min(width, 120)); }
    void heading(String trusted) { styled(trusted, TerminalTheme.Tone.BREAD_GOLD); }
    void note(String trusted) { styled(trusted, TerminalTheme.Tone.SECONDARY); }
    void warning(String trusted) { styled(trusted, TerminalTheme.Tone.WARNING); }
    void error(String trusted) { styled(trusted, TerminalTheme.Tone.ERROR); }
    void success(String trusted) { styled(trusted, TerminalTheme.Tone.SUCCESS); }
    void value(String trustedLabel, String value) {
        styled(trustedLabel, TerminalTheme.Tone.BREAD_LIGHT); plain(ContentSafety.terminal(value, 5000));
    }
    void plain(String value) { for (String line : value.split("\n", -1)) { styled(ContentSafety.terminal(line, 5000), null); } }
    void menu(String trustedLines) {
        for (String line : trustedLines.split("\n")) {
            int end = line.indexOf(']');
            if (line.startsWith("[") && end > 0 && line.length() <= width) {
                out.println(theme.label(TerminalTheme.Tone.BREAD_GOLD, line.substring(0, end + 1)) + line.substring(end + 1));
            } else { styled(line, TerminalTheme.Tone.BREAD_GOLD); }
        }
    }
    private void styled(String text, TerminalTheme.Tone tone) {
        for (String line : text.split("\n", -1)) {
            while (line.length() > width) {
                int end = width; if (Character.isHighSurrogate(line.charAt(end - 1))) { end--; }
                String segment = line.substring(0, end); out.println(tone == null ? segment : theme.label(tone, segment)); line = line.substring(end);
            }
            out.println(tone == null ? line : theme.label(tone, line));
        }
    }
}
