package breadcrumbs.b6;

import breadcrumbs.dsa.trie.PrefixTrie;
import breadcrumbs.search.SearchPolicy;
import breadcrumbs.search.Tokenizer;
import java.util.Arrays;
import static breadcrumbs.b4.Checks.*;

public final class TrieTest {
    private TrieTest() { }
    public static void main(String[] args) throws Exception { run(); }
    public static void run() throws Exception {
        SearchPolicy policy = SearchPolicy.defaults();
        require(Arrays.equals(Tokenizer.tokens("METHOD, methods methodology", policy),
                new String[] {"method", "methods", "methodology"}), "tokenize normalized lexical terms");
        PrefixTrie trie = new PrefixTrie(100, 1000, 64);
        require(trie.termCount() == 0 && trie.nodeCount() == 1 && trie.validate(), "empty Trie");
        for (String term : new String[] {"methods", "methodology", "method", "method"}) { trie.insert(term); }
        require(trie.termCount() == 3 && trie.nodeCount() == 13, "duplicate and shared prefix node reuse");
        require(trie.contains("method") && !trie.contains("meth") && !trie.contains("missing"), "exact versus prefix");
        require(Arrays.equals(trie.suggest("meth", 10, 100).terms(), new String[] {"method", "methodology", "methods"}), "ordered sibling traversal");
        require(trie.suggest("meth", 1, 100).terms().length == 1, "bounded suggestions");
        require(trie.suggest("meth", 10, 2).limited(), "bounded prefix traversal");
        require(trie.suggest("xyz", 10, 100).terms().length == 0 && trie.validate(), "missing prefix");
        reject(() -> trie.suggest("", 10, 100), "empty prefix cannot enumerate dictionary");
        String[] unicode = Tokenizer.tokens("CAFÉ cafe\u0301 ＡＢＣ 𐐀𐐁", policy);
        require(Arrays.equals(unicode, new String[] {"café", "café", "abc", "𐐨𐐩"}), "NFKC ROOT case combining and supplementary Unicode");
        for (String term : unicode) { trie.insert(term); }
        require(trie.suggest("𐐨", 5, 100).terms()[0].equals("𐐨𐐩") && trie.validate(), "code point prefix");
        reject(() -> Tokenizer.query("two words", policy), "exact term rejects phrase");
        reject(() -> Tokenizer.query("a\u001b[31m", policy), "escape query");
        reject(() -> Tokenizer.tokens("\ud800", policy), "malformed Unicode");
        reject(() -> Tokenizer.tokens("a".repeat(65), policy), "large token rejection");
        reject(() -> Tokenizer.query("", policy), "empty query");
        PrefixTrie small = new PrefixTrie(1, 4, 64);
        small.insert("cat");
        reject(() -> small.insert("dog"), "term capacity");
        require(small.contains("cat") && small.nodeCount() == 4 && small.validate(), "capacity rejection is atomic");
        trie.clear(); require(trie.termCount() == 0 && trie.validate(), "clear");
        trie.insert("again"); require(trie.contains("again"), "rebuild");
        System.out.println("PASS: B6 tokenizer and custom Trie");
    }
}
