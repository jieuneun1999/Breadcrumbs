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
        out.println(); styled(trustedLabel, TerminalTheme.Tone.BREAD_LIGHT); plain(ContentSafety.terminal(value, 5000));
    }
    void line(String trustedLabel, String value) {
        String neutral = ContentSafety.terminal(value, 5000);
        if (trustedLabel.length() + neutral.length() <= width) { out.println(theme.label(TerminalTheme.Tone.BREAD_LIGHT, trustedLabel) + neutral); }
        else { value(trustedLabel, neutral); }
    }
    void plain(String value) { for (String line : value.split("\n", -1)) { styled(ContentSafety.terminal(line, 5000), null); } }
    void menu(String trustedLines) {
        for (String line : trustedLines.split("\n")) {
            int end = line.indexOf(']');
            if (line.startsWith("[") && end > 0 && optionText(line).length() <= width) {
                option(line); out.println();
            } else { styled(optionText(line), TerminalTheme.Tone.BREAD_GOLD); }
        }
    }
    void choices(String trustedLines) {
        String[] lines = trustedLines.lines().filter(line -> !line.isBlank()).toArray(String[]::new);
        int column = (width - 4) / 2;
        if (width < 80 || java.util.Arrays.stream(lines).anyMatch(line -> optionText(line).length() > column)) {
            menu(trustedLines); return;
        }
        for (int i = 0; i < lines.length; i += 2) {
            option(lines[i]);
            if (i + 1 < lines.length) { out.print(" ".repeat(column - optionText(lines[i]).length() + 4)); option(lines[i + 1]); }
            out.println();
        }
    }
    private void option(String line) {
        String text = optionText(line); int end = text.indexOf(']');
        if (text.startsWith("[") && end > 0) { out.print(theme.label(TerminalTheme.Tone.BREAD_GOLD, text.substring(0, end + 1)) + text.substring(end + 1)); }
        else { out.print(line); }
    }
    private String optionText(String line) {
        int end = line.indexOf(']');
        if (!line.startsWith("[") || end < 1) { return line; }
        String key = line.substring(0, end + 1);
        return key + " ".repeat(Math.max(1, 6 - key.length())) + line.substring(end + 1).stripLeading();
    }
    void prompt(String trustedText) {
        out.println();
        String remaining = trustedText;
        while (remaining.length() > width) {
            int end = wrapAt(remaining);
            out.println(theme.label(TerminalTheme.Tone.BREAD_GOLD, remaining.substring(0, end)));
            remaining = remaining.substring(end + (remaining.charAt(end) == ' ' ? 1 : 0));
        }
        out.print(theme.label(TerminalTheme.Tone.BREAD_GOLD, remaining)); out.flush();
    }
    private int wrapAt(String text) {
        int end = text.lastIndexOf(' ', width);
        if (end < 1) { end = width; }
        if (Character.isHighSurrogate(text.charAt(end - 1))) { end--; }
        return end;
    }
    private void styled(String text, TerminalTheme.Tone tone) {
        for (String line : text.split("\n", -1)) {
            while (line.length() > width) {
                int end = wrapAt(line);
                String segment = line.substring(0, end); out.println(tone == null ? segment : theme.label(tone, segment)); line = line.substring(end + (line.charAt(end) == ' ' ? 1 : 0));
            }
            out.println(tone == null ? line : theme.label(tone, line));
        }
    }
}
