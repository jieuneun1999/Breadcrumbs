package breadcrumbs.b7;

import breadcrumbs.bootstrap.*;
import breadcrumbs.domain.*;
import breadcrumbs.inspector.*;
import breadcrumbs.storage.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import static breadcrumbs.b4.Checks.*;

/** Four profiles through actual Main processes, canonical services and disposable source roots. */
public final class ProcessScenarios {
    private ProcessScenarios() { }
    public static void main(String[] args) throws Exception {
        Path base = Files.createTempDirectory(Files.createDirectories(Path.of("build/b7-e2e")), "four-profiles-").toAbsolutePath();
        for (var profile : ProjectProfile.values()) {
            Path dir = Files.createDirectory(base.resolve(profile.name())), source = Files.createDirectory(dir.resolve("source"));
            Files.writeString(source.resolve("notes.md"), "# Objective\nPlan accessible community services.\n# Next Action\nReview the requirements.\n# Test Results\nTests passed according to this old report only.\n");
            Files.writeString(source.resolve("owners.csv"), "objective,owner,decision\nCoordinate deliveries,Alex,Use the small venue\n");
            Files.write(source.resolve("research.docx"), DocxTest.document("<w:p><w:r><w:t>Documented accessible methodology</w:t></w:r></w:p>"));
            Files.writeString(source.resolve("reference.pdf"), "%PDF-1.4\nunsupportedpdfonlyword\n%%EOF");
            String before = fingerprint(source);
            String created = app(dir, "import", "2\n2\n" + profile + " sample\n" + profile + "\n" + source + "\nyes\ns\nyes\nyes\n0\n");
            require(created.contains("Baseline B0 established") && created.contains("SOURCE_REPORTED"), profile + " actual first-import B0 without invented history");
            var store = new WorkspaceStore(dir.resolve("app")); var state = store.load(store.list()[0].id());
            require(state.checkpoints().isEmpty() && state.activeBaseline().profile() == profile, profile + " no native history before confirmation");
            String ref = state.activeBaseline().claims()[0].evidenceId();
            String tasks = "8\n2\nPrepare venue\nArrange access\nIN_PROGRESS\n\n\n\nyes\n2\nHold review\nReview plan\nNOT_STARTED\n\n\n\nyes\n3\nT-000001\nT-000002\n\nReview needs preparation\nyes\n7\nT-000001\n0\n";
            String quick = "2\nReviewed the imported context\nAwaiting stakeholder confirmation\nDraft the revised plan\na\n" + ref + "\nyes\n";
            String captured = app(dir, "checkpoint-package", "1\n1\n7\n" + tasks + quick + "11\n5\nyes\n6\nCP-000001\n" + dir.resolve("portable") + "\nyes\n0\n5\n6\nyes\n4\n1\nmethodology\n0\n1\nunsupportedpdfonlyword\n0\n1\n0\n0\n");
            require(captured.contains("Confirmed CP-000001") && captured.contains("Portable package exported") && captured.contains("paragraph 1"), profile + " quick confirm, graph, DOCX search and portable handoff");
            require(captured.contains("SOURCE_REPORTED") && captured.contains("unverified") && captured.contains("No indexed matches"), profile + " reported work and unsupported PDF terms remain honest");
            state = store.load(state.activeBaseline().workspaceId());
            require(state.dependencies().current().dependencies().length == 1 && state.checkpoints().size() == 1, profile + " explicit dependency and checkpoint persisted");
            String archive = Files.readString(dir.resolve("portable/checkpoints/CP-000001.md"));
            require(Files.readString(dir.resolve("portable/HANDOFF.md")).contains(state.activeBaseline().workspaceId()) && !archive.contains(source.toString()), profile + " standalone portable identity and privacy");
            String returned = app(dir, "return-second-checkpoint", "1\n1\n7\n2\nPrepared a draft\nunknown\nAsk for review\nyes\n1\n0\n0\n");
            require(returned.contains("Confirmed CP-000002") && returned.contains("FOLLOW THE BREADCRUMBS"), profile + " actual second process resumes and adds exactly one checkpoint");
            Files.writeString(source.resolve("notes.md"), "# Objective\nChanged accessible scope.\n# Next Action\nReview replacementword.\n");
            String changedBefore = fingerprint(source);
            String changed = app(dir, "changed-source", "1\n1\n7\n4\nyes\n1\nyes\n1\n5\n6\nyes\n4\n1\nreplacementword\n0\n0\n9\n5\n0\n0\n0\n");
            require(changed.contains("STALE") && changed.contains("MODIFIED") && changed.contains("replacementword"), profile + " explicit rescan, drift and rebuilt search");
            var restored = store.load(state.activeBaseline().workspaceId());
            require(restored.checkpoints().size() == 2 && restored.laterObservations().length == 1 && restored.checkpoints().findById("CP-000001").project().snapshotId().equals(state.currentObservationId()), profile + " original reference provenance survives later observations");
            require(fingerprint(source).equals(changedBefore) && !before.equals(changedBefore), profile + " only harness's explicit edit changes source");
        }
        System.out.println("PASS: B7E four-profile Main processes, B0/quick/restart/Graph/DOCX/drift/search/package (" + breadcrumbs.b4.Checks.count() + " assertions) | " + base);
    }
    private static String fingerprint(Path root) throws Exception {
        var out = new StringBuilder(); try (var files = Files.list(root)) { for (Path p : files.sorted().toArray(Path[]::new)) { out.append(p.getFileName()).append(WorkspaceCodec.digest(Files.readAllBytes(p))); } } return out.toString();
    }
    private static String app(Path dir, String name, String input) throws Exception {
        Path log = dir.resolve(name + ".txt"); Files.writeString(dir.resolve(name + ".input.txt"), input);
        Path java = Path.of(System.getProperty("java.home"), "bin", "java.exe");
        var pb = new ProcessBuilder(java.toString(), "-cp", "build/app", "breadcrumbs.Main", "--no-color", "--storage-root=" + dir.resolve("app"));
        pb.environment().put("NO_COLOR", "1");
        var process = pb.redirectErrorStream(true).redirectOutput(log.toFile()).start();
        try (var writer = process.outputWriter(StandardCharsets.UTF_8)) { writer.write(input); }
        if (!process.waitFor(45, TimeUnit.SECONDS)) { process.destroyForcibly(); throw new AssertionError("B7E process deadline " + name); }
        String text = Files.readString(log); require(process.exitValue() == 0 && text.contains("Goodbye."), "production Main clean exit " + name); return text;
    }
}
