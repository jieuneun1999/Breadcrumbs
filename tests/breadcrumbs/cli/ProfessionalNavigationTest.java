package breadcrumbs.cli;

import breadcrumbs.storage.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import static breadcrumbs.b4.Checks.*;

public final class ProfessionalNavigationTest {
    private ProfessionalNavigationTest() { }
    public static void main(String[] args) throws Exception { run(); }
    public static void run() throws Exception {
        Path root = Files.createTempDirectory(Files.createDirectories(Path.of("build/b7-fixtures")), "navigation-");
        var store = new WorkspaceStore(root);
        store.save(ContinuityTest.empty("W795", "Navigation review"));
        String script = "1\n1\n7\n1\n0\n0\n";
        var bytes = new ByteArrayOutputStream();
        String[] beforeSelection = {""};
        int checkAt = script.indexOf("\n0\n") + 1;
        Reader reader = new Reader() {
            int position;
            @Override public int read(char[] buffer, int offset, int length) {
                if (length == 0) { return 0; }
                if (position == checkAt) { beforeSelection[0] = bytes.toString(StandardCharsets.UTF_8); }
                if (position == script.length()) { return -1; }
                buffer[offset] = script.charAt(position++); return 1;
            }
            @Override public void close() { }
        };
        new ProjectConsole(reader, new PrintStream(bytes, true, StandardCharsets.UTF_8), store, new TerminalTheme(false), 120).run();
        String snapshot = beforeSelection[0].replace("\r\n", "\n");
        String afterSummary = snapshot.substring(snapshot.toLowerCase(java.util.Locale.ROOT).lastIndexOf("\nproject summary\n"));
        for (int i = 1; i <= 13; i++) { require(afterSummary.contains("[" + i + "]"), "Every workspace action remains visible after a result: " + i); }
        require(afterSummary.contains("[0]") && afterSummary.contains("[?]"), "navigation and help are visible at the decision point");
        var matcher = java.util.regex.Pattern.compile("\\[(\\d+)\\]").matcher(afterSummary);
        for (int i = 1; i <= 13; i++) { require(matcher.find() && Integer.parseInt(matcher.group(1)) == i, "workspace choices follow numerical order"); }
        require(matcher.find() && matcher.group(1).equals("0"), "Back follows the workspace actions");
        var wrapped = new ByteArrayOutputStream();
        new TerminalOutput(new PrintStream(wrapped, true, StandardCharsets.UTF_8), new TerminalTheme(false), 28)
                .plain("Review the project summary before recording another progress update.");
        String text = wrapped.toString(StandardCharsets.UTF_8);
        require(text.replaceAll("\\s+", " ").strip().equals("Review the project summary before recording another progress update."), "word wrapping retains complete readable words");
        require(store.load("W795").checkpoints().isEmpty(), "navigation does not create project history");
        var sessionStore = new WorkspaceStore(root.resolve("session-save"));
        var savedOutput = new ByteArrayOutputStream();
        String saveScript = "2\n1\nSession project\n1\nPrepare event\nEvent plan\nReview schedule\n\n\n\n4\nyes\n\n7\n12\nyes\n2\nPrepared the schedule\nnone\nConfirm volunteers\nyes\n5\n3\n0\n0\n0\n";
        new ProjectConsole(new StringReader(saveScript), new PrintStream(savedOutput, true, StandardCharsets.UTF_8), sessionStore, new TerminalTheme(false), 120).run();
        require(savedOutput.toString(StandardCharsets.UTF_8).contains("Discovered entries=0"), "search coverage shows actual project counts instead of generic help alone");
        var restored = sessionStore.load(sessionStore.list()[0].id());
        require(restored.checkpoints().size() == 1 && restored.checkpoints().getLatest().project().nextActions().equals("Confirm volunteers"), "explicit workspace Save enables persistence for subsequent updates");
        require(!savedOutput.toString(StandardCharsets.UTF_8).contains("This update is only open here"), "workspace status uses the current save mode after explicit Save");
        String confirmation = savedOutput.toString(StandardCharsets.UTF_8);
        for (String key : new String[] {"YES", "E", "A", "M", "O", "D", "F", "0", "?"}) {
            require(confirmation.contains("[" + key + "]"), "update actions are explicitly advertised: " + key);
        }
        for (int width : new int[] {28, 40, 80, 120}) {
            for (boolean color : new boolean[] {false, true}) {
                var layout = new ByteArrayOutputStream();
                var ui = new TerminalOutput(new PrintStream(layout, true, StandardCharsets.UTF_8), new TerminalTheme(color), width);
                ui.choices("[1] View project summary\n[2] Record progress update\n[10] Manage project notes\n[0] Back to workspace");
                ui.prompt("Review the selected details before confirming. Type yes to continue, or press Enter to cancel: ");
                String visible = layout.toString(StandardCharsets.UTF_8).replaceAll("\u001b\\[[0-9;]*m", "");
                for (String line : visible.split("\\r?\\n")) { require(line.length() <= width, "choices and prompts respect terminal width " + width); }
                require(visible.replaceAll("\\s+", " ").contains("Type yes to continue, or press Enter to cancel:"), "confirmation instructions stay complete at narrow widths");
            }
        }
        System.out.println("PASS: visible workspace actions, professional navigation and word-aware spacing");
    }
}
