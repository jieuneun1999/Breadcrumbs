package breadcrumbs.cli;

import static breadcrumbs.cli.BreadFrames.*;

public final class BreadGazeTest {
    private BreadGazeTest() { }
    public static void main(String[] args) { run(); }
    public static void run() {
        check(at(Size.REFERENCE, Mood.IDLE, 0).rows().equals(java.util.List.of(ReferenceBreadArt.MARKS)), "Original reference remains exact");
        for (Size size : new Size[] {Size.FULL, Size.MEDIUM, Size.COMPACT}) {
            Frame center = at(size, Mood.IDLE, 0), left = at(size, Mood.IDLE, 0, Gaze.LOOK_LEFT), right = at(size, Mood.IDLE, 0, Gaze.LOOK_RIGHT);
            check(!left.rows().equals(center.rows()) && !right.rows().equals(center.rows()), "Left and right gaze visibly change the face at " + size);
            for (boolean second : new boolean[] {false, true}) {
                double a = eye(left, second), b = eye(center, second), c = eye(right, second);
                check(a < b && b < c, "Both pupils move coherently left/forward/right at " + size);
            }
            for (int y = 0; y < size.height(); y++) {
                if (y < size.height() * 12 / 22 || y >= (size.height() * 14 + 21) / 22) {
                    check(left.rows().get(y).equals(center.rows().get(y)) && right.colors().get(y).equals(center.colors().get(y)), "Gaze preserves body, contour, mouth, paws and shading");
                }
            }
            check(at(size, Mood.IDLE, 19, Gaze.LOOK_LEFT).equals(at(size, Mood.IDLE, 19)), "Blinking never pulls closed eyes apart");
            check(left == at(size, Mood.IDLE, 0, Gaze.LOOK_LEFT), "Directional frame reuse is bounded and immutable");
        }
        check(gazeAt(43, Gaze.LOOK_RIGHT) != gazeAt(43 + 192, Gaze.LOOK_RIGHT), "Bounded cycle variation avoids identical glance timing every loop");
        var directions = java.util.EnumSet.noneOf(Gaze.class);
        for (int tick = 0; tick < 576; tick++) {
            Gaze value = gazeAt(tick, Gaze.LOOK_RIGHT); directions.add(value);
            check(value == gazeAt(tick + 576L, Gaze.LOOK_RIGHT), "Timeline repeats deterministically without uncontrolled randomness");
        }
        check(directions.containsAll(java.util.List.of(Gaze.LOOK_FORWARD, Gaze.LOOK_LEFT, Gaze.LOOK_RIGHT, Gaze.LOOK_SLIGHT_LEFT, Gaze.LOOK_SLIGHT_RIGHT)), "Held idle timeline includes every supported direction");
        System.out.println("PASS: coherent directional gaze, immutable identity and deterministic timeline across active sizes");
    }
    private static double eye(Frame frame, boolean right) {
        int from = frame.width() * (right ? 40 : 20) / 64, to = (frame.width() * (right ? 55 : 35) + 63) / 64;
        long total = 0, weighted = 0;
        for (int y = frame.height() * 12 / 22; y < (frame.height() * 14 + 21) / 22; y++) {
            for (int x = from; x < to; x++) {
                int rgb = frame.colors().get(y).get(x);
                if (rgb < 0) { continue; }
                int light = (((rgb >> 16) & 255) * 3 + ((rgb >> 8) & 255) * 6 + (rgb & 255)) / 10;
                int darkness = Math.max(0, 170 - light); weighted += (long) x * darkness; total += darkness;
            }
        }
        check(total > 0, "Every eye remains visible"); return (double) weighted / total;
    }
    private static void check(boolean value, String message) { if (!value) { throw new AssertionError(message); } }
}
