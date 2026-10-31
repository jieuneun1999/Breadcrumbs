package breadcrumbs.b6;

import breadcrumbs.b4.WorkflowTest;
import breadcrumbs.domain.*;
import breadcrumbs.search.*;
import breadcrumbs.dsa.trie.PrefixTrie;
import java.nio.file.*;
import java.lang.reflect.Field;
import static breadcrumbs.b4.Checks.*;

public final class BoundsTest {
    private BoundsTest() { }
    public static void main(String[] args) throws Exception { run(); }
    public static void run() throws Exception {
        Path root = Files.createTempDirectory(Files.createDirectories(Path.of("build/b6-fixtures")), "bounds-").toAbsolutePath();
        Files.writeString(root.resolve("notes.md"), "# Objective\nvsfwnsp hcdyrid alpha alpha beta\n# Next Steps\nalpha beta\n");
        Files.writeString(root.resolve("wide.txt"), "Objective: ＳＥＣＲＥＴ＝sensitivewideword\n");
        var state = WorkflowTest.imported(root, "W621");
        var index = new ContextSearchIndex(state); var policy = SearchPolicy.defaults().withUnreviewed(true);
        require(index.rebuild(policy, () -> false), "bounds fixture build");
        require(index.searchExact("sensitivewideword", 20).hits().length == 0, "normalization must not bypass sensitive marker policy");
        require("vsfwnsp".hashCode() == "hcdyrid".hashCode(), "deliberate normalized Hash collision fixture");
        require(index.searchExact("vsfwnsp", 20).totalMatches() == 1 && index.searchExact("hcdyrid", 20).totalMatches() == 1, "colliding term postings remain distinct");
        for (int dimension = 0; dimension < 9; dimension++) {
            final int bound = dimension;
            var limited = new SearchPolicy(bound == 0 ? 1 : 10000, bound == 1 ? 3 : 64, bound == 2 ? 4 : 1000000,
                    bound == 3 ? 1 : 2000, bound == 4 ? 1 : 50000, bound == 4 || bound == 5 ? 1 : 2000,
                    10, 5000, 20, 160, bound == 6 ? 1 : 10000, bound == 7 ? 2 : 200000, true,
                    "BOUNDS", bound == 8 ? "FUTURE-TOKENIZER" : "NFKC-ROOT-1", "CAPTURED-1");
            require(!index.rebuild(limited, () -> false), "whole candidate fails at resource/version boundary " + bound);
            require(index.searchExact("a", 20).hits().length == 0 && index.suggestTerms("a", 10).terms().length == 0, "no partial dictionary visible " + bound);
        }
        index.rebuild(policy, () -> false);
        int[] checks = {0};
        require(!index.rebuild(policy, () -> ++checks[0] == 3) && index.isCurrent(state), "mid-build cancellation preserves current candidate");
        require(!index.rebuild(policy, () -> { if (++checks[0] == 5) { state.markUnsaved(); } return false; }), "mutation during rebuild prevents publish");
        require(index.status() == SearchReport.Status.INDEX_OUTDATED, "raced candidate not current");
        index.rebuild(policy, () -> false);
        Field active = ContextSearchIndex.class.getDeclaredField("active"); active.setAccessible(true);
        Object candidate = active.get(index); Field locations = candidate.getClass().getDeclaredField("fields"); locations.setAccessible(true);
        SearchLocation[] refs = (SearchLocation[]) locations.get(candidate); SearchLocation original = refs[0];
        refs[0] = new SearchLocation("W999", original.kind(), original.recordId(), original.field(), original.observationId(), original.locator(), original.truth(), original.review(), original.sensitivity(), original.recordedAt(), false);
        require(!index.validateAgainstCanonicalState() && index.searchExact("alpha", 20).hits().length == 0, "foreign/orphan reference invalidates before hits");
        PrefixTrie trie = new PrefixTrie(10, 100, 64); trie.insert("alpha"); trie.insert("beta");
        Field trieRoot = PrefixTrie.class.getDeclaredField("root"); trieRoot.setAccessible(true); Object node = trieRoot.get(trie);
        Field first = node.getClass().getDeclaredField("firstChild"); first.setAccessible(true); Object child = first.get(node);
        Field sibling = child.getClass().getDeclaredField("nextSibling"); sibling.setAccessible(true); sibling.set(child, child);
        require(!trie.validate(), "manual sibling cycle rejected without hanging");
        require(index.coverage().contains("scan complete=true"), "coverage exposes scan completeness");
        for (String text : new String[] {"a\u0000b", "a\u202eb", "\udc00", "\ud800x"}) { reject(() -> Tokenizer.tokens(text, policy), "unsafe Unicode boundary"); }
        Files.writeString(root.resolve("unicode.txt"), "Objective: " + "a ".repeat(79) + "a𐐨 tail\n");
        var unicodeState = WorkflowTest.imported(root, "W622");
        var claim = WorkflowTest.find(unicodeState, "unicode.txt", EvidenceClaim.Field.OBJECTIVE);
        unicodeState.addReview(new ContextReview(unicodeState.currentObservationId(), claim.evidenceId(), claim.field(), claim.entityId(), claim.literalValue(), EvidenceClaim.ReviewDisposition.CONFIRMED, java.time.Instant.now()));
        var unicodeIndex = new ContextSearchIndex(unicodeState); unicodeIndex.rebuild(SearchPolicy.defaults(), () -> false);
        String snippet = unicodeIndex.viewField(unicodeIndex.searchExact("tail", 10).hits()[0].location(), true);
        Tokenizer.checkUnicode(snippet); require(snippet.length() <= 163, "safe snippet cannot split a surrogate pair");
        unicodeState.addReview(new ContextReview(unicodeState.currentObservationId(), claim.evidenceId(), claim.field(), claim.entityId(), claim.literalValue(), EvidenceClaim.ReviewDisposition.REJECTED, java.time.Instant.now()));
        require(unicodeIndex.status() == SearchReport.Status.INDEX_OUTDATED, "rejection invalidates source eligibility");
        unicodeIndex.rebuild(policy, () -> false); require(unicodeIndex.searchExact("tail", 10).totalMatches() == 0, "rejected evidence excluded even with opt-in");
        var future = new SearchPolicy(10000, 64, 1000000, 2000, 50000, 2000, 10, 5000, 20, 160, 10000, 200000, true, "SEARCH-2", "NFKC-ROOT-1", "CONTENT-2");
        require(!unicodeIndex.rebuild(future, () -> false) && unicodeIndex.status() == SearchReport.Status.INDEX_OUTDATED, "content policy version invalidates old terms");
        System.out.println("PASS: B6 collision, limits, cancellation, corruption and privacy boundaries");
    }
}
