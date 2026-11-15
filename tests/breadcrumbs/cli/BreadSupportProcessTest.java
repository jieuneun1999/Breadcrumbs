package breadcrumbs.cli;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class BreadSupportProcessTest {
    private BreadSupportProcessTest() { }

    public static void main(String[] args) throws Exception {
        Path root = Files.createTempDirectory(Path.of("build"), "bread-process-").toAbsolutePath();
        Result plain = run(root.resolve("plain"), "0\n", "--no-animation", "--no-mascot", "--no-color");
        require(plain.exit() == 0 && plain.text().contains("Goodbye."), "Production Main accepts accessibility preferences and exits normally");
        require(!plain.text().contains("\u001b["), "Plain redirected Main emits no ANSI");
        Result atlas = run(root.resolve("preview-must-not-exist"), "", "--bread-preview=all", "--no-animation", "--no-color");
        require(atlas.exit() == 0 && atlas.text().contains("Bread Support preview"), "Isolated production preview is available");
        require(atlas.text().contains("FULL") && atlas.text().contains("COMPACT"), "Both handcrafted sizes are previewed");
        for (String mood : List.of("IDLE", "WELCOME", "WORKING", "ENCOURAGING", "SUCCESS", "NEEDS_REVIEW", "ERROR", "GOODBYE")) {
            require(atlas.text().contains(mood), "Preview includes " + mood);
        }
        require(!Files.exists(root.resolve("preview-must-not-exist")), "Preview never opens project storage");
        require(!atlas.text().contains("\u001b["), "Monochrome atlas has no cursor movement or color");
        Result redirected = run(root.resolve("redirected"), "?\n0\n", "--color=always");
        require(redirected.exit() == 0 && redirected.text().contains("[8]"), "Redirected numbered routes and menu redisplay remain available");
        require(!redirected.text().contains("\u001b[2K") && !redirected.text().contains("\u001b7"), "Forced color does not force cursor animation");
        System.out.println("PASS BreadSupportProcessTest");
    }

    private record Result(int exit, String text) { }

    private static Result run(Path storage, String input, String... flags) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
        command.add("-cp"); command.add(System.getProperty("java.class.path")); command.add("breadcrumbs.Main");
        command.add("--storage-root=" + storage); command.addAll(List.of(flags));
        Path log = storage.resolveSibling(storage.getFileName() + ".log");
        ProcessBuilder builder = new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile());
        builder.environment().put("COLUMNS", "40"); builder.environment().remove("NO_COLOR");
        Process process = builder.start();
        try {
            process.getOutputStream().write(input.getBytes(StandardCharsets.UTF_8)); process.getOutputStream().close();
            require(process.waitFor(25, TimeUnit.SECONDS), "Production child exits within bound");
            return new Result(process.exitValue(), Files.readString(log, StandardCharsets.UTF_8));
        } finally { if (process.isAlive()) { process.destroyForcibly(); } }
    }

    private static void require(boolean passed, String message) {
        if (!passed) { throw new AssertionError(message); }
    }
}
