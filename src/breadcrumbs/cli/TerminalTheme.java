package breadcrumbs.cli;

/** Only trusted fixed labels receive SGR. User/source data always uses default foreground. */
public final class TerminalTheme {
    public enum Tone {
        BREAD_GOLD("1;38;5;214"), BREAD_LIGHT("1;38;5;222"), BREAD_SHADOW("38;5;130"),
        PRIMARY_ACTION("1;38;5;222;48;5;235"), SECONDARY("38;5;245"),
        SUCCESS("32"), WARNING("33"), ERROR("31");
        private final String code;
        Tone(String code) { this.code = code; }
    }
    private final boolean color;
    private final boolean richBrand;
    public TerminalTheme(boolean color) { this(color, false); }
    public TerminalTheme(boolean color, boolean richBrand) {
        this.color = color;
        this.richBrand = color && richBrand;
    }
    public boolean richBrandEnabled() { return richBrand; }
    public String label(Tone tone, String trustedLabel) {
        return color ? "\u001b[" + tone.code + "m" + trustedLabel + "\u001b[0m" : trustedLabel;
    }

    /** Only fixed artwork uses RGB; untrusted project data never reaches this helper. */
    String artText(int colorValue, String trustedText) {
        return color ? "\u001b[" + rgb("38", colorValue) + "m" + trustedText + "\u001b[0m" : trustedText;
    }

    private static String rgb(String channel, int colorValue) {
        return "1;" + channel + ";2;" + ((colorValue >> 16) & 255) + ";"
                + ((colorValue >> 8) & 255) + ";" + (colorValue & 255);
    }
    public static boolean enabled(boolean noColor, boolean always, boolean interactive,
                                  boolean noColorEnvironment, String term, boolean windows, boolean knownWindowsVt) {
        if (noColor || noColorEnvironment) { return false; }
        if (always) { return true; }
        return interactive && (term == null ? windows && knownWindowsVt : !term.equalsIgnoreCase("dumb"))
                && (!windows || knownWindowsVt);
    }
}
