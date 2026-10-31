package breadcrumbs;

import breadcrumbs.cli.ConsoleMenu;
import breadcrumbs.cli.ProjectConsole;
import breadcrumbs.cli.TerminalTheme;
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
            boolean noColor = false, always = false, dsa = false, richBrand = false;
            Path storage = Path.of(System.getProperty("user.home"), ".breadcrumbs");
            for (String arg : args) {
                if (arg.equals("--no-color")) { noColor = true; }
                else if (arg.equals("--color=always")) { always = true; }
                else if (arg.equals("--rich-brand")) { richBrand = true; }
                else if (arg.equals("--dsa")) { dsa = true; }
                else if (arg.startsWith("--storage-root=") && arg.length() > 15) { storage = Path.of(arg.substring(15)); }
                else { throw new IllegalArgumentException("Unknown option."); }
            }
            var input = new InputStreamReader(System.in, StandardCharsets.UTF_8);
            if (dsa) { new ConsoleMenu(input, System.out).run(); }
            else {
                boolean color = TerminalTheme.enabled(noColor, always, System.console() != null,
                        System.getenv().containsKey("NO_COLOR"), System.getenv("TERM"),
                        System.getProperty("os.name").startsWith("Windows"), System.getenv("WT_SESSION") != null
                                || System.getenv("ANSICON") != null);
                int width = 80;
                try { width = Math.max(28, Integer.parseInt(System.getenv("COLUMNS"))); }
                catch (NumberFormatException ignored) { /* Unknown dimensions use the compact 80-column layout. */ }
                // Context reports and source locators use UTF-8 in plain and colored modes alike.
                // Fix the stream encoding without changing the user's terminal or system settings.
                PrintStream output = new PrintStream(System.out, true, StandardCharsets.UTF_8);
                new ProjectConsole(input, output, new WorkspaceStore(storage),
                        new TerminalTheme(color, richBrand), width).run();
            }
        } catch (IOException failure) {
            System.err.println("Console input failed. Unsaved session records may be lost; reopen saved app storage.");
            System.exit(1);
        } catch (IllegalArgumentException invalid) {
            System.err.println("Invalid application arguments. Use --no-color, --color=always, --rich-brand, --dsa or --storage-root=<path>.");
            System.exit(1);
        }
    }
}
