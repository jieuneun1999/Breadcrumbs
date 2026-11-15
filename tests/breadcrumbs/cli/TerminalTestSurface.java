package breadcrumbs.cli;

import java.util.Arrays;

/** Small independent emulator for the ANSI subset emitted by the application. */
final class TerminalTestSurface {
    private final char[][] main, alternate;
    private char[][] cells, lastAlternate;
    private int x, y, savedX, savedY;
    TerminalTestSurface(int width, int height) {
        main = new char[height][width]; alternate = new char[height][width]; cells = main;
        clear(main); clear(alternate);
    }
    static TerminalTestSurface read(String text, int width, int height) {
        var result = new TerminalTestSurface(width, height); result.accept(text); return result;
    }
    void accept(String text) {
        for (int n = 0; n < text.length(); n++) {
            char value = text.charAt(n);
            if (value == 27 && n + 1 < text.length()) {
                char next = text.charAt(++n);
                if (next == '7') { savedX = x; savedY = y; }
                else if (next == '8') { x = savedX; y = savedY; }
                else if (next == '[') {
                    int start = n + 1;
                    while (n + 1 < text.length() && (text.charAt(n + 1) < '@' || text.charAt(n + 1) > '~')) { n++; }
                    if (n + 1 < text.length()) { n++; command(text.substring(start, n), text.charAt(n)); }
                }
            } else if (value == '\r') { x = 0; }
            else if (value == '\n') { y = Math.min(cells.length - 1, y + 1); x = 0; }
            else if (value >= 32 && value < 127) { cells[y][x] = value; x = Math.min(cells[0].length - 1, x + 1); }
        }
    }
    private void command(String parameters, char command) {
        if (parameters.equals("?1049")) {
            if (command == 'h') { cells = alternate; clear(cells); x = 0; y = 0; }
            else if (command == 'l') { lastAlternate = Arrays.stream(alternate).map(char[]::clone).toArray(char[][]::new); cells = main; x = 0; y = 0; }
            return;
        }
        if (parameters.startsWith("?")) { return; }
        String[] fields = parameters.split(";");
        int a = fields[0].isEmpty() ? 1 : Integer.parseInt(fields[0]);
        int b = fields.length < 2 || fields[1].isEmpty() ? 1 : Integer.parseInt(fields[1]);
        switch (command) {
            case 'H', 'f' -> { y = clamp(a - 1, cells.length); x = clamp(b - 1, cells[0].length); }
            case 'A' -> y = Math.max(0, y - a);
            case 'B' -> y = clamp(y + a, cells.length);
            case 'C' -> x = clamp(x + a, cells[0].length);
            case 'D' -> x = Math.max(0, x - a);
            case 'G' -> x = clamp(a - 1, cells[0].length);
            case 'J' -> { if (a == 2 || a == 3) { clear(cells); } }
            case 'K' -> { if (a == 2) { Arrays.fill(cells[y], ' '); } else if (parameters.isEmpty() || a == 0) { Arrays.fill(cells[y], x, cells[y].length, ' '); } }
            default -> { }
        }
    }
    private static int clamp(int value, int size) { return Math.max(0, Math.min(size - 1, value)); }
    private static void clear(char[][] buffer) { for (char[] row : buffer) { Arrays.fill(row, ' '); } }
    int cursorRow() { return y; }
    String line(int row) { return new String((lastAlternate == null ? cells : lastAlternate)[row]); }
    String text() {
        StringBuilder text = new StringBuilder();
        for (char[] row : lastAlternate == null ? cells : lastAlternate) { text.append(row).append('\n'); }
        return text.toString();
    }
}
