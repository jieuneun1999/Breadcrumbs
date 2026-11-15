package breadcrumbs.cli;

import java.io.PrintStream;

/** Prints the reference's fixed character/color transcription without an image protocol. */
final class RichBrandRenderer {
    private static final int CAPTION_START = 44;

    private RichBrandRenderer() { }

    static void render(PrintStream out, TerminalTheme theme) {
        out.print(frame(theme, false));
    }

    static String frame(TerminalTheme theme, boolean loafOnly) {
        StringBuilder frame = new StringBuilder();
        for (int row = 0; row < ReferenceBrandArt.MARKS.length; row++) {
            String caption = row == 1 ? "Breadcrumbs / project continuity"
                    : row == 16 ? "Leave context behind. Pick up where work stopped." : null;
            int end = caption == null ? ReferenceBrandArt.MARKS[row].length() : CAPTION_START;
            StringBuilder line = new StringBuilder("  ");
            for (int column = 0; column < end; column++) {
                char mark = column < ReferenceBrandArt.MARKS[row].length()
                        ? ReferenceBrandArt.MARKS[row].charAt(column) : ' ';
                if (loafOnly && column >= 39) { mark = ' '; }
                if (mark == ' ') {
                    line.append(' ');
                } else {
                    int band = Character.digit(ReferenceBrandArt.COLORS[row].charAt(column), 16);
                    line.append(theme.artText(ReferenceBrandArt.PALETTE[band], String.valueOf(mark)));
                }
            }
            if (caption != null && !loafOnly) {
                line.append(theme.label(row == 1 ? TerminalTheme.Tone.SECONDARY
                        : TerminalTheme.Tone.BREAD_LIGHT, caption));
            }
            frame.append(line.toString().stripTrailing()).append(System.lineSeparator());
        }
        return frame.toString();
    }
}
