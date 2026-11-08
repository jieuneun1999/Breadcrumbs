package breadcrumbs.cli;

import java.util.ArrayList;
import java.util.List;

/** Fixed visible-symbol assets. No blocks, background fills or image protocols. */
public final class BreadFrames {
    public enum Size {
        FULL(64, 22), COMPACT(28, 10);
        private final int width, height;
        Size(int width, int height) { this.width = width; this.height = height; }
        public int width() { return width; }
        public int height() { return height; }
    }
    public enum Mood { IDLE, WELCOME, WORKING, ENCOURAGING, SUCCESS, NEEDS_REVIEW, ERROR, GOODBYE, CURIOUS }
    public record Frame(List<String> rows, List<String> inks, List<List<Integer>> colors) {
        public Frame(List<String> rows, List<String> inks) { this(rows, inks, List.of()); }
        public Frame {
            rows = List.copyOf(rows); inks = List.copyOf(inks);
            colors = colors.stream().map(List::copyOf).toList();
            if (rows.isEmpty() || rows.size() != inks.size()) { throw new IllegalArgumentException("Frame rows required"); }
            int width = rows.get(0).length();
            for (int y = 0; y < rows.size(); y++) {
                if (rows.get(y).length() != width || inks.get(y).length() != width
                        || rows.get(y).chars().anyMatch(c -> c < 32 || c > 126)
                        || inks.get(y).chars().anyMatch(c -> c < '0' || c > '8')) {
                    throw new IllegalArgumentException("Every frame uses one fixed ASCII cell grid");
                }
            }
            if (!colors.isEmpty()) {
                if (colors.size() != rows.size()) { throw new IllegalArgumentException("Color row count"); }
                for (var row : colors) {
                    if (row.size() != width || row.stream().anyMatch(c -> c < -1 || c > 0xffffff)) { throw new IllegalArgumentException("Color cell dimensions"); }
                }
            }
        }
        public int width() { return rows.get(0).length(); }
        public int height() { return rows.size(); }
        public List<String> render(TerminalTheme theme) {
            List<String> result = new ArrayList<>();
            for (int y = 0; y < height(); y++) {
                StringBuilder line = new StringBuilder();
                for (int x = 0; x < width();) {
                    int rgb = colorAt(y, x), end = x + 1;
                    while (end < width() && colorAt(y, end) == rgb) { end++; }
                    String text = rows.get(y).substring(x, end);
                    line.append(rgb < 0 ? text : theme.companionText(rgb, text));
                    x = end;
                }
                result.add(line.toString());
            }
            return List.copyOf(result);
        }
        private int colorAt(int y, int x) {
            if (rows.get(y).charAt(x) == ' ') { return -1; }
            return colors.isEmpty() ? ASCII_PALETTE[inks.get(y).charAt(x) - '0'] : colors.get(y).get(x);
        }
    }
    private static final int[] ASCII_PALETTE = {0, 0x754122, 0xb27030, 0xdb962e, 0xf6bc51,
            0xffdfa0, 0xffefbf, 0xe99591, 0x4f291d};
    private static final Frame[][][] CACHE = build();
    private BreadFrames() { }
    public static Frame at(Size size, Mood mood, long tick) {
        int phase = Math.floorMod(tick, 96);
        int pose = phase == 18 || phase == 20 || phase == 70 || phase == 72 ? 1
                : phase == 19 || phase == 71 ? 2
                : phase >= 36 && phase <= 39 ? 3
                : phase >= 52 && phase <= 53 || phase >= 56 && phase <= 57 ? 4
                : phase >= 54 && phase <= 55 ? 5
                : phase >= 84 && phase <= 85 ? 6
                : phase >= 86 && phase <= 87 ? 7 : 0;
        if (mood == Mood.WELCOME && (phase >= 8 && phase <= 13 || phase >= 60 && phase <= 65)) {
            pose = phase % 6 < 2 ? 4 : phase % 6 < 4 ? 5 : 4;
        } else if (mood == Mood.WORKING) {
            pose = phase == 18 || phase == 70 ? 1 : phase == 19 || phase == 71 ? 2 : (phase / 8) % 2 == 0 ? 0 : 3;
        } else if (mood == Mood.NEEDS_REVIEW || mood == Mood.CURIOUS) {
            pose = phase == 18 || phase == 70 ? 1 : phase == 19 || phase == 71 ? 2 : phase >= 40 && phase <= 47 ? 3 : 0;
        }
        return CACHE[size.ordinal()][mood.ordinal()][pose];
    }
    private static Frame[][][] build() {
        Frame[][][] frames = new Frame[Size.values().length][Mood.values().length][8];
        for (Size size : Size.values()) {
            for (Mood mood : Mood.values()) {
                for (int pose = 0; pose < 8; pose++) { frames[size.ordinal()][mood.ordinal()][pose] = BreadPortrait.draw(size, mood, pose); }
            }
        }
        return frames;
    }
}
