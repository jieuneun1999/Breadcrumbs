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
    SearchConsole(ContextConsole.Input input, PrintStream out, ContextSearchIndex index, int width) {
        this.input = input; this.out = out; this.index = index; this.width = Math.max(28, Math.min(width, 120));
    }
    void run() throws IOException {
        while (true) {
            print("SEARCH PROJECT CONTEXT\n" + index.status()
                    + "\n[1] Search exact term\n[2] Prefix suggestions / choose term\n[3] Search coverage\n[4] Rebuild saved-field index\n[5] Index health / DSA demonstration\n[6] Source-term privacy policy\n[7] Enable / disable search index\n[0] Back");
            String choice = read("search > ");
            try {
                switch (choice) {
                    case "0" -> { return; }
                    case "1" -> matches(read("Exact term: "));
                    case "2" -> suggestions();
                    case "3" -> print(index.coverage());
                    case "4" -> print("Rebuild completed=" + index.rebuild(index.policy(), () -> false) + "\n" + index.diagnostics());
                    case "5" -> diagnostics();
                    case "6" -> policy();
                    case "7" -> { index.setEnabled(index.status() == SearchReport.Status.DISABLED); print("Search " + index.status()); }
                    default -> print("Choose 0-7.");
                }
            } catch (IllegalArgumentException invalid) { print("Rejected. " + ContentSafety.terminal(invalid.getMessage(), 200)); }
        }
    }
    private void policy() throws IOException {
        if (index.policy().includeUnreviewed()) {
            index.setPolicy(index.policy().withUnreviewed(false));
            print("Unreviewed source terms excluded. Rebuild required."); return;
        }
        print("Unreviewed source terms may disclose private captured wording. Snippets stay withheld. Instructions, owner fields and sensitive content stay excluded.");
        print("Include safe unreviewed captured terms for this workspace session? Type yes.");
        if (read("approve > ").equalsIgnoreCase("yes")) {
            index.setPolicy(index.policy().withUnreviewed(true)); print("Session opt-in recorded. Rebuild required.");
        } else { print("Privacy policy unchanged."); }
    }
    private void suggestions() throws IOException {
        String prefix = read("Prefix: ");
        var result = index.suggestTerms(prefix, index.policy().maxSuggestions());
        String[] terms = result.terms();
        print("Trie suggestions. " + index.status());
        for (int i = 0; i < terms.length; i++) { print("[" + (i + 1) + "] " + terms[i]); }
        if (result.limited()) { print("Suggestion result/visited-node limit reached or index unavailable; not exhaustive."); }
        if (terms.length == 0) { print("No suggestions in indexed fields. See coverage or rebuild."); return; }
        print("[0] Back"); String choice = read("term > ");
        if (choice.equals("0")) { return; }
        int number = number(choice, terms.length); matches(terms[number - 1]);
    }
    private void matches(String term) throws IOException {
        SearchReport report = index.searchExact(term, index.policy().maxResults());
        print(report.status() + "\nExact term: " + report.term() + "\n" + report.notice());
        if (report.status() != SearchReport.Status.READY) { return; }
        SearchReport.Hit[] hits = report.hits();
        print("Hash exact lookup -> manual posting chain -> canonical records");
        for (int i = 0; i < hits.length; i++) {
            var hit = hits[i]; var location = hit.location();
            print("[" + (i + 1) + "] " + location.recordId() + " | " + location.field());
            print(location.historical() ? "HISTORICAL CHECKPOINT" : "CURRENT SAVED " + location.kind());
            print("Source: " + location.locator() + "\nObservation: " + location.observationId()
                    + "\nRecorded: " + location.recordedAt() + "\nTruth: " + location.truth() + "\nReview: " + location.review()
                    + "\nSensitivity: " + location.sensitivity() + "\nOccurrences: " + hit.occurrences());
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
    private void print(String text) {
        for (String line : text.split("\\n", -1)) {
            String safe = ContentSafety.terminal(line, 4000);
            while (safe.codePointCount(0, safe.length()) > width) {
                int end = safe.offsetByCodePoints(0, width); out.println(safe.substring(0, end)); safe = safe.substring(end);
            }
            out.println(safe);
        }
    }
}
