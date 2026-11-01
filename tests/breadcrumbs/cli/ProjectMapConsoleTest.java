package breadcrumbs.cli;

import breadcrumbs.bootstrap.*;
import breadcrumbs.continuity.*;
import breadcrumbs.domain.*;
import breadcrumbs.inspector.*;
import breadcrumbs.storage.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Arrays;
import static breadcrumbs.b4.Checks.*;

public final class ProjectMapConsoleTest {
    private ProjectMapConsoleTest() { }
    public static void main(String[] args) throws Exception { run(); }
    public static void run() throws Exception {
        Path base = Files.createTempDirectory(Files.createDirectories(Path.of("build/b7-fixtures")), "map-cli-").toAbsolutePath();
        String setup = "2\n1\nCommunity food drive\n1\nPrepare 100 food packs\nPickup plan\nAsk sponsors\n\n\n\n";
        var store = new WorkspaceStore(base.resolve("app"));
        String first = session(store, setup + "2\n1\n1\nt\n0\n0\n0\n3\n2\nVenue is being arranged\nyes\n4\nyes\nyes\n7\n13\n2\n1\nt\n0\n0\n0\n2\nBooked the hall\nnone\nConfirm volunteers\nyes\n0\n0\n", false, 120);
        Files.writeString(base.resolve("first-setup-map.txt"), first);
        require(first.contains("Project overview / What we found") && first.contains("Prepare 100 food packs") && first.contains("Supplied by you") && first.contains("Not documented / Unknown"), "first-time overview explains actual supplied context and missing details");
        require(first.contains("No folder linked") && first.contains("Your proposed assessment; not committed"), "proposals stay uncommitted during setup without a fabricated file inventory");
        var saved = store.load(store.list()[0].id());
        require(saved.checkpoints().size() == 1 && saved.reviews().length == 1 && saved.reviews()[0].value().equals("Venue is being arranged"), "setup assessment uses canonical reviews only after approval; first update remains separate");
        String restart = session(store, "1\n1\n7\n13\n3\n0\n0\n0\n0\n", false, 120);
        Files.writeString(base.resolve("restart-map.txt"), restart);
        require(restart.contains("Booked the hall") && restart.contains("Recorded history"), "returning map displays recorded history after restart");
        Path imported = Files.createDirectory(base.resolve("source"));
        Files.writeString(imported.resolve("README.md"), "# Objective\nStudy accessible transport\n# Current State\nResearch interviews are scheduled\n# Next Steps\nPrepare interview questions\n");
        var sourceStore = new WorkspaceStore(base.resolve("source-app"));
        String start = "2\n2\nThesis\n2\n" + imported + "\nyes\n";
        String sourceText = session(sourceStore, start + "2\n1\n1\nr\nc\n2\n2\n1\nr\nr\n2\n4\n1\nr\nu\n4\nyes\nyes\n7\n13\n7\ns\n1\n1\nt\n0\n0\n0\n0\n0\n0\n0\n", false, 120);
        Files.writeString(base.resolve("source-review.txt"), sourceText);
        var sourceState = sourceStore.load(sourceStore.list()[0].id());
        require(sourceText.contains("Study accessible transport") && sourceText.contains("README.md line 2") && sourceText.contains("Research interviews are scheduled"), "overview supplies actual source-linked purpose, current state and next action before confirmation");
        require(sourceState.checkpoints().isEmpty() && sourceState.activeBaseline().claims()[0].review() == EvidenceClaim.ReviewDisposition.CONFIRMED, "selected source review changes disposition without inventing CP-000001");
        require(Arrays.stream(sourceState.activeBaseline().claims()).anyMatch(c -> c.field() == EvidenceClaim.Field.CURRENT_STATE && c.review() == EvidenceClaim.ReviewDisposition.REJECTED)
                && Arrays.stream(sourceState.activeBaseline().claims()).anyMatch(c -> c.field() == EvidenceClaim.Field.NEXT_ACTION && c.review() == EvidenceClaim.ReviewDisposition.LEFT_UNKNOWN), "individual reject and leave-unknown actions preserve original claim provenance");
        byte[] before = WorkspaceCodec.encode(sourceState);
        session(sourceStore, start + "3\n2\nDiscarded proposal\nyes\n0\n0\n", false, 120);
        require(Arrays.equals(before, WorkspaceCodec.encode(sourceStore.load(sourceState.activeBaseline().workspaceId()))), "cancelling setup discards proposed assessments and preserves saved canonical state");
        String cancelled = session(sourceStore, start + "3\n2\nDiscarded proposal\nyes\n/cancel\n0\n", false, 120);
        require(!cancelled.contains("End of input") && Arrays.equals(before, WorkspaceCodec.encode(sourceStore.load(sourceState.activeBaseline().workspaceId()))), "slash-cancel exits the entire proposed setup rather than being swallowed by overview validation");
        Files.move(imported, base.resolve("source-offline"));
        String offline = session(sourceStore, "1\n1\n7\n13\n0\n0\n0\n", false, 120);
        require(offline.contains("Files have not been checked for changes during this session") && offline.contains("Project Map"), "saved context opens while original files are offline without a hidden scan");
        var bootstrap = new BootstrapService(ScanPolicy.defaults(), () -> "W779");
        var draft = bootstrap.prepare(EntryMode.NEW_EMPTY, "Narrow map", ProjectProfile.GENERAL, null, new CurrentContext("Read sources", "Report", "Review evidence", "", ""), false, n -> true);
        var projection = new ProjectMapService().fromDraft(draft);
        var buckets = projection.buckets();
        buckets[ProjectMap.Section.SOURCES.ordinal()] = new ProjectMap.Bucket(ProjectMap.Section.SOURCES, 65, new ProjectMap.Item[0]);
        var sourceLimited = new ProjectMap(projection.workspaceId(), projection.project(), projection.profile(), true, false, projection.coverage(), buckets);
        var limitedText = new ByteArrayOutputStream();
        new ProjectMapConsole(prompt -> "0", new PrintStream(limitedText, true, StandardCharsets.UTF_8), new TerminalTheme(false), 120).overview(sourceLimited);
        require(limitedText.toString(StandardCharsets.UTF_8).contains("Read sources") && !limitedText.toString(StandardCharsets.UTF_8).contains("no unique source"), "unrelated document-text limit does not manufacture purpose/current-field uncertainty");
        for (int width : new int[] {28, 40, 80, 120, 160}) {
            for (boolean color : new boolean[] {false, true}) {
                var bytes = new ByteArrayOutputStream(); var out = new PrintStream(bytes, true, StandardCharsets.UTF_8);
                var choices = new java.util.ArrayDeque<>(Arrays.asList("?", "1", "n", "p", "1", "t", "0", "0", "2", "0", "3", "0", "4", "0", "5", "0", "6", "0", "7", "s", "1", "0", "0", "0", "8", "0", "0"));
                var console = new ProjectMapConsole(prompt -> choices.remove(), out, new TerminalTheme(color), width);
                console.overview(new ProjectMapService().fromDraft(draft)); console.run(new ProjectMapService().fromDraft(draft));
                require(choices.isEmpty(), "all map sections, details, help, paging, source groups and back actions consume their displayed route");
                String text = bytes.toString(StandardCharsets.UTF_8).replaceAll("\u001b\\[[0-9;]*m", "");
                for (String line : text.split("\\r?\\n")) { require(line.length() <= Math.min(width, 120), "overview/map respect terminal width " + width); }
                require(text.replaceAll("\\s+", " ").contains("recorded context, not independently verified work"), "uncertainty disclaimer survives narrow/color rendering");
            }
        }
        var notes = new WorkspaceState(); bootstrap.confirm(notes, draft, true);
        notes.manualContext().capture(ManualContext.Type.PERSONAL_OBSERVATION, "Recorded local note text", "Planning desk", "", ManualContext.Sensitivity.INTERNAL);
        notes.manualContext().capture(ManualContext.Type.PERSONAL_OBSERVATION, "Private note must remain absent", "Private origin", "", ManualContext.Sensitivity.SENSITIVE);
        var bytes = new ByteArrayOutputStream();
        var keys = new java.util.ArrayDeque<>(Arrays.asList("7", "2", "v", "yes", "r"));
        var manualRequest = new ProjectMapConsole(prompt -> keys.remove(), new PrintStream(bytes, true, StandardCharsets.UTF_8), new TerminalTheme(false), 120).run(new ProjectMapService().fromState(notes));
        require(manualRequest.action() == ProjectMapConsole.Action.REVIEW_NOTE && keys.isEmpty() && bytes.toString(StandardCharsets.UTF_8).contains("Recorded local note text") && !bytes.toString(StandardCharsets.UTF_8).contains("Private note must remain absent"), "non-private manual display requires explicit V consent and review routes to existing note owner");
        System.out.println("PASS: Project Overview/setup, selected review, proposed assessment, cancellation, restart/offline, source grouping and five widths | " + base);
    }
    private static String session(WorkspaceStore store, String input, boolean color, int width) throws Exception {
        var bytes = new ByteArrayOutputStream();
        new ProjectConsole(new StringReader(input), new PrintStream(bytes, true, StandardCharsets.UTF_8), store, new TerminalTheme(color), width).run();
        return bytes.toString(StandardCharsets.UTF_8);
    }
}
