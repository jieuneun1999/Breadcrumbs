package breadcrumbs.cli;

import java.util.ArrayList;
import java.util.List;

/** Fixed labels and numbered actions share one bounded ASCII menu frame. */
final class MenuBox {
    private MenuBox() { }
    static boolean fixedCells(String text) {
        return text.chars().allMatch(c -> c == '\n' || c >= 32 && c <= 126);
    }
    static List<String> rows(TerminalTheme theme, int width, String message, String actions) {
        return rows(theme, width, message, actions, false);
    }
    static List<String> rows(TerminalTheme theme, int width, String message, String actions, boolean paired) {
        int inside = width - 4;
        if (inside < 8) { throw new IllegalArgumentException("Menu box is too narrow"); }
        List<String> result = new ArrayList<>();
        result.add(theme.label(TerminalTheme.Tone.BREAD_SHADOW, "/" + "-".repeat(width - 2) + "\\"));
        if (!message.isBlank()) {
            for (String line : wrap(message, inside)) { result.add(row(theme, width, line, theme.label(TerminalTheme.Tone.BREAD_LIGHT, line))); }
            result.add(row(theme, width, "", ""));
        }
        List<String> options = actions.lines().filter(line -> !line.isBlank()).map(MenuBox::normalize).toList();
        int column = (inside - 4) / 2;
        boolean columns = paired && column >= 24 && options.stream().allMatch(line -> line.length() <= column);
        for (int n = 0; n < options.size(); n += columns ? 2 : 1) {
            String label = options.get(n);
            if (columns && n + 1 < options.size()) {
                String gap = " ".repeat(column + 4 - label.length());
                String next = options.get(n + 1);
                result.add(row(theme, width, label + gap + next, style(theme, label) + gap + style(theme, next)));
            } else {
                List<String> lines = wrap(label, inside);
                for (int i = 0; i < lines.size(); i++) {
                    String line = lines.get(i);
                    result.add(row(theme, width, line, i == 0 ? style(theme, line) : line));
                }
            }
        }
        result.add(theme.label(TerminalTheme.Tone.BREAD_SHADOW, "\\" + "-".repeat(width - 2) + "/"));
        return List.copyOf(result);
    }
    private static String style(TerminalTheme theme, String line) {
        int end = line.indexOf(']');
        return line.startsWith("[") && end > 0
                ? theme.label(TerminalTheme.Tone.BREAD_GOLD, line.substring(0, end + 1)) + line.substring(end + 1) : line;
    }
    private static String row(TerminalTheme theme, int width, String plain, String styled) {
        return theme.label(TerminalTheme.Tone.BREAD_SHADOW, "| ") + styled
                + " ".repeat(width - 4 - plain.length()) + theme.label(TerminalTheme.Tone.BREAD_SHADOW, " |");
    }
    private static String normalize(String line) {
        int end = line.indexOf(']');
        if (!line.startsWith("[") || end < 1) { return line; }
        return line.substring(0, end + 1) + " ".repeat(Math.max(1, 5 - end)) + line.substring(end + 1).stripLeading();
    }
    private static List<String> wrap(String value, int width) {
        List<String> result = new ArrayList<>();
        while (value.length() > width) {
            int cut = value.lastIndexOf(' ', width);
            if (cut < 1) { cut = width; }
            if (Character.isHighSurrogate(value.charAt(cut - 1))) { cut--; }
            result.add(value.substring(0, cut)); value = value.substring(cut).stripLeading();
        }
        result.add(value); return List.copyOf(result);
    }
}
