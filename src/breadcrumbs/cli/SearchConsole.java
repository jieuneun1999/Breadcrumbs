package breadcrumbs.cli;

import breadcrumbs.search.*;
import breadcrumbs.dsa.trie.PrefixTrie;
import breadcrumbs.inspector.ContentSafety;
import java.io.IOException;
import java.io.PrintStream;

/** A numbered, bounded lexical-search flow. No source filesystem or execution access. */
final class SearchConsole {
    private final ContextConsole.Input input;
    private final PrintStream out;
    private final ContextSearchIndex index;
    private final int width;
    private final TerminalTheme theme;
    private final boolean technical;
    SearchConsole(ContextConsole.Input input, PrintStream out, ContextSearchIndex index, int width) {
        this(input, out, index, width, new TerminalTheme(false));
    }
    SearchConsole(ContextConsole.Input input, PrintStream out, ContextSearchIndex index, int width, TerminalTheme theme) {
        this(input, out, index, width, theme, true);
    }
    SearchConsole(ContextConsole.Input input, PrintStream out, ContextSearchIndex index, int width, TerminalTheme theme, boolean technical) {
        this.input = input; this.out = out; this.index = index; this.width = Math.max(28, Math.min(width, 120));
        this.theme = theme;
        this.technical = technical;
    }
    private static final String ACTIONS = "[1] Search exact term\n[2] Suggest terms by prefix\n[3] View search coverage\n[4] Refresh search index\n[5] View diagnostics and DSA demonstration\n[6] Configure unreviewed-text search\n[7] Enable or disable search\n[0] Return to previous menu\n[?] Show menu";
    void run() throws IOException {
        boolean showMenu = true;
        while (true) {
            var ui = new TerminalOutput(out, theme, width);
            if (showMenu) {
                ui.heading(technical ? "\nSEARCH PROJECT CONTEXT" : "\nSearch project records");
                if (technical) { ui.plain(index.status().name()); }
                else { ui.note("Search the notes and document text you allowed Breadcrumbs to use. Refresh search after recording changes."); }
                ui.menu(ACTIONS); showMenu = false;
            } else { ui.heading("\nSearch actions"); ui.choices(ACTIONS); }
            String choice = read("Search > ");
            try {
                switch (choice) {
                    case "0" -> { return; }
                    case "?" -> showMenu = true;
                    case "1" -> matches(read(technical ? "Exact term: " : "Search term: "));
                    case "2" -> suggestions();
                    case "3" -> {
                        if (!technical) { print("Coverage describes the saved file check, not a new scan. Only captured fields allowed by your search settings are included."); }
                        print(index.coverage());
                    }
                    case "4" -> {
                        boolean rebuilt;
                        try (var feedback = new OperationFeedback(out, theme, width, OperationFeedback.Operation.REBUILD)) {
                            rebuilt = index.rebuild(index.policy(), () -> false);
                            if (rebuilt) { feedback.complete(false, technical ? "Search projection validated and published." : "You can search the text allowed for this project."); }
                            else { feedback.failed("Search was not updated. Check the file coverage and search settings before trying again."); }
                        }
                        if (technical) { print("Rebuild completed=" + rebuilt + "\n" + index.diagnostics()); }
                    }
                    case "5" -> diagnostics();
                    case "6" -> policy();
                    case "7" -> { index.setEnabled(index.status() == SearchReport.Status.DISABLED); print(technical ? "Search " + index.status() : index.status() == SearchReport.Status.DISABLED ? "Search is off." : "Search is on. Refresh it before looking for words."); }
                    default -> print("Choose 0-7.");
                }
            } catch (IllegalArgumentException invalid) { print("Rejected. " + ContentSafety.terminal(invalid.getMessage(), 200)); }
        }
    }
    private void policy() throws IOException {
        if (index.policy().includeUnreviewed()) {
            index.setPolicy(index.policy().withUnreviewed(false));
            print("Unreviewed document words are excluded. Refresh search to apply this change."); return;
        }
        print("Unreviewed document words may include wording you have not checked yet. Private notes and known sensitive content stay excluded; unreviewed passages are not displayed.");
        print("Allow safe unreviewed document words in search until you leave this project? Type yes.");
        if (read("approve > ").equalsIgnoreCase("yes")) {
            index.setPolicy(index.policy().withUnreviewed(true)); print("Allowed for this visit to the project. Refresh search to apply the change.");
        } else { print("Privacy policy unchanged."); }
    }
    private void suggestions() throws IOException {
        String prefix = read(technical ? "Prefix: " : "Term prefix: ");
        var result = index.suggestTerms(prefix, index.policy().maxSuggestions());
        String[] terms = result.terms();
        print(technical ? "Trie suggestions. " + index.status() : "Words found in the text you can search:");
        for (int i = 0; i < terms.length; i++) { print("[" + (i + 1) + "] " + terms[i]); }
        if (result.limited()) { print("Suggestion result/visited-node limit reached or index unavailable; not exhaustive."); }
        if (terms.length == 0) { print("No suggestions in indexed fields. See coverage or rebuild."); return; }
        print("[0] Back"); String choice = read("term > ");
        if (choice.equals("0")) { return; }
        int number = number(choice, terms.length); matches(terms[number - 1]);
    }
    private void matches(String term) throws IOException {
        SearchReport report = index.searchExact(term, index.policy().maxResults());
        var ui = new TerminalOutput(out, theme, width); ui.heading("Search results");
        if (!technical) { friendlyMatches(report); return; }
        ui.line("Index state: ", report.status().name()); ui.line("Exact term: ", report.term()); ui.plain(report.notice());
        if (report.status() != SearchReport.Status.READY) { return; }
        SearchReport.Hit[] hits = report.hits();
        print("Hash exact lookup -> manual posting chain -> canonical records");
        for (int i = 0; i < hits.length; i++) {
            var hit = hits[i]; var location = hit.location();
            print("[" + (i + 1) + "] " + location.recordId() + " | " + location.field());
            print(location.historical() ? "HISTORICAL CHECKPOINT" : "CURRENT SAVED " + location.kind());
            ui.line("Source: ", location.locator()); ui.line("Observation: ", location.observationId());
            ui.line("Recorded: ", location.recordedAt().toString()); ui.line("Truth: ", location.truth());
            ui.line("Review: ", location.review()); ui.line("Sensitivity: ", location.sensitivity()); ui.line("Occurrences: ", Integer.toString(hit.occurrences()));
        }
        print("Showing " + hits.length + " of " + report.totalMatches() + " indexed field matches." + (report.limited() ? " Results capped." : ""));
        if (hits.length == 0) { print("No indexed matches. This does not prove absence from source documents. See coverage."); return; }
        print("View a bounded confirmed matching field, or 0 to return. Pending source snippets are withheld.");
        String choice = read("record > "); if (choice.equals("0")) { return; }
        int number = number(choice, hits.length);
        print("Display this INTERNAL confirmed field? Type yes.");
        String value = index.viewField(hits[number - 1].location(), read("approve > ").equalsIgnoreCase("yes"));
        print(value.isEmpty() ? "Field withheld, cancelled or no longer eligible. Rebuild if outdated." : value);
    }
    private void friendlyMatches(SearchReport report) throws IOException {
        var ui = new TerminalOutput(out, theme, width); ui.line("Word: ", report.term());
        if (report.status() != SearchReport.Status.READY) {
            ui.warning(report.status() == SearchReport.Status.DISABLED ? "Search is turned off. Choose option 7 to turn it on." : "Search needs a refresh before showing results. Choose option 4."); return;
        }
        var hits = report.hits();
        for (int i = 0; i < hits.length; i++) {
            var hit = hits[i]; var location = hit.location();
            ui.line("[" + (i + 1) + "] ", location.kind() == SearchLocation.Kind.MANUAL ? "A note you added" : location.historical() ? "An earlier progress update" : "A project document or task");
            ui.line("Where: ", location.historical() ? "Your saved progress updates" : location.kind() == SearchLocation.Kind.WORK_ITEM ? "Your project tasks" : location.locator());
            if (location.kind() == SearchLocation.Kind.EVIDENCE || location.kind() == SearchLocation.Kind.MANUAL) { ui.note("This is recorded information. Breadcrumbs has not checked whether the reported work happened."); }
            ui.line("Matches in this item: ", Integer.toString(hit.occurrences()));
        }
        if (hits.length == 0) { ui.note("No matches in the text that can currently be searched. Other text may be unread, unsupported or not approved for search."); return; }
        if (report.limited()) { ui.note("More matches may exist. This screen shows a limited set."); }
        String choice = read("Item number to read, or 0 to go back: "); if (choice.equals("0")) { return; }
        int n = number(choice, hits.length);
        String value = index.viewField(hits[n - 1].location(), read("Show this reviewed text on this computer? Type yes: ").equalsIgnoreCase("yes"));
        print(value.isEmpty() ? "The text was not shown. It may be unreviewed, no longer available, or you chose not to display it." : value);
    }
    private void diagnostics() {
        print(index.diagnostics());
        PrefixTrie demo = new PrefixTrie(10, 100, 64);
        for (String term : new String[] {"method", "methods", "methodology"}) { demo.insert(term); }
        print("Separate synthetic Trie demonstration. Canonical records unchanged.");
        print("3 terms share nodes=" + demo.nodeCount() + "; exact method=" + demo.contains("method") + "; exact meth=" + demo.contains("meth"));
        print("meth -> " + String.join(", ", demo.suggest("meth", 10, 100).terms()) + "; valid=" + demo.validate());
        print("Trie navigation scans sorted siblings. Hash uses B5 separate chaining. Exact results show posting counts and canonical locators. No measured speedup claim.");
    }
    private static int number(String value, int maximum) {
        int number;
        try { number = Integer.parseInt(value); }
        catch (NumberFormatException invalid) { throw new IllegalArgumentException("Choose a listed number."); }
        if (number < 1 || number > maximum) { throw new IllegalArgumentException("Choose a listed number."); }
        return number;
    }
    private String read(String prompt) throws IOException {
        String value = input.read(prompt); out.println(); return value;
    }
    private void print(String text) { new TerminalOutput(out, theme, width).plain(text); }
}
