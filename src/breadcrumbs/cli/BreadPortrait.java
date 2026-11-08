package breadcrumbs.cli;

import java.util.ArrayList;
import java.util.List;
import static breadcrumbs.cli.BreadFrames.*;

/** Independently composed grids share the approved bread, lighting and facial identity. */
final class BreadPortrait {
    private record Features(int leftEye, int rightEye, int eyeY, int eyeWidth,
                            int mouthX, int mouthY, int mouthWidth, int mouthHeight,
                            int pawX, int pawY, int pawWidth, int pawHeight) { }
    private static final Features FULL = new Features(23, 43, 12, 9, 33, 14, 7, 3, 44, 17, 13, 5);
    private static final Features COMPACT = new Features(10, 19, 5, 3, 14, 6, 3, 1, 19, 7, 7, 3);
    private static final int[] ACCENTS = {0, 0x83512c, 0xa2632b, 0xbc7830, 0xf4c25c,
            0xffe9b8, 0xfff0ce, 0xe49b9a, 0x9b6b3d};
    private BreadPortrait() { }

    static Frame draw(Size size, Mood mood, int pose) {
        boolean full = size == Size.FULL;
        String[] marks = full ? ReferenceBreadArt.MARKS : ReferenceCompactBreadArt.MARKS;
        String[] bands = full ? ReferenceBreadArt.COLORS : ReferenceCompactBreadArt.COLORS;
        Features f = full ? FULL : COMPACT;
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
        if (!full) {
            text(rows, inks, colors, 11, 5, "^", 8);
            text(rows, inks, colors, 20, 5, "^", 8);
            text(rows, inks, colors, 15, 6, mood == Mood.SUCCESS ? "u" : "w", 8);
            text(rows, inks, colors, 8, 6, "/", 7);
            text(rows, inks, colors, 23, 6, "/", 7);
        }
        if (pose == 1 || pose == 2) {
            for (int x : new int[]{f.leftEye(), f.rightEye()}) {
                int height = full ? 2 : 1;
                for (int y = f.eyeY(); y < f.eyeY() + height; y++) {
                    for (int dx = 0; dx < f.eyeWidth(); dx++) {
                        int old = colors.get(y).get(x + dx);
                        if (old < 0 || luminance(old) < 170) {
                            int sampleY = full ? 14 : 4;
                            char grain = marks[sampleY].charAt(x + dx);
                            if (grain == ' ') { grain = 'O'; }
                            int light = colors.get(sampleY).get(x + dx);
                            text(rows, inks, colors, x + dx, y, String.valueOf(grain), 5);
                            colors.get(y).set(x + dx, light >= 0 && luminance(light) >= 190 ? light : ACCENTS[5]);
                        }
                    }
                }
                text(rows, inks, colors, x + 1, f.eyeY() + (full && pose == 2 ? 1 : 0),
                        full ? pose == 1 ? ".----." : "______" : pose == 1 ? "--" : "__", 8);
            }
        }
        if ((mood == Mood.CURIOUS || mood == Mood.NEEDS_REVIEW) && pose != 1 && pose != 2) {
            for (int x : new int[]{f.leftEye(), f.rightEye()}) {
                text(rows, inks, colors, x + (full ? 2 : 1), f.eyeY(), full ? "(o)" : "o", 8);
            }
        }
        if (mood == Mood.WORKING || mood == Mood.NEEDS_REVIEW || mood == Mood.CURIOUS || mood == Mood.ERROR) {
            for (int y = f.mouthY(); y < f.mouthY() + f.mouthHeight(); y++) {
                text(rows, inks, colors, f.mouthX(), y, "@".repeat(f.mouthWidth()), 5);
            }
            text(rows, inks, colors, f.mouthX() + (full ? 2 : 0), f.mouthY() + (full ? 1 : 0),
                    mood == Mood.WORKING ? "---" : mood == Mood.ERROR ? ".-." : " o ", 8);
        }
        if (pose == 3) {
            text(rows, inks, colors, full ? 19 : 8, full ? 19 : 9, "O", 5);
            text(rows, inks, colors, full ? 49 : 22, full ? 19 : 9, "O", 5);
        }
        if (pose == 4 || pose == 5) { paw(rows, colors, f, pose); }
        if (mood == Mood.SUCCESS || pose == 6 || pose == 7) {
            text(rows, inks, colors, full ? 57 : 26, 1, pose == 7 ? "+" : "*", 4);
            text(rows, inks, colors, full ? 2 : 0, full ? 18 : 8, pose == 7 ? "." : "+", 4);
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
