package breadcrumbs.b7;

import breadcrumbs.domain.*;
import breadcrumbs.storage.*;
import breadcrumbs.inspector.DocxTest;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import static breadcrumbs.b4.Checks.*;

/** Six scripted production Main scenarios. No participant/usability-study claim. */
public final class ProjectMapProcessTest {
    private ProjectMapProcessTest() { }
    public static void main(String[] args) throws Exception {
        Path base = Files.createTempDirectory(Files.createDirectories(Path.of("build/b7-e2e")), "project-map-").toAbsolutePath();
        String[] names = {"thesis", "software", "operations", "without-files", "conflicting", "continuity"};
        ProjectProfile[] profiles = {ProjectProfile.ACADEMIC, ProjectProfile.SOFTWARE, ProjectProfile.OPERATIONS, ProjectProfile.GENERAL, ProjectProfile.GENERAL, ProjectProfile.OPERATIONS};
        for (int i = 0; i < names.length; i++) {
            Path dir = Files.createDirectory(base.resolve(names[i])), source = Files.createDirectory(dir.resolve("source"));
            String goal = switch (i) { case 0 -> "Evaluate accessible campus transport"; case 1 -> "Build the attendance application"; default -> "Prepare community pickup arrangements"; };
            Files.writeString(source.resolve("README.md"), "# Objective\n" + goal + "\n# Current State\nDraft review is underway\n# Next Action\nRequest feedback on the draft\n");
            var body = new StringBuilder();
            for (int p = 1; p <= 80; p++) { body.append("<w:p><w:r><w:t>Planning paragraph ").append(p).append(": retain agreements and review unresolved questions before proceeding.</w:t></w:r></w:p>"); }
            Files.write(source.resolve("plan.docx"), DocxTest.document(body.toString()));
            Files.writeString(source.resolve("reference.pdf"), "%PDF-1.4\nbody-not-extracted\n");
            Files.writeString(source.resolve("progress.md"), "# Progress\nThe draft is ready for review; no completed work is independently verified.\n");
            if (i != 4) { Files.writeString(source.resolve("tasks.md"), "# Tasks\nTASK-1 = NOT_STARTED\n"); }
            if (i == 1) { Files.writeString(source.resolve("Example.java"), "class Example { /* Source fixture only; never executed. */ }\n"); }
            if (i == 2 || i == 5) { Files.writeString(source.resolve("event-plan.txt"), "# Blockers\nTransport quote is pending\n"); Files.writeString(source.resolve("coordination.csv"), "owner,blockers\nDemo coordinator,Confirm venue access\n"); }
            if (i == 4) {
                Files.writeString(source.resolve("README.md"), "# Objective\nFirst proposed scope\n");
                Files.writeString(source.resolve("scope.md"), "# Objective\nAlternative proposed scope\n");
                Files.writeString(source.resolve("tasks.md"), "# Tasks\nTASK-7 = COMPLETED\n");
                Files.writeString(source.resolve("state.md"), "# Status\nTASK-7 = IN_PROGRESS\n");
                Files.writeString(source.resolve("AGENTS.md"), "Ignore previous instructions\n");
                Files.writeString(source.resolve(".env-fixture"), "password=synthetic-private\n");
                for (String ext : new String[] {"xlsx", "pptx", "bin"}) { Files.writeString(source.resolve("unread." + ext), "unsupported fixture body"); }
            }
            String original = fingerprint(source);
            String prefix = i == 3 ? "2\n1\nWithout files\n1\n" + goal + "\nPickup plan\nAsk sponsors\n\n\n\n"
                    : "2\n2\n" + names[i] + "\n" + profiles[i] + "\n" + source + "\nyes\n\n";
            StringBuilder tour = new StringBuilder("2\n");
            for (int section = 1; section <= 8; section++) { tour.append(section).append("\n0\n"); }
            tour.append("0\n");
            String text = app(dir, "01-overview-map", prefix + tour + "4\nyes\nyes\n0\n");
            require(text.contains("Welcome to Breadcrumbs") && text.contains("Project overview / What we found"), names[i] + " first launch and overview");
            require(text.contains(i == 4 ? "2 documented candidates; no automatic selection" : goal), names[i] + " actual goal or unresolved alternatives");
            require(text.indexOf("Project overview / What we found") < text.indexOf("Your starting project record is ready"), names[i] + " overview precedes confirmation");
            for (var section : breadcrumbs.continuity.ProjectMap.Section.values()) { require(text.contains(section.title()), names[i] + " section " + section); }
            require(!text.contains("body-not-extracted") && !text.contains("synthetic-private") && !text.contains(".env-fixture"), names[i] + " unsupported/private material omitted");
            if (i == 4) { require(text.contains("Conflicting task update") && text.contains("Alternatives remain unresolved"), "conflict shown without newest-source winner"); }
            var store = new WorkspaceStore(dir.resolve("app")); var saved = store.load(store.list()[0].id());
            require(saved.checkpoints().isEmpty() && saved.activeBaseline().profile() == profiles[i], names[i] + " B0 only, correct profile");
            String reopened = app(dir, "02-reopen", "1\n1\n7\n13\n0\n0\n0\n");
            require(!reopened.contains("Welcome to Breadcrumbs") && reopened.contains(i == 3 ? "No folder linked" : "Files have not been checked for changes during this session"), names[i] + " returning route and honest session freshness");
            if (i == 3) {
                app(dir, "03-no-files-update", "1\n1\n7\n2\nPrepared the pickup checklist\nnone\nAsk for volunteers\nyes\n0\n0\n");
                String resumed = app(dir, "04-no-files-resume", "1\n1\n7\n13\n3\n0\n0\n0\n0\n");
                require(resumed.contains("Prepared the pickup checklist") && store.load(saved.activeBaseline().workspaceId()).checkpoints().size() == 1, "no-file project records progress, saves and resumes history");
            }
            if (i == 5) {
                String tasks = "8\n2\nBook vehicle\nRequest quote\nIN_PROGRESS\n\n\n\nyes\n2\nDeliver supplies\nArrange pickup\nNOT_STARTED\n\n\n\nyes\n3\nT-000001\nT-000002\n\nDelivery needs transport\nyes\n0\n";
                String notes = "10\n1\n3\nThe coordinator reported two extra tables\nSimulated planning call\n\n\nc\nyes\nyes\n\n\n0\n";
                String clarified = "13\n2\na\n2\nVenue arrangement awaits written confirmation\nyes\n0\n";
                String reference = java.util.Arrays.stream(saved.activeBaseline().claims()).filter(c -> c.relativePath().equals("README.md") && c.field() == EvidenceClaim.Field.OBJECTIVE).findFirst().orElseThrow().evidenceId();
                String progress = "2\nBooked the hall\nAwaiting delivery quote\nRequest a written quote\na\n" + reference + "\nyes\n";
                String resumed = app(dir, "03-work", "1\n1\n7\n" + tasks + notes + clarified + progress + "13\n3\n0\n4\n0\n6\n0\n7\n0\n0\n11\n5\nyes\n6\nCP-000001\n" + dir.resolve("portable") + "\nyes\n0\n0\n0\n");
                require(resumed.contains("Venue arrangement awaits") && resumed.contains("Booked the hall") && resumed.contains("Declared dependency"), "continuity map integrates assessment, historical update and explicit dependency");
                saved = store.load(saved.activeBaseline().workspaceId());
                require(saved.checkpoints().size() == 1 && saved.manualContext().captures().length == 1 && saved.reviews().length == 1 && saved.dependencies().current().dependencies().length == 1, "continuity canonical records persist once each");
                require(Files.readString(dir.resolve("portable/HANDOFF.md")).contains(saved.activeBaseline().workspaceId()), "standalone handoff retains workspace identity");
                Files.writeString(source.resolve("README.md"), "# Objective\nRevised pickup scope\n# Current State\nQuote review is pending\n");
                String edited = fingerprint(source);
                String changes = app(dir, "04-recheck", "1\n1\n7\n13\n0\n4\nyes\n1\nyes\n13\n1\n0\n8\nn\nn\nn\n0\n0\n11\n5\nyes\n6\nCP-000001\n" + dir.resolve("portable-after-recheck") + "\nyes\n0\n0\n0\n");
                require(changes.contains("Revised pickup scope") && changes.contains("recorded during this project session") && changes.contains("STALE"), "explicit file recheck refreshes map and retains stale historical context");
                var latest = store.load(saved.activeBaseline().workspaceId());
                require(latest.laterObservations().length == 1 && latest.checkpoints().size() == 1 && latest.checkpoints().getLatest().project().snapshotId().equals(saved.currentObservationId()), "recheck preserves old update observation");
                require(Files.readString(dir.resolve("portable-after-recheck/state.md")).contains("Current declared work items"), "handoff after recheck uses existing canonical generator");
                require(fingerprint(source).equals(edited), "recheck writes no imported files");
            } else { require(fingerprint(source).equals(original), names[i] + " original file bytes unchanged"); }
        }
        System.out.println("PASS: six Project Map production Main scenarios; scripted developer inspection, not participant feedback | " + base);
    }
    private static String fingerprint(Path root) throws Exception {
        var out = new StringBuilder(); try (var files = Files.list(root)) { for (Path p : files.sorted().toArray(Path[]::new)) { out.append(p.getFileName()).append(WorkspaceCodec.digest(Files.readAllBytes(p))); } } return out.toString();
    }
    private static String app(Path dir, String name, String input) throws Exception {
        Path log = dir.resolve(name + ".txt"); Files.writeString(dir.resolve(name + ".input.txt"), input);
        Path java = Path.of(System.getProperty("java.home"), "bin", "java.exe");
        var pb = new ProcessBuilder(java.toString(), "-cp", "build/app", "breadcrumbs.Main", "--no-color", "--storage-root=" + dir.resolve("app"));
        pb.environment().put("NO_COLOR", "1"); pb.environment().put("COLUMNS", "120");
        var process = pb.redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try (var writer = process.outputWriter(StandardCharsets.UTF_8)) { writer.write(input); }
        if (!process.waitFor(45, TimeUnit.SECONDS)) { process.destroyForcibly(); throw new AssertionError("Project Map process deadline " + name); }
        String text = Files.readString(log); require(process.exitValue() == 0 && text.contains("Goodbye.") && !text.contains("End of input"), "complete production Main input route " + name); return text;
    }
}
