package breadcrumbs.search;

public record SearchReport(Status status, String term, Hit[] hits, int totalMatches,
        boolean limited, String notice) {
    public enum Status { READY, INDEX_OUTDATED, INDEX_UNAVAILABLE, DISABLED }
    public record Hit(SearchLocation location, int occurrences) { }
    public SearchReport { hits = hits.clone(); }
    @Override public Hit[] hits() { return hits.clone(); }
}
