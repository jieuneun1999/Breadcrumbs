package breadcrumbs.cli;

import java.util.ArrayList;
import java.util.List;
import static breadcrumbs.cli.BreadFrames.*;

/** Every viewport scales the same approved full bread, lighting and facial identity. */
final class BreadPortrait {
    private record Features(int leftEye, int rightEye, int eyeY, int eyeWidth,
                            int mouthX, int mouthY, int mouthWidth, int mouthHeight,
                            int pawX, int pawY, int pawWidth, int pawHeight) { }
    private static final Features FULL = new Features(23, 43, 12, 9, 33, 14, 7, 3, 44, 17, 13, 5);
    private static final int[] ACCENTS = {0, 0x83512c, 0xa2632b, 0xbc7830, 0xf4c25c,
            0xffe9b8, 0xfff0ce, 0xe49b9a, 0x9b6b3d};
    private BreadPortrait() { }

    static Frame draw(Size size, Mood mood, int pose) {
        return draw(size, mood, pose, Gaze.LOOK_FORWARD);
    }
    static Frame draw(Size size, Mood mood, int pose, Gaze gaze) {
        String[] marks = ReferenceBreadArt.MARKS;
        String[] bands = ReferenceBreadArt.COLORS;
        Features f = FULL;
        List<String> rows = new ArrayList<>(), inks = new ArrayList<>();
        List<List<Integer>> colors = new ArrayList<>();
        for (int y = 0; y < marks.length; y++) {
            String row = marks[y]; StringBuilder ink = new StringBuilder();
            List<Integer> paint = new ArrayList<>();
            for (int x = 0; x < row.length(); x++) {
                int band = Character.digit(bands[y].charAt(x), 16);
                ink.append(row.charAt(x) == ' ' ? '0' : '3');
                paint.add(band == 0 ? -1 : ReferenceBreadArt.PALETTE[band]);
            }
            rows.add(row); inks.add(ink.toString()); colors.add(paint);
        }
        if (pose == 1 || pose == 2) {
            for (int x : new int[]{f.leftEye(), f.rightEye()}) {
                int height = 2;
                for (int y = f.eyeY(); y < f.eyeY() + height; y++) {
                    for (int dx = 0; dx < f.eyeWidth(); dx++) {
                        int old = colors.get(y).get(x + dx);
                        if (old < 0 || luminance(old) < 170) {
                            int sampleY = 14;
                            char grain = marks[sampleY].charAt(x + dx);
                            if (grain == ' ') { grain = 'O'; }
                            int light = colors.get(sampleY).get(x + dx);
                            text(rows, inks, colors, x + dx, y, String.valueOf(grain), 5);
                            colors.get(y).set(x + dx, light >= 0 && luminance(light) >= 190 ? light : ACCENTS[5]);
                        }
                    }
                }
                text(rows, inks, colors, x + 1, f.eyeY() + (pose == 2 ? 1 : 0),
                        pose == 1 ? ".----." : "______", 8);
            }
        }
        if ((mood == Mood.CURIOUS || mood == Mood.NEEDS_REVIEW) && pose != 1 && pose != 2) {
            for (int x : new int[]{f.leftEye(), f.rightEye()}) {
                text(rows, inks, colors, x + 2, f.eyeY(), "(o)", 8);
            }
        }
        if (mood == Mood.WORKING || mood == Mood.NEEDS_REVIEW || mood == Mood.CURIOUS || mood == Mood.ERROR) {
            for (int y = f.mouthY(); y < f.mouthY() + f.mouthHeight(); y++) {
                text(rows, inks, colors, f.mouthX(), y, "@".repeat(f.mouthWidth()), 5);
            }
            text(rows, inks, colors, f.mouthX() + 2, f.mouthY() + 1,
                    mood == Mood.WORKING ? "---" : mood == Mood.ERROR ? ".-." : " o ", 8);
        }
        if (pose == 3) {
            text(rows, inks, colors, 19, 19, "O", 5);
            text(rows, inks, colors, 49, 19, "O", 5);
        }
        if (pose == 4 || pose == 5) { paw(rows, colors, f, pose); }
        if (mood == Mood.SUCCESS || pose == 6 || pose == 7) {
            text(rows, inks, colors, 57, 1, pose == 7 ? "+" : "*", 4);
            text(rows, inks, colors, 2, 18, pose == 7 ? "." : "+", 4);
        }
        Frame frame = new Frame(rows, inks, colors);
        if (frame.width() != size.width() || frame.height() != size.height()) { frame = resize(frame, size); }
        return gaze == Gaze.LOOK_FORWARD || pose == 1 || pose == 2 ? frame : look(frame, gaze);
    }

    private static Frame look(Frame frame, Gaze gaze) {
        // Translate the sampled eye cells, rather than resampling a translated eye.
        // At 40 columns the latter can drop an entire dark eye edge between samples.
        int magnitude = gaze == Gaze.LOOK_LEFT || gaze == Gaze.LOOK_RIGHT ? Math.max(1, frame.width() / 28) : 1;
        int offset = gaze == Gaze.LOOK_LEFT || gaze == Gaze.LOOK_SLIGHT_LEFT ? -magnitude : magnitude;
        List<String> rows = new ArrayList<>(frame.rows()), inks = new ArrayList<>(frame.inks());
        List<List<Integer>> colors = new ArrayList<>();
        for (var line : frame.colors()) { colors.add(new ArrayList<>(line)); }
        for (int eye : new int[] {FULL.leftEye(), FULL.rightEye() - 1}) {
            for (int y = 0; y < frame.height(); y++) {
                int sy = (2 * y + 1) * 22 / (2 * frame.height());
                if (sy < FULL.eyeY() || sy >= FULL.eyeY() + 2) { continue; }
                int from = 0, to = frame.width();
                while ((2 * from + 1) * 64 / (2 * frame.width()) < eye) { from++; }
                while ((2 * (to - 1) + 1) * 64 / (2 * frame.width()) >= eye + FULL.eyeWidth()) { to--; }
                for (int x = from; x < to; x++) {
                    int sx = (2 * x + 1) * 64 / (2 * frame.width());
                    char grain = ReferenceBreadArt.MARKS[14].charAt(sx);
                    int fill = ReferenceBreadArt.PALETTE[Character.digit(ReferenceBreadArt.COLORS[14].charAt(sx), 16)];
                    text(rows, inks, colors, x, y, String.valueOf(grain == ' ' ? '@' : grain), 5);
                    colors.get(y).set(x, luminance(fill) >= 190 ? fill : ACCENTS[5]);
                }
                StringBuilder marks = new StringBuilder(rows.get(y)), bands = new StringBuilder(inks.get(y));
                for (int x = from; x < to; x++) {
                    marks.setCharAt(x + offset, frame.rows().get(y).charAt(x));
                    bands.setCharAt(x + offset, frame.inks().get(y).charAt(x));
                    colors.get(y).set(x + offset, frame.colors().get(y).get(x));
                }
                rows.set(y, marks.toString()); inks.set(y, bands.toString());
            }
        }
        return new Frame(rows, inks, colors);
    }

    private static Frame resize(Frame source, Size size) {
        List<String> rows = new ArrayList<>(), inks = new ArrayList<>();
        List<List<Integer>> colors = new ArrayList<>();
        for (int y = 0; y < size.height(); y++) {
            StringBuilder row = new StringBuilder(), ink = new StringBuilder(); List<Integer> paint = new ArrayList<>();
            for (int x = 0; x < size.width(); x++) {
                int sx = Math.min(source.width() - 1, (2 * x + 1) * source.width() / (2 * size.width()));
                int sy = Math.min(source.height() - 1, (2 * y + 1) * source.height() / (2 * size.height()));
                row.append(source.rows().get(sy).charAt(sx)); ink.append(source.inks().get(sy).charAt(sx));
                paint.add(source.colors().get(sy).get(sx));
            }
            rows.add(row.toString()); inks.add(ink.toString()); colors.add(paint);
        }
        return new Frame(rows, inks, colors);
    }

    private static void paw(List<String> rows, List<List<Integer>> colors, Features f, int pose) {
        List<String> original = new ArrayList<>(rows);
        List<List<Integer>> paint = new ArrayList<>();
        for (List<Integer> line : colors) { paint.add(new ArrayList<>(line)); }
        for (int y = f.pawY(); y < f.pawY() + f.pawHeight(); y++) {
            StringBuilder line = new StringBuilder(rows.get(y));
            for (int x = f.pawX(); x < f.pawX() + f.pawWidth(); x++) {
                int sx = x + (pose == 4 ? 1 : 0), sy = y + (pose == 5 ? 1 : 0);
                boolean inside = sx < f.pawX() + f.pawWidth() && sy < f.pawY() + f.pawHeight();
                line.setCharAt(x, inside ? original.get(sy).charAt(sx) : ' ');
                colors.get(y).set(x, inside ? paint.get(sy).get(sx) : -1);
            }
            rows.set(y, line.toString());
        }
    }

    private static void text(List<String> rows, List<String> inks, List<List<Integer>> colors,
                             int x, int y, String value, int band) {
        StringBuilder row = new StringBuilder(rows.get(y)), ink = new StringBuilder(inks.get(y));
        for (int i = 0; i < value.length(); i++) {
            row.setCharAt(x + i, value.charAt(i)); ink.setCharAt(x + i, (char) ('0' + band));
            colors.get(y).set(x + i, value.charAt(i) == ' ' ? -1 : ACCENTS[band]);
        }
        rows.set(y, row.toString()); inks.set(y, ink.toString());
    }
    private static int luminance(int rgb) {
        return (((rgb >> 16) & 255) * 3 + ((rgb >> 8) & 255) * 6 + (rgb & 255)) / 10;
    }
}
