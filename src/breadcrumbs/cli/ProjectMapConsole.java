package breadcrumbs.cli;

import breadcrumbs.continuity.*;
import breadcrumbs.domain.EvidenceClaim;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import static breadcrumbs.continuity.ProjectMap.*;

final class ProjectMapConsole {
    enum Action { BACK, REVIEW_SOURCE, REVIEW_NOTE, CLARIFY }
    record Request(Action action, Item item) { }
    private final ContextConsole.Input input;
    private final TerminalOutput ui;
    private final TerminalTheme theme;
    ProjectMapConsole(ContextConsole.Input input, PrintStream out, TerminalTheme theme, int width) {
        this.input = input; this.theme = theme; ui = new TerminalOutput(out, theme, width);
    }
    static String basis(Item item) {
        return switch (item.kind()) {
            case SOURCE -> "Source-reported; " + ProjectPresentation.review(item.review());
            case STARTING_CONTEXT -> "Supplied by you";
            case ASSESSMENT -> "Your current assessment";
            case PROPOSED_ASSESSMENT -> "Your proposed assessment; not committed";
            case HISTORY -> "Recorded history; not current verification";
            case MANUAL -> "Project note; " + ProjectPresentation.review(item.review());
            case TASK -> "Explicitly tracked task"; case DEPENDENCY -> "Declared dependency";
            case FILE -> "Observed file metadata"; case FINDING, CONFLICT -> "Derived review finding";
        };
    }
    void overview(ProjectMap map) {
        ui.heading("\nProject overview / What we found"); ui.line("Project: ", map.project()); ui.line("Profile: ", map.profile().name());
        ui.note(map.draft() ? "Initial review. The starting record has not been confirmed or saved." : "Current view of saved records; historical statements retain their original context.");
        boolean recognizable = Arrays.stream(map.buckets()).flatMap(b -> Arrays.stream(b.items())).anyMatch(i ->
                !i.historical() && !i.withheld() && i.review() != EvidenceClaim.ReviewDisposition.REJECTED && !i.value().startsWith("[Unsafe") && !i.value().equals("UNKNOWN") && !i.value().equals("NOT_DOCUMENTED")
                        && (i.field() == EvidenceClaim.Field.OBJECTIVE || i.field() == EvidenceClaim.Field.CURRENT_STATE || i.field() == EvidenceClaim.Field.NEXT_ACTION));
        if (!recognizable) { ui.note("Available supported content did not establish the project goal, current state or next action. You can supply those details below."); }
        field(map, "Project goal", EvidenceClaim.Field.OBJECTIVE);
        field(map, "Documented current state", EvidenceClaim.Field.CURRENT_STATE, EvidenceClaim.Field.PROGRESS);
        field(map, "Next action", EvidenceClaim.Field.NEXT_ACTION);
        Item[] unknowns = map.bucket(Section.UNKNOWNS).items();
        Item attention = Arrays.stream(unknowns).filter(i -> i.kind() == Kind.CONFLICT).findFirst().orElse(unknowns.length == 0 ? null : unknowns[0]);
        ui.value("Needs attention", attention == null ? "Review the recorded context before relying on it." : ProjectMapService.safe(attention.title() + ": " + attention.value(), 240));
        ui.value("Inspection coverage", map.coverage());
        ui.note("Statements describe recorded context, not independently verified work. Original project files are not modified.");
    }
    private void field(ProjectMap map, String label, EvidenceClaim.Field... fields) {
        Item[] found = Arrays.stream(map.buckets()).flatMap(b -> Arrays.stream(b.items()))
                .filter(i -> !i.historical() && !i.withheld() && i.review() != EvidenceClaim.ReviewDisposition.REJECTED
                        && !i.value().startsWith("[Unsafe") && Arrays.asList(fields).contains(i.field()) && i.kind() != Kind.FINDING && i.kind() != Kind.CONFLICT)
                .toArray(Item[]::new);
        Item[] human = Arrays.stream(found).filter(i -> i.kind() == Kind.ASSESSMENT || i.kind() == Kind.PROPOSED_ASSESSMENT).toArray(Item[]::new);
        if (human.length > 0) { found = human; }
        if (found.length == 0) {
            boolean limited = limited(map, fields);
            ui.value(label, limited ? "Not shown in this bounded view; additional source records may contain relevant context." : "Not documented / Unknown. Add a current assessment if you know it."); return;
        }
        boolean limited = limited(map, fields);
        ui.value(label, found.length == 1 ? (found[0].value().equals("UNKNOWN") || found[0].value().equals("NOT_DOCUMENTED") ? "Not documented / Unknown." : ProjectMapService.safe(found[0].value(), 220)) : found.length + " documented candidates; no automatic selection.");
        if (limited) { ui.note("This map is bounded. Further candidates may exist in the original source records; no unique source is established by this excerpt."); }
        for (int i = 0; i < Math.min(found.length, 2); i++) {
            Item item = found[i];
            if (found.length > 1) { ui.plain(ProjectMapService.safe(item.value(), 100)); }
            ui.line("Basis: ", ProjectMapService.safe(basis(item) + (item.link().path().isEmpty() ? "" : " | " + item.link().path() + " " + item.link().locator()), 200));
        }
        if (found.length > 2) { ui.note("More alternatives are available in the Project Map."); }
    }
    private static boolean limited(ProjectMap map, EvidenceClaim.Field[] fields) {
        return Arrays.stream(fields).map(ProjectMapService::section).distinct().map(map::bucket).anyMatch(b -> b.total() > b.items().length);
    }
    Request run(ProjectMap map) throws IOException {
        while (true) {
            ui.heading("\nProject Map"); ui.line("Project: ", map.project());
            ui.note(map.draft() ? "Unconfirmed proposal. Exploring does not create a project or update." : map.coverage().startsWith("No folder linked")
                    ? "No folder linked. Supplied context and recorded updates remain useful without files." : map.checkedThisSession()
                    ? "The latest file observation was recorded during this project session; it is not continuous monitoring."
                    : "Files have not been checked for changes during this session.");
            if (theme.companion() == null) {
                for (var bucket : map.buckets()) { ui.menu("[" + (bucket.section().ordinal() + 1) + "] " + bucket.section().title()); ui.note("      " + (bucket.total() == 0 ? "Not documented / Unknown" : bucket.total() + " recorded items")); }
                ui.menu("[0] Back to " + (map.draft() ? "overview" : "workspace") + "\n[?] Map help");
            } else {
                StringBuilder actions = new StringBuilder();
                for (var bucket : map.buckets()) {
                    actions.append('[').append(bucket.section().ordinal() + 1).append("] ").append(bucket.section().title()).append('\n')
                            .append("      ").append(bucket.total() == 0 ? "Not documented / Unknown" : bucket.total() + " recorded items").append('\n');
                }
                actions.append("[0] Back to ").append(map.draft() ? "overview" : "workspace").append("\n[?] Map help");
                ui.screen(BreadFrames.Mood.CURIOUS, "Want a clearer picture of your recorded project context?", actions.toString(), false);
            }
            String choice = input.read("Map > ");
            if (choice.equals("0")) { return new Request(Action.BACK, null); }
            if (choice.equals("?")) { ui.note("The map organizes saved context, not folders. Select a section, then an item. Review decisions do not verify reported work. Private notes and excluded file names are omitted."); continue; }
            int selected = number(choice, 8); if (selected < 0) { continue; }
            Request request = section(map.bucket(Section.values()[selected - 1]), map.draft(), false);
            if (request.action() != Action.BACK) { return request; }
        }
    }
    private Request section(Bucket bucket, boolean draft, boolean grouped) throws IOException {
        int page = 0; Item[] items = bucket.items();
        while (true) {
            ui.heading("\n" + bucket.section().title()); ui.note(bucket.section().explanation());
            if (items.length == 0) { ui.note("Not documented / Unknown. You can supply a current assessment without inventing a source statement."); }
            for (int i = page; i < Math.min(page + ProjectMapService.PAGE_SIZE, items.length); i++) {
                Item item = items[i]; ui.menu("[" + (i + 1) + "] " + item.title());
                ui.plain(item.withheld() ? "Recorded note text withheld; select the item to display locally." : ProjectMapService.safe(item.value(), 180));
                ui.line("Basis: ", basis(item));
                if (!item.link().path().isEmpty()) { ui.line("Source: ", item.link().path() + " " + item.link().locator()); }
            }
            if (bucket.total() > items.length) { ui.warning("Showing at most " + items.length + " of " + bucket.total() + " items. Existing history/source tools retain the remaining records."); }
            ui.menu("[N] Next page\n[P] Previous page\n[A] Add current assessment" + (bucket.section() == Section.SOURCES && !grouped ? "\n[S] Group file sources by role" : "") + "\n[0] Back to " + (grouped ? "source groups" : "map"));
            String choice = input.read("Section > ");
            if (choice.equals("0")) { return new Request(Action.BACK, null); }
            if (choice.equalsIgnoreCase("a")) { return new Request(Action.CLARIFY, null); }
            if (choice.equalsIgnoreCase("n")) { if (page + ProjectMapService.PAGE_SIZE < items.length) { page += ProjectMapService.PAGE_SIZE; } else { ui.note("Last page."); } continue; }
            if (choice.equalsIgnoreCase("p")) { page = Math.max(0, page - ProjectMapService.PAGE_SIZE); continue; }
            if (choice.equalsIgnoreCase("s") && bucket.section() == Section.SOURCES && !grouped) { Request request = sources(items, draft); if (request.action() != Action.BACK) { return request; } continue; }
            int selected = number(choice, items.length); if (selected < 0) { continue; }
            if (selected <= page || selected > Math.min(page + ProjectMapService.PAGE_SIZE, items.length)) { ui.warning("Select an item displayed on this page."); continue; }
            Request request = detail(items[selected - 1], draft); if (request.action() != Action.BACK) { return request; }
        }
    }
    private Request detail(Item item, boolean draft) throws IOException {
        while (true) {
            ui.heading("\nRecord details"); ui.value("Field / title", item.title()); ui.line("Basis: ", basis(item));
            ui.line("Recorded value (bounded excerpt):", "");
            ui.plain(item.withheld() ? "Text withheld. Use V to display this non-private note locally." : item.value());
            ui.plain(item.note());
            boolean review = item.kind() == Kind.SOURCE || item.kind() == Kind.MANUAL;
            ui.menu("[T] Technical record links\n[A] Add a current assessment" + (review ? "\n[R] Review this statement" : "") + (item.withheld() ? "\n[V] Display recorded text locally" : "") + "\n[0] Back to section");
            String choice = input.read("Record > ");
            if (choice.equals("0")) { return new Request(Action.BACK, null); }
            if (choice.equalsIgnoreCase("a")) { return new Request(Action.CLARIFY, item); }
            if (choice.equalsIgnoreCase("r") && review) { return new Request(item.kind() == Kind.MANUAL ? Action.REVIEW_NOTE : Action.REVIEW_SOURCE, item); }
            if (choice.equalsIgnoreCase("v") && item.withheld() && input.read("Display this recorded text on this computer? Type yes: ").equalsIgnoreCase("yes")) { ui.value("Recorded note", item.value()); }
            else if (choice.equalsIgnoreCase("t")) {
                var link = item.link(); ui.line("Workspace: ", link.workspaceId()); ui.line("Record: ", link.recordId()); ui.line("Observation: ", link.observationId());
                ui.line("Checkpoint: ", link.checkpointId()); ui.line("Source: ", link.path()); ui.line("Locator: ", link.locator()); ui.line("Recorded / observed: ", link.time().toString());
                ui.line("Original fingerprint: ", link.fingerprint().isEmpty() ? "Not applicable / not available" : link.fingerprint());
                ui.note(draft ? "This proposal is not committed." : "Historical references and original baseline are not rewritten by this view.");
            }
        }
    }
    private Request sources(Item[] items, boolean draft) throws IOException {
        while (true) {
            ui.heading("\nFile sources by role"); ui.note("Role is a classification hint, not proof of contents. Counts cover retained map rows only; excluded names are omitted.");
            for (var group : SourceGroup.values()) { long count = Arrays.stream(items).filter(i -> i.kind() == Kind.FILE && i.sourceGroup() == group).count(); ui.menu("[" + (group.ordinal() + 1) + "] " + groupName(group) + " | " + count + " shown files"); }
            ui.menu("[0] Back to source section"); String choice = input.read("Sources > "); if (choice.equals("0")) { return new Request(Action.BACK, null); }
            int selected = number(choice, SourceGroup.values().length); if (selected < 0) { continue; }
            var group = SourceGroup.values()[selected - 1]; Item[] selectedItems = Arrays.stream(items).filter(i -> i.kind() == Kind.FILE && i.sourceGroup() == group).toArray(Item[]::new);
            Request request = section(new Bucket(Section.SOURCES, selectedItems.length, selectedItems), draft, true);
            if (request.action() != Action.BACK) { return request; }
        }
    }
    private static String groupName(SourceGroup group) {
        return switch (group) { case PURPOSE -> "Purpose / context documents"; case CURRENT_STATE -> "Current state / progress documents"; case TASK_LIST -> "Task lists";
            case DECISION -> "Decision records"; case HANDOFF -> "Handoff documents"; case INSTRUCTIONS -> "Instructions (metadata only)";
            case OTHER -> "Other / unclassified"; case LIMITED -> "Unsupported or unextracted material"; };
    }
    private int number(String value, int max) {
        try { int number = Integer.parseInt(value); if (number >= 1 && number <= max) { return number; } }
        catch (NumberFormatException invalid) { }
        ui.warning("Select a displayed item or navigation key."); return -1;
    }
}
