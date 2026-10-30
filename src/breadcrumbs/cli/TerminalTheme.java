package breadcrumbs.cli;

/** Only trusted fixed labels receive SGR. User/source data always uses default foreground. */
public final class TerminalTheme {
    public enum Tone {
        BREAD_GOLD("1;38;5;214"), SUCCESS("32"), WARNING("33"), ERROR("31");
        private final String code;
        Tone(String code) { this.code = code; }
    }
    private final boolean color;
    public TerminalTheme(boolean color) { this.color = color; }
    public String label(Tone tone, String trustedLabel) {
        return color ? "\u001b[" + tone.code + "m" + trustedLabel + "\u001b[0m" : trustedLabel;
    }
    public static boolean enabled(boolean noColor, boolean always, boolean interactive,
                                  boolean noColorEnvironment, String term, boolean windows, boolean knownWindowsVt) {
        if (noColor || noColorEnvironment) { return false; }
        if (always) { return true; }
        return interactive && (term == null ? windows && knownWindowsVt : !term.equalsIgnoreCase("dumb"))
                && (!windows || knownWindowsVt);
    }
}
