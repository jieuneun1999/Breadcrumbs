package breadcrumbs.search;

import breadcrumbs.domain.WorkspaceState;
import breadcrumbs.dsa.trie.PrefixTrie;
import java.util.function.BooleanSupplier;
import java.util.Arrays;
import breadcrumbs.inspector.ContentSafety;
import breadcrumbs.inspector.SnapshotEntry;

public final class ContextSearchIndex {
    private final WorkspaceState state;
    private SearchPolicy policy = SearchPolicy.defaults();
    private SearchIndexBuilder active;
    private Revision revision;
    private boolean enabled = true, invalid;
    private String notice = "No index built. Rebuild uses saved canonical fields only.";
    private record Revision(String workspace, String observation, long epoch, long dll,
            String graph, SearchPolicy policy) { }
    public ContextSearchIndex(WorkspaceState state) {
        if (state == null) { throw new IllegalArgumentException("Workspace state required."); } this.state = state;
    }
    private Revision currentRevision() {
        return state.activeBaseline() == null ? null : new Revision(state.activeBaseline().workspaceId(), state.currentObservationId(),
                state.epoch(), state.checkpoints().modification(), state.dependencies().currentId(), policy);
    }
    public boolean rebuild(SearchPolicy policy, BooleanSupplier cancelled) {
        setPolicy(policy); Revision requested = currentRevision();
        try {
            if (requested == null) { throw new IllegalArgumentException("No active workspace."); }
            SearchIndexBuilder candidate = SearchIndexBuilder.build(state, policy, cancelled);
            if (!requested.equals(currentRevision())) { throw new IllegalArgumentException("Canonical state changed during build."); }
            active = candidate; revision = requested; invalid = false;
            notice = "READY for captured indexed fields. TXT/MD/CSV captured fields only. Unsupported bodies not searched. Scan complete="
                    + state.currentObservation().complete() + ". Live source freshness UNKNOWN; no source reread. Checkpoints are historical assessments.";
            return true;
        } catch (IllegalArgumentException failure) {
            notice = ContentSafety.terminal(failure.getMessage(), 200) + " No partial candidate published. Rebuild with compatible limits/policy.";
            return false;
        }
    }
    public void setPolicy(SearchPolicy policy) { if (policy == null) { throw new IllegalArgumentException("Search policy required."); } this.policy = policy; }
    public SearchPolicy policy() { return policy; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void invalidate(String reason) { invalid = true; notice = ContentSafety.terminal(reason, 200); }
    public SearchReport.Status status() {
        if (!enabled) { return SearchReport.Status.DISABLED; }
        if (active == null) { return SearchReport.Status.INDEX_UNAVAILABLE; }
        return invalid || !revision.equals(currentRevision()) ? SearchReport.Status.INDEX_OUTDATED : SearchReport.Status.READY;
    }
    public boolean isCurrent(WorkspaceState candidate) { return candidate == state && status() == SearchReport.Status.READY; }
    public boolean validateAgainstCanonicalState() {
        if (!isCurrent(state)) { return false; }
        try {
            boolean valid = active.validate(state, policy, () -> false);
            if (!valid) { invalidate("Canonical search validation failed."); } return valid;
        } catch (IllegalArgumentException failure) { invalidate("Canonical search validation failed."); return false; }
    }
    public SearchReport searchExact(String term, int limit) {
        String normalized = Tokenizer.query(term, policy);
        if (limit < 1) { throw new IllegalArgumentException("Positive result limit required."); }
        if (!isCurrent(state)) { return unavailable(normalized); }
        PostingList postings = active.dictionary.get(normalized);
        if (postings == null) { return new SearchReport(status(), normalized, new SearchReport.Hit[0], 0, false, notice); }
        if (!postings.validate(active.fieldCount)) { invalidate("Invalid posting chain. Rebuild required."); return unavailable(normalized); }
        SearchReport.Hit[] hits = new SearchReport.Hit[Math.min(Math.min(limit, policy.maxResults()), postings.size)]; int count = 0;
        for (PostingNode node = postings.first; node != null; node = node.next) {
            if (count == hits.length) { break; }
            SearchLocation location = active.fields[node.field];
            if (SearchFields.resolve(state, policy, location) == null) { invalidate("Unresolved or ineligible canonical reference."); return unavailable(normalized); }
            hits[count++] = new SearchReport.Hit(location, node.occurrences);
        }
        return new SearchReport(status(), normalized, Arrays.copyOf(hits, count), postings.size, count < postings.size, notice);
    }
    private SearchReport unavailable(String term) {
        return new SearchReport(status(), term, new SearchReport.Hit[0], 0, false, "Search incomplete/unavailable. Rebuild required. " + notice);
    }
    public PrefixTrie.Suggestions suggestTerms(String prefix, int limit) {
        String normalized = Tokenizer.query(prefix, policy);
        if (limit < 1) { throw new IllegalArgumentException("Positive suggestion limit required."); }
        return isCurrent(state) ? active.trie.suggest(normalized, Math.min(limit, policy.maxSuggestions()), policy.maxVisits())
                : new PrefixTrie.Suggestions(new String[0], true, 0);
    }
    public String viewField(SearchLocation location, boolean approved) {
        if (!approved || !isCurrent(state)) { return ""; }
        SearchFields.Field field = SearchFields.resolve(state, policy, location);
        if (field == null || !location.review().equals("CONFIRMED")) { return ""; }
        String value = field.value(); int end = Math.min(value.length(), policy.maxSnippet());
        if (end < value.length() && end > 0 && Character.isHighSurrogate(value.charAt(end - 1))) { end--; }
        return ContentSafety.terminal(value.substring(0, end), policy.maxSnippet()) + (end < value.length() ? "..." : "");
    }
    public String diagnostics() {
        if (!isCurrent(state)) { return status() + ". Rebuild required. " + notice; }
        return "Trie terms=" + active.trie.termCount() + " nodes=" + active.trie.nodeCount()
                + "; Hash terms=" + active.dictionary.size() + " buckets=" + active.dictionary.capacity()
                + "; manual posting nodes=" + active.postingCount + "; indexed fields=" + active.fieldCount
                + "; characters budgeted=" + active.characters + "; canonical valid=" + validateAgainstCanonicalState();
    }
    public String coverage() {
        if (state.activeBaseline() == null) { return "No active workspace."; }
        var scan = state.currentObservation(); int supported = 0, metadata = 0, skipped = 0, failed = 0, unsupported = 0, sensitive = 0, fingerprinted = 0;
        for (SnapshotEntry entry : scan.entries()) {
            if (entry.adapter() == SnapshotEntry.Adapter.METADATA_ONLY && entry.status() == SnapshotEntry.Status.UNSUPPORTED) { metadata++; }
            if (entry.status() == SnapshotEntry.Status.SUPPORTED && entry.adapter() != SnapshotEntry.Adapter.DIRECTORY) { supported++; }
            if (entry.status() == SnapshotEntry.Status.SKIPPED) { skipped++; }
            if (entry.status() == SnapshotEntry.Status.FAILED || entry.status() == SnapshotEntry.Status.UNSTABLE) { failed++; }
            if (entry.status() == SnapshotEntry.Status.UNSUPPORTED) { unsupported++; }
            if (!entry.sha256().isEmpty()) { fingerprinted++; }
            if (entry.reason().toLowerCase(java.util.Locale.ROOT).contains("sensitive")) { sensitive++; }
        }
        int[] eligible = {0};
        for (var claim : state.claimsAt(state.currentObservationId())) {
            SearchFields.visitEvidence(state, policy, claim, field -> eligible[0]++);
        }
        return "Search status " + status() + ". Observation " + state.currentObservationId()
                + "\nDiscovered entries=" + scan.entries().length + "; fingerprinted=" + fingerprinted + "; sensitivity-skipped=" + sensitive
                + "\nTXT/MD/CSV: captured fields only; supported files=" + supported
                + "; eligible evidence fields=" + eligible[0] + "; policy-excluded/unindexed evidence fields="
                + (state.claimsAt(state.currentObservationId()).length - eligible[0])
                + "\nmetadata-only files=" + metadata + "; unsupported=" + unsupported + "; skipped=" + skipped
                + "; failed/unstable=" + failed + "; scan complete=" + scan.complete()
                + "\nPDF/DOCX/XLSX/PPTX/images: body extraction unavailable, body not searched."
                + "\nJava/JSON/XML/YAML: metadata-only in this implementation. No full-text/OCR/semantic search."
                + "\nSkipped includes exclusions/sensitivity; unsupported/failed portions may contain unknown terms."
                + "\nSource terms require confirmation or explicit session opt-in; unreviewed snippets withheld."
                + "\nLive source freshness UNKNOWN. Current saved evidence and current work items; checkpoint fields labeled historical."
                + (isCurrent(state) ? "\nIndexed canonical fields=" + active.fieldCount : "\nNo complete current index. Rebuild before interpreting matches.");
    }
}
