package breadcrumbs.cli;

import java.util.ArrayList;
import java.util.List;

/** Fixed visible-symbol assets. No blocks, background fills or image protocols. */
public final class BreadFrames {
    public enum Size {
        REFERENCE(64, 22), FULL(56, 20), MEDIUM(48, 17), COMPACT(40, 14);
        private final int width, height;
        Size(int width, int height) { this.width = width; this.height = height; }
        public int width() { return width; }
        public int height() { return height; }
    }
    public enum Mood { IDLE, WELCOME, WORKING, ENCOURAGING, SUCCESS, NEEDS_REVIEW, ERROR, GOODBYE, CURIOUS }
    public enum Gaze { LOOK_FORWARD, LOOK_SLIGHT_LEFT, LOOK_LEFT, LOOK_SLIGHT_RIGHT, LOOK_RIGHT }
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
    private static final Frame[][][][] GAZE_CACHE = new Frame[Size.values().length][Mood.values().length][8][Gaze.values().length];
    private BreadFrames() { }
    public static Frame at(Size size, Mood mood, long tick) {
        return CACHE[size.ordinal()][mood.ordinal()][pose(mood, tick)];
    }
    public static synchronized Frame at(Size size, Mood mood, long tick, Gaze gaze) {
        if (gaze == Gaze.LOOK_FORWARD) { return at(size, mood, tick); }
        int pose = pose(mood, tick);
        Frame[] variants = GAZE_CACHE[size.ordinal()][mood.ordinal()][pose];
        if (variants[gaze.ordinal()] == null) { variants[gaze.ordinal()] = BreadPortrait.draw(size, mood, pose, gaze); }
        return variants[gaze.ordinal()];
    }
    public static Gaze gazeAt(long tick, Gaze attention) {
        int phase = Math.floorMod(tick, 192);
        int cycle = (int) Math.floorMod(Math.floorDiv(tick, 192), 3L);
        int left = cycle == 1 ? 48 : cycle == 2 ? 36 : 40;
        int right = cycle == 1 ? 114 : cycle == 2 ? 134 : 126;
        int hold = cycle == 1 ? 16 : cycle == 2 ? 12 : 14;
        if (phase < 8 || phase >= 168 && phase < 176) {
            int step = phase < 8 ? phase : phase - 168;
            return step < 2 || step >= 6 ? slight(attention) : attention;
        }
        if (phase >= left && phase < left + hold) { return phase < left + 2 || phase >= left + hold - 2 ? Gaze.LOOK_SLIGHT_LEFT : Gaze.LOOK_LEFT; }
        if (phase >= right && phase < right + hold) { return phase < right + 2 || phase >= right + hold - 2 ? Gaze.LOOK_SLIGHT_RIGHT : Gaze.LOOK_RIGHT; }
        return Gaze.LOOK_FORWARD;
    }
    private static Gaze slight(Gaze value) {
        return value == Gaze.LOOK_LEFT ? Gaze.LOOK_SLIGHT_LEFT : value == Gaze.LOOK_RIGHT ? Gaze.LOOK_SLIGHT_RIGHT : Gaze.LOOK_FORWARD;
    }
    private static int pose(Mood mood, long tick) {
        int phase = Math.floorMod(tick, 96);
        int pose = phase == 18 || phase == 20 || phase == 70 || phase == 72 ? 1
                : phase == 19 || phase == 71 ? 2
                : phase >= 36 && phase <= 39 ? 3
                : phase >= 52 && phase <= 53 || phase >= 56 && phase <= 57 ? 4
                : phase >= 54 && phase <= 55 ? 5
                : phase >= 84 && phase <= 85 ? 6
                : phase >= 86 && phase <= 87 ? 7 : (phase / 3) % 4 == 1 ? 3 : (phase / 3) % 4 == 3 ? 6 : 0;
        if (mood == Mood.WELCOME && (phase >= 8 && phase <= 13 || phase >= 60 && phase <= 65)) {
            pose = phase % 6 < 2 ? 4 : phase % 6 < 4 ? 5 : 4;
        } else if (mood == Mood.WORKING) {
            pose = phase == 18 || phase == 70 ? 1 : phase == 19 || phase == 71 ? 2 : (phase / 8) % 2 == 0 ? 0 : 3;
        } else if (mood == Mood.NEEDS_REVIEW || mood == Mood.CURIOUS) {
            pose = phase == 18 || phase == 70 ? 1 : phase == 19 || phase == 71 ? 2 : phase >= 40 && phase <= 47 ? 3 : 0;
        }
        return pose;
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
