package breadcrumbs.search;

import breadcrumbs.domain.WorkspaceState;
import breadcrumbs.dsa.hash.CustomHashTable;
import breadcrumbs.dsa.trie.PrefixTrie;
import java.util.function.BooleanSupplier;

/** Candidate structures stay detached until canonical and dictionary validation passes. */
final class SearchIndexBuilder {
    final PrefixTrie trie;
    final CustomHashTable<PostingList> dictionary;
    final SearchLocation[] fields;
    final String[] terms;
    int fieldCount, termCount, postingCount, characters;
    SearchIndexBuilder(SearchPolicy policy) {
        trie = new PrefixTrie(policy.maxTerms(), policy.maxTrieNodes(), policy.maxTermLength());
        dictionary = new CustomHashTable<>(4, policy.maxTerms());
        fields = new SearchLocation[policy.maxFields()]; terms = new String[policy.maxTerms()];
    }
    static SearchIndexBuilder build(WorkspaceState state, SearchPolicy policy, BooleanSupplier cancelled) {
        checkCancellation(cancelled);
        if (!policy.tokenizerVersion().equals("NFKC-ROOT-1") || !policy.contentVersion().equals("CAPTURED-1")) {
            throw new IllegalArgumentException("Unsupported tokenizer/content version. Index unavailable.");
        }
        SearchIndexBuilder candidate = new SearchIndexBuilder(policy);
        SearchFields.visit(state, policy, field -> {
            checkCancellation(cancelled);
            if (candidate.fieldCount == candidate.fields.length) { throw new IllegalArgumentException("Indexed field capacity exceeded."); }
            String[] tokens = Tokenizer.tokens(field.value(), policy);
            long added = field.value().length();
            for (String token : tokens) { added += token.length(); }
            if (candidate.characters + added > policy.maxCharacters()) { throw new IllegalArgumentException("Total indexed character budget exceeded."); }
            candidate.characters += (int) added;
            int slot = candidate.fieldCount; candidate.fields[candidate.fieldCount++] = field.location();
            for (String token : tokens) {
                checkCancellation(cancelled);
                PostingList postings = candidate.dictionary.get(token);
                if (postings == null) {
                    if (candidate.termCount == policy.maxTerms()) { throw new IllegalArgumentException("Unique term capacity exceeded."); }
                    candidate.trie.insert(token); postings = new PostingList(); candidate.dictionary.put(token, postings);
                    candidate.terms[candidate.termCount++] = token;
                }
                boolean repeated = postings.last != null && postings.last.field == slot;
                if (!repeated && candidate.postingCount == policy.maxPostings()) { throw new IllegalArgumentException("Total posting capacity exceeded."); }
                if (postings.add(slot, policy.maxPostingsPerTerm())) { candidate.postingCount++; }
            }
        });
        if (!candidate.validate(state, policy, cancelled)) { throw new IllegalArgumentException("Search candidate failed canonical validation."); }
        return candidate;
    }
    boolean validate(WorkspaceState state, SearchPolicy policy, BooleanSupplier cancelled) {
        checkCancellation(cancelled);
        if (!trie.validate() || !dictionary.validate() || trie.termCount() != termCount || dictionary.size() != termCount) { return false; }
        int postings = 0;
        for (int t = 0; t < termCount; t++) {
            checkCancellation(cancelled);
            PostingList list = dictionary.get(terms[t]);
            if (!trie.contains(terms[t]) || list == null || !list.validate(fieldCount)) { return false; }
            postings += list.size;
        }
        if (postings != postingCount) { return false; }
        int canonicalPostings = 0;
        for (int slot = 0; slot < fieldCount; slot++) {
            checkCancellation(cancelled);
            SearchFields.Field field = SearchFields.resolve(state, policy, fields[slot]);
            if (field == null) { return false; }
            String[] tokens = Tokenizer.tokens(field.value(), policy);
            CustomHashTable<Integer> counts = new CustomHashTable<>();
            for (String token : tokens) { Integer old = counts.get(token); counts.put(token, old == null ? 1 : old + 1); }
            for (String token : tokens) {
                Integer expected = counts.remove(token); if (expected == null) { continue; }
                PostingList list = dictionary.get(token); if (list == null) { return false; }
                PostingNode node = list.first;
                while (node != null && node.field < slot) { node = node.next; }
                if (node == null || node.field != slot || node.occurrences != expected) { return false; }
                canonicalPostings++;
            }
        }
        return canonicalPostings == postingCount;
    }
    private static void checkCancellation(BooleanSupplier cancelled) {
        if (cancelled.getAsBoolean() || Thread.currentThread().isInterrupted()) { throw new IllegalArgumentException("Search build cancelled."); }
    }
}
