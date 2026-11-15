package breadcrumbs.search;

/** Full immutable policy participates in revision equality, including all resource limits. */
public record SearchPolicy(int maxTerms, int maxTermLength, int maxCharacters,
        int maxTokensPerField, int maxPostings, int maxPostingsPerTerm,
        int maxSuggestions, int maxVisits, int maxResults, int maxSnippet,
        int maxFields, int maxTrieNodes, boolean includeUnreviewed,
        String version, String tokenizerVersion, String contentVersion) {
    public static SearchPolicy defaults() {
        return new SearchPolicy(10000, 64, 1000000, 2000, 50000, 2000,
                10, 5000, 20, 160, 10000, 200000, false, "SEARCH-1", "NFKC-ROOT-1", "CAPTURED-3");
    }
    public SearchPolicy {
        if (maxTerms < 1 || maxTerms > 50000 || maxTermLength < 1 || maxTermLength > 128
                || maxCharacters < 1 || maxCharacters > 4000000 || maxTokensPerField < 1 || maxTokensPerField > 10000
                || maxPostings < 1 || maxPostings > 100000 || maxPostingsPerTerm < 1 || maxPostingsPerTerm > maxPostings
                || maxSuggestions < 1 || maxSuggestions > 50 || maxVisits < 1 || maxVisits > 200000
                || maxResults < 1 || maxResults > 100 || maxSnippet < 1 || maxSnippet > 400
                || maxFields < 1 || maxFields > 20000 || maxTrieNodes < 2 || maxTrieNodes > 500000
                || version == null || version.length() > 40 || contentVersion == null || contentVersion.length() > 40
                || tokenizerVersion == null || tokenizerVersion.length() > 40) {
            throw new IllegalArgumentException("Unsupported search policy/version or unsafe resource bounds.");
        }
    }
    public SearchPolicy withUnreviewed(boolean allowed) {
        return new SearchPolicy(maxTerms, maxTermLength, maxCharacters, maxTokensPerField, maxPostings,
                maxPostingsPerTerm, maxSuggestions, maxVisits, maxResults, maxSnippet, maxFields,
                maxTrieNodes, allowed, version, tokenizerVersion, contentVersion);
    }
}
