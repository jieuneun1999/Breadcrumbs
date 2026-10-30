package breadcrumbs;

import breadcrumbs.cli.ConsoleMenu;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class Main {
    private Main() { }

    public static void main(String[] args) {
        try {
            new ConsoleMenu(new InputStreamReader(System.in, StandardCharsets.UTF_8), System.out).run();
        } catch (IOException failure) {
            System.err.println("Console input failed. Session-local records were not saved.");
            System.exit(1);
        }
    }
}
