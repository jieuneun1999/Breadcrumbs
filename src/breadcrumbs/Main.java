package breadcrumbs;

import breadcrumbs.cli.ConsoleMenu;
import breadcrumbs.cli.ProjectConsole;
import breadcrumbs.cli.TerminalTheme;
import breadcrumbs.cli.BreadSupport;
import breadcrumbs.cli.BreadPreview;
import breadcrumbs.cli.TerminalKeyboard;
import breadcrumbs.cli.TerminalScreenManager;
import breadcrumbs.storage.WorkspaceStore;
import java.nio.file.Path;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public final class Main {
    private Main() { }

    public static void main(String[] args) {
        try {
            boolean fullUi = Boolean.getBoolean("breadcrumbs.fullUi");
            boolean noColor = false, always = false, dsa = false, richBrand = true, noAnimation = false, noMascot = false;
            boolean classicUi = false, terminalDiagnostics = false;
            String preview = null, group = "The Bread";
            boolean classroom = false;
            Path storage = Path.of(System.getProperty("user.home"), ".breadcrumbs");
            for (String arg : args) {
                if (arg.equals("--no-color")) { noColor = true; }
                else if (arg.equals("--color=always")) { always = true; }
                else if (arg.equals("--rich-brand")) { richBrand = true; }
                else if (arg.equals("--dsa")) { dsa = true; }
                else if (arg.equals("--classroom")) { classroom = true; }
                else if (arg.startsWith("--group-name=")) { group = arg.substring(13); }
                else if (arg.equals("--no-animation")) { noAnimation = true; }
                else if (arg.equals("--no-mascot")) { noMascot = true; }
                else if (arg.equals("--classic-ui") || arg.equals("--ui=classic")) { classicUi = true; }
                else if (arg.equals("--ui=auto") || arg.equals("--ui=screen")) { classicUi = false; }
                else if (arg.equals("--terminal-diagnostics")) { terminalDiagnostics = true; }
                else if (arg.equals("--ascii")) { /* Companion artwork always uses visible ASCII symbols. */ }
                else if (arg.equals("--bread-preview")) { preview = "all"; }
                else if (arg.startsWith("--bread-preview=")) { preview = arg.substring(16); }
                else if (arg.startsWith("--storage-root=") && arg.length() > 15) { storage = Path.of(arg.substring(15)); }
                else { throw new IllegalArgumentException("Unknown option."); }
            }
            if (fullUi && (classicUi || noColor || noMascot || noAnimation || classroom || dsa || preview != null)) {
                System.err.println("Run run.bat without display overrides to open the full colored Breadcrumbs interface.");
                System.exit(1);
                return;
            }
            if (classroom) { classicUi = true; }
            var input = new InputStreamReader(System.in, StandardCharsets.UTF_8);
            {
                boolean color = TerminalTheme.enabled(noColor, always, System.console() != null,
                        System.getenv().containsKey("NO_COLOR"), System.getenv("TERM"),
                        System.getProperty("os.name").startsWith("Windows"), System.getenv("WT_SESSION") != null
                                || System.getenv("ANSICON") != null);
                int width = classroom ? 80 : 160;
                try { width = Math.max(28, Integer.parseInt(System.getenv("COLUMNS"))); }
                catch (NumberFormatException ignored) { /* The approved B3 artwork needs 160 columns; COLUMNS can request a smaller text-only layout. */ }
                // Context reports and source locators use UTF-8 in plain and colored modes alike.
                // Fix the stream encoding without changing the user's terminal or system settings.
                PrintStream output = new PrintStream(System.out, true, StandardCharsets.UTF_8);
                boolean animations = !noAnimation && color && TerminalTheme.enabled(noColor, false, System.console() != null,
                        System.getenv().containsKey("NO_COLOR"), System.getenv("TERM"),
                        System.getProperty("os.name").startsWith("Windows"), System.getenv("WT_SESSION") != null || System.getenv("ANSICON") != null);
                int height = 0;
                try { height = Math.max(0, Math.min(1000, Integer.parseInt(System.getenv("LINES")))); }
                catch (NumberFormatException ignored) { }
                PrintStream diagnostics = output;
                boolean diagnosticInput = terminalDiagnostics;
                TerminalKeyboard keys = preview == null ? TerminalKeyboard.open(color && System.console() != null,
                        message -> { if (diagnosticInput) { diagnostics.println("Terminal check: " + message); } })
                        : TerminalKeyboard.open(animations && !noMascot, message -> diagnostics.println("Bread Support terminal check: " + message));
                if (fullUi && (keys == null || !animations)) {
                    if (keys != null) { keys.close(); }
                    System.err.println("Breadcrumbs could not connect its Windows keyboard helper. Reopen run.bat in Command Prompt.");
                    System.exit(1);
                    return;
                }
                if (fullUi && (keys.columns() < 160 || keys.rows() < 52)) {
                    keys.close();
                    System.err.println("The full Breadcrumbs Home needs a terminal at least 160 columns wide and 52 rows tall. Maximize the window or reduce its font size, then reopen run.bat.");
                    System.exit(1);
                    return;
                }
                TerminalScreenManager screens = preview == null && !classicUi && keys != null && keys.columns() >= 40 && keys.rows() >= 15
                        ? new TerminalScreenManager(output, keys, animations) : null;
                if (screens != null) { output = screens.output(); }
                try (var bread = new BreadSupport(output, keys, width, height,
                        !noMascot && System.console() != null, animations, screens)) {
                    if (keys != null) { width = bread.columns(); }
                    boolean companionRgb = keys != null ? keys.rgbSupported() : !System.getProperty("os.name").startsWith("Windows")
                            || System.getenv("WT_SESSION") != null || "truecolor".equalsIgnoreCase(System.getenv("COLORTERM"))
                            || "24bit".equalsIgnoreCase(System.getenv("COLORTERM"));
                    var theme = new TerminalTheme(color, richBrand, animations, bread, companionRgb);
                    Thread cleanup = new Thread(bread::close, "breadcrumbs-terminal-cleanup");
                    if (screens != null) { Runtime.getRuntime().addShutdownHook(cleanup); }
                    try {
                        if (preview != null) { BreadPreview.run(output, input, theme, width, preview); }
                        else if (classroom) { ConsoleMenu.classroom(input, output, theme, width, group).run(); }
                        else if (dsa) { new ConsoleMenu(input, output, theme, width).run(); }
                        else { new ProjectConsole(input, output, new WorkspaceStore(storage), theme, width).run(); }
                    } finally {
                        if (screens != null) { try { Runtime.getRuntime().removeShutdownHook(cleanup); } catch (IllegalStateException shuttingDown) { } }
                    }
                }
            }
        } catch (IOException failure) {
            System.err.println("Console input failed. Unsaved session records may be lost; reopen saved app storage.");
            System.exit(1);
        } catch (IllegalArgumentException invalid) {
            System.err.println("Invalid application arguments. Use --ui=auto|screen|classic, --classic-ui, --no-color, --no-animation, --no-mascot, --ascii, --bread-preview[=all|reference|full|compact|idle], --terminal-diagnostics, --color=always, --rich-brand, --dsa, --classroom, --group-name=<name> or --storage-root=<path>.");
            System.exit(1);
        }
    }
}
